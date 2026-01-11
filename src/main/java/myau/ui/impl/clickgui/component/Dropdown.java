package myau.ui.impl.clickgui.component;

import myau.property.properties.ModeProperty;
import myau.ui.impl.clickgui.MaterialTheme;
import myau.util.RenderUtil;
import myau.util.font.FontManager;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class Dropdown extends Component {

    private final ModeProperty modeProperty;
    private boolean expanded;
    private static final int ITEM_HEIGHT = 18;

    public Dropdown(ModeProperty modeProperty, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.modeProperty = modeProperty;
        this.expanded = false;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset) {
        float easedProgress = 1.0f - (float) Math.pow(1.0f - animationProgress, 4);
        if (easedProgress <= 0) return;

        int scrolledY = y - scrollOffset;
        int alpha = (int)(255 * easedProgress);

        // 1. 背景框
        int bgColor = new Color(30, 30, 35, alpha).getRGB();
        RenderUtil.drawRoundedRect(x + 2, scrolledY, width - 4, height, 4.0f, bgColor, true, true, true, true);

        // 2. 文字
        if (easedProgress > 0.9f) {
            String text = modeProperty.getName() + ": " + modeProperty.getModeString();
            int textColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR, alpha);
            float textY = scrolledY + (height - 8) / 2f;

            if (FontManager.productSans16 != null) {
                FontManager.productSans16.drawString(text, x + 6, textY, textColor);
            } else {
                mc.fontRendererObj.drawStringWithShadow(text, x + 6, scrolledY + 6, textColor);
            }
        }

        // 3. 展开列表
        if (expanded && easedProgress >= 1.0f) {
            List<String> modes = Arrays.asList(modeProperty.getValuePrompt().split(", "));
            int dropdownY = scrolledY + height + 2;
            int totalH = modes.size() * ITEM_HEIGHT;

            // 列表背景
            RenderUtil.drawRoundedRect(x + 2, dropdownY, width - 4, totalH, 4.0f,
                    new Color(20, 20, 24, 240).getRGB(), true, true, true, true);

            for (int i = 0; i < modes.size(); i++) {
                String mode = modes.get(i);
                int itemY = dropdownY + i * ITEM_HEIGHT;

                boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
                int itemColor = hovered ? MaterialTheme.getRGB(MaterialTheme.PRIMARY_COLOR) : MaterialTheme.getRGB(MaterialTheme.TEXT_COLOR_SECONDARY);

                if (FontManager.productSans16 != null) {
                    FontManager.productSans16.drawString(mode, x + 8, itemY + 5, itemColor);
                } else {
                    mc.fontRendererObj.drawStringWithShadow(mode, x + 8, itemY + 5, itemColor);
                }
            }
        }
    }

    @Override public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) { return false; }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        int scrolledY = y - scrollOffset;

        // 点击主框
        if (mouseX >= x && mouseX <= x + width && mouseY >= scrolledY && mouseY <= scrolledY + height) {
            if (mouseButton == 0) {
                expanded = !expanded;
                return true;
            }
        }

        // 点击列表项
        if (expanded) {
            List<String> modes = Arrays.asList(modeProperty.getValuePrompt().split(", "));
            int dropdownY = scrolledY + height + 2;

            for (int i = 0; i < modes.size(); i++) {
                int itemY = dropdownY + i * ITEM_HEIGHT;
                if (mouseX >= x && mouseX <= x + width && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT) {
                    if (mouseButton == 0) {
                        modeProperty.setValue(i);
                        expanded = false;
                        return true;
                    }
                }
            }
            // 点外面关闭
            if (!isMouseOver(mouseX, mouseY, scrollOffset)) {
                expanded = false;
            }
        }
        return false;
    }

    @Override public int getHeight() {
        if (expanded) {
            List<String> modes = Arrays.asList(modeProperty.getValuePrompt().split(", "));
            return height + (modes.size() * ITEM_HEIGHT) + 4;
        }
        return height;
    }

    @Override public void mouseReleased(int mouseX, int mouseY, int mouseButton) {}
    @Override public void keyTyped(char typedChar, int keyCode) {}

    public boolean isMouseOver(int mouseX, int mouseY, int scrollOffset) {
        int scrolledY = y - scrollOffset;
        if (expanded) return mouseX >= x && mouseX <= x + width && mouseY >= scrolledY && mouseY <= scrolledY + getHeight();
        return mouseX >= x && mouseX <= x + width && mouseY >= scrolledY && mouseY <= scrolledY + height;
    }
}