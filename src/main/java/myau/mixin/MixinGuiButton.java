package myau.mixin;

import myau.util.AnimationUtil;
import myau.util.RenderUtil;
import myau.util.font.FontManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;

@Mixin(GuiButton.class)
public abstract class MixinGuiButton {

    // 映射原版字段
    @Shadow public boolean visible;
    @Shadow public int xPosition;
    @Shadow public int yPosition;
    @Shadow public int width;
    @Shadow public int height;
    @Shadow protected boolean hovered;
    @Shadow public String displayString;
    @Shadow public boolean enabled;

    // 添加新的动画状态变量 (Unique 保证不会冲突)
    @Unique private float hoverProgress = 0.0f;

    // 颜色常量 (与 ModernGuiButton 保持一致)
    @Unique private static final int BG_NORMAL = new Color(0, 0, 0, 80).getRGB();
    @Unique private static final int BG_HOVER = new Color(20, 20, 20, 150).getRGB();
    @Unique private static final int BG_DISABLED = new Color(0, 0, 0, 40).getRGB(); // 禁用时的背景
    
    @Unique private static final int OUTLINE_NORMAL = new Color(255, 255, 255, 30).getRGB();
    @Unique private static final int OUTLINE_HOVER = new Color(255, 255, 255, 150).getRGB();
    @Unique private static final int OUTLINE_DISABLED = new Color(100, 100, 100, 30).getRGB();

    /**
     * 注入到 drawButton 方法头部，取消原版绘制，使用自定义绘制
     */
    @Inject(method = "drawButton", at = @At("HEAD"), cancellable = true)
    public void onDrawButton(Minecraft mc, int mouseX, int mouseY, CallbackInfo ci) {
        if (this.visible) {
            // 1. 计算悬停状态
            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition 
                        && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;

            // 2. 更新动画 (如果按钮被禁用，强制动画进度为0)
            float target = (this.hovered && this.enabled) ? 1.0f : 0.0f;
            this.hoverProgress = AnimationUtil.animate(target, this.hoverProgress, 0.2f);

            // 3. 准备绘制环境
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.blendFunc(770, 771);

            // 4. 计算动态颜色
            int bgColor, outlineColor, textColor;

            if (this.enabled) {
                bgColor = AnimationUtil.interpolateColor(BG_NORMAL, BG_HOVER, hoverProgress);
                outlineColor = AnimationUtil.interpolateColor(OUTLINE_NORMAL, OUTLINE_HOVER, hoverProgress);
                textColor = AnimationUtil.interpolateColor(new Color(200, 200, 200).getRGB(), -1, hoverProgress);
            } else {
                // 禁用状态的颜色
                bgColor = BG_DISABLED;
                outlineColor = OUTLINE_DISABLED;
                textColor = new Color(100, 100, 100).getRGB(); // 深灰色文字
            }

            // 5. 绘制圆角背景 (圆角 4px)
            RenderUtil.drawRoundedRect(
                this.xPosition, this.yPosition, 
                this.width, this.height, 
                4.0f, 
                bgColor, 
                true, true, true, true
            );

            // 6. 绘制圆角边框
            RenderUtil.drawRoundedRectOutline(
                this.xPosition, this.yPosition, 
                this.width, this.height, 
                4.0f, 
                1.0f, 
                outlineColor, 
                true, true, true, true
            );

            // 7. 绘制文字 (尝试使用高清字体)
            this.drawButtonText(mc, textColor);

            // 8. 重置颜色防止污染其他渲染
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            
            // 9. 取消原版方法的执行
            ci.cancel();
        }
    }

    /**
     * 辅助方法：处理字体渲染
     */
    @Unique
    private void drawButtonText(Minecraft mc, int color) {
        String text = this.displayString;
        
        // 尝试使用 Product Sans 字体
        // 检查 null 避免崩溃
        if (FontManager.productSans20 != null) {
            // 计算垂直居中：注意自定义字体高度可能不同，微调 +1 或 +2
            float fontY = (float) (this.yPosition + (this.height - FontManager.productSans20.getHeight()) / 2) + 1;
            
            FontManager.productSans20.drawCenteredString(
                text, 
                this.xPosition + this.width / 2.0, 
                fontY, 
                color
            );
        } else {
            // 降级回退到原版像素字体
            mc.fontRendererObj.drawStringWithShadow(
                text, 
                this.xPosition + this.width / 2 - mc.fontRendererObj.getStringWidth(text) / 2, 
                this.yPosition + (this.height - 8) / 2, 
                color
            );
        }
    }
}