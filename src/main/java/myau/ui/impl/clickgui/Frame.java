package myau.ui.impl.clickgui;

import myau.Myau;
import myau.module.Category;
import myau.module.Module;
import myau.module.modules.ClickGUIModule; // 导入 ClickGUIModule
import myau.ui.impl.clickgui.component.Component;
import myau.ui.impl.clickgui.component.ModuleEntry;
import myau.util.RenderUtil;
import myau.util.ShadowUtil;
import myau.util.font.FontManager;
import net.minecraft.client.Minecraft;

import java.awt.*;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class Frame extends Component {

    private int dragX, dragY;
    private final Category category;
    private boolean dragging;
    private boolean expanded;
    private final ArrayList<ModuleEntry> moduleEntries;
    private ArrayList<ModuleEntry> visibleEntries;

    public Frame(Category category, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.category = category;
        this.dragging = false;
        this.expanded = true;
        this.moduleEntries = new ArrayList<>();

        if (Myau.moduleManager != null) {
            for (Module module : Myau.moduleManager.getModulesInCategory(this.category)) {
                // 高度 22
                this.moduleEntries.add(new ModuleEntry(module, x, 0, width, 22));
            }
        }
        this.visibleEntries = new ArrayList<>(this.moduleEntries);
    }

    public void refilter(String query) {
        if (query == null || query.isEmpty()) {
            this.visibleEntries = new ArrayList<>(this.moduleEntries);
        } else {
            this.visibleEntries = this.moduleEntries.stream()
                    .filter(entry -> entry.getModule().getName().toLowerCase().contains(query.toLowerCase()))
                    .collect(Collectors.toCollection(ArrayList::new));
        }
    }

    public boolean isAnyComponentBinding() {
        if (expanded) {
            for (ModuleEntry entry : visibleEntries) {
                if (entry.isBinding()) return true;
            }
        }
        return false;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset) {
        float easedProgress = 1.0f - (float) Math.pow(1.0f - animationProgress, 4);
        if (easedProgress <= 0) return;

        int scaledWidth = (int) (width * easedProgress);
        int scaledHeight = (int) (height * easedProgress);

        int scaledX = x + (width - scaledWidth) / 2;
        int scrolledY = y - scrollOffset;
        int scaledY = scrolledY + (height - scaledHeight) / 2;

        int alpha = (int)(255 * easedProgress);

        // 获取 ClickGUI 模块实例以读取 shadow 设置
        ClickGUIModule clickGUIModule = (ClickGUIModule) Myau.moduleManager.modules.get(ClickGUIModule.class);
        boolean shadowEnabled = clickGUIModule != null && clickGUIModule.shadow.getValue();

        // 绘制阴影 (增加判断：alpha > 0 且 shadowEnabled 为 true)
        if (alpha > 0 && shadowEnabled) {
            // 阴影颜色：纯黑，透明度随动画变化 (例如最大 120)
            int shadowAlpha = Math.min(120, (int)(alpha * 0.5));
            int shadowColor = new Color(0, 0, 0, shadowAlpha).getRGB();

            // 计算总高度 (头部 + 展开的列表)
            float totalHeight = expanded ? getTotalHeight() : height;

            // 绘制阴影 (radius=8, softness=12 看起来比较像 Bloom)
            ShadowUtil.drawShadow(scaledX, scaledY, scaledWidth, totalHeight,
                    MaterialTheme.CORNER_RADIUS_FRAME, 12.0f, shadowColor);
        }

        // 头部背景 (黑色)
        int headerColor = new Color(15, 15, 15, alpha).getRGB();
        boolean roundBottom = !expanded;

        RenderUtil.drawRoundedRect(scaledX, scaledY, scaledWidth, scaledHeight,
                MaterialTheme.CORNER_RADIUS_FRAME,
                headerColor,
                true, true, roundBottom, roundBottom);

        // 标题 & 图标
        if (easedProgress > 0.9f) {
            int textColor = new Color(255, 255, 255, alpha).getRGB();

            if (FontManager.productSans20 != null) {
                // 字体垂直居中微调
                float textY = (float) (scaledY + (height - FontManager.productSans20.getHeight()) / 2f + 1);
                FontManager.productSans20.drawString(category.getName(), scaledX + 8, textY, textColor);

                String arrow = expanded ? "-" : "+";
                float arrowW = (float) FontManager.productSans20.getStringWidth(arrow);
                FontManager.productSans20.drawString(arrow, scaledX + scaledWidth - arrowW - 8, textY, textColor);
            } else {
                Minecraft.getMinecraft().fontRendererObj.drawStringWithShadow(category.getName(), scaledX + 6, scaledY + 6, textColor);
            }
        }

        // 模块列表
        if (expanded && easedProgress > 0) {
            int contentHeight = getTotalHeight() - height;
            // 列表背景 (深灰)
            int listBgColor = new Color(28, 28, 28, alpha).getRGB();

            if (contentHeight > 0) {
                RenderUtil.drawRoundedRect(scaledX, scaledY + height, scaledWidth, contentHeight,
                        MaterialTheme.CORNER_RADIUS_FRAME,
                        listBgColor,
                        false, false, true, true);
            }

            int currentWorldY = y + height;
            for (int i = 0; i < visibleEntries.size(); i++) {
                ModuleEntry entry = visibleEntries.get(i);
                entry.setX(x);
                entry.setY(currentWorldY);
                entry.setWidth(width);

                boolean isLastEntry = (i == visibleEntries.size() - 1);
                entry.render(mouseX, mouseY, partialTicks, animationProgress, isLastEntry, scrollOffset);

                currentWorldY += entry.getTotalHeight();
            }
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) { return false; }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (isMouseOver(mouseX, mouseY, scrollOffset)) {
            if (mouseButton == 0) {
                this.dragging = true;
                this.dragX = mouseX - this.x;
                this.dragY = mouseY - this.y;
                return true;
            } else if (mouseButton == 1) {
                expanded = !expanded;
                return true;
            }
        }

        if (expanded) {
            for (ModuleEntry entry : visibleEntries) {
                if (entry.mouseClicked(mouseX, mouseY, mouseButton, scrollOffset)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) { }

    public void mouseReleased(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        this.dragging = false;
        if (expanded) {
            for (ModuleEntry entry : visibleEntries) {
                entry.mouseReleased(mouseX, mouseY, mouseButton, scrollOffset);
            }
        }
    }

    public void updatePosition(int mouseX, int mouseY) {
        if (this.dragging) {
            this.x = mouseX - this.dragX;
            this.y = mouseY - this.dragY;
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (expanded) {
            for (ModuleEntry entry : visibleEntries) {
                entry.keyTyped(typedChar, keyCode);
            }
        }
    }

    public int getTotalHeight() {
        int total = height;
        if (expanded) {
            for (ModuleEntry entry : visibleEntries) {
                total += entry.getTotalHeight();
            }
        }
        return total;
    }

}