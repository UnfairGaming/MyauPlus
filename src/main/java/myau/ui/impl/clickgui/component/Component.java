package myau.ui.impl.clickgui.component;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;

public abstract class Component {
    protected final Minecraft mc = Minecraft.getMinecraft();
    @Setter
    @Getter
    public int x;
    @Setter
    @Getter
    public int y;
    @Setter
    @Getter
    public int width;
    @Setter
    @Getter
    public int height;

    public Component(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    // 将此方法改为非抽象，默认调用带 deltaTime 的版本 (兼容性)
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset) {
        render(mouseX, mouseY, partialTicks, animationProgress, isLast, scrollOffset, 0.016f);
    }

    // 将此方法改为 abstract，强制子类实现带 deltaTime 的动画逻辑
    public abstract void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast, int scrollOffset, float deltaTime);

    public void render(int mouseX, int mouseY, float partialTicks) {
        render(mouseX, mouseY, partialTicks, 1.0f, false, 0);
    }
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress) {
        render(mouseX, mouseY, partialTicks, animationProgress, false, 0);
    }
    public void render(int mouseX, int mouseY, float partialTicks, float animationProgress, boolean isLast) {
        render(mouseX, mouseY, partialTicks, animationProgress, isLast, 0);
    }

    public boolean mouseClicked(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        return isMouseOver(mouseX, mouseY, scrollOffset) && mouseClicked(mouseX, mouseY, mouseButton);
    }
    public abstract boolean mouseClicked(int mouseX, int mouseY, int mouseButton);
    public abstract void mouseReleased(int mouseX, int mouseY, int mouseButton);
    public void mouseReleased(int mouseX, int mouseY, int mouseButton, int scrollOffset) {
        mouseReleased(mouseX, mouseY, mouseButton);
    }
    public abstract void keyTyped(char typedChar, int keyCode);
    public boolean isMouseOver(int mouseX, int mouseY, int scrollOffset) {
        int actualY = this.y - scrollOffset;
        return mouseX >= x && mouseX <= x + width && mouseY >= actualY && mouseY <= actualY + height;
    }

}