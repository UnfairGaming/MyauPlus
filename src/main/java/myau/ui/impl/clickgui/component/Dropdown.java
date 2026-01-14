package myau.ui.impl.clickgui.component;

import myau.property.properties.ModeProperty;
import myau.ui.impl.clickgui.MaterialTheme;
import myau.util.AnimationUtil;
import myau.util.RenderUtil;
import myau.util.font.FontManager;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class Dropdown extends Component {
    private final ModeProperty modeProperty;
    private boolean expanded;
    private static final int ITEM_HEIGHT = 18;
    private float expandAnim = 0.0f;
    private final int headerHeight; // 新增：保存头部固定的高度

    public Dropdown(ModeProperty modeProperty, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.modeProperty = modeProperty;
        this.expanded = false;
        this.headerHeight = height; // 初始化头部高度
    }

    // 核心修复：重写获取高度方法，返回 头部 + 展开列表的高度
    // 这样 ModuleEntry 在计算布局时，会自动把下方的组件向下推
    @Override
    public int getHeight() {
        return (int) (headerHeight + expandAnim);
    }

    public ModeProperty getProperty() {
        return this.modeProperty;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        if (!modeProperty.isVisible()) {
            return;
        }

        float easedProgress = 1.0f - (float) Math.pow(1.0f - animationProgress, 4);
        if (easedProgress <= 0) return;

        int scrolledY = y - scrollOffset;
        int alpha = (int)(255 * easedProgress);
        int bgColor = new Color(30, 30, 35, alpha).getRGB();

        // 修复：背景只绘制头部高度 (headerHeight)，而不是整个高度
        RenderUtil.drawRoundedRect(x + 2, scrolledY, width - 4, headerHeight, 4.0f, bgColor, true, true, true, true);

        if (easedProgress > 0.9f) {
            String text = modeProperty.getName() + ": " + modeProperty.getModeString();
            int textColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR, alpha);
            // 文字垂直居中于头部
            float textY = scrolledY + (headerHeight - 8) / 2f;
            if (FontManager.productSans16 != null) {
                FontManager.productSans16.drawString(text, x + 6, textY, textColor);
            } else {
                mc.fontRendererObj.drawStringWithShadow(text, x + 6, scrolledY + 6, textColor);
            }
        }

        List<String> modes = Arrays.asList(modeProperty.getValuePrompt().split(", "));
        float targetAnim = expanded ? modes.size() * ITEM_HEIGHT : 0f;
        // 动画更新
        this.expandAnim = AnimationUtil.animateSmooth(targetAnim, this.expandAnim, 12.0f, deltaTime);

        // 渲染下拉列表
        if (expandAnim > 0.5f && easedProgress >= 1.0f) {
            // 下拉列表的起始 Y 坐标在头部下方
            int dropdownY = scrolledY + headerHeight + 2;

            // 背景
            RenderUtil.drawRoundedRect(x + 2, dropdownY, width - 4, expandAnim, 4.0f, new Color(20, 20, 24, 240).getRGB(), true, true, true, true);

            // 裁剪区域
            RenderUtil.scissor(x, dropdownY, width, expandAnim);

            for (int i = 0; i < modes.size(); i++) {
                String mode = modes.get(i);
                int itemY = dropdownY + i * ITEM_HEIGHT;

                // 只有在展开动画足够显示该项时才检测悬停，防止透过遮罩检测
                boolean hovered = false;
                if (mouseY >= dropdownY && mouseY <= dropdownY + expandAnim) {
                    hovered = mouseX >= x && mouseX <= x + width && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT;
                }

                int itemColor = hovered ? MaterialTheme.getRGB(MaterialTheme.PRIMARY_COLOR) : MaterialTheme.getRGB(MaterialTheme.TEXT_COLOR_SECONDARY);

                if (FontManager.productSans16 != null) {
                    FontManager.productSans16.drawString(mode, x + 8, itemY + 5, itemColor);
                } else {
                    mc.fontRendererObj.drawStringWithShadow(mode, x + 8, itemY + 5, itemColor);
                }
            }
            RenderUtil.releaseScissor();
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) { return false; }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        int scrolledY = y - scrollOffset;

        // 修复：点击头部 (使用 headerHeight 判断)
        if (mouseX >= x && mouseX <= x + width && mouseY >= scrolledY && mouseY <= scrolledY + headerHeight) {
            if (mouseButton == 0 || mouseButton == 1) { // 支持左键或右键展开
                expanded = !expanded;
                return true;
            }
        }

        // 点击列表项
        if (expanded && expandAnim > 0) {
            List<String> modes = Arrays.asList(modeProperty.getValuePrompt().split(", "));
            int dropdownY = scrolledY + headerHeight + 2;

            // 检查点击是否在展开区域内
            if (mouseY >= dropdownY && mouseY <= dropdownY + expandAnim) {
                for (int i = 0; i < modes.size(); i++) {
                    int itemY = dropdownY + i * ITEM_HEIGHT;
                    if (mouseX >= x && mouseX <= x + width && mouseY >= itemY && mouseY < itemY + ITEM_HEIGHT) {
                        if (mouseButton == 0) {
                            modeProperty.setValue(i);
                            expanded = false; // 选择后收起，可根据喜好保留
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override public void mouseReleased(int mouseX, int mouseY, int mouseButton) {}
    @Override public void keyTyped(char typedChar, int keyCode) {}
}