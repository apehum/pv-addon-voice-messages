package dev.apehum.voicemessages.client.mixin;

//? if >=26.3 {
/*import com.mojang.renderpearl.api.commands.RenderPass;
*///?} else if >=1.21.6 {
/*import com.mojang.blaze3d.systems.RenderPass;
*///?}

//? if >=1.21.6 {
/*import com.mojang.blaze3d.systems.RenderSystem;
import dev.apehum.voicemessages.client.gui.render.WaveformUniforms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderSystem.class)
abstract class RenderSystemMixin {
    @Inject(method = "bindDefaultUniforms", at = @At("TAIL"))
    private static void voicemessages$bindWaveformUniforms(RenderPass pass, CallbackInfo ci) {
        WaveformUniforms.INSTANCE.bind(pass);
    }
}
*///?}
