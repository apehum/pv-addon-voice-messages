package dev.apehum.voicemessages.client.gui.render

import com.mojang.blaze3d.platform.NativeImage
import dev.apehum.voicemessages.client.modResource
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.ResourceLocation
import kotlin.math.roundToInt

private const val TEXELS = WAVEFORM_SEGMENTS + 1

private val LOCATION = modResource("waveform_parameters")

// we can't use uniforms properly in GUI context, so texture params are used instead
object WaveformParameters {
    private val texture: DynamicTexture by lazy {
        //? if >=1.21.11 {
        /*val texture = DynamicTexture("waveform parameters", TEXELS, 1, false)
        *///?} else {
        val texture = DynamicTexture(TEXELS, 1, false)
        texture.setFilter(false, false)
        //?}

        Minecraft.getInstance().textureManager.register(LOCATION, texture)

        texture
    }

    fun write(
        radii: FloatArray,
        amplitude: Float,
        outerSize: Int,
        circleSize: Int,
        blobAlpha: Float,
    ): ResourceLocation {
        val pixels = texture.pixels ?: return LOCATION

        for (segment in 0 until WAVEFORM_SEGMENTS) {
            pixels.writeTexel(segment, rgba(unorm(radii[segment]), 0, 0, 0))
        }

        pixels.writeTexel(
            WAVEFORM_SEGMENTS,
            rgba(unorm(amplitude), outerSize.coerceIn(0, 255), circleSize.coerceIn(0, 255), unorm(blobAlpha)),
        )

        texture.upload()

        return LOCATION
    }
}

private fun NativeImage.writeTexel(
    x: Int,
    abgr: Int,
) {
    //? if >=1.21.11 {
    /*setPixelABGR(x, 0, abgr)
    *///?} else {
    setPixelRGBA(x, 0, abgr)
    //?}
}

private fun rgba(
    red: Int,
    green: Int,
    blue: Int,
    alpha: Int,
): Int = (alpha shl 24) or (blue shl 16) or (green shl 8) or red

private fun unorm(value: Float): Int = (value.coerceIn(0f, 1f) * 255f).roundToInt()
