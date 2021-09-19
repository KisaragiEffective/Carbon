package net.draycia.carbon.common.messages;

import java.util.UUID;
import net.draycia.carbon.api.util.SourcedAudience;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.moonshine.annotation.Message;
import net.kyori.moonshine.annotation.Placeholder;

public interface CarbonMessageService {

    @Message("example.command.hello")
    void exampleCommandFeedback(
        final Audience audience,
        @Placeholder final Component plugin
    );

    @Message("unsupported.legacy")
    void unsupportedLegacyChar(
        final Audience audience,
        @Placeholder final String message
    );

    @Message("test.phrase")
    void localeTestMessage(final Audience audience);

    @Message("channel.format.basic")
    Component basicChatFormat(
        final Audience audience,
        @Placeholder UUID uuid,
        @Placeholder Component displayname,
        @Placeholder String username,
        @Placeholder Component message
    );

    @Message("mute.spy.prefix")
    Component muteSpyPrefix(final Audience audience);

    @Message("channel.change")
    void changedChannels(
        final Audience audience,
        @Placeholder final String channel // TODO: allow MiniMessage based channel "names"
    );

    @Message("whisper.to")
    void whisperSender(
        final SourcedAudience audience,
        @Placeholder Component senderDisplayName,
        @Placeholder Component recipientDisplayName,
        @Placeholder String message
    );

    @Message("whisper.from")
    void whisperRecipient(
        final SourcedAudience audience,
        @Placeholder Component senderDisplayName,
        @Placeholder Component recipientDisplayName,
        @Placeholder String message
    );

}