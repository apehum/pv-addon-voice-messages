package dev.apehum.voicemessages.client.gui.render

import su.plo.lib.mod.client.compat.vulkan.VulkanCompat
import su.plo.lib.mod.client.render.gui.GuiColorPipeline
import su.plo.lib.mod.client.render.gui.GuiRenderContext
import java.awt.Color

//? if >=26.2
/*import com.mojang.blaze3d.pipeline.BindGroupLayout*/
//? if >=1.21.6 {
/*import dev.apehum.voicemessages.client.modResource
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.shaders.UniformType
import dev.apehum.voicemessages.client.mixin.RenderPipelinesAccessor
import org.joml.Vector2f
*///?} else {
import org.joml.Vector3f
//?}

class WaveformRect(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
)

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
    if (!isWaveformAvailable()) return

    val left = centerX - outerSize / 2
    val top = centerY - outerSize / 2
    val right = left + outerSize
    val bottom = top + outerSize

    writeParameters(
        shape.radii,
        amplitude.coerceIn(0f, 1f),
        outerSize,
        circleSize,
        blobAlpha.coerceIn(0f, 1f),
        transformedRect(left, top, right, bottom),
    )

    fill(left, top, right, bottom, color, WAVEFORM_PIPELINE)
}

private fun GuiRenderContext.transformedRect(
    left: Int,
    top: Int,
    right: Int,
    bottom: Int,
): WaveformRect {
    //? if >=1.21.6 {
    /*val start = stack.transformPosition(left.toFloat(), top.toFloat(), Vector2f())
    val end = stack.transformPosition(right.toFloat(), bottom.toFloat(), Vector2f())
    *///?} else {
    val pose = stack.last().pose()
    val start = pose.transformPosition(left.toFloat(), top.toFloat(), 0f, Vector3f())
    val end = pose.transformPosition(right.toFloat(), bottom.toFloat(), 0f, Vector3f())
    //?}

    return WaveformRect(start.x, start.y, end.x - start.x, end.y - start.y)
}

//? if >=1.21.6 {
/*private fun isWaveformAvailable(): Boolean = !VulkanCompat.hasVulkan()

private fun writeParameters(
    radii: FloatArray,
    amplitude: Float,
    outerSize: Int,
    circleSize: Int,
    blobAlpha: Float,
    rect: WaveformRect,
) = WaveformUniforms.write(radii, amplitude, outerSize, circleSize, blobAlpha, rect)
*///?} else {
private fun isWaveformAvailable(): Boolean = !VulkanCompat.hasVulkan() && WaveformShader.instance != null

private fun writeParameters(
    radii: FloatArray,
    amplitude: Float,
    outerSize: Int,
    circleSize: Int,
    blobAlpha: Float,
    rect: WaveformRect,
) {
    val shader = WaveformShader.instance ?: return

    shader.safeGetUniform("RadiiA").set(radii[0], radii[1], radii[2], radii[3])
    shader.safeGetUniform("RadiiB").set(radii[4], radii[5], radii[6], radii[7])
    shader.safeGetUniform("Scalars").set(amplitude, outerSize.toFloat(), circleSize.toFloat(), blobAlpha)
    shader.safeGetUniform("Rect").set(rect.x, rect.y, rect.width, rect.height)
}
//?}

//? if >=26.2 {
/*private val WAVEFORM_SHADER = modResource("core/waveform")

private val WAVEFORM_PIPELINE =
    GuiColorPipeline(
        RenderPipeline
            .builder(RenderPipelinesAccessor.`voicemessages$guiSnippet`())
            .withLocation(modResource("pipeline/waveform"))
            .withVertexShader(WAVEFORM_SHADER)
            .withFragmentShader(WAVEFORM_SHADER)
            .withBindGroupLayout(
                BindGroupLayout.builder().withUniform(WAVEFORM_UNIFORM, UniformType.UNIFORM_BUFFER).build(),
            ).build(),
    )
*///?} else if >=1.21.6 {
/*private val WAVEFORM_SHADER = modResource("core/waveform")

private val WAVEFORM_PIPELINE =
    GuiColorPipeline(
        RenderPipeline
            .builder(RenderPipelinesAccessor.`voicemessages$guiSnippet`())
            .withLocation(modResource("pipeline/waveform"))
            .withVertexShader(WAVEFORM_SHADER)
            .withFragmentShader(WAVEFORM_SHADER)
            .withUniform(WAVEFORM_UNIFORM, UniformType.UNIFORM_BUFFER)
            .build(),
    )
*///?} else {
private val WAVEFORM_PIPELINE = GuiColorPipeline(WaveformRenderType())
//?}
