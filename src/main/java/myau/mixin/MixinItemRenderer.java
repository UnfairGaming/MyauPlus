package myau.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import myau.module.modules.Animations;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(ItemRenderer.class)
public abstract class MixinItemRenderer {

    @Shadow
    private Minecraft mc;

    @Shadow
    private ItemStack itemToRender;

    @Shadow
    private float equippedProgress;

    @Shadow
    private float prevEquippedProgress;

    @Shadow
    protected abstract void doBlockTransformations();

    private float delay = 0;
    private long lastUpdateTime = System.currentTimeMillis();

    @Inject(method = "renderItemInFirstPerson", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemRenderer;doBlockTransformations()V"))
    private void onRenderBlockingItem(float partialTicks, CallbackInfo ci) {
        Animations animations = Animations.getInstance();
        if (animations == null || !animations.isEnabled()) return;

        EntityPlayerSP player = mc.thePlayer;
        if (player == null) return;

        float itemScale = animations.scale.getValue() + 1.0F;
        if (itemScale != 1.0F) {
            GlStateManager.scale(itemScale, itemScale, itemScale);
        }

        if (!player.isBlocking()) return;

        float swingProgress = player.getSwingProgress(partialTicks);
        String mode = animations.swordMode.getModeString();

        GL11.glTranslated(animations.blockPosX.getValue().doubleValue(),
                animations.blockPosY.getValue().doubleValue(),
                animations.blockPosZ.getValue().doubleValue());

        switch (mode) {
            case "1.8":
                break;
            case "Swing":
                applySwingAnimation(swingProgress);
                break;
            case "Old":
                applyOldAnimation(swingProgress);
                break;
            case "Push":
                applyPushAnimation(swingProgress);
                break;
            case "Dash":
                applyDashAnimation(swingProgress);
                break;
            case "Slash":
                applySlashAnimation(swingProgress);
                break;
            case "Slide":
                applySlideAnimation(swingProgress);
                break;
            case "Scale":
                applyScaleAnimation(swingProgress);
                break;
            case "Swank":
                applySwankAnimation(swingProgress);
                break;
            case "Swang":
                applySwangAnimation(swingProgress);
                break;
            case "Swonk":
                applySwonkAnimation(swingProgress);
                break;
            case "Stella":
                applyStellaAnimation(swingProgress);
                break;
            case "Small":
                applySmallAnimation(swingProgress);
                break;
            case "Edit":
                applyEditAnimation(swingProgress);
                break;
            case "Rhys":
                applyRhysAnimation(swingProgress, partialTicks);
                break;
            case "Stab":
                applyStabAnimation(swingProgress);
                break;
            case "Float":
                applyFloatAnimation(swingProgress);
                break;
            case "Remix":
                applyRemixAnimation(swingProgress);
                break;
            case "Avatar":
                applyAvatarAnimation(swingProgress);
                break;
            case "Xiv":
                applyXivAnimation(swingProgress);
                break;
            case "Winter":
                applyWinterAnimation(swingProgress);
                break;
            case "Yamato":
                applyYamatoAnimation(swingProgress);
                break;
            case "SlideSwing":
                applySlideSwingAnimation(swingProgress);
                break;
            case "SmallPush":
                applySmallPushAnimation(swingProgress);
                break;
            case "Reverse":
                applyReverseAnimation(swingProgress);
                break;
            case "Invent":
                applyInventAnimation(swingProgress);
                break;
            case "Leaked":
                applyLeakedAnimation(swingProgress);
                break;
            case "Aqua":
                applyAquaAnimation(swingProgress);
                break;
            case "Astro":
                applyAstroAnimation(swingProgress);
                break;
            case "Fadeaway":
                applyFadeawayAnimation(swingProgress);
                break;
            case "Astolfo":
                applyAstolfoAnimation(swingProgress);
                break;
            case "AstolfoSpin":
                applyAstolfoSpinAnimation(swingProgress);
                break;
            case "Moon":
                applyMoonAnimation(swingProgress);
                break;
            case "MoonPush":
                applyMoonPushAnimation(swingProgress);
                break;
            case "Smooth":
                applySmoothAnimation(swingProgress);
                break;
            case "Jigsaw":
                applyJigsawAnimation(swingProgress);
                break;
            case "Tap1":
                applyTap1Animation(swingProgress);
                break;
            case "Tap2":
                applyTap2Animation(swingProgress);
                break;
            case "Sigma3":
                applySigma3Animation(swingProgress);
                break;
            case "Sigma4":
                applySigma4Animation(swingProgress);
                break;
        }
    }

    private void applySwingAnimation(float swingProgress) {
        doBlockTransformations();
    }

    private void applyOldAnimation(float swingProgress) {
        GL11.glTranslated(0.08, -0.14, -0.05);
        GlStateManager.translate(-0.35F, 0.2F, 0.0F);
        doBlockTransformations();
    }

    private void applyPushAnimation(float swingProgress) {
        float var9 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        GlStateManager.rotate(-var9 * 40.0F / 2.0F, var9 / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-var9 * 30.0F, 1.0F, var9 / 3.0F, -0.0F);
    }

    private void applyDashAnimation(float swingProgress) {
        float var9 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GL11.glRotated(-var9 * 22.0F, var9 / 2, 0.0F, 9.0F);
        GL11.glRotated(-var9 * 50.0F, 0.8F, var9 / 2, 0F);
    }

    private void applySlashAnimation(float swingProgress) {
        GL11.glTranslated(0.08, 0.08, 0.0);
        float var = MathHelper.sin((float) (MathHelper.sqrt_float(swingProgress) * Math.PI));
        GlStateManager.rotate(-var * 70F, 5F, 13F, 50F);
    }

    private void applySlideAnimation(float swingProgress) {
        GL11.glTranslated(0.08, -0.11, -0.07);
        float var91 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        GlStateManager.translate(-0.4f, 0.28f, 0.0f);
        GlStateManager.rotate(-var91 * 35.0f, -8.0f, -0.0f, 9.0f);
        GlStateManager.rotate(-var91 * 70.0f, 1.0f, -0.4f, -0.0f);
    }

    private void applyScaleAnimation(float swingProgress) {
        GL11.glTranslated(0.84, -0.77, -1.1);
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float var3 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float var4 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(var3 * -27.0F, 0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(var4 * -27.0F, 0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(var4 * -27.0F, 0.0F, 0.0F, 0.0F);
    }

    private void applySwankAnimation(float swingProgress) {
        float var9 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GL11.glRotatef(var9 * 30.0F / 2.0F, -var9, -0.0F, 9.0F);
        GL11.glRotatef(var9 * 40.0F, 1.0F, -var9 / 2.0F, -0.0F);
    }

    private void applySwangAnimation(float swingProgress) {
        GL11.glTranslated(0.0, 0.03, 0.0);
        float var9 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        GlStateManager.rotate(-var9 * 74.0F / 2.0F, var9 / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-var9 * 52.0F, 1.0F, var9 / 3.0F, -0.0F);
    }

    private void applySwonkAnimation(float swingProgress) {
        GL11.glTranslated(0.0, 0.03, 0.0);
        float var9 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        GL11.glRotated(-var9 * -30.0F / 2.0F, var9 / 2.0F, 1.0F, 4.0F);
        GL11.glRotated(-var9 * 7.5F, 1.0F, var9 / 3.0F, -0.0F);
    }

    private void applyStellaAnimation(float swingProgress) {
        GlStateManager.translate(-0.5F, 0.3F, -0.2F);
        GlStateManager.rotate(32, 0, 1, 0);
        GlStateManager.rotate(-70, 1, 0, 0);
        GlStateManager.rotate(40, 0, 1, 0);
        doBlockTransformations();
    }

    private void applySmallAnimation(float swingProgress) {
        GL11.glTranslated(-0.01, 0.03, -0.24);
        doBlockTransformations();
    }

    private void applyEditAnimation(float swingProgress) {
        GL11.glTranslated(-0.04, 0.06, 0.0);
        float Swang = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        GlStateManager.rotate(Swang * 16.0F / 2.0F, -Swang, -0.0F, 2.0F);
        GlStateManager.rotate(Swang * 22.0F, 1.0F, -Swang / 3.0F, -0.0F);
    }

    private void applyRhysAnimation(float swingProgress, float partialTicks) {
        GL11.glTranslated(0.0, 0.19, 0.0);
        GlStateManager.translate(0.41F, -0.25F, -0.5555557F);
        GlStateManager.rotate(35.0F, 0f, 1.5F, 0.0F);
        float racism = MathHelper.sin(swingProgress * swingProgress / 64 * (float) Math.PI);
        float f4 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.rotate(racism * -5.0F, 0.0F, 0.0F, 0.0F);
        GlStateManager.rotate(f4 * -12.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f4 * -65.0F, 1.0F, 0.0F, 0.0F);
    }

    private void applyStabAnimation(float swingProgress) {
        GL11.glTranslated(-0.25, 0.45, 0.8);
        float spin = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI);
        GlStateManager.translate(0.6f, 0.3f, -0.6f + -spin * 0.7);
        GlStateManager.rotate(6090, 0.0f, 0.0f, 0.1f);
        GlStateManager.rotate(6085, 0.0f, 0.1f, 0.0f);
        GlStateManager.rotate(6110, 0.1f, 0.0f, 0.0f);
    }

    private void applyFloatAnimation(float swingProgress) {
        GlStateManager.rotate(-MathHelper.sin(swingProgress * swingProgress * (float)Math.PI) * 40.0F / 2.0F,
                MathHelper.sin(swingProgress * swingProgress * (float)Math.PI) / 2.0F, -0.0F, 9.0F);
        GlStateManager.rotate(-MathHelper.sin(swingProgress * swingProgress * (float)Math.PI) * 30.0F,
                1.0F, MathHelper.sin(swingProgress * swingProgress * (float)Math.PI) / 2.0F, -0.0F);
    }

    private void applyRemixAnimation(float swingProgress) {
        float var = MathHelper.sin((float) (MathHelper.sqrt_float(swingProgress) * Math.PI));
        GlStateManager.rotate(0.0F, -2.0F, 0.0F, 10.0F);
        GlStateManager.rotate(-var * 25.0F, 0.5F, 0F, 1F);
    }

    private void applyAvatarAnimation(float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.72F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float f = MathHelper.sin(swingProgress * swingProgress * (float)Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        GlStateManager.rotate(f * -20.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f1 * -20.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f1 * -40.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
        doBlockTransformations();
    }

    private void applyXivAnimation(float swingProgress) {
        float var = MathHelper.sin((float) (MathHelper.sqrt_float(swingProgress) * Math.PI));
        float var16 = MathHelper.sin((float) (swingProgress * swingProgress * Math.PI));
        GlStateManager.rotate(-var16 * 20.0f, 0.0f, 1.0f, 0.0f);
        GlStateManager.rotate(-var * 20.0f, 0.0f, 0.0f, 1.0f);
        GlStateManager.rotate(-var * 80.0f, 1.0f, 0.0f, 0.0f);
    }

    private void applyWinterAnimation(float swingProgress) {
        GL11.glTranslated(0.0, -0.16, 0.0);
        GL11.glTranslatef(-0.35F, 0.1F, 0.0F);
        GL11.glTranslatef(-0.05F, -0.1F, 0.1F);
        doBlockTransformations();
    }

    private void applyYamatoAnimation(float swingProgress) {
        float var9 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        GL11.glRotatef(-var9 * 200F / 2.0F, -9.0F, 5.0F, 9.0F);
    }

    private void applySlideSwingAnimation(float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.72F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float f = MathHelper.sin(swingProgress * swingProgress * (float)Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        GlStateManager.rotate(f * -0.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(f1 * -0.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(f1 * -80.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
        doBlockTransformations();
    }

    private void applySmallPushAnimation(float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.72F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        float f = MathHelper.sin(swingProgress * swingProgress * (float)Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        GlStateManager.rotate(f * -10.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(f1 * -10.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.rotate(f1 * -10.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
        doBlockTransformations();
    }

    private void applyReverseAnimation(float swingProgress) {
        GL11.glTranslated(0.0, 0.1, -0.12);
        GL11.glTranslated(0.08D, -0.1D, -0.3D);
        doBlockTransformations();
    }

    private void applyInventAnimation(float swingProgress) {
        float table = MathHelper.sin((float) (MathHelper.sqrt_float(swingProgress) * Math.PI));
        GlStateManager.rotate(-table * 30.0F, -8.0F, -0.2F, 9.0F);
    }

    private void applyLeakedAnimation(float swingProgress) {
        GL11.glTranslated(0.08, 0.02, 0.0);
        float var = MathHelper.sin((float) (MathHelper.sqrt_float(swingProgress) * Math.PI));
        GlStateManager.rotate(-var * 41F, 1.1F, 0.8F, -0.3F);
    }

    private void applyAquaAnimation(float swingProgress) {
        float var9 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        GlStateManager.rotate(-var9 * 17.0F / 2.0F, var9 / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-var9 * 6.0F, 1.0F, var9 / 3.0F, -0.0F);
    }

    private void applyAstroAnimation(float swingProgress) {
        float var9 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        GlStateManager.rotate(var9 * 50.0F / 9.0F, -var9, -0.0F, 90.0F);
        GlStateManager.rotate(var9 * 50.0F, 200.0F, -var9 / 2.0F, -0.0F);
    }

    private void applyFadeawayAnimation(float swingProgress) {
        float var = MathHelper.sin((float) (MathHelper.sqrt_float(swingProgress) * Math.PI));
        float var16 = MathHelper.sin((float) (swingProgress * swingProgress * Math.PI));
        GlStateManager.rotate(-var16 * 45f, 0.0f, 0.0f, 1.0f);
        GlStateManager.rotate(-var * 0f, 0.0f, 0.0f, 1.0f);
        GlStateManager.rotate(-var * 0f, 1.5f, 0.0f, 0.0f);
    }

    private void applyAstolfoAnimation(float swingProgress) {
        float var9 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        GlStateManager.rotate(-var9 * 58.0F / 2.0F, var9 / 2.0F, 1.0F, 0.5F);
        GlStateManager.rotate(-var9 * 43.0F, 1.0F, var9 / 3.0F, -0.0F);
    }

    private void applyAstolfoSpinAnimation(float swingProgress) {
        GlStateManager.rotate(this.delay, 0.0F, 0.0F, -0.1F);
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - lastUpdateTime;
        this.delay += elapsedTime * 360.0 / 850.0;
        lastUpdateTime = currentTime;
        if (this.delay > 360.0F) {
            this.delay = 0.0F;
        }
        doBlockTransformations();
    }

    private void applyMoonAnimation(float swingProgress) {
        GL11.glTranslated(-0.08, 0.12, 0.0);
        float var9 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        GlStateManager.rotate(-var9 * 65.0F / 2.0F, var9 / 2.0F, 1.0F, 4.0F);
        GlStateManager.rotate(-var9 * 60.0F, 1.0F, var9 / 3.0F, -0.0F);
    }

    private void applyMoonPushAnimation(float swingProgress) {
        float sin = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        GlStateManager.translate(-0.2F, 0.45F, 0.25F);
        GlStateManager.rotate(-sin * 20.0F, -5.0F, -5.0F, 9.0F);
    }

    private void applySmoothAnimation(float swingProgress) {
        GL11.glTranslated(0.14, -0.1, -0.24);
        float var91 = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        GlStateManager.translate(-0.36f, 0.25f, -0.06f);
        GlStateManager.rotate(-var91 * 35.0f, -8.0f, -0.0f, 9.0f);
        GlStateManager.rotate(-var91 * 70.0f, 1.0f, 0.4f, -0.0f);
    }

    private void applyJigsawAnimation(float swingProgress) {
        GL11.glTranslated(0.0, -0.18, -0.1);
        GlStateManager.translate(-0.5D, 0.0D, 0.0D);
        doBlockTransformations();
    }

    private void applyTap1Animation(float swingProgress) {
        GlStateManager.translate(0.56F, -0.52F, -0.71999997F);
        GlStateManager.rotate(45.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((swingProgress * 0.8f - (swingProgress * swingProgress) * 0.8f) * -90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(0.37F, 0.37F, 0.37F);
    }

    private void applyTap2Animation(float swingProgress) {
        GL11.glTranslated(0.0, -0.1f, 0.0);
        GlStateManager.translate(0.56F, -0.42F, -0.71999997F);
        GlStateManager.rotate(30, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float) Math.PI) * -30.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.scale(0.4F, 0.4F, 0.4F);
    }

    private void applySigma3Animation(float swingProgress) {
        GL11.glTranslated(0.02, 0.02, 0.0);
        GL11.glTranslated(0.4D, -0.06D, -0.46D);
        float Swang = MathHelper.sin(MathHelper.sqrt_float(swingProgress) * (float)Math.PI);
        GlStateManager.rotate(Swang * 25.0F / 2.0F, -Swang, -0.0F, 9.0F);
        GlStateManager.rotate(Swang * 15.0F, 1.0F, -Swang / 2.0F, -0.0F);
    }

    private void applySigma4Animation(float swingProgress) {
        GL11.glTranslated(-0.6, 0.2, 0.11);
        float var = MathHelper.sin((float) (MathHelper.sqrt_float(swingProgress) * Math.PI));
        GlStateManager.rotate(-var * 55 / 2.0F, -8.0F, -0.0F, 9.0F);
        GlStateManager.rotate(-var * 45, 1.0F, var / 2, 0.0F);
        doBlockTransformations();
        GL11.glTranslated(-0.08, -1.25, 1.25);
    }
}