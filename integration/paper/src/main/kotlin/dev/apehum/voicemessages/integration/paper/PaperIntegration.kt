package dev.apehum.voicemessages.integration.paper

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerPriority
import dev.apehum.voicemessages.api.event.MessageSenderRegistrationEvent
import org.bukkit.Bukkit

class PaperIntegration(
    private val pluginName: String,
) {
    private val plugin = Bukkit.getPluginManager().getPlugin(pluginName)!!

    fun load() {
        if (plugin.server.pluginManager.getPlugin("packetevents") == null) return

        MessageSenderRegistrationEvent.registerListener { registry ->
            registry.register("default", PaperMessageSender(plugin))
            registry.register("direct", PaperDirectMessageSender(plugin))
        }

        PacketEvents.getAPI().eventManager.registerListener(
            PacketChatListener(),
            PacketListenerPriority.NORMAL,
        )
        plugin.slF4JLogger.info("packetevents chat integration loaded")
    }
}
