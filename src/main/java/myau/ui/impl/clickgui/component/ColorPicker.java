package myau.ui.impl.clickgui.component;

import myau.property.properties.ColorProperty;
import myau.ui.impl.clickgui.MaterialTheme;
import myau.util.RenderUtil;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.input.Mouse;

import java.awt.Color;

public class ColorPicker extends Component {
    private final ColorProperty colorProperty;
    private boolean draggingHue;
    private boolean draggingSV;
    private float hue;
    private float saturation;
    private float brightness;

    public ColorPicker(ColorProperty colorProperty, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.colorProperty = colorProperty;
        updateHSB();
    }
    
    // 添加获取属性的方法
    public ColorProperty getProperty() {
        return this.colorProperty;
    }

    private void updateHSB() {
        int color = colorProperty.getValue();
        float[] hsb = Color.RGBtoHSB((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        // 检查属性是否可见
        if (!colorProperty.isVisible()) {
            return; // 如果不可见，直接返回，不渲染
        }
        
        int scrolledY = y - scrollOffset;
        float easedProgress = 1.0f - (float) Math.pow(1.0f - animationProgress, 4);
        if (easedProgress <= 0) return;

        int alpha = (int)(255 * easedProgress);

        // 布局计算
        float padding = 4;
        float pickerX = x + padding;
        float pickerY = scrolledY + padding;
        float pickerW = width - (padding * 2);
        float pickerH = height - (padding * 2);

        float hueHeight = 6;
        float svHeight = pickerH - hueHeight - 4; // 留出 4px 间距

        // 交互逻辑更新
        if (draggingSV) {
            float s = (mouseX - pickerX) / pickerW;
            float b = 1.0f - ((mouseY - pickerY) / svHeight);
            saturation = Math.max(0, Math.min(1, s));
            brightness = Math.max(0, Math.min(1, b));
            updateColor();
        } else if (draggingHue) {
            float h = (mouseX - pickerX) / pickerW;
            hue = Math.max(0, Math.min(1, h));
            updateColor();
        } else if (!Mouse.isButtonDown(0)) {
            updateHSB();
        }

        // ==========================================
        // 1. 绘制 SV 面板 (颜色 + 饱和度 + 亮度)
        // ==========================================

        // 1.1 底色：当前色相的纯色
        int hueColor = Color.HSBtoRGB(hue, 1.0f, 1.0f);
        RenderUtil.drawRect(pickerX, pickerY, pickerX + pickerW, pickerY + svHeight, hueColor);

        // 1.2 蒙版1 (水平)：左白 -> 右透明 (控制饱和度)
        // 关键修复：强制使用水平渐变
        drawGradientRect(pickerX, pickerY, pickerW, svHeight, 0xFFFFFFFF, 0x00FFFFFF, true);

        // 1.3 蒙版2 (垂直)：上透明 -> 下黑 (控制亮度)
        // 关键修复：强制使用垂直渐变
        drawGradientRect(pickerX, pickerY, pickerW, svHeight, 0x00000000, 0xFF000000, false);

        // 1.4 SV 指示器 (圆圈)
        float indicatorX = pickerX + (saturation * pickerW);
        float indicatorY = pickerY + ((1 - brightness) * svHeight);

        // 双层描边，确保在任何颜色上都可见
        RenderUtil.drawCircleOutline(indicatorX, indicatorY, 3, 2.0f, 0xFF000000); // 黑底
        RenderUtil.drawCircleOutline(indicatorX, indicatorY, 3, 1.0f, 0xFFFFFFFF); // 白芯

        // ==========================================
        // 2. 绘制 Hue 彩虹条
        // ==========================================
        float hueY = pickerY + svHeight + 4;
        drawRainbowRect(pickerX, hueY, pickerW, hueHeight);

        // 2.1 Hue 指示器
        float hueIndicatorX = pickerX + (hue * pickerW);
        RenderUtil.drawRect(hueIndicatorX - 1, hueY, hueIndicatorX + 1, hueY + hueHeight, 0xFFFFFFFF);

        // 3. 整体外边框
        RenderUtil.drawRectOutline(pickerX - 1, pickerY - 1, pickerW + 2, pickerH + 2, 1.0f, MaterialTheme.getRGBWithAlpha(MaterialTheme.OUTLINE_COLOR, alpha));
    }

    private void updateColor() {
        colorProperty.setValue(Color.HSBtoRGB(hue, saturation, brightness));
    }

    /**
     * 修复核心：手动绘制彩虹条，确保每个片段都是平滑过渡
     */
    private void drawRainbowRect(float x, float y, float width, float height) {
        int[] colors = {
                0xFFFF0000, // 红
                0xFFFFFF00, // 黄
                0xFF00FF00, // 绿
                0xFF00FFFF, // 青
                0xFF0000FF, // 蓝
                0xFFFF00FF, // 紫
                0xFFFF0000  // 红
        };

        // 将宽度分成 6 份
        float segmentWidth = width / 6.0f;

        for (int i = 0; i < 6; i++) {
            float currentX = x + (i * segmentWidth);
            // 绘制当前颜色到下一个颜色的 水平 渐变
            drawGradientRect(currentX, y, segmentWidth, height, colors[i], colors[i + 1], true);
        }
    }

    /**
     * 修复核心：完全独立的渐变绘制方法，不依赖 RenderUtil
     * @param horizontal true 为水平渐变(左->右)，false 为垂直渐变(上->下)
     */
    private void drawGradientRect(float x, float y, float width, float height, int startColor, int endColor, boolean horizontal) {
        float f = (float)(startColor >> 24 & 255) / 255.0F;
        float f1 = (float)(startColor >> 16 & 255) / 255.0F;
        float f2 = (float)(startColor >> 8 & 255) / 255.0F;
        float f3 = (float)(startColor & 255) / 255.0F;
        float f4 = (float)(endColor >> 24 & 255) / 255.0F;
        float f5 = (float)(endColor >> 16 & 255) / 255.0F;
        float f6 = (float)(endColor >> 8 & 255) / 255.0F;
        float f7 = (float)(endColor & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425); // GL_SMOOTH 核心，解决色块问题的关键

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);

        if (horizontal) {
            // 水平渐变：左侧顶点用 startColor，右侧顶点用 endColor
            worldrenderer.pos(x + width, y, 0).color(f5, f6, f7, f4).endVertex(); // 右上
            worldrenderer.pos(x, y, 0).color(f1, f2, f3, f).endVertex();          // 左上
            worldrenderer.pos(x, y + height, 0).color(f1, f2, f3, f).endVertex(); // 左下
            worldrenderer.pos(x + width, y + height, 0).color(f5, f6, f7, f4).endVertex(); // 右下
        } else {
            // 垂直渐变：上方顶点用 startColor，下方顶点用 endColor
            worldrenderer.pos(x + width, y, 0).color(f1, f2, f3, f).endVertex(); // 右上
            worldrenderer.pos(x, y, 0).color(f1, f2, f3, f).endVertex();          // 左上
            worldrenderer.pos(x, y + height, 0).color(f5, f6, f7, f4).endVertex(); // 左下
            worldrenderer.pos(x + width, y + height, 0).color(f5, f6, f7, f4).endVertex(); // 右下
        }

        tessellator.draw();

        GlStateManager.shadeModel(7424); // 恢复 GL_FLAT
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (mouseButton != 0) return false;

        int scrolledY = y - scrollOffset;
        float padding = 4;
        float pickerX = x + padding;
        float pickerY = scrolledY + padding;
        float pickerW = width - (padding * 2);
        float pickerH = height - (padding * 2);
        float hueHeight = 6;
        float svHeight = pickerH - hueHeight - 4;

        // 点击 SV 面板
        if (mouseX >= pickerX && mouseX <= pickerX + pickerW && mouseY >= pickerY && mouseY <= pickerY + svHeight) {
            draggingSV = true;
            return true;
        }

        // 点击 Hue 条
        float hueY = pickerY + svHeight + 4;
        if (mouseX >= pickerX && mouseX <= pickerX + pickerW && mouseY >= hueY && mouseY <= hueY + hueHeight) {
            draggingHue = true;
            return true;
        }

        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {}

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        draggingSV = false;
        draggingHue = false;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {}
}