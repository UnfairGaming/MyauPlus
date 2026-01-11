package myau.ui.impl.clickgui.component;

import myau.Myau;
import myau.module.Module;
import myau.property.Property;
import myau.property.properties.*;
import myau.ui.impl.clickgui.MaterialTheme;
import myau.util.RenderUtil;
import myau.util.font.FontManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ModuleEntry extends Component {

    private final Module module;
    private boolean expanded;
    private List<Component> propertiesComponents;
    private boolean hovered;

    public ModuleEntry(Module module, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.module = module;
        this.expanded = false;
        this.propertiesComponents = new ArrayList<>();
        initializePropertiesComponents();
    }

    private void initializePropertiesComponents() {
        // --- 新增：添加 Keybind 组件 ---
        int currentY = y + height;

        // 1. 先添加按键绑定 (高度设为 20)
        KeybindComponent keybindComp = new KeybindComponent(module, x, currentY, width, 20);
        propertiesComponents.add(keybindComp);
        currentY += 20;

        // --- 原有逻辑 ---
        if (Myau.propertyManager != null) {
            List<Property<?>> properties = Myau.propertyManager.properties.get(module.getClass());
            if (properties != null) {
                for (Property<?> property : properties) {
                    Component comp = null;
                    int compHeight = 20; // 统一高度

                    if (property instanceof BooleanProperty) {
                        comp = new Switch((BooleanProperty) property, x, currentY, width, compHeight);
                    } else if (property instanceof IntProperty || property instanceof FloatProperty || property instanceof PercentProperty) {
                        comp = new Slider(property, x, currentY, width, compHeight);
                    } else if (property instanceof ModeProperty) {
                        comp = new Dropdown((ModeProperty) property, x, currentY, width, compHeight);
                    } else if (property instanceof ColorProperty) {
                        comp = new ColorPicker((ColorProperty) property, x, currentY, width, 60);
                        compHeight = 60;
                    }

                    if (comp != null) {
                        propertiesComponents.add(comp);
                        currentY += compHeight;
                    }
                }
            }
        }
    }

    public Module getModule() {
        return this.module;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset) {
        int scrolledY = y - scrollOffset;
        hovered = isMouseOver(mouseX, mouseY, scrollOffset);

        float easedProgress = 1.0f - (float) Math.pow(1.0f - animationProgress, 4);
        int scaledHeight = (int) (height * easedProgress);
        int scaledY = scrolledY + (height - scaledHeight) / 2;

        if (easedProgress <= 0) return;

        int alpha = (int)(255 * easedProgress);

        // 1. 悬停背景 (极淡)
        if (hovered) {
            RenderUtil.drawRoundedRect(x + 2, scaledY, width - 4, scaledHeight,
                    4, MaterialTheme.getRGBWithAlpha(MaterialTheme.SURFACE_CONTAINER_HIGH, alpha), true, true, true, true);
        }

        if (easedProgress > 0.9f) {
            int textColor;
            if (module.isEnabled()) {
                textColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.PRIMARY_COLOR, alpha);
            } else {
                textColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR, alpha);
            }

            if (FontManager.productSans16 != null) {
                float textY = (float) (scaledY + (height - FontManager.productSans16.getHeight()) / 2f + 1);
                FontManager.productSans16.drawString(module.getName(), x + 10, textY, textColor);

                if (!propertiesComponents.isEmpty()) {
                    String icon = expanded ? "..." : ":";
                    float iconW = (float) FontManager.productSans16.getStringWidth(icon);
                    FontManager.productSans16.drawString(icon, x + width - iconW - 8, textY,
                            MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR_SECONDARY, alpha));
                }
            } else {
                mc.fontRendererObj.drawStringWithShadow(module.getName(), x + 8, scaledY + 6, textColor);
            }

            if (module.isEnabled()) {
                int dotColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.PRIMARY_COLOR, alpha);
                RenderUtil.drawRoundedRect(x + 4, scaledY + height/2f - 1.5f, 3, 3, 1.5f, dotColor, true, true, true, true);
            }
        }

        // 6. 渲染设置项
        if (expanded && easedProgress >= 1.0f) {
            int propertiesHeight = getTotalHeight() - height;
            RenderUtil.drawRect(x + 2, scaledY + height, width - 4, propertiesHeight, new Color(10, 10, 12, 100).getRGB());

            int currentY = y + height;
            for (int i = 0; i < propertiesComponents.size(); i++) {
                Component comp = propertiesComponents.get(i);
                comp.setX(x + 4);
                comp.setY(currentY);
                comp.setWidth(width - 8);

                boolean isLastComponent = isLast && (i == propertiesComponents.size() - 1);
                comp.render(mouseX, mouseY, partialTicks, animationProgress, isLastComponent, scrollOffset);
                currentY += comp.getHeight();
            }
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) { return false; }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (isMouseOver(mouseX, mouseY, scrollOffset)) {
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

        if (expanded) {
            for (Component comp : propertiesComponents) {
                if (comp instanceof KeybindComponent) {
                    if (((KeybindComponent) comp).mouseClicked(mouseX, mouseY, mouseButton, scrollOffset)) return true;
                }
                if (comp instanceof ColorPicker) {
                    if (((ColorPicker) comp).mouseClicked(mouseX, mouseY, mouseButton, scrollOffset)) return true;
                } else if (comp instanceof Switch) {
                    if (((Switch) comp).mouseClicked(mouseX, mouseY, mouseButton, scrollOffset)) return true;
                } else if (comp instanceof Slider) {
                    if (((Slider) comp).mouseClicked(mouseX, mouseY, mouseButton, scrollOffset)) return true;
                } else if (comp instanceof Dropdown) {
                    if (((Dropdown) comp).mouseClicked(mouseX, mouseY, mouseButton, scrollOffset)) return true;
                }
            }
        }
        return false;
    }

    public boolean isBinding() {
        if (expanded) {
            for (Component comp : propertiesComponents) {
                if (comp instanceof KeybindComponent && ((KeybindComponent) comp).isBinding()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {}

    public void mouseReleased(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        if (expanded) {
            for (Component comp : propertiesComponents) {
                if (comp instanceof ColorPicker) ((ColorPicker) comp).mouseReleased(mouseX, mouseY, mouseButton, scrollOffset);
                else if (comp instanceof Slider) ((Slider) comp).mouseReleased(mouseX, mouseY, mouseButton, scrollOffset);
                else if (comp instanceof Dropdown) ((Dropdown) comp).mouseReleased(mouseX, mouseY, mouseButton, scrollOffset);
            }
        }
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (expanded) {
            for (Component comp : propertiesComponents) {
                comp.keyTyped(typedChar, keyCode);
            }
        }
    }

    public int getTotalHeight() {
        int total = height;
        if (expanded) {
            for (Component comp : propertiesComponents) {
                total += comp.getHeight();
            }
        }
        return total;
    }
}