package myau.ui.impl.gui;

import myau.util.ShaderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

import java.awt.*;

public class BackgroundRenderer {
    private static ShaderUtil backgroundShader;
    private static long initTime = System.currentTimeMillis();
    public static int currentBackgroundIndex = 1;

    public static void init() {
        if (backgroundShader == null) {
            reloadShader(currentBackgroundIndex);
        }
    }

    public static void reloadShader(int index) {
        currentBackgroundIndex = index;
        String pathFsh = "myau/shader/background" + index + ".fsh";
        backgroundShader = new ShaderUtil(pathFsh);

        if (backgroundShader.getProgramID() == 0) {
            String pathFrag = "myau/shader/background" + index + ".frag";
            backgroundShader = new ShaderUtil(pathFrag);
        }

        initTime = System.currentTimeMillis();
    }

    /**
     * 绘制 Shader 背景
     * @param width 当前 GUI 的缩放宽度 (this.width)
     * @param height 当前 GUI 的缩放高度 (this.height)
     */
    public static void draw(int width, int height, int mouseX, int mouseY) {
        if (backgroundShader != null && backgroundShader.getProgramID() != 0) {
            GlStateManager.disableCull();
            GlStateManager.disableAlpha();

            backgroundShader.init();

            float time = (System.currentTimeMillis() - initTime) / 1000f;
            backgroundShader.setUniformf("time", time);

            // --- 核心修复 ---
            // 传给 Shader 真实的物理分辨率，而不是 GUI 的缩放分辨率
            Minecraft mc = Minecraft.getMinecraft();
            backgroundShader.setUniformf("resolution", (float) mc.displayWidth, (float) mc.displayHeight);
            // ----------------

            // 绘制铺满 GUI 屏幕的四边形
            // 这里的坐标依然使用 width/height，因为 Minecraft 的投影矩阵是缩放过的
            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            worldrenderer.begin(7, DefaultVertexFormats.POSITION);
            worldrenderer.pos(0.0D, height, 0.0D).endVertex();
            worldrenderer.pos(width, height, 0.0D).endVertex();
            worldrenderer.pos(width, 0.0D, 0.0D).endVertex();
            worldrenderer.pos(0.0D, 0.0D, 0.0D).endVertex();
            tessellator.draw();

            backgroundShader.unload();

            GlStateManager.enableAlpha();
            GlStateManager.enableCull();
        } else {
            drawGradient(width, height, new Color(20, 20, 30).getRGB(), new Color(5, 5, 10).getRGB());
        }
    }

    private static void drawGradient(int width, int height, int topColor, int bottomColor) {
        float f = (float)(topColor >> 24 & 255) / 255.0F;
        float f1 = (float)(topColor >> 16 & 255) / 255.0F;
        float f2 = (float)(topColor >> 8 & 255) / 255.0F;
        float f3 = (float)(topColor & 255) / 255.0F;
        float f4 = (float)(bottomColor >> 24 & 255) / 255.0F;
        float f5 = (float)(bottomColor >> 16 & 255) / 255.0F;
        float f6 = (float)(bottomColor >> 8 & 255) / 255.0F;
        float f7 = (float)(bottomColor & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(width, 0.0D, 0.0D).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos(0.0D, 0.0D, 0.0D).color(f1, f2, f3, f).endVertex();
        worldrenderer.pos(0.0D, height, 0.0D).color(f5, f6, f7, f4).endVertex();
        worldrenderer.pos(width, height, 0.0D).color(f5, f6, f7, f4).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }
}