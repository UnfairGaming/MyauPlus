package myau.ui.impl.clickgui.component;

import myau.property.Property;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.property.properties.PercentProperty;
import myau.ui.impl.clickgui.MaterialTheme;
import myau.util.RenderUtil;
import myau.util.font.FontManager;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class Slider extends Component {

    private final Property<?> property;
    private boolean dragging;
    private boolean hovered;

    public Slider(Property<?> property, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.property = property;
        this.dragging = false;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset) {
        float easedProgress = 1.0f - (float) Math.pow(1.0f - animationProgress, 4);
        if (easedProgress <= 0) return;

        int scaledHeight = (int) (height * easedProgress);
        int scrolledY = y - scrollOffset;
        int scaledY = scrolledY + (height - scaledHeight) / 2;

        hovered = isMouseOver(mouseX, mouseY, scrollOffset);
        int alpha = (int)(255 * easedProgress);

        double min = 0, max = 0, value = 0;
        if (this.property instanceof IntProperty) {
            min = ((IntProperty) this.property).getMinimum();
            max = ((IntProperty) this.property).getMaximum();
            value = (Integer) this.property.getValue();
        } else if (this.property instanceof FloatProperty) {
            min = ((FloatProperty) this.property).getMinimum();
            max = ((FloatProperty) this.property).getMaximum();
            value = (Float) this.property.getValue();
        } else if (this.property instanceof PercentProperty) {
            min = 0; max = 100;
            value = (Integer) this.property.getValue();
        }

        double fillProgress = (max - min != 0) ? (value - min) / (max - min) : 0;
        fillProgress = Math.max(0, Math.min(1, fillProgress));

        // 1. 文字 (Title & Value)
        if (easedProgress > 0.5f) {
            String name = property.getName();
            String valStr = "" + round(value, 2) + (this.property instanceof PercentProperty ? "%" : "");
            int textColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR, alpha);

            float textY = scaledY + 2; // 顶部

            if (FontManager.productSans16 != null) {
                FontManager.productSans16.drawString(name, x + 2, textY, textColor);
                float valW = (float) FontManager.productSans16.getStringWidth(valStr);
                FontManager.productSans16.drawString(valStr, x + width - valW - 2, textY, textColor);
            } else {
                mc.fontRendererObj.drawStringWithShadow(name, x + 2, textY, textColor);
                mc.fontRendererObj.drawStringWithShadow(valStr, x + width - mc.fontRendererObj.getStringWidth(valStr) - 2, textY, textColor);
            }
        }

        // 2. 轨道 (Track)
        int trackHeight = 4;
        int trackY = scaledY + height - 8;
        int trackX = x + 2;
        int trackWidth = width - 4;

        int trackColor = new Color(60, 60, 65, alpha).getRGB();
        RenderUtil.drawRoundedRect(trackX, trackY, trackWidth, trackHeight, 2.0f, trackColor, true, true, true, true);

        // 3. 填充 (Fill)
        int fillWidth = (int) (trackWidth * fillProgress);
        if (fillWidth > 0) {
            int fillColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.PRIMARY_COLOR, alpha);
            RenderUtil.drawRoundedRect(trackX, trackY, fillWidth, trackHeight, 2.0f, fillColor, true, true, true, true);

            // 光晕
            int glowColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.PRIMARY_COLOR, (int)(alpha * 0.4));
            float kX = trackX + fillWidth;
            float kY = trackY + trackHeight / 2.0f;
            RenderUtil.drawRoundedRect(kX - 5, kY - 5, 10, 10, 5.0f, glowColor, true, true, true, true);
        }

        // 4. 拖拽球
        float knobX = trackX + fillWidth;
        float knobY = trackY + trackHeight / 2.0f;
        int knobColor = MaterialTheme.getRGBWithAlpha(Color.WHITE, alpha);
        float radius = dragging ? 4.0f : 2.5f;
        RenderUtil.drawRoundedRect(knobX - radius, knobY - radius, radius*2, radius*2, radius, knobColor, true, true, true, true);

        if (this.dragging) updateValue(mouseX, scrollOffset);
    }

    private void updateValue(int mouseX, int scrollOffset) {
        double min = 0, max = 0;
        if (this.property instanceof IntProperty) { min = ((IntProperty)property).getMinimum(); max = ((IntProperty)property).getMaximum(); }
        else if (this.property instanceof FloatProperty) { min = ((FloatProperty)property).getMinimum(); max = ((FloatProperty)property).getMaximum(); }
        else if (this.property instanceof PercentProperty) { min = 0; max = 100; }

        int trackX = x + 2;
        int trackWidth = width - 4;

        double percent = (double) (mouseX - trackX) / (double) trackWidth;
        percent = Math.max(0, Math.min(1, percent));
        double value = min + (max - min) * percent;

        if (this.property instanceof IntProperty) property.setValue((int) round(value, 0));
        else if (this.property instanceof FloatProperty) property.setValue((float) round(value, 2));
        else if (this.property instanceof PercentProperty) property.setValue((int) round(value, 0));
    }

    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

    @Override public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) { return false; }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (isMouseOver(mouseX, mouseY, scrollOffset) && mouseButton == 0) {
            this.dragging = true;
            updateValue(mouseX, scrollOffset);
            return true;
        }
        return false;
    }

    @Override public void mouseReleased(int mouseX, int mouseY, int mouseButton) {}

    public void mouseReleased(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        this.dragging = false;
    }

    @Override public void keyTyped(char typedChar, int keyCode) {}

    public boolean isMouseOver(int mouseX, int mouseY, int scrollOffset) {
        int actualY = this.y - scrollOffset;
        return mouseX >= x && mouseX <= x + width && mouseY >= actualY && mouseY < actualY + height;
    }
}