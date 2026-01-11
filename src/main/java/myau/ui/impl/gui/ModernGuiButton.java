package myau.ui.impl.gui;

import myau.util.AnimationUtil;
import myau.util.RenderUtil;
import myau.util.font.FontManager; // 导入字体管理器
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import java.awt.Color;

public class ModernGuiButton extends GuiButton {

    private static final int BG_NORMAL = new Color(0, 0, 0, 100).getRGB();
    private static final int BG_HOVER = new Color(20, 20, 20, 200).getRGB();
    private static final int OUTLINE_NORMAL = new Color(255, 255, 255, 150).getRGB();
    private static final int OUTLINE_HOVER = new Color(255, 255, 255, 200).getRGB();

    private float hoverProgress = 0.0f;

    public ModernGuiButton(int buttonId, int x, int y, int width, int height, String buttonText) {
        super(buttonId, x, y, width, height, buttonText);
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (this.visible) {
            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition
                    && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

            // 平滑动画
            hoverProgress = AnimationUtil.animate(this.hovered ? 1.0f : 0.0f, hoverProgress, 0.25f);

            // 混合颜色
            int bgNormal = new Color(20, 20, 20, 120).getRGB();
            int bgHover = new Color(40, 40, 45, 200).getRGB();
            int outlineNormal = new Color(255, 255, 255, 60).getRGB();
            int outlineHover = new Color(255, 255, 255, 180).getRGB();

            int finalBg = AnimationUtil.interpolateColor(bgNormal, bgHover, hoverProgress);
            int finalOutline = AnimationUtil.interpolateColor(outlineNormal, outlineHover, hoverProgress);
            int finalText = AnimationUtil.interpolateColor(new Color(200, 200, 200).getRGB(), -1, hoverProgress);

            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

            // 绘制
            RenderUtil.drawRoundedRect(this.xPosition, this.yPosition, this.width, this.height, 5.0f, finalBg, true, true, true, true);
            RenderUtil.drawRoundedRectOutline(this.xPosition, this.yPosition, this.width, this.height, 5.0f, 1.0f, finalOutline, true, true, true, true);

            // 字体绘制
            if (FontManager.productSans20 != null) {
                // 精确居中：y + (height / 2) - (fontHeight / 2)
                // 大部分自定义字体渲染器实际上是以顶部为基准，有些是基线，需要微调
                float fontY = (float) (this.yPosition + (this.height - FontManager.productSans20.getHeight()) / 2.0f + 1.5f);
                FontManager.productSans20.drawCenteredString(this.displayString, this.xPosition + this.width / 2.0f, fontY, finalText);
            } else {
                this.drawCenteredString(mc.fontRendererObj, this.displayString,
                        this.xPosition + this.width / 2,
                        this.yPosition + (this.height - 8) / 2,
                        finalText);
            }

            GlStateManager.color(1, 1, 1, 1); // 重置颜色防止污染后续渲染
        }
    }
}