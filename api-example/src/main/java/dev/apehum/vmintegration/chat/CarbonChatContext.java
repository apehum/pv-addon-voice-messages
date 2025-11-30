package dev.apehum.vmintegration.chat;

import dev.apehum.voicemessages.api.chat.ChatContext;
import net.draycia.carbon.api.users.CarbonPlayer;
import net.draycia.carbon.api.users.Party;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record CarbonChatContext(@NotNull CarbonPlayer sender, @Nullable Party party) implements ChatContext {
}
