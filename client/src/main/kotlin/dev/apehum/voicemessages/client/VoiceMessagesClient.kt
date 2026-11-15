package dev.apehum.voicemessages.client

import dev.apehum.voicemessages.client.channel.VoiceMessagesChannelClient
import dev.apehum.voicemessages.client.channel.createChannelClient
import dev.apehum.voicemessages.client.session.VoiceMessageSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import su.plo.slib.api.logging.McLoggerFactory
import su.plo.voice.api.client.PlasmoVoiceClient

object VoiceMessagesClient {
    private val logger = McLoggerFactory.createLogger(BuildConfig.ADDON_ID)

    private val scope = CoroutineScope(Dispatchers.Default)

    private lateinit var addon: ClientVoiceMessagesAddon
    private lateinit var voiceClient: PlasmoVoiceClient
    private lateinit var channel: VoiceMessagesChannelClient

    private val sessionLock = Any()

    @JvmStatic
    @Volatile
    var session: VoiceMessageSession? = null
        private set

    fun initialize(
        addon: ClientVoiceMessagesAddon,
        voiceClient: PlasmoVoiceClient,
    ) {
        this.addon = addon
        this.voiceClient = voiceClient
        channel = VoiceMessagesChannelClient(createChannelClient(modResource("channel")))
    }

    fun handshake() {
        scope.launch {
            val response =
                try {
                    channel.handshake()
                } catch (e: Throwable) {
                    logger.debug("Voice messages handshake failed", e)
                    null
                } ?: return@launch

            synchronized(sessionLock) {
                if (channel.session !== response) return@launch

                session?.close()
                session =
                    VoiceMessageSession(
                        addon,
                        voiceClient,
                        channel,
                        scope,
                        response,
                    )
            }
        }
    }

    fun disconnect() {
        synchronized(sessionLock) {
            session?.close()
            session = null
            channel.reset()
        }
    }
}
