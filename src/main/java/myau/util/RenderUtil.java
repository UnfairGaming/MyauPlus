package myau.util;

import myau.enums.ChatColors;
import myau.mixin.IAccessorEntityRenderer;
import myau.mixin.IAccessorMinecraft;
import myau.mixin.IAccessorRenderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.entity.Entity;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import javax.vecmath.Vector3d;
import javax.vecmath.Vector4d;
import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

public class RenderUtil {
    private static Minecraft mc;
    private static Frustum cameraFrustum;
    private static IntBuffer viewportBuffer;
    private static FloatBuffer modelViewBuffer;
    private static FloatBuffer projectionBuffer;
    private static FloatBuffer vectorBuffer;
    private static Map<Integer, EnchantmentData> enchantmentMap;

    // Shader 实例 (保留用于绘制背景等)
    private static final ShaderUtil roundedShader = new ShaderUtil("myau/shader/roundedRect.fsh");

    static {
        RenderUtil.mc = Minecraft.getMinecraft();
        RenderUtil.cameraFrustum = new Frustum();
        RenderUtil.viewportBuffer = GLAllocation.createDirectIntBuffer(16);
        RenderUtil.modelViewBuffer = GLAllocation.createDirectFloatBuffer(16);
        RenderUtil.projectionBuffer = GLAllocation.createDirectFloatBuffer(16);
        RenderUtil.vectorBuffer = GLAllocation.createDirectFloatBuffer(4);
        RenderUtil.enchantmentMap = new EnchantmentMap();
    }

    /**
     * 绘制圆角头像 (终极修复版)
     * 使用 GL_POLYGON 绘制单一大轮廓，配合强制状态重置。
     * 无论切换什么模式，无论 CULL_FACE 状态如何，都能保证形状完整。
     */
    public static void drawRoundedHead(ResourceLocation skin, float x, float y, float width, float height, float radius) {
        // 1. 检查 FBO 状态
        checkStencil();

        // 2. 备份当前 GL 状态 (非常重要，防止影响后续渲染)
        boolean needRestoreCull = GL11.glIsEnabled(GL11.GL_CULL_FACE);
        boolean needRestoreAlpha = GL11.glIsEnabled(GL11.GL_ALPHA_TEST);

        GlStateManager.pushMatrix();

        // 3. 准备 Stencil
        // 清除 Stencil 缓冲，防止上一帧的残留干扰
        GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
        GL11.glEnable(GL11.GL_STENCIL_TEST);
        GL11.glStencilFunc(GL11.GL_ALWAYS, 1, 0xFF);
        GL11.glStencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_REPLACE);
        GL11.glStencilMask(0xFF); // 允许写入

        // 4. 强制设置绘制遮罩所需的 GL 状态
        GlStateManager.colorMask(false, false, false, false); // 不写颜色
        GlStateManager.disableTexture2D(); // 不贴图
        GlStateManager.disableLighting();  // 不光照
        GlStateManager.disableAlpha();     // 关掉 Alpha Test，确保 alpha=0 也能写入 Stencil

        // --- 关键修复：强制关闭剔除 ---
        // 这能解决 "右边圆角消失" 的问题，无论顶点顺序顺时针还是逆时针，都会显示
        GlStateManager.disableCull();

        // 5. 绘制遮罩 (使用单一大轮廓)
        drawSolidRoundedRect(x, y, width, height, radius);

        // 6. 恢复状态，准备绘制图片
        if (needRestoreCull) GlStateManager.enableCull(); // 恢复之前的剔除状态
        if (needRestoreAlpha) GlStateManager.enableAlpha();

        GlStateManager.colorMask(true, true, true, true);
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // 7. 设置 Stencil 匹配条件
        GL11.glStencilFunc(GL11.GL_EQUAL, 1, 0xFF);
        GL11.glStencilMask(0x00); // 锁住 Stencil

        // 8. 绘制头像
        GlStateManager.color(1, 1, 1, 1);
        mc.getTextureManager().bindTexture(skin);
        // 使用浮点坐标绘制
        drawScaledCustomSizeModalRect(x, y, 8, 8, 8, 8, width, height, 64, 64);
        drawScaledCustomSizeModalRect(x, y, 40, 8, 8, 8, width, height, 64, 64);

        // 9. 结束清理
        GL11.glDisable(GL11.GL_STENCIL_TEST);
        GlStateManager.popMatrix();
    }

    /**
     * 内部专用：绘制实心圆角多边形
     * 不使用拼接，而是画一个闭合的环，确保不会出现接缝或剔除问题
     */
    private static void drawSolidRoundedRect(float x, float y, float width, float height, float radius) {
        // 限制半径
        float r = Math.min(radius, Math.min(width, height) / 2.0f);

        // 开始绘制多边形
        GL11.glBegin(GL11.GL_POLYGON);

        // 1. 左上角 (Top-Left) - 180度到270度 (注意：Y向下，270是上)
        // 逆时针顺序绘制：左上 -> 左下 -> 右下 -> 右上

        // 左上角弧线
        for (int i = 180; i <= 270; i += 5) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x + r + Math.cos(rad) * r, y + r + Math.sin(rad) * r);
        }

        // 右上角弧线 (270度到360度)
        for (int i = 270; i <= 360; i += 5) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x + width - r + Math.cos(rad) * r, y + r + Math.sin(rad) * r);
        }

        // 右下角弧线 (0度到90度)
        for (int i = 0; i <= 90; i += 5) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x + width - r + Math.cos(rad) * r, y + height - r + Math.sin(rad) * r);
        }

        // 左下角弧线 (90度到180度)
        for (int i = 90; i <= 180; i += 5) {
            double rad = Math.toRadians(i);
            GL11.glVertex2d(x + r + Math.cos(rad) * r, y + height - r + Math.sin(rad) * r);
        }

        GL11.glEnd();
    }

    /**
     * 支持 Float 坐标的图片绘制方法，防止动画抖动
     */
    public static void drawScaledCustomSizeModalRect(float x, float y, float u, float v, float uWidth, float vHeight, float width, float height, float tileWidth, float tileHeight) {
        float f = 1.0F / tileWidth;
        float f1 = 1.0F / tileHeight;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(x, y + height, 0.0D).tex(u * f, (v + vHeight) * f1).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0D).tex((u + uWidth) * f, (v + vHeight) * f1).endVertex();
        worldrenderer.pos(x + width, y, 0.0D).tex((u + uWidth) * f, v * f1).endVertex();
        worldrenderer.pos(x, y, 0.0D).tex(u * f, v * f1).endVertex();
        tessellator.draw();
    }

    /**
     * 检查并初始化 FBO 的 Stencil Buffer
     */
    private static void checkStencil() {
        Framebuffer fbo = mc.getFramebuffer();
        if (fbo != null && fbo.depthBuffer > -1) {
            EXTFramebufferObject.glDeleteRenderbuffersEXT(fbo.depthBuffer);
            int stencil_depth_buffer_id = EXTFramebufferObject.glGenRenderbuffersEXT();
            EXTFramebufferObject.glBindRenderbufferEXT(36161, stencil_depth_buffer_id);
            EXTFramebufferObject.glRenderbufferStorageEXT(36161, 34041, mc.displayWidth, mc.displayHeight);
            EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36128, 36161, stencil_depth_buffer_id);
            EXTFramebufferObject.glFramebufferRenderbufferEXT(36160, 36096, 36161, stencil_depth_buffer_id);
            fbo.depthBuffer = -1;
        }
    }

    // =========================================================================
    //                            SHADERS & RECTANGLES
    // =========================================================================

    /**
     * 绘制圆角矩形 (使用 Shader) - 用于背景板等
     */
    public static void drawRoundedRect(float x, float y, float width, float height, float radius, boolean blur, Color color) {
        GlStateManager.resetColor();
        GlStateManager.enableBlend();
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.alphaFunc(516, 0.0f);

        roundedShader.init();

        setupRoundedRectUniforms(x, y, width, height, radius, roundedShader);
        roundedShader.setUniformi("blur", blur ? 1 : 0);
        roundedShader.setUniformf("color", color.getRed() / 255f, color.getGreen() / 255f, color.getBlue() / 255f, color.getAlpha() / 255f);

        drawQuads(x - 1, y - 1, width + 2, height + 2);

        roundedShader.unload();
        GlStateManager.disableBlend();
    }

    private static void setupRoundedRectUniforms(float x, float y, float width, float height, float radius, ShaderUtil successShader) {
        ScaledResolution sr = new ScaledResolution(mc);
        successShader.setUniformf("location", x * sr.getScaleFactor(), (mc.displayHeight - (height * sr.getScaleFactor())) - (y * sr.getScaleFactor()));
        successShader.setUniformf("rectSize", width * sr.getScaleFactor(), height * sr.getScaleFactor());
        successShader.setUniformf("radius", radius * sr.getScaleFactor());
    }

    public static void drawQuads(float x, float y, float width, float height) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0); GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(0, 1); GL11.glVertex2f(x, y + height);
        GL11.glTexCoord2f(1, 1); GL11.glVertex2f(x + width, y + height);
        GL11.glTexCoord2f(1, 0); GL11.glVertex2f(x + width, y);
        GL11.glEnd();
    }

    /**
     * 绘制普通圆角矩形 (使用 GL 顶点，不支持模糊，但兼容性更好)
     */
    public static void drawRoundedRect(double x, double y, double width, double height, double radius, int color, boolean roundTopLeft, boolean roundTopRight, boolean roundBottomLeft, boolean roundBottomRight) {
        enableRenderState();
        setColor(color);

        double maxRadius = Math.min(width, height) / 2.0;
        radius = Math.min(radius, maxRadius);

        int segments = (int) Math.max(14, radius * 1.5);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();

        // 中心填充
        drawRect((float)(x + radius), (float)(y + radius), (float)(x + width - radius), (float)(y + height - radius), color);
        drawRect((float)(x + radius), (float)y, (float)(x + width - radius), (float)(y + radius), color);
        drawRect((float)(x + radius), (float)(y + height - radius), (float)(x + width - radius), (float)(y + height), color);
        drawRect((float)x, (float)(y + radius), (float)(x + radius), (float)(y + height - radius), color);
        drawRect((float)(x + width - radius), (float)(y + radius), (float)(x + width), (float)(y + height - radius), color);

        enableRenderState();

        float a = (float)(color >> 24 & 255) / 255.0F;
        float r = (float)(color >> 16 & 255) / 255.0F;
        float g = (float)(color >> 8 & 255) / 255.0F;
        float b = (float)(color & 255) / 255.0F;

        GlStateManager.color(r, g, b, a);

        // 绘制四个角
        if (roundTopLeft) {
            worldrenderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
            double centerX = x + radius;
            double centerY = y + radius;
            worldrenderer.pos(centerX, centerY, 0).endVertex();
            for (int i = 0; i <= segments; i++) {
                double angle = Math.PI + (i * Math.PI / (2 * segments));
                worldrenderer.pos(centerX + Math.cos(angle) * radius, centerY + Math.sin(angle) * radius, 0).endVertex();
            }
            tessellator.draw();
        } else {
            drawRect((float)x, (float)y, (float)(x + radius), (float)(y + radius), color);
            enableRenderState();
        }

        if (roundTopRight) {
            worldrenderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
            double centerX = x + width - radius;
            double centerY = y + radius;
            worldrenderer.pos(centerX, centerY, 0).endVertex();
            for (int i = 0; i <= segments; i++) {
                double angle = Math.PI * 1.5 + (i * Math.PI / (2 * segments));
                worldrenderer.pos(centerX + Math.cos(angle) * radius, centerY + Math.sin(angle) * radius, 0).endVertex();
            }
            tessellator.draw();
        } else {
            drawRect((float)(x + width - radius), (float)y, (float)(x + width), (float)(y + radius), color);
            enableRenderState();
        }

        if (roundBottomRight) {
            worldrenderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
            double centerX = x + width - radius;
            double centerY = y + height - radius;
            worldrenderer.pos(centerX, centerY, 0).endVertex();
            for (int i = 0; i <= segments; i++) {
                double angle = (i * Math.PI / (2 * segments));
                worldrenderer.pos(centerX + Math.cos(angle) * radius, centerY + Math.sin(angle) * radius, 0).endVertex();
            }
            tessellator.draw();
        } else {
            drawRect((float)(x + width - radius), (float)(y + height - radius), (float)(x + width), (float)(y + height), color);
            enableRenderState();
        }

        if (roundBottomLeft) {
            worldrenderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION);
            double centerX = x + radius;
            double centerY = y + height - radius;
            worldrenderer.pos(centerX, centerY, 0).endVertex();
            for (int i = 0; i <= segments; i++) {
                double angle = Math.PI * 0.5 + (i * Math.PI / (2 * segments));
                worldrenderer.pos(centerX + Math.cos(angle) * radius, centerY + Math.sin(angle) * radius, 0).endVertex();
            }
            tessellator.draw();
        } else {
            drawRect((float)x, (float)(y + height - radius), (float)(x + radius), (float)(y + height), color);
            enableRenderState();
        }

        disableRenderState();
    }

    // =========================================================================
    //                            BASIC UTILITIES
    // =========================================================================

    private static ChatColors getColorForLevel(int currentLevel, int maxLevel) {
        if (currentLevel > maxLevel) return ChatColors.LIGHT_PURPLE;
        if (currentLevel == maxLevel) return ChatColors.RED;
        switch (currentLevel) {
            case 1: return ChatColors.AQUA;
            case 2: return ChatColors.GREEN;
            case 3: return ChatColors.YELLOW;
            case 4: return ChatColors.GOLD;
        }
        return ChatColors.GRAY;
    }

    public static void drawCircle(float x, float y, float radius, int color) {
        enableRenderState();
        setColor(color);
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(x, y);
        for (int i = 0; i <= 360; i++) {
            double angle = Math.toRadians(i);
            GL11.glVertex2f(x + (float)(Math.cos(angle) * radius), y + (float)(Math.sin(angle) * radius));
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        disableRenderState();
    }

    public static void drawGradientCircle(float x, float y, float radius, int startColor, int endColor) {
        enableRenderState();
        GL11.glShadeModel(GL11.GL_SMOOTH);
        float startA = (float)(startColor >> 24 & 255) / 255.0F;
        float startR = (float)(startColor >> 16 & 255) / 255.0F;
        float startG = (float)(startColor >> 8 & 255) / 255.0F;
        float startB = (float)(startColor & 255) / 255.0F;
        float endA = (float)(endColor >> 24 & 255) / 255.0F;
        float endR = (float)(endColor >> 16 & 255) / 255.0F;
        float endG = (float)(endColor >> 8 & 255) / 255.0F;
        float endB = (float)(endColor & 255) / 255.0F;

        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GlStateManager.color(startR, startG, startB, startA);
        GL11.glVertex2f(x, y);

        for (int i = 0; i <= 360; i++) {
            double angle = Math.toRadians(i);
            float currentX = x + (float)(Math.cos(angle) * radius);
            float currentY = y + (float)(Math.sin(angle) * radius);
            float t = (float)i / 360.0f;
            float r = startR + (endR - startR) * t;
            float g = startG + (endG - startG) * t;
            float b = startB + (endB - startB) * t;
            float a = startA + (endA - startA) * t;
            GlStateManager.color(r, g, b, a);
            GL11.glVertex2f(currentX, currentY);
        }
        GL11.glEnd();
        GL11.glShadeModel(GL11.GL_FLAT);
        disableRenderState();
    }

    public static void drawCircleOutline(float x, float y, float radius, float lineWidth, int color) {
        enableRenderState();
        setColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= 360; i++) {
            double angle = Math.toRadians(i);
            GL11.glVertex2f(x + (float)(Math.cos(angle) * radius), y + (float)(Math.sin(angle) * radius));
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.0f);
        disableRenderState();
    }

    public static void drawImage(ResourceLocation resource, float x, float y, float width, float height) {
        try {
            mc.getTextureManager().bindTexture(resource);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            GL11.glBegin(GL11.GL_QUADS);
            GL11.glTexCoord2f(0, 0); GL11.glVertex2f(x, y);
            GL11.glTexCoord2f(1, 0); GL11.glVertex2f(x + width, y);
            GL11.glTexCoord2f(1, 1); GL11.glVertex2f(x + width, y + height);
            GL11.glTexCoord2f(0, 1); GL11.glVertex2f(x, y + height);
            GL11.glEnd();
            GL11.glDisable(GL11.GL_BLEND);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void drawGradientRect(float x, float y, float width, float height, int startColor, int endColor) {
        enableRenderState();
        GL11.glShadeModel(GL11.GL_SMOOTH);
        float startA = (float)(startColor >> 24 & 255) / 255.0F;
        float startR = (float)(startColor >> 16 & 255) / 255.0F;
        float startG = (float)(startColor >> 8 & 255) / 255.0F;
        float startB = (float)(startColor & 255) / 255.0F;
        float endA = (float)(endColor >> 24 & 255) / 255.0F;
        float endR = (float)(endColor >> 16 & 255) / 255.0F;
        float endG = (float)(endColor >> 8 & 255) / 255.0F;
        float endB = (float)(endColor & 255) / 255.0F;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldrenderer.pos(x + width, y, 0.0D).color(startR, startG, startB, startA).endVertex();
        worldrenderer.pos(x, y, 0.0D).color(startR, startG, startB, startA).endVertex();
        worldrenderer.pos(x, y + height, 0.0D).color(endR, endG, endB, endA).endVertex();
        worldrenderer.pos(x + width, y + height, 0.0D).color(endR, endG, endB, endA).endVertex();
        tessellator.draw();
        GL11.glShadeModel(GL11.GL_FLAT);
        disableRenderState();
    }

    public static void drawOutlinedString(String text, float x, float y) {
        String string2 = text.replaceAll("(?i)§[\\da-f]", "");
        RenderUtil.mc.fontRendererObj.drawString(string2, x + 1.0f, y, 0, false);
        RenderUtil.mc.fontRendererObj.drawString(string2, x - 1.0f, y, 0, false);
        RenderUtil.mc.fontRendererObj.drawString(string2, x, y + 1.0f, 0, false);
        RenderUtil.mc.fontRendererObj.drawString(string2, x, y - 1.0f, 0, false);
        RenderUtil.mc.fontRendererObj.drawString(text, x, y, -1, false);
    }

    public static void renderEnchantmentText(ItemStack itemStack, float x, float y, float scale) {
        NBTTagList nBTTagList;
        nBTTagList = itemStack.getItem() == Items.enchanted_book ? Items.enchanted_book.getEnchantments(itemStack) : itemStack.getEnchantmentTagList();
        if (nBTTagList != null) {
            for (int i = 0; i < nBTTagList.tagCount(); ++i) {
                EnchantmentData enchantmentData = enchantmentMap.get(nBTTagList.getCompoundTagAt(i).getInteger("id"));
                if (enchantmentData == null) continue;
                short s = nBTTagList.getCompoundTagAt(i).getShort("lvl");
                ChatColors chatColors = RenderUtil.getColorForLevel(s, enchantmentData.maxLevel);
                RenderUtil.drawOutlinedString(ChatColors.formatColor(String.format("&r%s%s%d&r", enchantmentData.shortName, chatColors, (int) s)), x * (1.0f / scale), (y + (float) i * 4.0f) * (1.0f / scale));
            }
        }
    }

    public static void renderItemInGUI(ItemStack itemStack, int x, int y) {
        GlStateManager.pushMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.clear(256);
        RenderHelper.enableGUIStandardItemLighting();
        GL11.glDisable(GL11.GL_LIGHTING);
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0f, 1.0f, -0.01f);
        RenderUtil.mc.getRenderItem().zLevel = -150.0f;
        mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, x, y);
        mc.getRenderItem().renderItemOverlays(RenderUtil.mc.fontRendererObj, itemStack, x, y);
        RenderUtil.mc.getRenderItem().zLevel = 0.0f;
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.5f, 0.5f, 0.5f);
        GlStateManager.disableDepth();
        RenderUtil.renderEnchantmentText(itemStack, x, y, 0.5f);
        GlStateManager.enableDepth();
        GlStateManager.scale(2.0f, 2.0f, 2.0f);
        GlStateManager.popMatrix();
    }

    public static void renderPotionEffect(PotionEffect potionEffect, int x, int y) {
        int n3 = Potion.potionTypes[potionEffect.getPotionID()].getStatusIconIndex();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.pushMatrix();
        GlStateManager.depthMask(true);
        GlStateManager.clear(256);
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0f, 1.0f, -0.01f);
        mc.getTextureManager().bindTexture(new ResourceLocation("textures/gui/container/inventory.png"));
        Gui.drawModalRectWithCustomSizedTexture(x, y, n3 % 8 * 18, 198 + (float) n3 / 8 * 18, 18, 18, 256.0f, 256.0f);
        GlStateManager.popMatrix();
        GlStateManager.enableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    public static void drawRect(double left, double top, double right, double bottom, int color) {
        if (left < right) {
            double i = left;
            left = right;
            right = i;
        }
        if (top < bottom) {
            double j = top;
            top = bottom;
            bottom = j;
        }
        float f = (float)(color >> 24 & 255) / 255.0F;
        float g = (float)(color >> 16 & 255) / 255.0F;
        float h = (float)(color >> 8 & 255) / 255.0F;
        float j = (float)(color & 255) / 255.0F;
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(g, h, j, f);
        worldRenderer.begin(7, DefaultVertexFormats.POSITION);
        worldRenderer.pos(left, bottom, 0.0D).endVertex();
        worldRenderer.pos(right, bottom, 0.0D).endVertex();
        worldRenderer.pos(right, top, 0.0D).endVertex();
        worldRenderer.pos(left, top, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void drawRect3D(float x1, float y1, float x2, float y2, int color) {
        if (color == 0) return;
        RenderUtil.setColor(color);
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_POLYGON);
        for (int i = 0; i < 2; ++i) {
            GL11.glVertex2f(x1, y1);
            GL11.glVertex2f(x1, y2);
            GL11.glVertex2f(x2, y2);
            GL11.glVertex2f(x2, y1);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        GlStateManager.resetColor();
    }

    public static void drawOutlineRect(float x1, float y1, float x2, float y2, float lineWidth, int backgroundColor, int lineColor) {
        RenderUtil.drawRect(0.0f, 0.0f, x2, 27.0f, backgroundColor);
        if (lineColor == 0) return;
        RenderUtil.setColor(lineColor);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x1, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x1, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        GlStateManager.resetColor();
    }

    public static void drawLine(float x1, float y1, float x2, float y2, float lineWidth, int color) {
        RenderUtil.setColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(x1, y1);
        GL11.glVertex2f(x2, y2);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        GlStateManager.resetColor();
    }

    public static void drawLine3D(Vec3 start, double endX, double endY, double endZ, float red, float green, float blue, float alpha, float lineWidth) {
        GlStateManager.pushMatrix();
        GlStateManager.color(red, green, blue, alpha);
        boolean bl = RenderUtil.mc.gameSettings.viewBobbing;
        RenderUtil.mc.gameSettings.viewBobbing = false;
        ((IAccessorEntityRenderer) RenderUtil.mc.entityRenderer).callSetupCameraTransform(((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks, 2);
        RenderUtil.mc.gameSettings.viewBobbing = bl;
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex3d(start.xCoord, start.yCoord, start.zCoord);
        GL11.glVertex3d(endX - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(), endY - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(), endZ - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ());
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        GlStateManager.resetColor();
        GlStateManager.popMatrix();
    }

    public static void drawArrow(float centerX, float centerY, float angle, float length, float lineWidth, int color) {
        float f6 = angle + (float) Math.toRadians(45.0);
        float f7 = angle - (float) Math.toRadians(45.0);
        RenderUtil.setColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINES);
        GL11.glVertex2f(centerX, centerY);
        GL11.glVertex2f(centerX + length * (float) Math.cos(f6), centerY + length * (float) Math.sin(f6));
        GL11.glVertex2f(centerX, centerY);
        GL11.glVertex2f(centerX + length * (float) Math.cos(f7), centerY + length * (float) Math.sin(f7));
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        GlStateManager.resetColor();
    }

    public static void drawTriangle(float centerX, float centerY, float angle, float length, int color) {
        float f5 = angle + (float) Math.toRadians(26.25);
        float f6 = angle - (float) Math.toRadians(26.25);
        RenderUtil.setColor(color);
        GL11.glEnable(GL11.GL_POLYGON_SMOOTH);
        GL11.glHint(GL11.GL_POLYGON_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(9);
        GL11.glVertex2f(centerX, centerY);
        GL11.glVertex2f(centerX + length * (float) Math.cos(f5), centerY + length * (float) Math.sin(f5));
        GL11.glVertex2f(centerX + length * (float) Math.cos(f6), centerY + length * (float) Math.sin(f6));
        GL11.glEnd();
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH);
        GlStateManager.resetColor();
    }

    public static void drawFramebuffer(Framebuffer framebuffer) {
        ScaledResolution scaledResolution = new ScaledResolution(mc);
        GlStateManager.bindTexture(framebuffer.framebufferTexture);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2d(0.0, 1.0);
        GL11.glVertex2d(0.0, 0.0);
        GL11.glTexCoord2d(0.0, 0.0);
        GL11.glVertex2d(0.0, scaledResolution.getScaledHeight());
        GL11.glTexCoord2d(1.0, 0.0);
        GL11.glVertex2d(scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
        GL11.glTexCoord2d(1.0, 1.0);
        GL11.glVertex2d(scaledResolution.getScaledWidth(), 0.0);
        GL11.glEnd();
    }

    public static void drawCircle(double centerX, double centerY, double centerZ, double radius, int segments, int color) {
        RenderUtil.setColor(color);
        GL11.glLineWidth(3.0f);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= segments; ++i) {
            double d5 = (double) i * (Math.PI * 2 / (double) segments);
            GL11.glVertex3d(centerX + Math.cos(d5) * radius, centerY, centerZ + Math.sin(d5) * radius);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        GlStateManager.resetColor();
    }

    public static void drawEntityCircle(Entity entity, double radius, int segments, int color) {
        double d2 = RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
        double d3 = RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY();
        double d4 = RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks) - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();
        RenderUtil.drawCircle(d2, d3, d4, radius, segments, color);
    }

    public static void drawFilledBox(AxisAlignedBB axisAlignedBB, int red, int green, int blue) {
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ).color(red, green, blue, 63).endVertex();
        worldRenderer.pos(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ).color(red, green, blue, 63).endVertex();
        tessellator.draw();
    }

    public static void drawBoundingBox(AxisAlignedBB axisAlignedBB, int red, int green, int blue, int alpha, float lineWidth) {
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        RenderGlobal.drawOutlinedBoundingBox(axisAlignedBB, red, green, blue, alpha);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
    }

    public static void drawEntityBox(Entity entity, int red, int green, int blue) {
        double d2 = RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
        double d3 = RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
        double d4 = RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
        RenderUtil.drawFilledBox(entity.getEntityBoundingBox().expand(0.1f, 0.1f, 0.1f).offset(d2 - entity.posX, d3 - entity.posY, d4 - entity.posZ).offset(-((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(), -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(), -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()), red, green, blue);
    }

    public static void drawEntityBoundingBox(Entity entity, int red, int green, int blue, int alpha, float lineWidth, double expand) {
        double d2 = RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
        double d3 = RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
        double d4 = RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
        RenderUtil.drawBoundingBox(entity.getEntityBoundingBox().expand(expand, expand, expand).offset(d2 - entity.posX, d3 - entity.posY, d4 - entity.posZ).offset(-((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(), -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(), -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()), red, green, blue, alpha, lineWidth);
    }

    public static void drawBlockBox(BlockPos blockPos, double height, int red, int green, int blue) {
        RenderUtil.drawFilledBox(new AxisAlignedBB(blockPos.getX(), blockPos.getY(), blockPos.getZ(), (double) blockPos.getX() + 1.0, (double) blockPos.getY() + height, (double) blockPos.getZ() + 1.0).offset(-((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(), -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(), -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()), red, green, blue);
    }

    public static void drawBlockBoundingBox(BlockPos blockPos, double height, int red, int green, int blue, int alpha, float lineWidth) {
        RenderUtil.drawBoundingBox(new AxisAlignedBB(blockPos.getX(), blockPos.getY(), blockPos.getZ(), (double) blockPos.getX() + 1.0, (double) blockPos.getY() + height, (double) blockPos.getZ() + 1.0).offset(-((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX(), -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY(), -((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()), red, green, blue, alpha, lineWidth);
    }

    public static Vector4d projectToScreen(Entity entity, double screenScale) {
        Vector4d vector4d;
        {
            double d3 = RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
            double d4 = RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
            double d5 = RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ, ((IAccessorMinecraft) RenderUtil.mc).getTimer().renderPartialTicks);
            AxisAlignedBB axisAlignedBB = entity.getEntityBoundingBox().expand(0.1f, 0.1f, 0.1f).offset(d3 - entity.posX, d4 - entity.posY, d5 - entity.posZ);
            vector4d = null;
            for (Vector3d vector3d : new Vector3d[]{new Vector3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vector3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vector3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vector3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vector3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vector3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ), new Vector3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vector3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)}) {
                GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelViewBuffer);
                GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projectionBuffer);
                GL11.glGetInteger(GL11.GL_VIEWPORT, viewportBuffer);
                if (!GLU.gluProject((float) (vector3d.x - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX()), (float) (vector3d.y - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY()), (float) (vector3d.z - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ()), modelViewBuffer, projectionBuffer, viewportBuffer, vectorBuffer))
                    continue;
                vector3d = new Vector3d((double) vectorBuffer.get(0) / screenScale, (double) ((float) Display.getHeight() - vectorBuffer.get(1)) / screenScale, vectorBuffer.get(2));
                if (!(vector3d.z >= 0.0) || !(vector3d.z < 1.0)) continue;
                if (vector4d == null) {
                    vector4d = new Vector4d(vector3d.x, vector3d.y, vector3d.z, 0.0);
                }
                vector4d.x = Math.min(vector3d.x, vector4d.x);
                vector4d.y = Math.min(vector3d.y, vector4d.y);
                vector4d.z = Math.max(vector3d.x, vector4d.z);
                vector4d.w = Math.max(vector3d.y, vector4d.w);
            }
        }
        return vector4d;
    }

    public static boolean isInViewFrustum(AxisAlignedBB axisAlignedBB, double expand) {
        cameraFrustum.setPosition(RenderUtil.mc.getRenderViewEntity().posX, RenderUtil.mc.getRenderViewEntity().posY, RenderUtil.mc.getRenderViewEntity().posZ);
        return cameraFrustum.isBoundingBoxInFrustum(axisAlignedBB.expand(expand, expand, expand));
    }

    public static void enableRenderState() {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableTexture2D();
        GlStateManager.disableCull();
        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();
    }

    public static void disableRenderState() {
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    public static void setColor(int argb) {
        float f = (float) (argb >> 24 & 0xFF) / 255.0f;
        float f2 = (float) (argb >> 16 & 0xFF) / 255.0f;
        float f3 = (float) (argb >> 8 & 0xFF) / 255.0f;
        float f4 = (float) (argb & 0xFF) / 255.0f;
        GlStateManager.color(f2, f3, f4, f);
    }

    public static float lerpFloat(float current, float previous, float t) {
        return previous + (current - previous) * t;
    }

    public static double lerpDouble(double current, double previous, double t) {
        return previous + (current - previous) * t;
    }

    public static final class EnchantmentData {
        public final String shortName;
        public final int maxLevel;

        public EnchantmentData(String shortName, int maxLevel) {
            this.shortName = shortName;
            this.maxLevel = maxLevel;
        }
    }

    static final class EnchantmentMap extends HashMap<Integer, EnchantmentData> {
        EnchantmentMap() {
            this.put(0, new EnchantmentData("Pr", 4));
            this.put(1, new EnchantmentData("Fp", 4));
            this.put(2, new EnchantmentData("Ff", 4));
            this.put(3, new EnchantmentData("Bp", 4));
            this.put(4, new EnchantmentData("Pp", 4));
            this.put(5, new EnchantmentData("Re", 3));
            this.put(6, new EnchantmentData("Aq", 1));
            this.put(7, new EnchantmentData("Th", 3));
            this.put(8, new EnchantmentData("Ds", 3));
            this.put(16, new EnchantmentData("Sh", 5));
            this.put(17, new EnchantmentData("Sm", 5));
            this.put(18, new EnchantmentData("BoA", 5));
            this.put(19, new EnchantmentData("Kb", 2));
            this.put(20, new EnchantmentData("Fa", 2));
            this.put(21, new EnchantmentData("Lo", 3));
            this.put(32, new EnchantmentData("Ef", 5));
            this.put(33, new EnchantmentData("St", 1));
            this.put(34, new EnchantmentData("Ub", 3));
            this.put(35, new EnchantmentData("Fo", 3));
            this.put(48, new EnchantmentData("Po", 5));
            this.put(49, new EnchantmentData("Pu", 2));
            this.put(50, new EnchantmentData("Fl", 1));
            this.put(51, new EnchantmentData("Inf", 1));
            this.put(61, new EnchantmentData("LoS", 3));
            this.put(62, new EnchantmentData("Lu", 3));
        }
    }

    public static void drawRectOutline(double x, double y, double width, double height, float lineWidth, int color) {
        RenderUtil.enableRenderState();
        RenderUtil.setColor(color);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        GL11.glVertex2d(x, y);
        GL11.glVertex2d(x + width, y);
        GL11.glVertex2d(x + width, y + height);
        GL11.glVertex2d(x, y + height);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0f);
        RenderUtil.disableRenderState();
    }

    public static void scissor(double x, double y, double width, double height) {
        if (width < 0.5 || height < 0.5) return;
        ScaledResolution sr = new ScaledResolution(mc);
        double scale = sr.getScaleFactor();
        double finalHeight = height * scale;
        double finalY = (sr.getScaledHeight_double() - (y + height)) * scale;
        double finalX = x * scale;
        double finalWidth = width * scale;
        finalX = Math.max(0, finalX);
        finalY = Math.max(0, finalY);
        finalWidth = Math.max(0, finalWidth);
        finalHeight = Math.max(0, finalHeight);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) finalX, (int) finalY, (int) finalWidth, (int) finalHeight);
    }

    public static void releaseScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    /**
     * 绘制可自定义四个角状态的圆角矩形描边
     */
    public static void drawRoundedRectOutline(float x, float y, float width, float height, float radius, float lineWidth, int color, boolean topLeft, boolean topRight, boolean bottomLeft, boolean bottomRight) {
        radius = Math.min(radius, Math.min(width, height) / 2.0f);
        float f = (float)(color >> 24 & 255) / 255.0F;
        float f1 = (float)(color >> 16 & 255) / 255.0F;
        float f2 = (float)(color >> 8 & 255) / 255.0F;
        float f3 = (float)(color & 255) / 255.0F;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(f1, f2, f3, f);
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glBegin(GL11.GL_LINE_LOOP);

        if (topLeft) {
            for (int i = 180; i <= 270; i += 3) {
                double rad = Math.toRadians(i);
                GL11.glVertex2d(x + radius + Math.cos(rad) * radius, y + radius + Math.sin(rad) * radius);
            }
        } else {
            GL11.glVertex2f(x, y);
        }

        if (topRight) {
            for (int i = 270; i <= 360; i += 3) {
                double rad = Math.toRadians(i);
                GL11.glVertex2d(x + width - radius + Math.cos(rad) * radius, y + radius + Math.sin(rad) * radius);
            }
        } else {
            GL11.glVertex2f(x + width, y);
        }

        if (bottomRight) {
            for (int i = 0; i <= 90; i += 3) {
                double rad = Math.toRadians(i);
                GL11.glVertex2d(x + width - radius + Math.cos(rad) * radius, y + height - radius + Math.sin(rad) * radius);
            }
        } else {
            GL11.glVertex2f(x + width, y + height);
        }

        if (bottomLeft) {
            for (int i = 90; i <= 180; i += 3) {
                double rad = Math.toRadians(i);
                GL11.glVertex2d(x + radius + Math.cos(rad) * radius, y + height - radius + Math.sin(rad) * radius);
            }
        } else {
            GL11.glVertex2f(x, y + height);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}