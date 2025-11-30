package dev.apehum.vmintegration.chat;

import dev.apehum.voicemessages.api.VoiceMessage;
import dev.apehum.voicemessages.api.VoiceMessagesAPI;
import dev.apehum.voicemessages.api.VoiceMessagesAPIProvider;
import dev.apehum.voicemessages.api.chat.ChatMessageSender;
import dev.apehum.voicemessages.api.command.dsl.CommandContext;
import net.draycia.carbon.api.CarbonChat;
import net.draycia.carbon.api.CarbonChatProvider;
import net.draycia.carbon.api.channels.ChatChannel;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.jetbrains.annotations.NotNull;
import su.plo.slib.api.server.entity.player.McServerPlayer;

import java.util.concurrent.CompletableFuture;

public final class CarbonPartyMessageSender implements ChatMessageSender<CarbonChatContext> {

    private final CarbonChat carbonChat = CarbonChatProvider.carbonChat();
    private final VoiceMessagesAPI voiceMessages = VoiceMessagesAPIProvider.getInstance();

    private static final String FORMAT = "(party: <party_name>) <sender>: <message>";

    @Override
    public void sendVoiceMessage(@NotNull CarbonChatContext context, @NotNull VoiceMessage message) {
        ChatChannel partyChannel = carbonChat.channelRegistry().channel(Key.key("carbon", "partychat"));
        if (partyChannel == null) return;

        partyChannel.recipients(context.sender())
                .forEach(audience -> {
                    String json = voiceMessages.formatVoiceMessageToJson(message);
                    Component voiceMessageComponent = GsonComponentSerializer.gson().deserialize(json);

                    Component finalMessage = MiniMessage.miniMessage().deserialize(
                            FORMAT,
                            Placeholder.component("party_name", context.party().name()),
                            Placeholder.component("sender", context.sender().displayName()),
                            Placeholder.component("message", voiceMessageComponent)
                    );

                    audience.sendMessage(finalMessage);
                });
    }

    @Override
    public boolean canSendMessage(@NotNull CarbonChatContext context) {
        return context.party() != null;
    }

    @Override
    public @NotNull CompletableFuture<@NotNull CarbonChatContext> createContext(@NotNull CommandContext context) {
        CarbonChat carbonChat = CarbonChatProvider.carbonChat();
        McServerPlayer serverPlayer = (McServerPlayer) context.getSource();

        return carbonChat.userManager().user(serverPlayer.getUuid())
                .thenCompose(carbonPlayer ->
                    carbonPlayer.party()
                            .thenApply(party -> new CarbonChatContext(carbonPlayer, party))
                );
    }
}
