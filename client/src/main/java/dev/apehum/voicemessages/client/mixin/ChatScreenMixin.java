package dev.apehum.voicemessages.client.mixin;

import dev.apehum.voicemessages.client.VoiceMessagesClient;
import dev.apehum.voicemessages.client.gui.chat.DraftBar;
import dev.apehum.voicemessages.client.gui.chat.RecordButton;
import dev.apehum.voicemessages.client.gui.chat.RecordingBar;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
abstract class ChatScreenMixin extends Screen {
    @Unique
    private static final int voicemessages$INPUT_GAP = 2;

    @Shadow
    protected EditBox input;

    @Unique
    private RecordButton voicemessages$recordButton;
    @Unique
    private RecordingBar voicemessages$recordingBar;
    @Unique
    private DraftBar voicemessages$draftBar;

    private ChatScreenMixin() {
        super(null);
        throw new AssertionError();
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void voicemessages$addRecordWidgets(CallbackInfo ci) {
        // todo: this hides an icon when you're muted
        //  this also doesn't update when vc is connected or player is unmuted
        if (!VoiceMessagesClient.canRecord()) {
            return;
        }

        int barLeft = input.getX() - 2;
        int barTop = input.getY() - 2;
        int barHeight = input.getHeight();

        this.voicemessages$recordButton = addRenderableWidget(new RecordButton(
                barLeft,
                barTop,
                barHeight
        ));

        this.voicemessages$recordingBar = addRenderableWidget(new RecordingBar(
                barLeft,
                barTop,
                barHeight
        ));

        this.voicemessages$draftBar = addRenderableWidget(new DraftBar(
                barLeft,
                barTop,
                this.width - barLeft * 2,
                barHeight
        ));

        input.setX(input.getX() + barHeight + voicemessages$INPUT_GAP);
        input.setWidth(input.getWidth() - barHeight - voicemessages$INPUT_GAP);
    }

    //~ if >=26.3 'render' -> 'extractRenderState'
    @Inject(method = "render", at = @At("HEAD"))
    private void voicemessages$swapInputForRecording(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (voicemessages$recordingBar == null) {
            return;
        }

        boolean recording = VoiceMessagesClient.isRecording();
        boolean active = VoiceMessagesClient.isActive();

        voicemessages$recordButton.visible = !active;
        voicemessages$recordingBar.visible = recording;
        voicemessages$draftBar.visible = active && !recording;

        input.setVisible(!active);
        input.setEditable(!active);
    }

    @Inject(method = "removed", at = @At("TAIL"))
    private void voicemessages$discardRecording(CallbackInfo ci) {
        // todo: stop & discard prompt?
        VoiceMessagesClient.discard();
    }
}
