package net.draycia.carbon.common.users.db.postgresql;

import java.util.UUID;
import net.draycia.carbon.common.users.SaveOnChange;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

@DefaultQualifier(NonNull.class)
public interface PostgreSQLSaveOnChange extends SaveOnChange {

    @SqlUpdate("UPDATE carbon_users SET displayname = :displayName WHERE id = :id")
    int saveDisplayName(final UUID id, final @Nullable Component displayName);

    @SqlUpdate("UPDATE carbon_users SET muted = :muted WHERE id = :id")
    int saveMuted(final UUID id, final boolean muted);

    @SqlUpdate("UPDATE carbon_users SET deafened = :deafened WHERE id = :id")
    int saveDeafened(final UUID id, final boolean deafened);

    @SqlUpdate("UPDATE carbon_users SET spying = :spying WHERE id = :id")
    int saveSpying(final UUID id, final boolean spying);

    @SqlUpdate("UPDATE carbon_users SET selectedchannel = :selectedChannel WHERE id = :id")
    int saveSelectedChannel(final UUID id, final @Nullable Key selectedChannel);

    @SqlUpdate("UPDATE carbon_users SET lastwhispertarget = :lastWhisperTarget WHERE id = :id")
    int saveLastWhisperTarget(final UUID id, final @Nullable UUID lastWhisperTarget);

    @SqlUpdate("UPDATE carbon_users SET whisperreplytarget = :whisperReplyTarget WHERE id = :id")
    int saveWhisperReplyTarget(final UUID id, final @Nullable UUID whisperReplyTarget);

    @SqlUpdate("INSERT INTO carbon_ignores VALUES id = :id, ignoredplayer = :ignoredPlayer")
    int addIgnore(final UUID id, final UUID ignoredPlayer);

    @SqlUpdate("DELETE FROM carbon_ignores WHERE id = :id, ignoredplayer = :ignoredPlayer")
    int removeIgnore(final UUID id, final UUID ignoredPlayer);

}
