package myau.ui.impl.clickgui.component;

import myau.property.properties.BooleanProperty;
import myau.ui.impl.clickgui.MaterialTheme;
import myau.util.RenderUtil;
import myau.util.font.FontManager;

import java.awt.*;

public class Switch extends Component {

    private final BooleanProperty booleanProperty;

    public Switch(BooleanProperty booleanProperty, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.booleanProperty = booleanProperty;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset) {
        float easedProgress = 1.0f - (float) Math.pow(1.0f - animationProgress, 4);
        if (easedProgress <= 0) return;

        int scrolledY = y - scrollOffset;
        int alpha = (int)(255 * easedProgress);

        // 1. 文字
        if (easedProgress > 0.9f) {
            int textColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR, alpha);
            float textY = scrolledY + (height - 8) / 2f; // 居中

            if (FontManager.productSans16 != null) {
                FontManager.productSans16.drawString(booleanProperty.getName(), x + 2, textY, textColor);
            } else {
                mc.fontRendererObj.drawStringWithShadow(booleanProperty.getName(), x + 2, scrolledY + 6, textColor);
            }
        }

        // 2. 开关本体
        int switchW = 22;
        int switchH = 12;
        int switchX = x + width - switchW - 2;
        int switchY = scrolledY + (height - switchH) / 2;

        boolean enabled = booleanProperty.getValue();
        int trackColor = enabled ? MaterialTheme.getRGBWithAlpha(MaterialTheme.PRIMARY_COLOR, alpha)
                : new Color(60, 60, 65, alpha).getRGB();

        // 轨道
        RenderUtil.drawRoundedRect(switchX, switchY, switchW, switchH, 6.0f, trackColor, true, true, true, true);

        // 滑块球
        float knobSize = 8;
        float knobX = enabled ? (switchX + switchW - knobSize - 2) : (switchX + 2);
        float knobY = switchY + 2;

        int knobColor = MaterialTheme.getRGBWithAlpha(Color.WHITE, alpha);
        RenderUtil.drawRoundedRect(knobX, knobY, knobSize, knobSize, knobSize/2, knobColor, true, true, true, true);
    }

    @Override public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) { return false; }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (isMouseOver(mouseX, mouseY, scrollOffset) && mouseButton == 0) {
            booleanProperty.setValue(!booleanProperty.getValue());
            return true;
        }
        return false;
    }

    @Override public void mouseReleased(int mouseX, int mouseY, int mouseButton) {}
    @Override public void keyTyped(char typedChar, int keyCode) {}

    public boolean isMouseOver(int mouseX, int mouseY, int scrollOffset) {
        int actualY = this.y - scrollOffset;
        return mouseX >= x && mouseX <= x + width && mouseY >= actualY && mouseY <= actualY + height;
    }
}