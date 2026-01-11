package myau.module.modules;

import myau.Myau;
import myau.enums.BlinkModules;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.Render2DEvent;
import myau.events.TickEvent;
import myau.mixin.IAccessorGuiChat;
import myau.module.Category;
import myau.module.Module;
import myau.property.properties.*;
import myau.util.AnimationUtil; // 确保导入了 AnimationUtil
import myau.util.ColorUtil;
import myau.util.RenderUtil;
import myau.util.font.FontManager;
import myau.util.font.impl.FontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class HUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    // --- 动画状态存储 ---
    private final Map<Module, Float> animationMap = new HashMap<>();

    // --- 新增：字体选择属性 ---
    public final ModeProperty fontMode = new ModeProperty("font", 1, new String[]{"MINECRAFT", "PRODUCT_SANS", "REGULAR", "TENACITY", "VISION", "NBP_INFORMA", "TAHOMA_BOLD"});

    // --- 新增：动画属性 ---
    public final ModeProperty animationMode = new ModeProperty("animation", 1, new String[]{"NONE", "SLIDE", "SCALE", "FADE", "ZIPPER"});
    public final FloatProperty animationSpeed = new FloatProperty("anim-speed", 1.0F, 0.1F, 10.0F);

    public final ModeProperty colorMode = new ModeProperty(
            "color", 3, new String[]{"RAINBOW", "CHROMA", "ASTOLFO", "CUSTOM1", "CUSTOM12", "CUSTOM123"}
    );
    public final FloatProperty colorSpeed = new FloatProperty("color-speed", 1.0F, 0.5F, 1.5F);
    public final PercentProperty colorSaturation = new PercentProperty("color-saturation", 50);
    public final PercentProperty colorBrightness = new PercentProperty("color-brightness", 100);
    public final ColorProperty custom1 = new ColorProperty("custom-color-1", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 3 || this.colorMode.getValue() == 4 || this.colorMode.getValue() == 5);
    public final ColorProperty custom2 = new ColorProperty("custom-color-2", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 4 || this.colorMode.getValue() == 5);
    public final ColorProperty custom3 = new ColorProperty("custom-color-3", Color.WHITE.getRGB(), () -> this.colorMode.getValue() == 5);
    public final ModeProperty posX = new ModeProperty("position-x", 1, new String[]{"LEFT", "RIGHT"});
    public final ModeProperty posY = new ModeProperty("position-y", 0, new String[]{"TOP", "BOTTOM"});
    public final IntProperty offsetX = new IntProperty("offset-x", 2, 0, 255);
    public final IntProperty offsetY = new IntProperty("offset-y", 2, 0, 255);
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 1.5F);
    public final PercentProperty background = new PercentProperty("background", 25);
    public final BooleanProperty showBar = new BooleanProperty("bar", true);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final BooleanProperty suffixes = new BooleanProperty("suffixes", true);
    public final BooleanProperty lowerCase = new BooleanProperty("lower-case", false);
    public final BooleanProperty chatOutline = new BooleanProperty("chat-outline", true);
    public final BooleanProperty blinkTimer = new BooleanProperty("blink-timer", true);
    public final BooleanProperty toggleSound = new BooleanProperty("toggle-sounds", true);

    public HUD() {
        super("HUD", "Heads-up display.", Category.RENDER, 0, true, true);
    }

    // --- 字体辅助方法 Start ---
    private FontRenderer getCustomFont() {
        switch (fontMode.getValue()) {
            case 1: if (FontManager.productSans20 != null) return FontManager.productSans20; break;
            case 2: if (FontManager.regular22 != null) return FontManager.regular22; break;
            case 3: if (FontManager.tenacity20 != null) return FontManager.tenacity20; break;
            case 4: if (FontManager.vision20 != null) return FontManager.vision20; break;
            case 5: if (FontManager.nbpInforma20 != null) return FontManager.nbpInforma20; break;
            case 6: if (FontManager.tahomaBold20 != null) return FontManager.tahomaBold20; break;
        }
        return null;
    }

    private float getFontHeight() {
        FontRenderer fr = getCustomFont();
        if (fr != null) return (float) fr.getHeight();
        return mc.fontRendererObj.FONT_HEIGHT;
    }

    private float getStringWidth(String text) {
        FontRenderer fr = getCustomFont();
        if (fr != null) return (float) fr.getStringWidth(text);
        return mc.fontRendererObj.getStringWidth(text);
    }

    private void drawString(String text, float x, float y, int color, boolean shadow) {
        FontRenderer fr = getCustomFont();
        if (fr != null) {
            if (shadow) fr.drawStringWithShadow(text, x, y, color);
            else fr.drawString(text, x, y, color);
        } else {
            if (shadow) mc.fontRendererObj.drawStringWithShadow(text, x, y, color);
            else mc.fontRendererObj.drawString(text, x, y, color, false);
        }
    }
    // --- 字体辅助方法 End ---

    private String getModuleName(Module module) {
        String moduleName = module.getName();
        if (this.lowerCase.getValue()) moduleName = moduleName.toLowerCase(Locale.ROOT);
        return moduleName;
    }

    private String[] getModuleSuffix(Module module) {
        String[] moduleSuffix = module.getSuffix();
        if (this.lowerCase.getValue()) {
            for (int i = 0; i < moduleSuffix.length; i++) moduleSuffix[i] = moduleSuffix[i].toLowerCase();
        }
        return moduleSuffix;
    }

    private int getModuleWidth(Module module) {
        return (int) this.calculateStringWidth(getModuleName(module), getModuleSuffix(module));
    }

    private float calculateStringWidth(String string, String[] arr) {
        float width = getStringWidth(string);
        if (this.suffixes.getValue()) {
            for (String str : arr) width += 3 + getStringWidth(str);
        }
        return width;
    }

    private float getColorCycle(long time, long offset) {
        long speed = (long) (3000.0 / Math.pow(Math.min(Math.max(0.5F, this.colorSpeed.getValue()), 1.5F), 3.0));
        return 1.0F - (float) (Math.abs(time - offset * 300L) % speed) / (float) speed;
    }

    public Color getColor(long time) {
        return this.getColor(time, 0L);
    }

    public Color getColor(long time, long offset) {
        Color color = Color.white;
        switch (this.colorMode.getValue()) {
            case 0: color = ColorUtil.fromHSB(this.getColorCycle(time, offset), 1.0F, 1.0F); break;
            case 1: color = ColorUtil.fromHSB(this.getColorCycle(time / 3L, 0L), 1.0F, 1.0F); break;
            case 2:
                float cycle = this.getColorCycle(time, offset);
                if (cycle % 1.0F < 0.5F) cycle = 1.0F - cycle % 1.0F;
                color = ColorUtil.fromHSB(cycle, 1.0F, 1.0F);
                break;
            case 3: color = new Color(this.custom1.getValue()); break;
            case 4:
                double cycle1 = this.getColorCycle(time, offset);
                color = ColorUtil.interpolate((float) (2.0 * Math.abs(cycle1 - Math.floor(cycle1 + 0.5))), new Color(this.custom1.getValue()), new Color(this.custom2.getValue()));
                break;
            case 5:
                double cycle2 = this.getColorCycle(time, offset);
                float floor = (float) (2.0 * Math.abs(cycle2 - Math.floor(cycle2 + 0.5)));
                if (floor <= 0.5F) color = ColorUtil.interpolate(floor * 2.0F, new Color(this.custom1.getValue()), new Color(this.custom2.getValue()));
                else color = ColorUtil.interpolate((floor - 0.5F) * 2.0F, new Color(this.custom2.getValue()), new Color(this.custom3.getValue()));
                break;
        }
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        return Color.getHSBColor(hsb[0], hsb[1] * (this.colorSaturation.getValue().floatValue() / 100.0F), hsb[2] * (this.colorBrightness.getValue().floatValue() / 100.0F));
    }

    // 移除 onTick 中的列表排序逻辑，将其移动到 Render2D 以保持动画流畅
    @EventTarget
    public void onTick(TickEvent event) {
        // 空方法，保留以防未来需要
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        // --- 聊天框描边逻辑 ---
        if (this.chatOutline.getValue() && mc.currentScreen instanceof GuiChat) {
            String text = ((IAccessorGuiChat) mc.currentScreen).getInputField().getText().trim();
            if (Myau.commandManager != null && Myau.commandManager.isTypingCommand(text)) {
                RenderUtil.enableRenderState();
                RenderUtil.drawOutlineRect(2.0F, (float) (mc.currentScreen.height - 14), (float) (mc.currentScreen.width - 2), (float) (mc.currentScreen.height - 2), 1.5F, 0, this.getColor(System.currentTimeMillis(), 0).getRGB());
                RenderUtil.disableRenderState();
            }
        }

        if (this.isEnabled() && !mc.gameSettings.showDebugInfo) {
            float fontHeight = getFontHeight() - 1.0F;
            if (getCustomFont() != null) fontHeight += 2.0F;

            // --- 核心动画状态更新逻辑 ---
            // 遍历所有模块，更新动画值
            for (Module module : Myau.moduleManager.modules.values()) {
                // 目标值：开启且未隐藏为 1，否则为 0
                float target = module.isEnabled() && !module.isHidden() ? 1.0F : 0.0F;
                float current = animationMap.getOrDefault(module, 0.0F);

                // 使用 AnimationUtil 进行平滑过渡
                current = AnimationUtil.animate(target, current, this.animationSpeed.getValue() * 0.05F);

                // 限制范围防止溢出
                if (Math.abs(current - target) < 0.01F) current = target;

                animationMap.put(module, current);
            }

            // 获取需要渲染的模块列表（动画值 > 0 的模块）
            List<Module> renderList = Myau.moduleManager.modules.values().stream()
                    .filter(m -> animationMap.getOrDefault(m, 0.0F) > 0.01F)
                    .sorted(Comparator.comparingInt(this::getModuleWidth).reversed())
                    .collect(Collectors.toList());

            // --- 渲染计算 ---
            float startX = (float) this.offsetX.getValue() + (1.0F + (this.showBar.getValue() ? (this.shadow.getValue() ? 2.0F : 1.0F) : 0.0F)) * this.scale.getValue();
            float startY = (float) this.offsetY.getValue() + 1.0F * this.scale.getValue();

            if (this.posX.getValue() == 1) startX = (float) new ScaledResolution(mc).getScaledWidth() - startX;
            if (this.posY.getValue() == 1) startY = (float) new ScaledResolution(mc).getScaledHeight() - startY - fontHeight * this.scale.getValue();

            GlStateManager.pushMatrix();
            GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 0.0F);

            long l = System.currentTimeMillis();
            long offset = 0L;
            float currentY = startY;

            // --- 渲染循环 ---
            for (Module module : renderList) {
                float animValue = animationMap.getOrDefault(module, 0.0F);
                if (animValue <= 0.01F) continue; // 双重检查

                String moduleName = this.getModuleName(module);
                String[] moduleSuffix = this.getModuleSuffix(module);
                float totalWidth = (float) (this.calculateStringWidth(moduleName, moduleSuffix) - (this.shadow.getValue() ? 0 : 1));

                // 动态高度（用于 Zipper 动画）
                float itemHeight = (fontHeight + (this.shadow.getValue() ? 1.0F : 0.0F)) * this.scale.getValue();

                // 获取颜色
                int color = this.getColor(l, offset).getRGB();

                // 应用透明度（Fade 动画）
                if (this.animationMode.getValue() == 3) { // FADE
                    color = ColorUtil.applyOpacity(color, animValue);
                }

                GlStateManager.pushMatrix();

                // --- 动画变换应用区域 ---
                float xPos = currentY / this.scale.getValue(); // 这里只是个临时变量名，实际是Y轴逻辑
                float realX = startX / this.scale.getValue();
                float realY = currentY / this.scale.getValue();

                switch (this.animationMode.getValue()) {
                    case 1: // SLIDE (从侧边滑入)
                        float slideDist = (totalWidth + 10) * (1.0F - animValue);
                        if (this.posX.getValue() == 1) { // Right
                            GlStateManager.translate(slideDist, 0, 0);
                        } else { // Left
                            GlStateManager.translate(-slideDist, 0, 0);
                        }
                        break;
                    case 2: // SCALE (缩放弹出)
                        // 计算中心点以实现中心缩放效果
                        float centerX = realX + (this.posX.getValue() == 1 ? -totalWidth / 2 : totalWidth / 2);
                        float centerY = realY + fontHeight / 2;

                        GlStateManager.translate(centerX, centerY, 0);
                        GlStateManager.scale(animValue, animValue, 1.0F);
                        GlStateManager.translate(-centerX, -centerY, 0);
                        break;
                    case 3: // FADE (透明度) - 已在上面处理颜色
                        // 无额外GL变换
                        break;
                    case 4: // ZIPPER (垂直挤压/展开)
                        GlStateManager.translate(0, (itemHeight * (1.0F - animValue)) * (this.posY.getValue() == 1 ? -0.5 : 0.5), 0);
                        GlStateManager.scale(1.0F, animValue, 1.0F);
                        break;
                }

                RenderUtil.enableRenderState();

                // 背景
                if (this.background.getValue() > 0) {
                    int bgColor = new Color(0.0F, 0.0F, 0.0F, this.background.getValue().floatValue() / 100.0F).getRGB();
                    if (this.animationMode.getValue() == 3) bgColor = ColorUtil.applyOpacity(bgColor, animValue);

                    RenderUtil.drawRect(
                            realX - 1.0F - (this.posX.getValue() == 0 ? 0.0F : totalWidth),
                            realY - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : (this.shadow.getValue() ? 1.0F : 0.0F)),
                            realX + 1.0F + (this.posX.getValue() == 0 ? totalWidth : 0.0F),
                            realY + fontHeight + (this.posY.getValue() == 0 ? (this.shadow.getValue() ? 1.0F : 0.0F) : (offset == 0L ? 1.0F : 0.0F)),
                            bgColor
                    );
                }

                // 侧边条
                if (this.showBar.getValue()) {
                    float barTop = realY - (this.posY.getValue() == 0 ? (offset == 0L ? 1.0F : 0.0F) : 0.0F);
                    float barBottom = realY + fontHeight + (this.posY.getValue() == 0 ? 0.0F : (offset == 0L ? 1.0F : 0.0F));
                    float xBase = realX;

                    if (this.shadow.getValue()) {
                        RenderUtil.drawRect(xBase + (this.posX.getValue() == 0 ? -3.0F : 1.0F), barTop - 1.0f, xBase + (this.posX.getValue() == 0 ? -2.0F : 2.0F), barBottom + 1.0f, color);
                        RenderUtil.drawRect(xBase + (this.posX.getValue() == 0 ? -2.0F : 2.0F), barTop - 1.0f, xBase + (this.posX.getValue() == 0 ? -1.0F : 3.0F), barBottom + 1.0f, (color & 16579836) >> 2 | color & 0xFF000000);
                    } else {
                        RenderUtil.drawRect(xBase + (this.posX.getValue() == 0 ? -2.0F : 1.0F), barTop, xBase + (this.posX.getValue() == 0 ? -1.0F : 2.0F), barBottom, color);
                    }
                }

                RenderUtil.disableRenderState();
                GlStateManager.disableDepth();

                // 文字
                float textX = realX - (this.posX.getValue() == 1 ? totalWidth : 0.0F);
                float textY = realY + (this.posY.getValue() == 1 ? 1.0F : 0.0F);

                this.drawString(moduleName, textX, textY, color, this.shadow.getValue());

                if (this.suffixes.getValue() && moduleSuffix.length > 0) {
                    float width = getStringWidth(moduleName) + 3.0F;
                    int suffixColor = ChatColors.GRAY.toAwtColor();
                    if (this.animationMode.getValue() == 3) suffixColor = ColorUtil.applyOpacity(suffixColor, animValue);

                    for (String string : moduleSuffix) {
                        this.drawString(string, textX + width, textY, suffixColor, this.shadow.getValue());
                        width += getStringWidth(string) + (this.shadow.getValue() ? 3.0F : 2.0F);
                    }
                }

                GlStateManager.popMatrix(); // 结束当前模块的变换

                // 更新 Y 轴位置 (根据动画值动态调整间距，实现 Zipper 效果，或者平滑移动)
                float offsetHeight = itemHeight * (this.animationMode.getValue() == 4 ? animValue : 1.0F);
                currentY += offsetHeight * (this.posY.getValue() == 0 ? 1.0F : -1.0F);

                offset++;
            }

            // Blink Timer 渲染 (保持原样)
            if (this.blinkTimer.getValue()) {
                BlinkModules blinkingModule = Myau.blinkManager.getBlinkingModule();
                if (blinkingModule != BlinkModules.NONE && blinkingModule != BlinkModules.AUTO_BLOCK) {
                    long movementPacketSize = Myau.blinkManager.countMovement();
                    if (movementPacketSize > 0L) {
                        GlStateManager.enableBlend();
                        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                        String blinkText = String.valueOf(movementPacketSize);
                        float blinkX = (float) new ScaledResolution(mc).getScaledWidth() / 2.0F / this.scale.getValue()
                                - getStringWidth(blinkText) / 2.0F;
                        float blinkY = (float) new ScaledResolution(mc).getScaledHeight() / 5.0F * 3.0F / this.scale.getValue();

                        this.drawString(blinkText, blinkX, blinkY, this.getColor(l, offset).getRGB() & 16777215 | -1090519040, this.shadow.getValue());
                        GlStateManager.disableBlend();
                    }
                }
            }
            GlStateManager.enableDepth();
            GlStateManager.popMatrix();
        }
    }
}