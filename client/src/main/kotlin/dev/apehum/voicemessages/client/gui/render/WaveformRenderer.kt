package dev.apehum.voicemessages.client.gui.render

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import dev.apehum.voicemessages.client.modResource
import su.plo.lib.mod.client.compat.vulkan.VulkanCompat
import su.plo.lib.mod.client.render.DestFactor
import su.plo.lib.mod.client.render.SourceFactor
import su.plo.lib.mod.client.render.VertexFormatMode
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import su.plo.lib.mod.client.render.pipeline.BlendFunc
import su.plo.lib.mod.client.render.pipeline.RenderPipeline
import su.plo.lib.mod.client.render.pipeline.RenderPipelineKt

//? if <1.21.5
import su.plo.lib.mod.client.render.shader.ShadersCache
import java.awt.Color
import su.plo.voice.libs.kotlin.Unit as PvUnit

//? if >=1.21.6 {
/*private val WAVEFORM_SHADER = modResource("waveform_1_21_6")
*///?} else {
private val WAVEFORM_SHADER = modResource("waveform")
//?}

private val WAVEFORM_VERTEX_FORMAT = DefaultVertexFormat.POSITION_TEX_COLOR

fun GuiRenderContext.renderCircleWaveform(
    centerX: Int,
    centerY: Int,
    circleSize: Int,
    outerSize: Int,
    amplitude: Float,
    shape: WaveformShape,
    color: Color,
    blobAlpha: Float,
) {
    if (!isWaveformAvailable) return

    val parameters =
        WaveformParameters.write(
            shape.radii,
            amplitude.coerceIn(0f, 1f),
            outerSize,
            circleSize,
            blobAlpha.coerceIn(0f, 1f),
        )

    val left = centerX - outerSize / 2
    val top = centerY - outerSize / 2

    blitColor(
        parameters,
        left,
        left + outerSize,
        top,
        top + outerSize,
        0f,
        1f,
        0f,
        1f,
        color,
        WAVEFORM_PIPELINE,
    )
}

private val WAVEFORM_PIPELINE: RenderPipeline by lazy {
    RenderPipelineKt.renderPipeline(
        modResource("pipeline/waveform"),
        WAVEFORM_SHADER,
        WAVEFORM_VERTEX_FORMAT,
        VertexFormatMode.QUADS,
    ) {
        it.samplers += "Sampler0"
        it.blendFunc =
            BlendFunc(
                SourceFactor.SRC_ALPHA,
                DestFactor.ONE_MINUS_SRC_ALPHA,
                SourceFactor.ONE,
                DestFactor.ONE_MINUS_SRC_ALPHA,
            )

        PvUnit.INSTANCE
    }
}

private val isWaveformAvailable: Boolean by lazy {
    //? if >=1.21.5 {
    /*!VulkanCompat.hasVulkan()
    *///?} else {
    !VulkanCompat.hasVulkan() && ShadersCache.isUsable(WAVEFORM_SHADER, WAVEFORM_VERTEX_FORMAT)
    //?}
}
