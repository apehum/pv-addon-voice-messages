package dev.apehum.vmintegration;

import dev.apehum.vmintegration.chat.CarbonPartyMessageSender;
import dev.apehum.voicemessages.api.event.MessageSenderRegistrationEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class VoiceMessagesIntegration extends JavaPlugin {

    @Override
    public void onLoad() {
        MessageSenderRegistrationEvent.INSTANCE.registerListener(registry -> {
            registry.register("party", new CarbonPartyMessageSender());
            getSLF4JLogger().info("Registered CarbonPartyMessageSender");
        });
    }
}
