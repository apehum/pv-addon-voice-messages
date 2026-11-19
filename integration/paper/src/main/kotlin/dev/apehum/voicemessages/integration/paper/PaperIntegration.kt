package dev.apehum.voicemessages.integration.paper

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerPriority
import dev.apehum.voicemessages.api.chat.ChatMessageSenderRegistry
import org.bukkit.Bukkit

class PaperIntegration(
    pluginName: String,
) {
    private val plugin = Bukkit.getPluginManager().getPlugin(pluginName)!!
    private val isPacketEventsInstalled = plugin.server.pluginManager.getPlugin("packetevents") != null

    fun load(): Boolean {
        if (!isPacketEventsInstalled) return false

        PacketEvents.getAPI().eventManager.registerListener(
            PacketChatListener(),
            PacketListenerPriority.NORMAL,
        )
        plugin.slF4JLogger.info("packetevents chat integration loaded")
        return true
    }

    fun registerSenders(registry: ChatMessageSenderRegistry) {
        if (!isPacketEventsInstalled) return

        registry.register("default", PaperMessageSender(plugin))
        registry.register("direct", PaperDirectMessageSender(plugin))
    }
}
