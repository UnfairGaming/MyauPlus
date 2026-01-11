package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.module.Category;
import myau.module.Module;
import myau.util.font.FontManager;
import myau.util.font.impl.FontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;

public class WaterMark extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public WaterMark() {
        super("WaterMark", "Just watermark,hmm", Category.RENDER, 0, false, false);
    }

    // --- 字体辅助方法 Start ---

    /**
     * 从 HUD 模块获取当前选中的字体渲染器
     */
    private FontRenderer getCustomFont() {
        HUD hud = (HUD) Myau.moduleManager.getModule("HUD");
        if (hud != null) {
            switch (hud.fontMode.getValue()) {
                case 1: 
                    if (FontManager.productSans20 != null) return FontManager.productSans20;
                    break;
                case 2: 
                    if (FontManager.regular22 != null) return FontManager.regular22;
                    break;
                case 3: 
                    if (FontManager.tenacity20 != null) return FontManager.tenacity20;
                    break;
                case 4: 
                    if (FontManager.vision20 != null) return FontManager.vision20;
                    break;
                case 5: 
                    if (FontManager.nbpInforma20 != null) return FontManager.nbpInforma20;
                    break;
                case 6: 
                    if (FontManager.tahomaBold20 != null) return FontManager.tahomaBold20;
                    break;
                // 0 是 MINECRAFT，返回 null
            }
        }
        return null;
    }

    private float getStringWidth(String text) {
        FontRenderer fr = getCustomFont();
        if (fr != null) {
            return (float) fr.getStringWidth(text);
        }
        return mc.fontRendererObj.getStringWidth(text);
    }

    private void drawStringWithShadow(String text, float x, float y, int color) {
        FontRenderer fr = getCustomFont();
        if (fr != null) {
            fr.drawStringWithShadow(text, x, y, color);
        } else {
            mc.fontRendererObj.drawStringWithShadow(text, x, y, color);
        }
    }

    // --- 字体辅助方法 End ---

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        if (this.isEnabled()) {
            // 获取FPS和延迟
            int fps = Minecraft.getDebugFPS();
            int ping = 0;

            if (mc.thePlayer != null && mc.theWorld != null) {
                if (mc.thePlayer.sendQueue != null && mc.thePlayer.sendQueue.getPlayerInfo(mc.thePlayer.getUniqueID()) != null) {
                    ping = mc.thePlayer.sendQueue.getPlayerInfo(mc.thePlayer.getUniqueID()).getResponseTime();
                }
            }

            // 构建文本部分
            String exhibitionText = "E";
            String restText = "xhibition ";
            String fpsValue = fps + "FPS";
            String pingValue = ping + "ms";

            // 获取HUD模块以获取颜色设置
            HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);

            // 计算位置 (左上角)
            float x = 2.0f;
            float y = 2.0f;

            // 修正自定义字体可能的垂直偏移
            if (getCustomFont() != null) {
                y += 1.0f;
            }

            GlStateManager.pushMatrix();

            // 1. 绘制 "E" 使用 HUD 颜色
            long time = System.currentTimeMillis();
            int rainbowColor = hud != null ? hud.getColor(time).getRGB() : 0xFFFFFFFF;
            drawStringWithShadow(exhibitionText, x, y, rainbowColor);

            float currentX = x + getStringWidth(exhibitionText);

            // 2. 绘制 "xhibition " 使用白色
            int whiteColor = 0xFFFFFFFF;
            drawStringWithShadow(restText, currentX, y, whiteColor);
            currentX += getStringWidth(restText);

            // 3. 绘制 "[" 使用灰色
            int grayColor = 0xFFAAAAAA;
            drawStringWithShadow("[", currentX, y, grayColor);
            currentX += getStringWidth("[");

            // 4. 绘制 FPS数值 使用白色
            drawStringWithShadow(fpsValue, currentX, y, whiteColor);
            currentX += getStringWidth(fpsValue);

            // 5. 绘制 "]" 使用灰色
            drawStringWithShadow("]", currentX, y, grayColor);
            currentX += getStringWidth("]");

            // 6. 绘制空格
            String space = " ";
            drawStringWithShadow(space, currentX, y, whiteColor);
            currentX += getStringWidth(space);

            // 7. 绘制 "[" 使用灰色
            drawStringWithShadow("[", currentX, y, grayColor);
            currentX += getStringWidth("[");

            // 8. 绘制 Ping数值 使用白色
            drawStringWithShadow(pingValue, currentX, y, whiteColor);
            currentX += getStringWidth(pingValue);

            // 9. 绘制 "]" 使用灰色
            drawStringWithShadow("]", currentX, y, grayColor);

            GlStateManager.popMatrix();
        }
    }
}