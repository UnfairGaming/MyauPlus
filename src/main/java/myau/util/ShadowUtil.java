package myau.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class ShadowUtil {

    private static final ShaderUtil shadowShader = new ShaderUtil("myau/shader/shadow.fsh");

    public static void drawShadow(float x, float y, float width, float height, float radius, float softness, int color) {
        // 1. 保存当前 GL 状态，防止污染
        GlStateManager.pushMatrix();

        // 2. 开启混合，设置混合模式
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D(); // 纯色绘制不需要纹理
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0); // SRC_ALPHA, ONE_MINUS_SRC_ALPHA

        // 3. 【关键修复】关闭 Alpha Test
        // MC 默认只渲染 alpha > 0.1 的像素，这会导致阴影边缘被切断
        GlStateManager.disableAlpha();

        // 4. 关闭裁剪测试 (可选，防止阴影被父容器裁剪，视情况开启)
        // GL11.glDisable(GL11.GL_SCISSOR_TEST);

        shadowShader.init();

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        float scale = sr.getScaleFactor();

        // 坐标转换
        float xCoords = x * scale;
        // 修正 Y 轴坐标计算：确保基于左下角
        float yCoords = (Minecraft.getMinecraft().displayHeight - (y + height) * scale);

        shadowShader.setUniformf("location", xCoords, yCoords);
        shadowShader.setUniformf("rectSize", width * scale, height * scale);
        shadowShader.setUniformf("radius", radius * scale);
        shadowShader.setUniformf("softness", softness * scale);

        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        shadowShader.setUniformf("color", r, g, b, a);

        // 5. 扩大绘制区域
        // padding 必须大于 softness，这里由 *2 改为 + softness + 5 确保万无一失
        float padding = softness + 10;

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x - padding, y - padding);
        GL11.glVertex2f(x - padding, y + height + padding);
        GL11.glVertex2f(x + width + padding, y + height + padding);
        GL11.glVertex2f(x + width + padding, y - padding);
        GL11.glEnd();

        shadowShader.unload();

        // 6. 恢复状态
        GlStateManager.enableAlpha(); // 必须恢复，否则文字渲染会出问题
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }
}