package myau.ui.impl.clickgui.component;

import myau.ui.impl.clickgui.MaterialTheme;
import myau.util.RenderUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.util.ChatAllowedCharacters;
import org.lwjgl.input.Keyboard;

import java.awt.*;

public class TextField extends Component {

    private String text;
    private String hintText;
    private boolean focused;
    private int cursorPosition;
    private long lastCursorToggle;
    private boolean drawBackground = true; // Add this field

    public TextField(int x, int y, int width, int height, String hintText) {
        super(x, y, width, height);
        this.text = "";
        this.hintText = hintText;
        this.focused = false;
        this.cursorPosition = 0;
        this.lastCursorToggle = System.currentTimeMillis();
    }

    public void setDrawBackground(boolean drawBackground) { // Add this method
        this.drawBackground = drawBackground;
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset) {
        int scrolledY = y - scrollOffset;
        RenderUtil.drawRect(x, scrolledY, width, height, 0xFF000000); // Black background
        mc.fontRendererObj.drawStringWithShadow(this.text, x + 2, scrolledY + 2, 0xFFFFFFFF); // White text
    }

    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast) {
        if (this.drawBackground) {
            // Background
            RenderUtil.drawRoundedRect(x, y, width, height, MaterialTheme.CORNER_RADIUS_SMALL, MaterialTheme.getRGB(MaterialTheme.SURFACE_CONTAINER_LOW), true, true, true, true);
            // Border
            Color borderColor = focused ? MaterialTheme.PRIMARY_COLOR : MaterialTheme.OUTLINE_COLOR;
            RenderUtil.drawRoundedRectOutline(x, y, width, height, MaterialTheme.CORNER_RADIUS_SMALL, 1.0f, MaterialTheme.getRGB(borderColor), true, true, true, true);
        }

        int textColorValue = text.isEmpty() && !focused ? MaterialTheme.getRGB(MaterialTheme.TEXT_COLOR_SECONDARY) : MaterialTheme.getRGB(MaterialTheme.TEXT_COLOR);
        int textColor = (0xFF << 24) | (textColorValue & 0x00FFFFFF); // Ensure full alpha

        // Text
        String displayedText = text.isEmpty() && !focused ? hintText : text;
        mc.fontRendererObj.drawStringWithShadow(displayedText, x + 5, y + (height - mc.fontRendererObj.FONT_HEIGHT) / 2, textColor);

        // Cursor
        if (focused && System.currentTimeMillis() - lastCursorToggle > 500) {
            if (System.currentTimeMillis() % 1000 < 500) { // Blink every 500ms
                String currentTextUntilCursor = text.substring(0, cursorPosition);
                int cursorX = x + 5 + mc.fontRendererObj.getStringWidth(currentTextUntilCursor);
                Gui.drawRect(cursorX, y + (height - mc.fontRendererObj.FONT_HEIGHT) / 2 - 1, cursorX + 1, y + (height + mc.fontRendererObj.FONT_HEIGHT) / 2 + 1, textColor);
            }
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (isMouseOver(mouseX, mouseY) && mouseButton == 0) {
            focused = true;
            lastCursorToggle = System.currentTimeMillis(); // Reset cursor blink
            // Set cursor position to end of text when clicked inside
            cursorPosition = text.length();
            return true;
        } else {
            focused = false;
            return false;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        // Not applicable
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (!focused) return;

        if (keyCode == Keyboard.KEY_BACK) {
            if (text.length() > 0 && cursorPosition > 0) {
                text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
                cursorPosition--;
            }
        } else if (keyCode == Keyboard.KEY_DELETE) {
            if (cursorPosition < text.length()) {
                text = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
            }
        } else if (keyCode == Keyboard.KEY_LEFT) {
            if (cursorPosition > 0) {
                cursorPosition--;
            }
        } else if (keyCode == Keyboard.KEY_RIGHT) {
            if (cursorPosition < text.length()) {
                cursorPosition++;
            }
        } else if (ChatAllowedCharacters.isAllowedCharacter(typedChar)) {
            text = text.substring(0, cursorPosition) + typedChar + text.substring(cursorPosition);
            cursorPosition++;
        }
        lastCursorToggle = System.currentTimeMillis(); // Reset cursor blink on key press
    }

    @Override
    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        this.cursorPosition = text.length();
    }

    public boolean isFocused() {
        return focused;
    }
}
