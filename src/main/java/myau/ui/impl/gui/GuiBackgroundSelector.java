package myau.ui.impl.gui;

import myau.Myau;
import myau.util.AnimationUtil;
import myau.util.RenderUtil;
import myau.util.font.FontManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.io.IOException;

public class GuiBackgroundSelector extends GuiScreen {
    private final GuiScreen parent;

    // 常量定义
    private final int PANEL_WIDTH = 320;
    private final int PANEL_HEIGHT = 220;

    // 动画状态
    private float openAnim = 0.0f;
    private boolean closing = false; //用于处理关闭动画

    public GuiBackgroundSelector(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        // 确保 Shader 初始化
        BackgroundRenderer.init();
        this.buttonList.clear();
        this.closing = false;
        this.openAnim = 0.0f;

        int centerX = this.width / 2;
        int centerY = this.height / 2;

        // 布局计算
        int btnWidth = 80;
        int btnHeight = 60;
        int pad = 12; // 稍微增加间距

        // 计算 3列 Grid 的起始位置
        int gridWidth = (btnWidth * 3) + (pad * 2);
        int startX = centerX - (gridWidth / 2);
        int startY = centerY - 70;

        for (int i = 1; i <= 5; i++) {
            int row = (i - 1) / 3;
            int col = (i - 1) % 3;

            int x = startX + col * (btnWidth + pad);
            int y = startY + row * (btnHeight + pad);

            this.buttonList.add(new ModernGuiButton(i, x, y, btnWidth, btnHeight, getShaderName(i)));
        }

        // Done 按钮
        this.buttonList.add(new ModernGuiButton(0, centerX - 50, centerY + 80, 100, 20, "Done"));
    }

    private String getShaderName(int id) {
        switch (id) {
            case 1: return "MyauPlus";
            case 2: return "Rise";
            case 3: return "Cosmos";
            case 4: return "Minecraft"; // 原版风格
            case 5: return "Circle";
            default: return "Unknown";
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 1. 绘制 Shader 背景
        BackgroundRenderer.draw(this.width, this.height, mouseX, mouseY);

        // 2. 绘制全屏黑色遮罩 (Fade in)
        // 随着动画进度加深背景，聚焦中间面板
        drawRect(0, 0, this.width, this.height, new Color(0, 0, 0, (int)(100 * openAnim)).getRGB());

        // 3. 动画逻辑
        if (closing) {
            openAnim = AnimationUtil.animate(0.0f, openAnim, 0.2f, 1.0f);
            if (openAnim < 0.05f) {
                this.mc.displayGuiScreen(parent);
                return;
            }
        } else {
            openAnim = AnimationUtil.animate(1.0f, openAnim, 0.15f, 1.0f);
        }

        // 4. 绘制面板
        GlStateManager.pushMatrix();

        // 从中心缩放
        GlStateManager.translate(this.width / 2.0f, this.height / 2.0f, 0);
        GlStateManager.scale(openAnim, openAnim, 1f);
        GlStateManager.translate(-this.width / 2.0f, -this.height / 2.0f, 0);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        // 背景
        RenderUtil.drawRoundedRect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 12.0f, new Color(18, 18, 22, 220).getRGB(), true, true, true, true);
        // 边框
        RenderUtil.drawRoundedRectOutline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 12.0f, 1.0f, new Color(255, 255, 255, 30).getRGB(), true, true, true, true);

        // 标题
        if (FontManager.productSans20 != null) {
            FontManager.productSans20.drawCenteredString("Select Atmosphere", this.width / 2.0f, panelY + 15, -1);
        } else {
            this.drawCenteredString(fontRendererObj, "Select Atmosphere", this.width / 2, panelY + 15, -1);
        }

        // 简单的分割线
        RenderUtil.drawRect(panelX + 30, panelY + 32, panelX + PANEL_WIDTH - 30, panelY + 33, new Color(255, 255, 255, 15).getRGB());

        // 绘制高亮光晕 (当前选中的背景)
        for (GuiButton btn : this.buttonList) {
            if (btn.id == BackgroundRenderer.currentBackgroundIndex && btn.id != 0) {
                RenderUtil.drawRoundedRectOutline(
                        btn.xPosition - 1, btn.yPosition - 1,
                        btn.width + 2, btn.height + 2,
                        5.0f, 1.5f,
                        new Color(80, 160, 255, 180).getRGB(), // 蓝色高亮边框
                        true, true, true, true
                );
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
        GlStateManager.popMatrix();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            this.closing = true; // 触发关闭动画
        } else if (button.id >= 1 && button.id <= 5) {
            BackgroundRenderer.reloadShader(button.id);
            if (Myau.globalConfig != null) Myau.globalConfig.save();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.closing = true; // ESC 触发关闭动画
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }
}