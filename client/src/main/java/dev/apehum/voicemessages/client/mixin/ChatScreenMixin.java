package dev.apehum.voicemessages.client.mixin;

import dev.apehum.voicemessages.client.VoiceMessagesClient;
import dev.apehum.voicemessages.client.gui.chat.DraftBar;
import dev.apehum.voicemessages.client.gui.chat.RecordButton;
import dev.apehum.voicemessages.client.gui.chat.RecordingBar;
import dev.apehum.voicemessages.client.gui.menu.SenderMenu;
import dev.apehum.voicemessages.client.session.VoiceMessageSession;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if >=1.21.11 {
/*import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
*///?}

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
    @Unique
    private SenderMenu voicemessages$senderMenu;

    @Unique
    private int voicemessages$inputX;
    @Unique
    private int voicemessages$inputWidth;
    @Unique
    private int voicemessages$inputOffset;

    private ChatScreenMixin() {
        super(null);
        throw new AssertionError();
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean charTyped(CharacterEvent event) {
        if (voicemessages$isMenuOpen()) {
            return voicemessages$senderMenu.charTyped(event);
        }

        return super.charTyped(event);
    }
    *///?} else {
    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (voicemessages$isMenuOpen()) {
            return voicemessages$senderMenu.charTyped(codePoint, modifiers);
        }

        return super.charTyped(codePoint, modifiers);
    }
    //?}

    @Inject(method = "init", at = @At("TAIL"))
    private void voicemessages$addRecordWidgets(CallbackInfo ci) {
        // added unconditionally and hidden in render, because the handshake, the voice connection
        // and the mute state can all flip while the screen is open
        int barLeft = input.getX() - 2;
        int barTop = input.getY() - 2;
        int barHeight = input.getHeight();

        this.voicemessages$senderMenu = new SenderMenu();

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
                barHeight,
                voicemessages$senderMenu
        ));

        addRenderableOnly(voicemessages$senderMenu);

        this.voicemessages$inputX = input.getX();
        this.voicemessages$inputWidth = input.getWidth();
        this.voicemessages$inputOffset = barHeight + voicemessages$INPUT_GAP;
    }

    //~ if >=26.3 'render' -> 'extractRenderState'
    @Inject(method = "render", at = @At("HEAD"))
    private void voicemessages$swapInputForRecording(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (voicemessages$recordingBar == null) {
            return;
        }

        VoiceMessageSession session = VoiceMessagesClient.getSession();
        boolean recording = session != null && session.getRecorder().isRecording();
        boolean active = session != null && session.isActive();
        boolean available = active || (session != null && session.canRecord());

        voicemessages$recordButton.visible = available && !active;
        voicemessages$recordingBar.visible = recording;
        voicemessages$draftBar.visible = active && !recording;

        if (!voicemessages$draftBar.visible) {
            voicemessages$senderMenu.close();
        }

        int offset = available ? voicemessages$inputOffset : 0;
        input.setX(voicemessages$inputX + offset);
        input.setWidth(voicemessages$inputWidth - offset);
        input.setVisible(!active);
        input.setEditable(!active);
    }

    //? if >=1.21.11 {
    /*@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void voicemessages$routeMenuClick(MouseButtonEvent event, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (voicemessages$isMenuOpen()) {
            cir.setReturnValue(voicemessages$senderMenu.mouseClicked(event, doubled));
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void voicemessages$routeMenuKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (voicemessages$isMenuOpen()) {
            cir.setReturnValue(voicemessages$senderMenu.keyPressed(event));
        }
    }
    *///?} else {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void voicemessages$routeMenuClick(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (voicemessages$isMenuOpen()) {
            cir.setReturnValue(voicemessages$senderMenu.mouseClicked(mouseX, mouseY, button));
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void voicemessages$routeMenuKey(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (voicemessages$isMenuOpen()) {
            cir.setReturnValue(voicemessages$senderMenu.keyPressed(keyCode, scanCode, modifiers));
        }
    }
    //?}

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void voicemessages$routeMenuScroll(double mouseX, double mouseY, double scrollX, double scrollY, CallbackInfoReturnable<Boolean> cir) {
        if (voicemessages$isMenuOpen()) {
            cir.setReturnValue(voicemessages$senderMenu.mouseScrolled(mouseX, mouseY, scrollX, scrollY));
        }
    }

    @Inject(method = "removed", at = @At("TAIL"))
    private void voicemessages$discardRecording(CallbackInfo ci) {
        if (voicemessages$senderMenu != null) {
            voicemessages$senderMenu.close();
        }

        // todo: stop & discard prompt?
        VoiceMessageSession session = VoiceMessagesClient.getSession();
        if (session != null) {
            session.discard();
        }
    }

    @Unique
    private boolean voicemessages$isMenuOpen() {
        return voicemessages$senderMenu != null && voicemessages$senderMenu.isOpen();
    }
}
