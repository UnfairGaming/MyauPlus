package myau.module.modules;

import myau.Myau;
import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.events.Render2DEvent;
import myau.module.Category;
import myau.module.Module;
import myau.property.properties.*;
import myau.util.*;
import myau.util.font.FontManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C02PacketUseEntity.Action;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class TargetHUD extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private final TimerUtil lastAttackTimer = new TimerUtil();

    private EntityLivingBase lastTarget = null;
    private EntityLivingBase target = null;

    // Animation Variables
    private float healthAnim = 0.0f;
    private float absorptionAnim = 0.0f;

    // Properties
    public final ModeProperty style = new ModeProperty("style", 1, new String[]{
            "ASTOLFO", "EXHIBITION", "MOON", "RISE", "NEVERLOSE", "TENACITY"
    });

    public final ModeProperty colorMode = new ModeProperty("color", 0, new String[]{"SYNC", "CUSTOM", "HEALTH", "ASTOLFO"});
    public final ModeProperty posX = new ModeProperty("position-x", 1, new String[]{"LEFT", "MIDDLE", "RIGHT"});
    public final ModeProperty posY = new ModeProperty("position-y", 1, new String[]{"TOP", "MIDDLE", "BOTTOM"});

    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 2.0F);
    public final IntProperty offX = new IntProperty("offset-x", 0, -500, 500);
    public final IntProperty offY = new IntProperty("offset-y", 40, -500, 500);

    // Settings
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final BooleanProperty kaOnly = new BooleanProperty("ka-only", true);
    public final BooleanProperty chatPreview = new BooleanProperty("chat-preview", false);

    public TargetHUD() {
        super("TargetHUD", "TargetHUD", Category.RENDER, 0, false, true);
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (!this.isEnabled() || mc.thePlayer == null) return;

        EntityLivingBase entityLivingBase = this.target;
        this.target = this.resolveTarget();

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS); // 保存所有OpenGL属性
        
        if (this.target != null) {
            if (this.target != entityLivingBase) {
                this.healthAnim = this.target.getHealth();
                this.absorptionAnim = this.target.getAbsorptionAmount();
            }

            float frameTime = 20f / Minecraft.getDebugFPS();
            this.healthAnim = RenderUtil.lerpFloat(this.target.getHealth(), this.healthAnim, 0.2f * frameTime);
            this.absorptionAnim = RenderUtil.lerpFloat(this.target.getAbsorptionAmount(), this.absorptionAnim, 0.2f * frameTime);
            this.healthAnim = MathHelper.clamp_float(this.healthAnim, 0.0f, this.target.getMaxHealth());

            // ==========================================================
            //                   STATE RESET FIX (核心修复)
            // ==========================================================
            // 1. 允许写入 Stencil (否则 glClear 清除不了缓存，导致模式切换后残留)
            GL11.glStencilMask(0xFF);
            // 2. 清除 Stencil 缓存，防止上一个模式的遮罩残留
            // 注释掉这行，因为它可能干扰阴影渲染
            // GL11.glClear(GL11.GL_STENCIL_BUFFER_BIT);
            // 3. 确保 Stencil 测试默认是关闭的
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            // 4. 确保 Scissor (裁剪) 测试默认是关闭的 (防止 Exhibition 模式残留)
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            // 5. 确保混合开启
            GlStateManager.enableBlend();
            // ==========================================================

            switch (this.style.getValue()) {
                case 0: renderAstolfo(); break;
                case 1: renderExhibition(); break;
                case 2: renderMoon(); break;
                case 3: renderRise(); break;
                case 4: renderNeverlose(); break;
                case 5: renderTenacity(); break;
            }

            // 渲染结束后重置颜色，防止影响 Minecraft 其他部分的渲染
            GlStateManager.resetColor();
        }
        
        GL11.glPopAttrib(); // 恢复所有OpenGL属性
    }

    // =========================================================================
    //                            MODE IMPLEMENTATIONS
    // =========================================================================

    private void renderAstolfo() {
        float width = Math.max(130, (float) (FontManager.regular18.getStringWidth(target.getName()) + 60));
        float height = 56;
        float[] pos = getPosition(width, height);
        float x = pos[0];
        float y = pos[1];

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 1.0F);

        GL11.glPushMatrix();
        if (shadow.getValue()) ShadowUtil.drawShadow(0, 0, width, height, 6, 6, new Color(0,0,0,100).getRGB());
        RenderUtil.drawRect(0, 0, width, height, new Color(0, 0, 0, 100).getRGB());
        GL11.glPopMatrix();
        drawEntityOnScreen(target);
        FontManager.regular18.drawString(target.getName(), 50, 6, -1, true);

        GlStateManager.pushMatrix();
        GlStateManager.scale(1.5, 1.5, 1.5);
        String hpText = String.format("%.1f", target.getHealth()) + " ❤";
        FontManager.regular18.drawString(hpText, (50) / 1.5f, (22) / 1.5f, getColor().getRGB(), true);
        GlStateManager.popMatrix();

        float healthPct = healthAnim / target.getMaxHealth();
        float barWidth = width - 54;
        float barX = 48;
        float barY = 42;
        float barHeight = 7;

        RenderUtil.drawRect(barX, barY, barX + barWidth, barY + barHeight, ColorUtil.darker(getColor(), 0.3f).getRGB());
        if (healthPct > 0) {
            RenderUtil.drawRect(barX, barY, barX + barWidth * healthPct, barY + barHeight, getColor().getRGB());
        }

        GlStateManager.popMatrix();
    }

    private void renderExhibition() {
        ScaledResolution resolution = new ScaledResolution(mc);
        double boxWidth = 40 + FontManager.tahomaBold16.getStringWidth(target.getName());
        double renderWidth = Math.max(boxWidth, 120);

        float posX = this.offX.getValue().floatValue() / this.scale.getValue();
        switch (this.posX.getValue()) {
            case 1:
                posX += (float) resolution.getScaledWidth() / this.scale.getValue() / 2.0F - (float)renderWidth / 2.0F;
                break;
            case 2:
                posX *= -1.0F;
                posX += (float) resolution.getScaledWidth() / this.scale.getValue() - (float)renderWidth;
        }

        float posY = this.offY.getValue().floatValue() / this.scale.getValue();
        switch (this.posY.getValue()) {
            case 1:
                posY += (float) resolution.getScaledHeight() / this.scale.getValue() / 2.0F - 20.0F;
                break;
            case 2:
                posY *= -1.0F;
                posY += (float) resolution.getScaledHeight() / this.scale.getValue() - 40.0F;
        }

        GlStateManager.pushMatrix();
        GlStateManager.translate(posX + (float)renderWidth / 2.0F, posY + 20.0F, 0);
        GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 0.0F);
        GlStateManager.translate(-(float)renderWidth / 2.0F, -20.0F, 0);

        GL11.glPushMatrix();
        if (shadow.getValue()) {
            ShadowUtil.drawShadow(-2.5f, -2.5f, (float)renderWidth + 5.0f, 45.0f, 4, 4, new Color(0, 0, 0, 100).getRGB());
        }
        drawExhibitionBorderedRect(-2.5F, -2.5F, (float)renderWidth + 2.5F, 40 + 2.5F, 0.5F, getExhibitionColor(60), getExhibitionColor(10));
        drawExhibitionBorderedRect(-1.5F, -1.5F, (float)renderWidth + 1.5F, 40 + 1.5F, 1.5F, getExhibitionColor(60), getExhibitionColor(40));
        drawExhibitionBorderedRect(0, 0, (float)renderWidth, 40, 0.5F, getExhibitionColor(22), getExhibitionColor(60));
        drawExhibitionBorderedRect(2, 2, 38, 38, 0.5F, getExhibitionColor(0, 0), getExhibitionColor(10));
        drawExhibitionBorderedRect(2.5F, 2.5F, 38 - 0.5F, 38 - 0.5F, 0.5F, getExhibitionColor(17), getExhibitionColor(48));

        GlStateManager.pushMatrix();
        int factor = resolution.getScaleFactor();
        float sx = (posX + 3) * this.scale.getValue();
        float sy = (posY + 3) * this.scale.getValue();
        float sw = 34 * this.scale.getValue();
        float sh = 34 * this.scale.getValue();

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor((int) (sx * factor), (int) (mc.displayHeight - (sy + sh) * factor), (int) (sw * factor), (int) (sh * factor));
        drawEntityOnScreen(target);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GlStateManager.popMatrix();

        GlStateManager.translate(2, 0, 0);
        GlStateManager.pushMatrix();
        FontManager.tahomaBold16.drawString(target.getName(), 46, 4, -1, true);
        GlStateManager.popMatrix();

        float health = target.getHealth();
        float maxHealth = target.getMaxHealth();
        float progress = health / (maxHealth + target.getAbsorptionAmount());
        float realHealthProgress = (health / maxHealth);

        Color customColor = health >= 0 ? blendColors(new float[]{0f, 0.5f, 1f}, new Color[]{Color.RED, Color.YELLOW, Color.GREEN}, realHealthProgress).brighter() : Color.RED;
        double width = Math.min(FontManager.tahomaBold16.getStringWidth(target.getName()), 60);
        width = getIncremental(width);
        if (width < 60) width = 60;

        double healthLocation = width * progress;

        drawExhibitionBorderedRect(37, 12, (float)(39 + width), 16, 0.5F, getExhibitionColor(0, 0), getExhibitionColor(0));
        drawExhibitionRect((float)(38 + healthLocation + 0.5F), 12.5F, (float)(38 + width + 0.5F), 15.5F, getExhibitionColorOpacity(customColor.getRGB()));
        drawExhibitionRect(37.5F, 12.5F, (float)(38 + healthLocation + 0.5F), 15.5F, customColor.getRGB());

        for (int i = 1; i < 10; i++) {
            double dThing = (width / 10) * i;
            drawExhibitionRect((float)(38 + dThing), 12, (float)(38 + dThing + 0.5F), 16, getExhibitionColor(0));
        }

        String str = "HP: " + (int) health + " | Dist: " + (int) mc.thePlayer.getDistanceToEntity(target);
        GlStateManager.pushMatrix();
        FontManager.tahomaBold12.drawString(str, 53, 26, -1, true);
        GlStateManager.popMatrix();

        if (target instanceof EntityPlayer) {
            EntityPlayer targetPlayer = (EntityPlayer) target;
            GL11.glPushMatrix();
            List<ItemStack> items = new ArrayList<>();
            int split = 20;

            for (int index = 3; index >= 0; --index) {
                if (targetPlayer.inventory.armorInventory[index] != null) items.add(targetPlayer.inventory.armorInventory[index]);
            }
            if (targetPlayer.getCurrentEquippedItem() != null) items.add(targetPlayer.getCurrentEquippedItem());

            RenderHelper.enableGUIStandardItemLighting();
            int yOffset = 23;
            for (ItemStack itemStack : items) {
                if (mc.theWorld != null) split += 16;
                GlStateManager.pushMatrix();
                GlStateManager.disableAlpha();
                GlStateManager.clear(256);
                mc.getRenderItem().zLevel = -150.0f;
                mc.getRenderItem().renderItemAndEffectIntoGUI(itemStack, split, yOffset);
                mc.getRenderItem().renderItemOverlays(mc.fontRendererObj, itemStack, split, yOffset);
                mc.getRenderItem().zLevel = 0.0f;

                if (itemStack.getItem() instanceof ItemSword) {
                    int sLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, itemStack);
                    if (sLevel > 0) { drawEnchantTag("S" + sLevel, split, yOffset);
                    }
                } else if ((itemStack.getItem() instanceof ItemArmor)) {
                    int pLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.protection.effectId, itemStack);
                    if (pLevel > 0) drawEnchantTag("P" + pLevel, split, yOffset);
                }

                GlStateManager.disableBlend();
                GlStateManager.disableLighting();
                GlStateManager.enableAlpha();
                GlStateManager.popMatrix();
            }
            RenderHelper.disableStandardItemLighting();
            GL11.glPopMatrix();
        }
        GL11.glPopMatrix();
        GlStateManager.popMatrix();
    }

    private void renderMoon() {
        float width = (float) (35 + FontManager.tenacity16.getStringWidth(target.getName()) + 33);
        width = Math.max(width, 110);
        float height = 40.5f;
        float[] pos = getPosition(width, height);
        float x = pos[0];
        float y = pos[1];

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 1.0F);

        float healthPercentage = healthAnim / target.getMaxHealth();
        float space = (width - 48);

        GL11.glPushMatrix();
        if (shadow.getValue()) ShadowUtil.drawShadow(0, 0, width, height, 8, 5, new Color(0,0,0,100).getRGB());
        RenderUtil.drawRoundedRect(0, 0, width, height, 8, false, new Color(20, 20, 20, 220));
        GL11.glPopMatrix();
        RenderUtil.drawRoundedRect(42, 26.5f, space, 8, 4, false, new Color(0, 0, 0, 150));

        if (healthPercentage > 0) {
            RenderUtil.scissor(x + 42, y + 26.5f, space * healthPercentage, 8.5f);
            RenderUtil.drawRoundedRect(42, 26.5f, space, 8.5f, 4, false, getColor());
            RenderUtil.releaseScissor();
        }

        drawFace(target, 2.5f, 2.5f, 35, 35, 8); // Uses new helper

        String text = String.format("%.1f", target.getHealth());
        FontManager.tenacity12.drawString(text + "HP", 40, 17, -1, true);
        FontManager.tenacity16.drawString(target.getName(), 40, 6, -1, true);

        GlStateManager.popMatrix();
    }

    private void renderRise() {
        float width = Math.max(160, (float) (FontManager.productSans18.getStringWidth(target.getName()) + 30));
        float height = 40.5f;
        float[] pos = getPosition(width, height);
        float x = pos[0];
        float y = pos[1];

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 1.0F);

        float healthPercentage = healthAnim / target.getMaxHealth();
        float space = (width - 48);

        GL11.glPushMatrix();
        if (shadow.getValue()) ShadowUtil.drawShadow(0, 0, width, height, 8, 5, new Color(0,0,0,100).getRGB());
        RenderUtil.drawRoundedRect(0, 0, width, height, 8, false, new Color(10, 10, 10, 200));
        GL11.glPopMatrix();
        RenderUtil.drawRoundedRect(42, 22f, space, 6, 3, false, new Color(0, 0, 0, 120));

        if (healthPercentage > 0) {
            RenderUtil.scissor(x + 42, y + 22f, space * healthPercentage, 6);
            RenderUtil.drawRoundedRect(42, 22f, space, 6, 3, false, getColor());
            RenderUtil.releaseScissor();
        }

        drawFace(target, 4.0f, 3.3f, 33, 33, 6);

        String text = String.format("%.1f", target.getHealth());
        FontManager.productSans18.drawString(text + " ", 134, 9, getColor().getRGB(), true);
        FontManager.productSans18.drawString(target.getName(), 42, 9, -1, true);

        GlStateManager.popMatrix();
    }

    private void renderNeverlose() {
        float width = 125.0f;
        float height = 32.5f;
        float xoffset = 1;
        float yoffset = -1;

        float dynamicWidth = Math.max(width, (float) FontManager.tenacity16.getStringWidth(target.getName()) + 42);
        float[] pos = getPosition(dynamicWidth, height);
        float x = pos[0];
        float y = pos[1];
        float circleX = xoffset + dynamicWidth - 27.5f;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 1.0F);

        GL11.glPushMatrix();
        if (shadow.getValue()) ShadowUtil.drawShadow(xoffset, yoffset, dynamicWidth, height, 4, 4, new Color(0,0,0,150).getRGB());
        RenderUtil.drawRoundedRect(xoffset, yoffset, dynamicWidth, height, 4, false, new Color(10, 10, 16, 240));
        GL11.glPopMatrix();

        drawFace(target, xoffset + 3f, yoffset + 3f, 26, 26, 4);

        drawCircle(circleX, new Color(0,0,0,100).getRGB());
        drawArc(circleX, 360 * (healthAnim / target.getMaxHealth()), getColor().getRGB());

        FontManager.tenacity16.drawString(target.getName(), xoffset + 34, yoffset + 8, -1, true);
        FontManager.tenacity12.drawString("Distance: " + String.format("%.1f", target.getDistanceToEntity(mc.thePlayer)) + "m", xoffset + 34, yoffset + 20, getColor().getRGB(), true);

        String text = String.format("%.0f", target.getHealth());
        FontManager.tenacity12.drawString(text, circleX - (float)FontManager.tenacity12.getStringWidth(text)/2f, 16 - 3, -1, true);

        GlStateManager.popMatrix();
    }

    private void renderTenacity() {
        float width = Math.max((long) 120F, (float) (FontManager.tenacity20.getStringWidth(target.getName()) + 50));
        float height = 44;
        float[] pos = getPosition(width, height);
        float x = pos[0];
        float y = pos[1];

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(this.scale.getValue(), this.scale.getValue(), 1.0F);

        GL11.glPushMatrix();
        if (shadow.getValue()) ShadowUtil.drawShadow(0, 0, width, height, 6, 6, new Color(0,0,0,120).getRGB());
        RenderUtil.drawRoundedRect(0, 0, width, height, 6, false, new Color(0, 0, 0, 180));
        GL11.glPopMatrix();

        drawFace(target, 4, 4, 34, 34, 6);

        FontManager.tenacity20.drawString(target.getName(), 43, 10, -1, true);
        FontManager.tenacity12.drawString("HP: " + String.format("%.1f", healthAnim), 43, 20, -1, true);

        float barW = width - 52;
        float pct = healthAnim / target.getMaxHealth();
        RenderUtil.drawRoundedRect(44, 30, barW, 6, 3, false, new Color(0,0,0,150));

        if (pct > 0) {
            RenderUtil.scissor(x + 44, y + 30, barW * pct, 6);
            RenderUtil.drawRoundedRect(44, 30, barW, 6, 3, false, getColor());
            RenderUtil.releaseScissor();
        }

        GlStateManager.popMatrix();
    }

    // Helpers
    private void drawFace(EntityLivingBase entity, float x, float y, float width, float height, float radius) {
        if (entity instanceof EntityPlayer) {
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(entity.getName());
            ResourceLocation skin = (info != null) ? info.getLocationSkin() : new ResourceLocation("textures/entity/steve.png");

            // 直接调用 RenderUtil 绘制，不需要这里手动检查 Stencil
            RenderUtil.drawRoundedHead(skin, x, y, width, height, radius);
        }
    }

    private void drawEntityOnScreen(EntityLivingBase ent) {
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate(20.0F, 36.0F, 50.0F);

        float largestSize = Math.max(ent.height, ent.width);
        float relativeScale = Math.max(largestSize / 1.8F, 1);

        GlStateManager.scale((float) -16 / relativeScale, (float) 16 / relativeScale, (float) 16 / relativeScale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-((float) Math.atan((float) 17 / 40.0F)) * 20.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(0.0F, 0.0F, 0.0F);
        RenderManager renderManager = mc.getRenderManager();
        renderManager.setPlayerViewY(180.0F);
        renderManager.setRenderShadow(false);
        renderManager.renderEntityWithPosYaw(ent, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
        renderManager.setRenderShadow(true);
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    private double getIncremental(double value) {
        return Math.ceil(value / 10.0) * 10.0;
    }

    // Exhibition Color Helpers
    private int getExhibitionColor(int brightness) {
        return getExhibitionColor(brightness, brightness, brightness, 255);
    }

    private int getExhibitionColor(int brightness, int alpha) {
        return getExhibitionColor(brightness, brightness, brightness, alpha);
    }

    private int getExhibitionColor(int red, int green, int blue, int alpha) {
        int color = 0;
        color |= Math.max(0, Math.min(255, alpha)) << 24;
        color |= Math.max(0, Math.min(255, red)) << 16;
        color |= Math.max(0, Math.min(255, green)) << 8;
        color |= Math.max(0, Math.min(255, blue));
        return color;
    }

    private int getExhibitionColorOpacity(int color) {
        int red = (color >> 16 & 0xFF);
        int green = (color >> 8 & 0xFF);
        int blue = (color & 0xFF);
        return getExhibitionColor(red, green, blue, Math.min(255, 35));
    }

    private void drawExhibitionRect(float x1, float y1, float x2, float y2, int color) {
        RenderUtil.enableRenderState();
        RenderUtil.drawRect(x1, y1, x2, y2, color);
        RenderUtil.disableRenderState();
    }

    private void drawExhibitionBorderedRect(float x1, float y1, float x2, float y2, float borderWidth, int fillColor, int borderColor) {
        RenderUtil.enableRenderState();
        RenderUtil.drawRect(x1, y1, x2, y2, borderColor);
        RenderUtil.drawRect(x1 + borderWidth, y1 + borderWidth, x2 - borderWidth, y2 - borderWidth, fillColor);
        RenderUtil.disableRenderState();
    }

    private void drawEnchantTag(String text, int x, float y) {
        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        mc.fontRendererObj.drawString(text, x * 2, (int)(y * 2), -1, true);
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
    }

    private Color blendColors(float[] fractions, Color[] colors, float progress) {
        if (fractions.length == colors.length) {
            int[] indicies = getFractionIndicies(fractions, progress);
            float[] range = new float[]{fractions[indicies[0]], fractions[indicies[1]]};
            Color[] colorRange = new Color[]{colors[indicies[0]], colors[indicies[1]]};
            float max = range[1] - range[0];
            float value = progress - range[0];
            float weight = value / max;
            return blend(colorRange[0], colorRange[1], 1.0F - weight);
        } else {
            return colors[0];
        }
    }

    private int[] getFractionIndicies(float[] fractions, float progress) {
        int[] range = new int[2];
        int startPoint = 0;
        while (startPoint < fractions.length && fractions[startPoint] <= progress) {
            ++startPoint;
        }
        if (startPoint >= fractions.length) {
            startPoint = fractions.length - 1;
        }
        range[0] = startPoint - 1;
        range[1] = startPoint;
        return range;
    }

    private Color blend(Color color1, Color color2, double ratio) {
        float r = (float) ratio;
        float ir = 1.0F - r;
        float[] rgb1 = color1.getColorComponents(new float[3]);
        float[] rgb2 = color2.getColorComponents(new float[3]);
        return new Color(rgb1[0] * r + rgb2[0] * ir, rgb1[1] * r + rgb2[1] * ir, rgb1[2] * r + rgb2[2] * ir);
    }

    private void drawCircle(float x, int color) {
        RenderUtil.enableRenderState();
        RenderUtil.setColor(color);
        GL11.glLineWidth((float) 3);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= 360; i++) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(x + Math.sin(angle) * (float) 12, (float) 16 + Math.cos(angle) * (float) 12);
        }
        GL11.glEnd();
        RenderUtil.disableRenderState();
    }

    private void drawArc(float x, float degrees, int color) {
        RenderUtil.enableRenderState();
        RenderUtil.setColor(color);
        GL11.glLineWidth((float) 3);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 0; i <= degrees; i++) {
            double angle = Math.toRadians(i);
            GL11.glVertex2d(x + Math.sin(angle) * (float) 12, (float) 16 - Math.cos(angle) * (float) 12);
        }
        GL11.glEnd();
        RenderUtil.disableRenderState();
    }

    private int astolfoColors() {
        float speed = 3000f;
        float hue = (float) (System.currentTimeMillis() % (int)speed) + 0;
        while (hue > speed) {
            hue -= speed;
        }
        hue /= speed;
        if (hue > 0.5) {
            hue = 0.5F - (hue - 0.5F);
        }
        hue += 0.5F;
        return Color.HSBtoRGB(hue, 0.5F, 1F);
    }

    private Color getColor() {
        switch (colorMode.getValue()) {
            case 0: return new Color(0, 150, 255);
            case 1: return new Color(255, 50, 50);
            case 2: return ColorUtil.getHealthBlend(target.getHealth() / target.getMaxHealth());
            case 3: return new Color(astolfoColors());
        }
        return Color.WHITE;
    }

    private EntityLivingBase resolveTarget() {
        KillAura killAura = (KillAura) Myau.moduleManager.modules.get(KillAura.class);
        if (killAura.isEnabled() && killAura.isAttackAllowed() && TeamUtil.isEntityLoaded(killAura.getTarget())) {
            return killAura.getTarget();
        } else if (!(Boolean) this.kaOnly.getValue() && !this.lastAttackTimer.hasTimeElapsed(1500L) && TeamUtil.isEntityLoaded(this.lastTarget)) {
            return this.lastTarget;
        } else {
            return this.chatPreview.getValue() && mc.currentScreen instanceof GuiChat ? mc.thePlayer : null;
        }
    }

    private float[] getPosition(float width, float height) {
        ScaledResolution sr = new ScaledResolution(mc);
        float posX = this.offX.getValue().floatValue() / this.scale.getValue();
        float posY = this.offY.getValue().floatValue() / this.scale.getValue();

        switch (this.posX.getValue()) {
            case 1: posX += (float) sr.getScaledWidth() / this.scale.getValue() / 2.0F - width / 2.0F; break;
            case 2: posX *= -1.0F; posX += (float) sr.getScaledWidth() / this.scale.getValue() - width; break;
        }

        switch (this.posY.getValue()) {
            case 1: posY += (float) sr.getScaledHeight() / this.scale.getValue() / 2.0F - height / 2.0F; break;
            case 2: posY *= -1.0F; posY += (float) sr.getScaledHeight() / this.scale.getValue() - height; break;
        }
        return new float[]{posX, posY};
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (event.getType() == EventType.SEND && event.getPacket() instanceof C02PacketUseEntity) {
            C02PacketUseEntity packet = (C02PacketUseEntity) event.getPacket();
            if (packet.getAction() != Action.ATTACK) return;
            Entity entity = packet.getEntityFromWorld(mc.theWorld);
            if (entity instanceof EntityLivingBase && !(entity instanceof EntityArmorStand)) {
                this.lastAttackTimer.reset();
                this.lastTarget = (EntityLivingBase) entity;
            }
        }
    }

    @Override
    public void onDisabled() {
        super.onDisabled();
        this.target = null;
        this.lastTarget = null;
    }
}