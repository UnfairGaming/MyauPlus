package myau.ui.impl.clickgui;

import myau.Myau;
import myau.module.Category;
import myau.module.Module;
import myau.module.modules.ClickGUIModule;
import myau.ui.impl.clickgui.component.SearchBar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;

public class ClickGuiScreen extends GuiScreen {
    private static ClickGuiScreen instance;
    private ArrayList<Frame> frames;
    private Frame draggingComponent = null;

    private int scrollY = 0;
    private int targetScrollY = 0;
    private double velocity = 0;
    private static final double FRICTION = 0.85;
    private static final double SNAP_STRENGTH = 0.15;

    // Animation states
    private boolean isClosing = false;
    private long openTime = 0L;
    private static final long ANIMATION_DURATION = 250L; // 稍微放慢一点显得更高级
    private static final long STAGGER_DELAY = 40L;

    // Search Bar
    private SearchBar searchBar;
    private String searchQuery = "";

    public ClickGuiScreen() {
        this.frames = new ArrayList<>();
        // 初始位置，后面会自动居中
        this.searchBar = new SearchBar(0, 15, 140, 22);

        int currentX = 20;
        int currentY = 50;
        int frameWidth = 110; // 稍微窄一点更精致
        int frameHeight = 24;

        for (Category category : Category.values()) {
            Frame frame = new Frame(category, currentX, currentY, frameWidth, frameHeight);
            this.frames.add(frame);
            currentX += (frameWidth + 15);
        }
    }

    public static ClickGuiScreen getInstance() {
        if (instance == null) {
            instance = new ClickGuiScreen();
        }
        return instance;
    }

    @Override
    public void initGui() {
        super.initGui();
        this.isClosing = false;
        this.openTime = System.currentTimeMillis();
        // 重置滚动
        this.scrollY = 0;
        this.targetScrollY = 0;
        this.velocity = 0;

        for (Frame frame : frames) {
            frame.refilter(this.searchQuery);
        }
    }

    public void close() {
        if (isClosing) return;
        this.isClosing = true;
        this.openTime = System.currentTimeMillis();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        updateScroll();

        // Handle Search Query
        String newQuery = searchBar.getText();
        if (!newQuery.equals(this.searchQuery)) {
            this.searchQuery = newQuery;
            for (Frame frame : frames) {
                frame.refilter(this.searchQuery);
            }
        }

        long elapsedTime = System.currentTimeMillis() - openTime;
        long totalAnimationTime = (frames.size() * STAGGER_DELAY) + ANIMATION_DURATION;

        if (isClosing && elapsedTime > ANIMATION_DURATION) {
            mc.displayGuiScreen(null);
            return;
        }

        // Render Frames
        for (int i = 0; i < frames.size(); i++) {
            int frameIndex = isClosing ? (frames.size() - 1 - i) : i;
            Frame frame = frames.get(frameIndex);

            // 简单的交错动画
            long startTime = i * STAGGER_DELAY;

            float progress = 0f;
            if (elapsedTime >= startTime) {
                long animationElapsedTime = elapsedTime - startTime;
                progress = Math.min(1.0f, (float) animationElapsedTime / (float) ANIMATION_DURATION);
            }

            float finalProgress = isClosing ? (1.0f - Math.min(1.0f, (float)elapsedTime / ANIMATION_DURATION)) : progress;

            if (finalProgress > 0) {
                frame.render(mouseX, mouseY, partialTicks, finalProgress, false, scrollY);
            }
        }

        // SearchBar
        ScaledResolution sr = new ScaledResolution(mc);
        searchBar.updatePosition(sr.getScaledWidth() / 2 - searchBar.getWidth() / 2, searchBar.getY());

        float searchBarProgress = Math.min(1.0f, (float) elapsedTime / (float) ANIMATION_DURATION);
        if (isClosing) searchBarProgress = 1.0f - searchBarProgress;

        searchBar.render(mouseX, mouseY, partialTicks, searchBarProgress);

        // InvWalk
        try {
            Module invWalkModule = Myau.moduleManager.getModule("InvWalk");
            if (invWalkModule != null && invWalkModule.isEnabled()) {
                handleInvWalk();
            }
        } catch (Exception ignored) {}

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void handleInvWalk() {
        KeyBinding[] keys = {
                mc.gameSettings.keyBindForward, mc.gameSettings.keyBindBack,
                mc.gameSettings.keyBindLeft, mc.gameSettings.keyBindRight,
                mc.gameSettings.keyBindJump, mc.gameSettings.keyBindSprint,
                mc.gameSettings.keyBindSneak
        };
        for (KeyBinding key : keys) {
            KeyBinding.setKeyBindState(key.getKeyCode(), Keyboard.isKeyDown(key.getKeyCode()));
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        if (isClosing) return;
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) {
            velocity += wheel > 0 ? -30 : 30;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (isClosing) return;
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (searchBar.mouseClicked(mouseX, mouseY, mouseButton)) return;

        // 倒序遍历，保证点击最上层的 Frame
        for (int i = frames.size() - 1; i >= 0; i--) {
            Frame frame = frames.get(i);
            if (frame.mouseClicked(mouseX, mouseY, mouseButton, scrollY)) {
                draggingComponent = frame;
                // 把点击的 Frame 移到列表最后，这样渲染时就在最上层
                frames.remove(i);
                frames.add(frame);
                return;
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (isClosing) return;
        super.mouseReleased(mouseX, mouseY, state);

        if (draggingComponent != null) {
            draggingComponent.mouseReleased(mouseX, mouseY, state, scrollY);
            draggingComponent = null;
        }

        for (Frame frame : frames) {
            frame.mouseReleased(mouseX, mouseY, state, scrollY);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (isClosing) return;
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (draggingComponent != null) {
            draggingComponent.updatePosition(mouseX, mouseY, scrollY);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (isClosing) return;

        if (System.currentTimeMillis() - this.openTime < 100) return;

        if (searchBar.isFocused()) {
            searchBar.keyTyped(typedChar, keyCode);
            return;
        }

        // --- 新增：检查是否有组件正在绑定按键 ---
        boolean isBindingKey = false;
        for (Frame frame : frames) {
            // 假设 Frame 有方法可以检查内部是否有 KeybindComponent 处于 binding 状态
            // 或者简单一点，先让 Frame 把事件传下去
            if (frame.isAnyComponentBinding()) {
                isBindingKey = true;
                break;
            }
        }

        // 如果正在绑定按键，不要处理关闭逻辑，直接传给子组件
        if (isBindingKey) {
            for (Frame frame : frames) {
                frame.keyTyped(typedChar, keyCode);
            }
            return;
        }
        // ----------------------------------------

        Module clickGUIModule = Myau.moduleManager.getModule("ClickGUI");
        if (keyCode == Keyboard.KEY_ESCAPE || (clickGUIModule != null && keyCode == clickGUIModule.getKey())) {
            close();
            return;
        }

        for (Frame frame : frames) {
            frame.keyTyped(typedChar, keyCode);
        }
    }

    @Override
    public boolean doesGuiPauseGame() { return false; }

    private void updateScroll() {
        targetScrollY += (int)velocity;
        velocity *= FRICTION;
        int maxScroll = getMaxScroll();
        targetScrollY = Math.max(0, Math.min(targetScrollY, maxScroll));
        int delta = targetScrollY - scrollY;
        scrollY += (int)(delta * SNAP_STRENGTH);
        if (Math.abs(velocity) < 0.5) velocity = 0;
        if (Math.abs(delta) < 1 && Math.abs(velocity) < 0.5) scrollY = targetScrollY;
    }

    private int getMaxScroll() {
        int max = 0;
        for (Frame frame : frames) {
            int bottom = frame.getY() + frame.getTotalHeight();
            if (bottom > max) max = bottom;
        }
        ScaledResolution sr = new ScaledResolution(mc);
        return Math.max(0, max - sr.getScaledHeight() + 20);
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        ClickGUIModule guiModule = (ClickGUIModule) Myau.moduleManager.getModule("ClickGUI");
        if (guiModule != null) {
            guiModule.setEnabled(false);
        }
    }
}