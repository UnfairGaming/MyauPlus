package myau.ui.impl.clickgui.component;

import myau.ui.impl.clickgui.MaterialTheme;
import myau.util.RenderUtil;

public class SearchBar extends Component {

    private final TextField textField;
    private boolean focused = false;
    private long focusTime = 0;
    private static final long FOCUS_ANIMATION_DURATION = 200L; // ms for the click animation

    public SearchBar(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.textField = new TextField(x + 10, y + 5, width - 20, height - 10, "Search modules...");
        this.textField.setDrawBackground(false); // Disable the inner background
    }

    public void updatePosition(int x, int y) {
        this.x = x;
        this.y = y;
        this.textField.setY(y + (this.height - this.textField.getHeight()) / 2);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        render(mouseX, mouseY, partialTicks, 1.0f);
    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset) {
        // Opening animation (eased)
        float openProgress = 1.0f - (float) Math.pow(1.0f - animationProgress, 4);
        if (openProgress <= 0) return;

        // Click/Focus animation (eased)
        long focusElapsedTime = System.currentTimeMillis() - focusTime;
        float focusProgress = Math.min(1.0f, (float) focusElapsedTime / (float) FOCUS_ANIMATION_DURATION);
        float easedFocusProgress = 1.0f - (float) Math.pow(1.0f - focusProgress, 3);

        // Determine scale based on focus state and animation progress
        float scale;
        if (this.focused) {
            scale = 1.0f + 0.05f * easedFocusProgress; // Scale up to 105%
        } else {
            scale = 1.05f - 0.05f * easedFocusProgress; // Scale back down from 105%
        }

        float animatedWidth = this.width * openProgress * scale;
        float animatedHeight = this.height * scale;

        // Center the expanding and scaling bar
        float currentX = this.x + (this.width - animatedWidth) / 2.0f;
        float currentY = this.y + (this.height - animatedHeight) / 2.0f;

        // Background
        RenderUtil.drawRoundedRect(currentX, currentY, animatedWidth, animatedHeight, MaterialTheme.CORNER_RADIUS_SMALL, MaterialTheme.getRGB(MaterialTheme.SURFACE_CONTAINER_LOW), true, true, true, true);
        
        // Border - only show when focused to guide the user
        if (this.focused) {
            RenderUtil.drawRoundedRectOutline(currentX, currentY, animatedWidth, animatedHeight, MaterialTheme.CORNER_RADIUS_SMALL, 1.0f, MaterialTheme.getRGB(MaterialTheme.PRIMARY_COLOR), true, true, true, true);
        }

        // Render the text field only when the bar is almost fully expanded
        if (openProgress > 0.9f) {
            // Adjust text field position to be inside the animated bar
            this.textField.setX((int)(currentX + 5 * scale));
            this.textField.setWidth((int)(animatedWidth - 10 * scale));
            this.textField.render(mouseX, mouseY, partialTicks, 1.0f, isLast, scrollOffset);
        }
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton) {
        boolean isOver = isMouseOver(mouseX, mouseY);
        if (isOver && mouseButton == 0) {
            if (!this.focused) {
                this.focused = true;
                this.focusTime = System.currentTimeMillis();
            }
            return textField.mouseClicked(mouseX, mouseY, mouseButton);
        } else {
            if (this.focused) {
                this.focused = false;
                this.focusTime = System.currentTimeMillis();
            }
            return textField.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        textField.mouseReleased(mouseX, mouseY, mouseButton);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (this.focused) {
            textField.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    public String getText() {
        return textField.getText();
    }

    public boolean isFocused() {
        return this.focused;
    }
}
