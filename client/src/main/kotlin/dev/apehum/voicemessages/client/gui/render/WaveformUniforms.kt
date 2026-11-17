package dev.apehum.voicemessages.client.gui.render

//? if >=1.21.6 {
/*import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.buffers.Std140SizeCalculator
import com.mojang.blaze3d.systems.RenderSystem
import org.lwjgl.system.MemoryStack

const val WAVEFORM_UNIFORM = "WaveformParams"

object WaveformUniforms {
    private val size =
        Std140SizeCalculator()
            .putVec4()
            .putVec4()
            .putVec4()
            .putVec4()
            .get()

    private val buffer: GpuBuffer by lazy {
        MemoryStack.stackPush().use { stack ->
            RenderSystem.getDevice().createBuffer(
                { "voice messages waveform UBO" },
                GpuBuffer.USAGE_UNIFORM or GpuBuffer.USAGE_COPY_DST,
                stack.calloc(size),
            )
        }
    }

    fun write(
        radii: FloatArray,
        amplitude: Float,
        outerSize: Int,
        circleSize: Int,
        blobAlpha: Float,
        rect: WaveformRect,
    ) {
        MemoryStack.stackPush().use { stack ->
            val data =
                Std140Builder
                    .onStack(stack, size)
                    .putVec4(radii[0], radii[1], radii[2], radii[3])
                    .putVec4(radii[4], radii[5], radii[6], radii[7])
                    .putVec4(amplitude, outerSize.toFloat(), circleSize.toFloat(), blobAlpha)
                    .putVec4(rect.x, rect.y, rect.width, rect.height)
                    .get()

            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(buffer.slice(), data)
        }
    }

    fun bind(pass: RenderPass) {
        pass.setUniform(WAVEFORM_UNIFORM, buffer)
    }
}
*///?}
