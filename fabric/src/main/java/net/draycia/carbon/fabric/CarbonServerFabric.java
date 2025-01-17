/*
 * CarbonChat
 *
 * Copyright (c) 2021 Josua Parks (Vicarious)
 *                    Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.draycia.carbon.fabric;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.draycia.carbon.api.CarbonServer;
import net.draycia.carbon.api.users.CarbonPlayer;
import net.draycia.carbon.api.users.ComponentPlayerResult;
import net.draycia.carbon.api.users.UserManager;
import net.draycia.carbon.common.users.CarbonPlayerCommon;
import net.draycia.carbon.fabric.users.CarbonPlayerFabric;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.platform.fabric.FabricServerAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.NotNull;

import static net.kyori.adventure.text.Component.text;

@Singleton
@DefaultQualifier(NonNull.class)
public final class CarbonServerFabric implements CarbonServer, ForwardingAudience.Single {

    private final CarbonChatFabric carbonChatFabric;
    private final UserManager<CarbonPlayerCommon> userManager;

    private final Map<UUID, CarbonPlayerFabric> userCache = new ConcurrentHashMap<>();

    private final HttpClient client = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @Inject
    private CarbonServerFabric(final CarbonChatFabric carbonChatFabric, final UserManager<CarbonPlayerCommon> userManager) {
        this.carbonChatFabric = carbonChatFabric;
        this.userManager = userManager;
    }

    @Override
    public @NotNull Audience audience() {
        return FabricServerAudiences.of(this.carbonChatFabric.minecraftServer()).all();
    }

    @Override
    public Audience console() {
        return FabricServerAudiences.of(this.carbonChatFabric.minecraftServer()).console();
    }

    @Override
    public List<? extends CarbonPlayer> players() {
        final var players = new ArrayList<CarbonPlayer>();

        for (final var player : this.carbonChatFabric.minecraftServer().getPlayerList().getPlayers()) {
            final @Nullable ComponentPlayerResult<CarbonPlayer> result = this.player(player).join();

            if (result.player() != null) {
                players.add(result.player());
            }
        }

        return players;
    }

    private CompletableFuture<ComponentPlayerResult<CarbonPlayer>> wrapPlayer(final UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            final @Nullable CarbonPlayerFabric cachedPlayer = this.userCache.get(uuid);

            if (cachedPlayer != null) {
                return new ComponentPlayerResult<>(cachedPlayer, Component.empty());
            }

            final ComponentPlayerResult<CarbonPlayerCommon> result = this.userManager.carbonPlayer(uuid).join();

            if (result.player() != null) {
                final CarbonPlayerFabric carbonPlayerFabric = new CarbonPlayerFabric(result.player(), this.carbonChatFabric);

                this.userCache.put(uuid, carbonPlayerFabric);

                return new ComponentPlayerResult<>(carbonPlayerFabric, Component.empty());
            }

            final @Nullable String name = this.resolveName(uuid).join();

            if (name != null) {
                final CarbonPlayerCommon player = new CarbonPlayerCommon(name, uuid);
                final CarbonPlayerFabric carbonPlayerFabric = new CarbonPlayerFabric(player, this.carbonChatFabric);

                this.userCache.put(uuid, carbonPlayerFabric);

                return new ComponentPlayerResult<>(carbonPlayerFabric, Component.empty());
            }

            return new ComponentPlayerResult<>(null, text("Name not found for uuid!"));
        });
    }

    @Override
    public CompletableFuture<ComponentPlayerResult<CarbonPlayer>> player(final UUID uuid) {
        return this.wrapPlayer(uuid);
    }

    @Override
    public CompletableFuture<ComponentPlayerResult<CarbonPlayer>> player(final String username) {
        return CompletableFuture.supplyAsync(() -> {
            final @Nullable UUID uuid = this.resolveUUID(username).join();

            if (uuid != null) {
                return this.player(uuid).join();
            }

            return new ComponentPlayerResult<>(null, text("No UUID found for name."));
        });
    }

    public CompletableFuture<ComponentPlayerResult<CarbonPlayer>> player(final Player player) {
        return this.player(player.getUUID());
    }

    @Override
    public CompletableFuture<@Nullable UUID> resolveUUID(final String username) {
        final @Nullable ServerPlayer serverPlayer = this.carbonChatFabric.minecraftServer().getPlayerList().getPlayerByName(username);
        if (serverPlayer == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.completedFuture(serverPlayer.getUUID());
    }

    @Override
    public CompletableFuture<@Nullable String> resolveName(final UUID uuid) {
        final @Nullable ServerPlayer serverPlayer = this.carbonChatFabric.minecraftServer().getPlayerList().getPlayer(uuid);
        if (serverPlayer == null) {
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.completedFuture(serverPlayer.getGameProfile().getName());
    }

    private @Nullable JsonObject queryMojang(final URI uri) {
        final HttpRequest request = HttpRequest
            .newBuilder(uri)
            .GET()
            .build();

        try {
            final HttpResponse<String> response =
                this.client.send(request, HttpResponse.BodyHandlers.ofString());
            final String mojangResponse = response.body();

            final JsonArray jsonArray = this.gson.fromJson(mojangResponse, JsonObject.class).getAsJsonArray();
            return (JsonObject) jsonArray.get(1);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

}
