package dev.apehum.voicemessages.client.gui.render

//? if <1.21.6 {
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.logging.LogUtils
import dev.apehum.voicemessages.client.NAMESPACE
import dev.apehum.voicemessages.client.modResource
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.resources.ResourceLocation

object WaveformShader {
    val LOCATION: ResourceLocation = modResource("waveform")
    val VERTEX_FORMAT: VertexFormat = DefaultVertexFormat.POSITION_COLOR

    private val logger = LogUtils.getLogger()

    var instance: ShaderInstance? = null

    fun register(registration: () -> Unit) {
        try {
            registration()
        } catch (e: Exception) {
            instance = null
            logger.error("Failed to load waveform shader", e)
        }
    }
}

class WaveformRenderType private constructor(
    shards: List<RenderStateShard>,
) : RenderType(
        "$NAMESPACE:waveform",
        WaveformShader.VERTEX_FORMAT,
        VertexFormat.Mode.QUADS,
        1536,
        false,
        false,
        { shards.forEach { it.setupRenderState() } },
        { shards.forEach { it.clearRenderState() } },
    ) {
    constructor() : this(
        listOf(
            ShaderStateShard { WaveformShader.instance },
            TRANSLUCENT_TRANSPARENCY,
            LEQUAL_DEPTH_TEST,
        ),
    )
}
//?}
