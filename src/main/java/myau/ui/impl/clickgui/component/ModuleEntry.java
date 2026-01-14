package myau.ui.impl.clickgui.component;

import lombok.Getter;
import myau.Myau;
import myau.module.Module;
import myau.property.Property;
import myau.property.properties.*;
import myau.ui.impl.clickgui.MaterialTheme;
import myau.util.AnimationUtil;
import myau.util.RenderUtil;
import myau.util.font.FontManager;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ModuleEntry extends Component {
    @Getter
    private final Module module;
    private boolean expanded;
    private final List<Component> propertiesComponents;
    private float hoverOpacity = 0f;
    private float currentSettingsHeight = 0f;
    private int currentColor;

    public ModuleEntry(Module module, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.module = module;
        this.expanded = false;
        this.propertiesComponents = new ArrayList<>();
        this.currentColor = MaterialTheme.getRGB(MaterialTheme.TEXT_COLOR);
        initializePropertiesComponents();
    }

    private void initializePropertiesComponents() {
        int currentY = y + height;

        // 1. 添加绑定组件
        KeybindComponent keybindComp = new KeybindComponent(module, x, currentY, width, 20);
        propertiesComponents.add(keybindComp);

        // 2. 添加所有属性组件 (此时不检查可见性，因为可见性是动态变化的，要全部加进去)
        if (Myau.propertyManager != null) {
            List<Property<?>> properties = Myau.propertyManager.properties.get(module.getClass());
            if (properties != null) {
                for (Property<?> property : properties) {
                    Component comp = null;
                    int compHeight = 20;

                    if (property instanceof BooleanProperty) {
                        comp = new Switch((BooleanProperty) property, x, currentY, width, compHeight);
                    } else if (property instanceof IntProperty || property instanceof FloatProperty || property instanceof PercentProperty) {
                        comp = new Slider(property, x, currentY, width, compHeight);
                    } else if (property instanceof ModeProperty) {
                        comp = new Dropdown((ModeProperty) property, x, currentY, width, compHeight);
                    } else if (property instanceof ColorProperty) {
                        comp = new ColorPicker((ColorProperty) property, x, currentY, width, 60);
                    }

                    if (comp != null) {
                        propertiesComponents.add(comp);
                        // 这里不需要 currentY += compHeight，因为初始化位置不重要，渲染时会重算
                    }
                }
            }
        }
    }

    /**
     * 核心辅助方法：检查组件对应的属性当前是否可见
     * 这是实现 Lambda 表达式联动的关键
     */
    private boolean isComponentVisible(Component comp) {
        if (comp instanceof Switch) return ((Switch) comp).getProperty().isVisible();
        if (comp instanceof Slider) return ((Slider) comp).getProperty().isVisible();
        if (comp instanceof Dropdown) return ((Dropdown) comp).getProperty().isVisible();
        if (comp instanceof ColorPicker) return ((ColorPicker) comp).getProperty().isVisible();
        // KeybindComponent 始终可见
        return true;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime) {
        int scrolledY = y - scrollOffset;
        boolean hovered = isMouseOverHeader(mouseX, mouseY, scrollOffset);
        int alpha = (int)(255 * animationProgress);

        // --- 绘制模块头部 (Header) ---
        float targetHover = hovered ? 1.0f : 0.0f;
        this.hoverOpacity = AnimationUtil.animateSmooth(targetHover, this.hoverOpacity, 10.0f, deltaTime);
        if (hoverOpacity > 0.01f) {
            int hoverColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.SURFACE_CONTAINER_HIGH, (int)(alpha * hoverOpacity));
            RenderUtil.drawRoundedRect(x + 2, scrolledY, width - 4, height, 4, hoverColor, true, true, true, true);
        }

        int targetColor = module.isEnabled() ? MaterialTheme.getRGB(MaterialTheme.PRIMARY_COLOR) : MaterialTheme.getRGB(MaterialTheme.TEXT_COLOR);
        this.currentColor = AnimationUtil.interpolateColor(this.currentColor, targetColor, 10.0f * deltaTime);
        int finalTextColor = (this.currentColor & 0x00FFFFFF) | (alpha << 24);

        if (alpha > 5) {
            if (FontManager.productSans16 != null) {
                float textY = (float) (scrolledY + (height - FontManager.productSans16.getHeight()) / 2f + 1);
                FontManager.productSans16.drawString(module.getName(), x + 10, textY, finalTextColor);
                if (!propertiesComponents.isEmpty()) {
                    String icon = expanded ? "..." : ":";
                    float iconW = (float) FontManager.productSans16.getStringWidth(icon);
                    FontManager.productSans16.drawString(icon, x + width - iconW - 8, textY, MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR_SECONDARY, alpha));
                }
            } else {
                mc.fontRendererObj.drawStringWithShadow(module.getName(), x + 8, scrolledY + 6, finalTextColor);
            }
            if (module.isEnabled()) {
                RenderUtil.drawRoundedRect(x + 4, scrolledY + height/2f - 1.5f, 3, 3, 1.5f, finalTextColor, true, true, true, true);
            }
        }

        // --- 绘制设置区域 (Settings) ---
        // 1. 计算当前可见组件的总高度
        float visibleHeightSum = 0;
        if (expanded) {
            for (Component comp : propertiesComponents) {
                if (isComponentVisible(comp)) {
                    visibleHeightSum += comp.getHeight();
                }
            }
        }

        // 2. 动画过渡高度
        this.currentSettingsHeight = AnimationUtil.animateSmooth(visibleHeightSum, this.currentSettingsHeight, 12.0f, deltaTime);

        if (currentSettingsHeight > 1.0f) {
            float bgLeft = x + 2;
            float bgTop = scrolledY + height;
            float bgRight = x + width - 2;
            float bgBottom = bgTop + currentSettingsHeight;

            // 绘制背景
            RenderUtil.drawRect(bgLeft, bgTop, bgRight, bgBottom, new Color(10, 10, 12, (int)(100 * (alpha/255f))).getRGB());

            // 开启裁剪，防止溢出
            RenderUtil.scissor(x, scrolledY + height, width, currentSettingsHeight);

            // 3. 动态布局渲染
            // 关键：currentCompY 必须从头开始累加，不能使用组件里存的旧 Y 值
            float dynamicY = y + height;

            for (int i = 0; i < propertiesComponents.size(); i++) {
                Component comp = propertiesComponents.get(i);

                // 如果属性不可见（Lambda返回false），直接跳过，不渲染也不增加 dynamicY
                if (!isComponentVisible(comp)) continue;

                // 只有当组件位于可见区域内时才渲染 (优化性能 + 防止裁剪溢出)
                float relativeY = dynamicY - (y + height);
                if (relativeY < currentSettingsHeight) {
                    comp.setX(x + 4);
                    comp.setY((int) dynamicY); // 更新组件位置为当前动态位置
                    comp.setWidth(width - 8);

                    // 渲染组件
                    comp.render(mouseX, mouseY, partialTicks, animationProgress, isLast && (i == propertiesComponents.size() - 1), scrollOffset, deltaTime);
                }

                // 累加高度，为下一个组件做准备
                dynamicY += comp.getHeight();
            }
            RenderUtil.releaseScissor();
        }
    }

    /**
     * 获取当前模块条目的实际渲染高度
     * 用于 Frame 计算布局
     */
    public float getCurrentHeight() {
        float heightSum = height; // 头部高度
        if (expanded || currentSettingsHeight > 0) {
            heightSum += currentSettingsHeight;
        }
        return heightSum;
    }

    /**
     * 获取展开后的完整目标高度
     * 用于滚动条计算等
     */
    public int getTotalHeight() {
        int total = height;
        if (expanded) {
            for (Component comp : propertiesComponents) {
                // 只累加可见组件的高度
                if (isComponentVisible(comp)) {
                    total += comp.getHeight();
                }
            }
        }
        return total;
    }

    private boolean isMouseOverHeader(int mouseX, int mouseY, int scrollOffset) {
        int actualY = this.y - scrollOffset;
        return mouseX >= x && mouseX <= x + width && mouseY >= actualY && mouseY <= actualY + height;
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) { return false; }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        // 点击头部
        if (isMouseOverHeader(mouseX, mouseY, scrollOffset)) {
            if (mouseButton == 0) {
                module.toggle();
                return true;
            } else if (mouseButton == 1) {
                if (!propertiesComponents.isEmpty()) {
                    expanded = !expanded;
                }
                return true;
            }
        }

        // 点击设置项
        if (expanded) {
            if (currentSettingsHeight < 10) return false;

            for (Component comp : propertiesComponents) {
                // 忽略不可见组件的点击事件
                if (!isComponentVisible(comp)) continue;

                // 传递点击事件
                if (comp.mouseClicked(mouseX, mouseY, mouseButton, scrollOffset)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean isBinding() {
        if (expanded) {
            for (Component comp : propertiesComponents) {
                if (!isComponentVisible(comp)) continue;
                if (comp instanceof KeybindComponent && ((KeybindComponent) comp).isBinding()) return true;
            }
        }
        return false;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (expanded) {
            for (Component comp : propertiesComponents) {
                if (isComponentVisible(comp)) {
                    comp.keyTyped(typedChar, keyCode);
                }
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {}

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (expanded) {
            for (Component comp : propertiesComponents) {
                if (isComponentVisible(comp)) {
                    comp.mouseReleased(mouseX, mouseY, mouseButton, scrollOffset);
                }
            }
        }
    }
}