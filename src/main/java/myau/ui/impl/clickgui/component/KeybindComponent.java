package myau.ui.impl.clickgui.component;

import myau.module.Module;
import myau.ui.impl.clickgui.MaterialTheme;
import myau.util.KeyBindUtil;
import myau.util.font.FontManager;
import org.lwjgl.input.Keyboard;

import java.awt.Color;

public class KeybindComponent extends Component {

    private final Module module;
    private boolean binding;

    public KeybindComponent(Module module, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.module = module;
        this.binding = false;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset) {
        float easedProgress = 1.0f - (float) Math.pow(1.0f - animationProgress, 4);
        if (easedProgress <= 0) return;

        int scrolledY = y - scrollOffset;
        int scaledHeight = (int) (height * easedProgress);
        // int scaledY = scrolledY + (height - scaledHeight) / 2; // 不再需要，因为没有背景了

        int alpha = (int)(255 * easedProgress);

        // --- 修改 1: 移除背景绘制逻辑 ---
        // RenderUtil.drawRoundedRect(...); // 已删除

        if (easedProgress > 0.9f) {
            int textColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR, alpha);
            
            // 绑定状态时，右侧文字高亮显示 (例如主题色)，否则显示次要文本色
            int valueColor;
            if (binding) {
                valueColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.PRIMARY_COLOR, alpha);
            } else {
                valueColor = MaterialTheme.getRGBWithAlpha(MaterialTheme.TEXT_COLOR_SECONDARY, alpha);
            }

            String nameText = "Keybind";
            
            // 获取显示的按键名
            String bindText;
            if (binding) {
                bindText = "..."; // 正在绑定
            } else {
                int key = module.getKey();
                if (key == 0 || key == Keyboard.KEY_NONE) {
                    bindText = "None";
                } else {
                    bindText = KeyBindUtil.getKeyName(key);
                }
            }
            
            float textY = scrolledY + (height - 8) / 2f; // 垂直居中

            // 绘制文字
            if (FontManager.productSans16 != null) {
                // 左侧 Label
                FontManager.productSans16.drawString(nameText, x + 6, textY, textColor);
                
                // 右侧 Value
                float w = (float) FontManager.productSans16.getStringWidth(bindText);
                FontManager.productSans16.drawString(bindText, x + width - w - 6, textY, valueColor);
            } else {
                mc.fontRendererObj.drawStringWithShadow(nameText, x + 6, scrolledY + 6, textColor);
                mc.fontRendererObj.drawStringWithShadow(bindText, x + width - mc.fontRendererObj.getStringWidth(bindText) - 6, scrolledY + 6, valueColor);
            }
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        return false;
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        // --- 修改 2: 点击逻辑优化 ---
        // 只有点击在文字区域行内才触发，且只响应左键
        if (isMouseOver(mouseX, mouseY, scrollOffset) && mouseButton == 0) {
            this.binding = !this.binding;
            return true;
        }
        
        // 点击外部取消绑定状态
        if (this.binding && !isMouseOver(mouseX, mouseY, scrollOffset)) {
            this.binding = false;
        }
        return false;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (this.binding) {
            // --- 修改 3: ESC 键逻辑修复 ---
            // 如果按下 DELETE, BACKSPACE 或 ESCAPE，都视为解绑 (设为 0 / NONE)
            if (keyCode == Keyboard.KEY_DELETE || keyCode == Keyboard.KEY_BACK || keyCode == Keyboard.KEY_ESCAPE) {
                module.setKey(Keyboard.KEY_NONE); // KEY_NONE 通常是 0
            } else {
                module.setKey(keyCode);
            }
            
            // 退出绑定模式
            this.binding = false;
        }
    }
    
    // 对外提供查询方法，供主屏幕判断是否拦截 ESC
    public boolean isBinding() {
        return this.binding;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {}

    public boolean isMouseOver(int mouseX, int mouseY, int scrollOffset) {
        int actualY = this.y - scrollOffset;
        return mouseX >= x && mouseX <= x + width && mouseY >= actualY && mouseY <= actualY + height;
    }
}