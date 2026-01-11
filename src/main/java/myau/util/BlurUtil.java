package myau.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

public class BlurUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static Framebuffer framebuffer;
    private static int lastScale;
    private static int lastScaleWidth;
    private static int lastScaleHeight;
    private static final ShaderUtil blurShader = new ShaderUtil("myau/shader/blur.frag");
    
    private static void initFramebuffer() {
        if (framebuffer == null) {
            framebuffer = new Framebuffer(mc.displayWidth, mc.displayHeight, true);
            framebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
        }
    }
    
    private static void updateFramebuffer() {
        ScaledResolution scale = new ScaledResolution(mc);
        int factor = scale.getScaleFactor();
        int width = scale.getScaledWidth();
        int height = scale.getScaledHeight();
        
        if (lastScale != factor || lastScaleWidth != width || lastScaleHeight != height) {
            initFramebuffer();
            framebuffer.createBindFramebuffer(width, height);
            blurShader.init();
            lastScale = factor;
            lastScaleWidth = width;
            lastScaleHeight = height;
        }
    }
    
    public static void renderBlur(float intensity) {
        if (!OpenGlHelper.isFramebufferEnabled()) return;
        
        updateFramebuffer();
        
        // 将当前屏幕内容渲染到帧缓冲区
        framebuffer.framebufferRender(mc.displayWidth, mc.displayHeight);
        
        // 保存当前OpenGL状态
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        
        // 设置渲染状态
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        
        // 绑定帧缓冲区纹理并应用模糊着色器
        framebuffer.bindFramebufferTexture();
        blurShader.init();
        blurShader.setUniformf("radius", intensity);
        blurShader.setUniformi("textureIn", 0);
        
        // 绘制全屏矩形
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.loadIdentity();
        GlStateManager.ortho(0, mc.displayWidth, mc.displayHeight, 0, 1, -1);
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.loadIdentity();
        
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GlStateManager.bindTexture(framebuffer.framebufferTexture);
        
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
        worldrenderer.pos(0, mc.displayHeight, 0.0D).tex(0, 0).endVertex();
        worldrenderer.pos(mc.displayWidth, mc.displayHeight, 0.0D).tex(1, 0).endVertex();
        worldrenderer.pos(mc.displayWidth, 0, 0.0D).tex(1, 1).endVertex();
        worldrenderer.pos(0, 0, 0.0D).tex(0, 1).endVertex();
        tessellator.draw();
        
        // 清理
        blurShader.unload();
        framebuffer.unbindFramebufferTexture();
        GlStateManager.bindTexture(0);
        
        // 恢复OpenGL状态
        GL11.glPopAttrib();
    }
}