package myau.ui.impl.clickgui.component;

import myau.property.properties.ColorProperty;
import myau.ui.impl.clickgui.MaterialTheme;
import myau.util.RenderUtil;
import myau.util.font.FontManager;
import net.minecraft.client.gui.Gui;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class ColorPicker extends Component {

    private final ColorProperty colorProperty;
    private boolean hovered;
    private boolean pickingColor;
    private boolean draggingHue;
    private boolean draggingSatBri;

    private float hue, saturation, brightness;

    private static final int PICKER_HEIGHT = 80;
    private static final int HUE_SLIDER_HEIGHT = 10;

    public ColorPicker(ColorProperty colorProperty, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.colorProperty = colorProperty;
        this.pickingColor = false;
        this.draggingHue = false;
        this.draggingSatBri = false;
        updateHSBFromProperty();
    }

    private void updateHSBFromProperty() {
        int rgb = colorProperty.getValue();
        float[] hsb = Color.RGBtoHSB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, null);
        this.hue = hsb[0];
        this.saturation = hsb[1];
        this.brightness = hsb[2];
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset) {
        float easedProgress = 1.0f - (float) Math.pow(1.0f - animationProgress, 4);
        if (easedProgress <= 0) return;

        int scaledHeight = (int) (height * easedProgress);

        int scrolledY = y - scrollOffset;
        int scaledY = scrolledY + (height - scaledHeight) / 2;

        hovered = isMouseOver(mouseX, mouseY, scrollOffset);

        boolean shouldRoundBottom = isLast && !pickingColor;

        // 背景
        RenderUtil.drawRoundedRect(x, scaledY, width, scaledHeight, MaterialTheme.CORNER_RADIUS_SMALL * easedProgress,
                MaterialTheme.getRGB(hovered ? MaterialTheme.SURFACE_CONTAINER_HIGH : MaterialTheme.SURFACE_CONTAINER_LOW),
                false, false, shouldRoundBottom, shouldRoundBottom);

        if (easedProgress > 0.9f) {
            int alpha = (int) (((easedProgress - 0.9f) / 0.1f) * 255);
            alpha = Math.max(0, Math.min(255, alpha));

            int textColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR, alpha);
            int colorValue = (alpha << 24) | (colorProperty.getValue() & 0x00FFFFFF);
            int outlineColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.OUTLINE_COLOR, alpha);

            // --- 字体修改部分 Start ---
            String name = colorProperty.getName();
            // 垂直居中稍微调整
            float textY = scrolledY + (height - 8) / 2f;

            if (FontManager.productSans16 != null) {
                FontManager.productSans16.drawString(name, x + 5, textY, textColor);
            } else {
                mc.fontRendererObj.drawStringWithShadow(name, x + 5, scrolledY + (height - mc.fontRendererObj.FONT_HEIGHT) / 2, textColor);
            }
            // --- 字体修改部分 End ---

            // 颜色预览块
            int previewSize = 14;
            int previewX = x + width - previewSize - 5;
            int previewY = scrolledY + (height - previewSize) / 2;

            RenderUtil.drawRoundedRect(previewX, previewY, previewSize, previewSize, 4.0f, colorValue, true, true, true, true);
            RenderUtil.drawRoundedRectOutline(previewX, previewY, previewSize, previewSize, 4.0f, 1.0f, outlineColor, true, true, true, true);
        }

        if (pickingColor && easedProgress >= 1.0f) {
            int pickerX = x;
            int pickerY = scrolledY + height;
            int pickerWidth = width;

            if (Mouse.isButtonDown(0)) {
                updateColorDuringRender(mouseX, mouseY, scrollOffset);
            }

            drawPicker(pickerX, pickerY, pickerWidth);
        }
    }

    private void drawPicker(int pickerX, int pickerY, int pickerWidth) {
        int satBriHeight = PICKER_HEIGHT - HUE_SLIDER_HEIGHT;

        // 绘制饱和度/亮度选择器
        Color hueColor = Color.getHSBColor(this.hue, 1.0f, 1.0f);
        drawGradientRect(pickerX, pickerY, pickerX + pickerWidth, pickerY + satBriHeight, Color.WHITE.getRGB(), hueColor.getRGB(), true);
        drawGradientRect(pickerX, pickerY, pickerX + pickerWidth, pickerY + satBriHeight, 0, Color.BLACK.getRGB(), false);

        // 绘制色相滑块
        int hueSliderY = pickerY + satBriHeight;
        for (int i = 0; i < pickerWidth; i++) {
            float currentHue = (float) i / (pickerWidth - 1);
            RenderUtil.drawRect(pickerX + i, hueSliderY, pickerX + i + 1, hueSliderY + HUE_SLIDER_HEIGHT, Color.getHSBColor(currentHue, 1.0f, 1.0f).getRGB());
        }

        // 绘制边框
        RenderUtil.drawRectOutline(pickerX, pickerY, pickerWidth, PICKER_HEIGHT, 1.0f, MaterialTheme.getRGB(MaterialTheme.OUTLINE_COLOR));

        // 绘制饱和度/亮度指示器
        float indicatorX = pickerX + this.saturation * pickerWidth;
        float indicatorY = pickerY + (1 - this.brightness) * satBriHeight;
        Gui.drawRect((int)(indicatorX - 2), (int)(indicatorY - 0.5f), (int)(indicatorX + 2), (int)(indicatorY + 0.5f), Color.BLACK.getRGB());
        Gui.drawRect((int)(indicatorX - 0.5f), (int)(indicatorY - 2), (int)(indicatorX + 0.5f), (int)(indicatorY + 2), Color.BLACK.getRGB());

        // 绘制色相指示器
        float hueIndicatorX = pickerX + this.hue * pickerWidth;
        Gui.drawRect((int)(hueIndicatorX - 0.5f), hueSliderY, (int)(hueIndicatorX + 0.5f), hueSliderY + HUE_SLIDER_HEIGHT, Color.WHITE.getRGB());
    }

    private void drawGradientRect(int left, int top, int right, int bottom, int startColor, int endColor, boolean horizontal) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glShadeModel(GL11.GL_SMOOTH);

        GL11.glBegin(GL11.GL_QUADS);

        if (horizontal) {
            float startA = (float)((startColor >> 24) & 0xFF) / 255.0F;
            float startR = (float)((startColor >> 16) & 0xFF) / 255.0F;
            float startG = (float)((startColor >> 8) & 0xFF) / 255.0F;
            float startB = (float)(startColor & 0xFF) / 255.0F;

            float endA = (float)((endColor >> 24) & 0xFF) / 255.0F;
            float endR = (float)((endColor >> 16) & 0xFF) / 255.0F;
            float endG = (float)((endColor >> 8) & 0xFF) / 255.0F;
            float endB = (float)(endColor & 0xFF) / 255.0F;

            GL11.glColor4f(startR, startG, startB, startA);
            GL11.glVertex2f(left, top);
            GL11.glVertex2f(left, bottom);

            GL11.glColor4f(endR, endG, endB, endA);
            GL11.glVertex2f(right, bottom);
            GL11.glVertex2f(right, top);
        } else {
            float startA = (float)((startColor >> 24) & 0xFF) / 255.0F;
            float startR = (float)((startColor >> 16) & 0xFF) / 255.0F;
            float startG = (float)((startColor >> 8) & 0xFF) / 255.0F;
            float startB = (float)(startColor & 0xFF) / 255.0F;

            float endA = (float)((endColor >> 24) & 0xFF) / 255.0F;
            float endR = (float)((endColor >> 16) & 0xFF) / 255.0F;
            float endG = (float)((endColor >> 8) & 0xFF) / 255.0F;
            float endB = (float)(endColor & 0xFF) / 255.0F;

            GL11.glColor4f(startR, startG, startB, startA);
            GL11.glVertex2f(left, top);
            GL11.glVertex2f(right, top);

            GL11.glColor4f(endR, endG, endB, endA);
            GL11.glVertex2f(right, bottom);
            GL11.glVertex2f(left, bottom);
        }

        GL11.glEnd();

        GL11.glShadeModel(GL11.GL_FLAT);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (mouseButton == 0) {
            if (isMouseOver(mouseX, mouseY, scrollOffset)) {
                pickingColor = !pickingColor;
                if (pickingColor) updateHSBFromProperty();
                return true;
            }

            if (pickingColor) {
                int pickerX = x;
                int pickerY = y - scrollOffset + height;
                int pickerWidth = width;
                int satBriHeight = PICKER_HEIGHT - HUE_SLIDER_HEIGHT;
                int hueSliderY = pickerY + satBriHeight;

                if (mouseX >= pickerX && mouseX <= pickerX + pickerWidth && mouseY >= pickerY && mouseY <= pickerY + satBriHeight) {
                    draggingSatBri = true;
                    updateColorByCoordinates(mouseX, mouseY, scrollOffset);
                    return true;
                }
                if (mouseX >= pickerX && mouseX <= pickerX + pickerWidth && mouseY >= hueSliderY && mouseY <= hueSliderY + HUE_SLIDER_HEIGHT) {
                    draggingHue = true;
                    updateColorByCoordinates(mouseX, mouseY, scrollOffset);
                    return true;
                }
            }
        }

        return false;
    }

    private void updateColorDuringRender(int mouseX, int mouseY, int scrollOffset) {
        int scrolledY = y - scrollOffset;
        int pickerX = x;
        int pickerY = scrolledY + height;
        int pickerWidth = width;
        int satBriHeight = PICKER_HEIGHT - HUE_SLIDER_HEIGHT;

        if ((draggingSatBri || draggingHue) && isMouseWithinExpandedArea(mouseX, mouseY, pickerX, pickerY, pickerWidth, satBriHeight, scrollOffset)) {
            if (draggingSatBri) {
                float relX = Math.max(0, Math.min(1, (float) (mouseX - pickerX) / pickerWidth));
                float relY = Math.max(0, Math.min(1, 1 - (float) (mouseY - pickerY) / satBriHeight));

                this.saturation = relX;
                this.brightness = relY;
            }

            if (draggingHue) {
                float relX = Math.max(0, Math.min(1, (float) (mouseX - pickerX) / pickerWidth));
                this.hue = relX;
            }

            int newColor = Color.getHSBColor(this.hue, this.saturation, this.brightness).getRGB();
            colorProperty.setValue(newColor & 0xFFFFFF);
        }
    }

    private void updateColorByCoordinates(int mouseX, int mouseY, int scrollOffset) {
        int scrolledY = y - scrollOffset;
        int pickerX = x;
        int pickerY = scrolledY + height;
        int pickerWidth = width;
        int satBriHeight = PICKER_HEIGHT - HUE_SLIDER_HEIGHT;

        if (draggingSatBri) {
            float relX = Math.max(0, Math.min(1, (float) (mouseX - pickerX) / pickerWidth));
            float relY = Math.max(0, Math.min(1, 1 - (float) (mouseY - pickerY) / satBriHeight));

            this.saturation = relX;
            this.brightness = relY;
        }

        if (draggingHue) {
            float relX = Math.max(0, Math.min(1, (float) (mouseX - pickerX) / pickerWidth));
            this.hue = relX;
        }

        int newColor = Color.getHSBColor(this.hue, this.saturation, this.brightness).getRGB();
        colorProperty.setValue(newColor & 0xFFFFFF);
    }

    private boolean isMouseWithinExpandedArea(int mouseX, int mouseY, int pickerX, int pickerY, int pickerWidth, int satBriHeight, int scrollOffset) {
        int hueSliderY = pickerY + satBriHeight;
        boolean inSatBriArea = mouseX >= pickerX && mouseX <= pickerX + pickerWidth && mouseY >= pickerY && mouseY <= pickerY + satBriHeight;
        boolean inHueSliderArea = mouseX >= pickerX && mouseX <= pickerX + pickerWidth && mouseY >= hueSliderY && mouseY <= hueSliderY + HUE_SLIDER_HEIGHT;
        return inSatBriArea || inHueSliderArea;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
    }

    public void mouseReleased(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (mouseButton == 0) {
            draggingHue = false;
            draggingSatBri = false;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {}

    @Override
    public int getHeight() {
        return pickingColor ? height + PICKER_HEIGHT : height;
    }

    @Override
    public boolean isMouseOver(int mouseX, int mouseY, int scrollOffset) {
        int actualY = this.y - scrollOffset;
        return mouseX >= x && mouseX <= x + width && mouseY >= actualY && mouseY <= actualY + height;
    }
}