package myau.mixin;

import myau.Myau;
import myau.management.altmanager.AltManagerGui;
import myau.ui.impl.gui.GuiBackgroundSelector;
import myau.ui.impl.gui.ModernGuiButton;
import myau.util.AnimationUtil;
import myau.ui.impl.gui.BackgroundRenderer;
import myau.util.font.FontManager;

import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.io.IOException;

@Mixin(GuiMainMenu.class)
public abstract class MixinGuiMainMenu extends GuiScreen {

    @Unique private float animProgress = 0.0f;

    @Inject(method = "initGui", at = @At("TAIL"))
    public void onInitGui(CallbackInfo ci) {
        BackgroundRenderer.init();
        this.buttonList.clear();
        this.animProgress = 0.0f;

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // --- 紧凑布局参数 ---
        int btnWidth = 160;
        int btnHeight = 22;
        int spacing = 5;

        // --- 修正后的垂直位置 ---
        // 按钮组起始位置：从屏幕中心稍微向上偏移 25px
        // 这样按钮组的主体大致在屏幕下半部分，但不会太低
        int startY = centerY - 25;

        // 1. Singleplayer
        this.buttonList.add(new ModernGuiButton(1, centerX - btnWidth / 2, startY, btnWidth, btnHeight, "Singleplayer"));

        // 2. Multiplayer
        this.buttonList.add(new ModernGuiButton(2, centerX - btnWidth / 2, startY + btnHeight + spacing, btnWidth, btnHeight, "Multiplayer"));

        // 3. Alt Manager
        this.buttonList.add(new ModernGuiButton(11, centerX - btnWidth / 2, startY + (btnHeight + spacing) * 2, btnWidth, btnHeight, "Alt Manager"));

        // --- 底部小按钮 ---
        int bottomY = startY + (btnHeight + spacing) * 3;
        int smallW = (btnWidth - (spacing * 2)) / 3;
        int startSmallX = centerX - btnWidth / 2;

        this.buttonList.add(new ModernGuiButton(0, startSmallX, bottomY, smallW, btnHeight, "Settings"));
        this.buttonList.add(new ModernGuiButton(10, startSmallX + smallW + spacing, bottomY, smallW, btnHeight, "Theme"));
        this.buttonList.add(new ModernGuiButton(4, startSmallX + (smallW + spacing) * 2, bottomY, smallW, btnHeight, "Exit"));
    }

    @Inject(method = "drawScreen", at = @At("HEAD"), cancellable = true)
    public void onDrawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        BackgroundRenderer.draw(this.width, this.height, mouseX, mouseY);
        animProgress = AnimationUtil.animate(1.0f, animProgress, 0.14f);

        // --- 修正后的标题位置 ---
        // centerY - 95
        // 副标题通常在标题下方 +40px 处，即 -55
        // 按钮开始于 -25
        // 留白 = (-25) - (-55) = 30px
        // 这是一个非常标准的 UI 间距，既不拥挤也不空旷
        float titleY = (this.height / 2.0f) - 95;

        drawTitle(this.width / 2.0f, titleY);

        int count = 0;
        for (GuiButton button : this.buttonList) {
            GlStateManager.pushMatrix();
            float btnAnim = Math.max(0, Math.min(1, (animProgress - (count * 0.03f)) * 2.5f));

            float cx = button.xPosition + button.width / 2.0f;
            float cy = button.yPosition + button.height / 2.0f;

            GlStateManager.translate(cx, cy, 0);
            GlStateManager.scale(btnAnim, btnAnim, 1f);
            GlStateManager.translate(-cx, -cy, 0);
            GlStateManager.translate(0, (1.0f - btnAnim) * 5, 0);

            if (btnAnim > 0.01f) {
                button.drawButton(this.mc, mouseX, mouseY);
            }

            GlStateManager.popMatrix();
            count++;
        }

        drawFooter();
        ci.cancel();
    }

    @Unique
    private void drawTitle(float x, float y) {
        GlStateManager.pushMatrix();

        float s = 1.0f + (1.0f - animProgress) * 0.2f;
        int alpha = (int)(255 * animProgress);
        int color = new Color(255, 255, 255, alpha).getRGB();
        int subColor = new Color(180, 180, 180, Math.max(0, alpha - 80)).getRGB();

        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(s, s, 1f);
        GlStateManager.translate(-x, -y, 0);

        if (FontManager.tenacity80 != null) {
            FontManager.tenacity80.drawCenteredString("Myau+", x, y, color);

            // 副标题位置保持不变，确保与按钮的间距固定
            if (FontManager.productSans16 != null) {
                FontManager.productSans16.drawCenteredString(Myau.clientVersion, x, y + 40, subColor);
            } else {
                FontManager.productSans20.drawCenteredString(Myau.clientVersion, x, y + 40, subColor);
            }
        } else {
            GlStateManager.scale(3.5, 3.5, 1);
            this.drawCenteredString(this.fontRendererObj, "Myau+", (int)(x/3.5), (int)(y/3.5), color);
        }
        GlStateManager.popMatrix();
    }

    @Unique
    private void drawFooter() {
        String text = "Myau+ 1.8.9";
        int color = new Color(255, 255, 255, 40).getRGB();
        if (FontManager.productSans16 != null) {
            FontManager.productSans16.drawString(text, this.width - FontManager.productSans16.getStringWidth(text) - 4, this.height - 10, color);
        } else {
            this.drawString(this.fontRendererObj, text, this.width - this.fontRendererObj.getStringWidth(text) - 4, this.height - 10, color);
        }
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"), cancellable = true)
    protected void onActionPerformed(GuiButton button, CallbackInfo ci) throws IOException {
        GuiScreen parent = (GuiScreen)(Object)this;
        switch (button.id) {
            case 0: this.mc.displayGuiScreen(new GuiOptions(parent, this.mc.gameSettings)); break;
            case 1: this.mc.displayGuiScreen(new GuiSelectWorld(parent)); break;
            case 2: this.mc.displayGuiScreen(new GuiMultiplayer(parent)); break;
            case 4: this.mc.shutdown(); break;
            case 10: this.mc.displayGuiScreen(new GuiBackgroundSelector(parent)); break;
            case 11: this.mc.displayGuiScreen(new AltManagerGui((GuiMainMenu)(Object)this)); break;
        }
        if (button.id >= 0 && button.id <= 11 && button.id != 3 && button.id != 5) ci.cancel();
    }
}