package dev.apehum.voicemessages.client.mixin;

//? if >=26.3 {
/*import com.mojang.renderpearl.api.pipeline.RenderPipeline;
*///?} else if >=1.21.6 {
/*import com.mojang.blaze3d.pipeline.RenderPipeline;
*///?}

//? if >=1.21.6 {
/*import net.minecraft.client.renderer.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderPipelines.class)
public interface RenderPipelinesAccessor {
    @Accessor("GUI_SNIPPET")
    static RenderPipeline.Snippet voicemessages$guiSnippet() {
        throw new AssertionError();
    }
}
*///?}
