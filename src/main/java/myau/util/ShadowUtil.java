package myau.util;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

public class ShadowUtil {

    private static final ShaderUtil shadowShader = new ShaderUtil("myau/shader/shadow.fsh");

    public static void drawShadow(float x, float y, float width, float height, float radius, float softness, int color) {
        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

        // Critical: Disable Alpha Test so low-alpha shadow pixels aren't cut off
        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();

        shadowShader.init();

        // We pass the dimensions of the "inner" box (the actual TargetHUD size)
        shadowShader.setUniformf("rectSize", width, height);
        shadowShader.setUniformf("radius", radius);
        shadowShader.setUniformf("softness", softness);

        // Parse color
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        shadowShader.setUniformf("color", r, g, b, a);

        // Determine the bounds of the quad.
        // We expand the quad by 'softness' in all directions to provide space for the blur.
        float x1 = x - softness;
        float y1 = y - softness;
        float x2 = x + width + softness;
        float y2 = y + height + softness;

        GL11.glBegin(GL11.GL_QUADS);
        // Map 0,0 to top-left and 1,1 to bottom-right
        // The shader uses these UVs to determine where the pixel is relative to the box
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex2f(x1, y1);

        GL11.glTexCoord2f(0, 1);
        GL11.glVertex2f(x1, y2);

        GL11.glTexCoord2f(1, 1);
        GL11.glVertex2f(x2, y2);

        GL11.glTexCoord2f(1, 0);
        GL11.glVertex2f(x2, y1);
        GL11.glEnd();

        shadowShader.unload();

        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}