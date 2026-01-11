package myau.util;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class ColorUtils {
    
    public static void setColor(int color) {
        float alpha = (color >> 24 & 255) / 255.0F;
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        
        GlStateManager.color(red, green, blue, alpha);
    }
    
    public static void setColor(int color, double alpha) {
        float red = (color >> 16 & 255) / 255.0F;
        float green = (color >> 8 & 255) / 255.0F;
        float blue = (color & 255) / 255.0F;
        
        GlStateManager.color(red, green, blue, (float) alpha);
    }
    
    public static void setColor(float red, float green, float blue, float alpha) {
        GlStateManager.color(red, green, blue, alpha);
    }
    
    public static void resetColor() {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
    
    public static int getColor(int brightness, int alpha) {
        return getColor(brightness, brightness, brightness, alpha);
    }
    
    public static int getColor(int red, int green, int blue) {
        return getColor(red, green, blue, 255);
    }
    
    public static int getColor(int red, int green, int blue, int alpha) {
        int color = 0;
        color |= Math.max(0, Math.min(255, alpha)) << 24;
        color |= Math.max(0, Math.min(255, red)) << 16;
        color |= Math.max(0, Math.min(255, green)) << 8;
        color |= Math.max(0, Math.min(255, blue));
        return color;
    }
    
    public static int getColorWithAlpha(int color, int alpha) {
        int red = (color >> 16 & 255);
        int green = (color >> 8 & 255);
        int blue = (color & 255);
        return getColor(red, green, blue, Math.max(0, Math.min(255, alpha)));
    }
    
    public static int getRainbow(float seconds, float saturation, float brightness, int index) {
        float hue = (System.currentTimeMillis() % (int) (seconds * 1000)) / (seconds * 1000);
        hue += index / 1000f;
        return Color.HSBtoRGB(hue, saturation, brightness);
    }
}