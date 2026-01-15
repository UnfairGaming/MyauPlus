package myau.ui.impl.notification;

import lombok.Getter;
import myau.Myau;
import myau.module.modules.NotificationModule;
import myau.util.RenderUtil;
import myau.util.ShadowUtil;
import myau.util.font.FontManager;
import myau.util.font.impl.FontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import java.awt.*;

@Getter
public class Notification {
    private static final Minecraft mc = Minecraft.getMinecraft();

    private float height = 30.0f;

    private final NotificationType notificationType;
    private final String title;
    private final String description;
    private final float maxTime;
    private final long startTime;

    private float animationX;
    private float animationY;
    private boolean showing = true;

    private static final FontRenderer ICON_FONT = FontManager.noti20;
    private static final FontRenderer TITLE_FONT = FontManager.tenacity20;
    private static final FontRenderer DESC_FONT = FontManager.tenacity16;

    public Notification(NotificationType type, String title, String description, long time) {
        this.notificationType = type;
        this.title = title;
        this.description = description;
        this.maxTime = (float) time;
        this.startTime = System.currentTimeMillis();

        this.animationX = getWidth() + 20;
        this.animationY = -1;
    }

    public void render(float targetY) {
        if (!showing) return;

        NotificationModule module = (NotificationModule) Myau.moduleManager.modules.get(NotificationModule.class);
        String mode = module.notificationMode.getModeString();
        boolean shadowEnabled = module.shadow.getValue();
        boolean isLeft = module.positionX.getValue() == 0; // 0 表示 LEFT，1 表示 RIGHT

        // 动态高度设置
        switch (mode) {
            case "Idk":
                this.height = 26.0f;
                break;
            case "Clean":
                this.height = 32.0f;
                break;
            case "Compact":
                this.height = 22.0f; // 紧凑模式高度更小

                break;
            case "Sharp":
                this.height = 28.0f;
                break;
            default:
                this.height = 30.0f;
                break;
        }

        long elapsed = System.currentTimeMillis() - startTime;
        boolean timeUp = elapsed > maxTime;

        float targetAnimX = timeUp ? (isLeft ? -getWidth() - 20 : getWidth() + 20) : 0;
        this.animationX = lerp(this.animationX, targetAnimX, 0.15f);

        if (this.animationY == -1) {
            this.animationY = targetY;
        } else {
            this.animationY = lerp(this.animationY, targetY, 0.3f);
        }

        if (timeUp && Math.abs(this.animationX - targetAnimX) < 1.0f) {
            showing = false;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        float width = getWidth();
        // 根据X轴位置决定通知的渲染位置
        float renderX;
        if (isLeft) {
            renderX = 5 + this.animationX; // 从左边距5开始
        } else {
            renderX = sr.getScaledWidth() - width - 5 + this.animationX; // 从右边距5开始
        }
        float renderY = this.animationY;

        int accentColor = notificationType.getColor().getRGB();
        int descColor = new Color(180, 180, 180).getRGB();
        float progress = Math.max(0, Math.min(1, 1.0f - (float)elapsed / maxTime));

        GL11.glPushMatrix();

        // 通用黑色阴影逻辑 (部分模式除外)
        if (shadowEnabled && !mode.equals("Clean") && !mode.equals("Glow")) {
            ShadowUtil.drawShadow(renderX, renderY, width, height, 4.0f, 6.0f, new Color(0, 0, 0, 120).getRGB());
        }

        switch (mode) {
            case "Idk":
                RenderUtil.drawRoundedRect(renderX, renderY, width, height, 4.0f, new Color(20, 20, 20, 245).getRGB(), true, true, true, true);
                RenderUtil.drawRoundedRect(renderX, renderY, 2.5f, height, 4.0f, accentColor, true, false, true, false);
                drawIconAndText(renderX, renderY, height, accentColor, -1, descColor, true);
                if (!timeUp) {
                    float barWidth = (width - 4.0f) * progress;
                    if (barWidth > 0) RenderUtil.drawRect(renderX + 2.5f, renderY + height - 1f, renderX + 2.5f + barWidth, renderY + height, accentColor);
                }
                break;

            case "Modern":
                RenderUtil.drawRoundedRect(renderX, renderY, width, height, 5.0f, new Color(28, 28, 28, 255).getRGB(), true, true, true, true);
                drawIconAndText(renderX, renderY, height, accentColor, -1, descColor, true);
                if (!timeUp) {
                    RenderUtil.drawRoundedRect(renderX + 2, renderY + height - 2, (width - 4) * progress, 2, 1, accentColor, true, true, true, true);
                }
                break;

            case "Clean":
                if (shadowEnabled) ShadowUtil.drawShadow(renderX, renderY, width, height, 3.0f, 8.0f, new Color(200, 200, 200, 100).getRGB());
                RenderUtil.drawRoundedRect(renderX, renderY, width, height, 3.0f, new Color(250, 250, 250).getRGB(), true, true, true, true);
                drawIconAndText(renderX, renderY, height, accentColor, new Color(40, 40, 40).getRGB(), new Color(100, 100, 100).getRGB(), false);
                break;

            case "Split": // [新增] 分割模式：左侧方块主题色，右侧黑色
                // 右半部分深色背景
                RenderUtil.drawRoundedRect(renderX + height, renderY, width - height, height, 4.0f, new Color(24, 24, 24).getRGB(), false, true, false, true);
                // 左半部分主题色方块
                RenderUtil.drawRoundedRect(renderX, renderY, height, height, 4.0f, accentColor, true, false, true, false);

                // 图标绘制在左侧中心，使用白色
                String splitIcon = getIconChar(false); // 用简洁图标
                float sIconX = (float) (renderX + height/2.0f - ICON_FONT.getStringWidth(splitIcon)/2.0f);
                float sIconY = (float) (renderY + height/2.0f - ICON_FONT.getHeight()/2.0f + 1);
                ICON_FONT.drawString(splitIcon, sIconX, sIconY, -1);

                // 文字绘制
                float textLeft = renderX + height + 5;
                TITLE_FONT.drawString(title, textLeft, renderY + 4, -1);
                DESC_FONT.drawString(description, textLeft, renderY + 15, descColor);
                break;

            case "Glow": // [新增] 柔光模式：背景半透黑，阴影为主题色
                // 主题色光晕
                if (shadowEnabled) {
                    ShadowUtil.drawShadow(renderX, renderY, width, height, 4.0f, 12.0f, accentColor);
                }
                RenderUtil.drawRoundedRect(renderX, renderY, width, height, 4.0f, new Color(15, 15, 15, 240).getRGB(), true, true, true, true);
                // 底部荧光条
                if (!timeUp) {
                    RenderUtil.drawRoundedRect(renderX + 4, renderY + height - 2, (width - 8) * progress, 1.5f, 0.5f, accentColor, true, true, true, true);
                }
                drawIconAndText(renderX, renderY, height, accentColor, -1, descColor, false);
                break;

            case "Compact": // [新增] 紧凑模式：高度很小(22)，左侧细条
                RenderUtil.drawRoundedRect(renderX, renderY, width, height, 3.0f, new Color(20, 20, 20, 250).getRGB(), true, true, true, true);
                RenderUtil.drawRoundedRect(renderX, renderY, 2.0f, height, 3.0f, accentColor, true, false, true, false);

                // 紧凑排版：图标缩小或微调位置
                String cpIcon = getIconChar(false);
                float cpIconW = (float) ICON_FONT.getStringWidth(cpIcon);
                float cpIconY = (float) (renderY + height/2.0f - ICON_FONT.getHeight()/2.0f + 1);
                ICON_FONT.drawString(cpIcon, renderX + 6, cpIconY, accentColor);

                // 标题和描述更紧凑
                TITLE_FONT.drawString(title, renderX + 6 + cpIconW + 4, renderY + 3, -1);
                // 描述字体如果太大可能会挤，这里微调位置
                DESC_FONT.drawString(description, renderX + 6 + cpIconW + 4, renderY + 12, descColor);
                break;

            case "Dark":
                RenderUtil.drawRoundedRect(renderX, renderY, width, height, 2.0f, new Color(10, 10, 10).getRGB(), true, true, true, true);
                drawIconAndText(renderX, renderY, height, accentColor, -1, descColor, true);
                RenderUtil.drawRect(renderX + width - 3, renderY + 4, renderX + width - 1, renderY + height - 4, accentColor);
                break;

            case "Outline":
                RenderUtil.drawRoundedRect(renderX, renderY, width, height, 4.0f, new Color(20, 20, 20, 200).getRGB(), true, true, true, true);
                RenderUtil.drawRoundedRectOutline(renderX, renderY, width, height, 4.0f, 1.0f, accentColor, true, true, true, true);
                drawIconAndText(renderX, renderY, height, accentColor, -1, descColor, false);
                if (!timeUp) {
                    RenderUtil.drawRect(renderX + 4, renderY + height - 2, renderX + 4 + (width - 8) * progress, renderY + height - 1, accentColor);
                }
                break;

            case "Glass":
                RenderUtil.drawRoundedRect(renderX, renderY, width, height, 4.0f, new Color(40, 40, 40, 140).getRGB(), true, true, true, true);
                RenderUtil.drawRoundedRectOutline(renderX, renderY, width, height, 4.0f, 0.5f, new Color(255, 255, 255, 100).getRGB(), true, true, true, true);
                drawIconAndText(renderX, renderY, height, accentColor, -1, new Color(220, 220, 220).getRGB(), true);
                break;

            case "Sharp":
                RenderUtil.drawRect(renderX, renderY, renderX + width, renderY + height, new Color(20, 20, 20).getRGB());
                RenderUtil.drawRect(renderX, renderY, renderX + 2, renderY + height, accentColor);
                drawIconAndText(renderX, renderY, height, accentColor, -1, descColor, false);
                RenderUtil.drawRect(renderX, renderY + height - 1, renderX + width * progress, renderY + height, accentColor);
                break;
        }

        GL11.glPopMatrix();
    }

    private void drawIconAndText(float x, float y, float h, int iconColor, int titleColor, int descColor, boolean useCircleIcon) {
        String iconStr = getIconChar(useCircleIcon);
        float iconW = (float) ICON_FONT.getStringWidth(iconStr);
        float textStartX = x + 8 + iconW + 4;

        float iconY = (float) (y + (h - ICON_FONT.getHeight()) / 2.0f + 1.0f);
        ICON_FONT.drawString(iconStr, x + 8, iconY, iconColor);

        TITLE_FONT.drawString(title, textStartX, y + (h/2) - 8, titleColor);
        DESC_FONT.drawString(description, textStartX, y + (h/2) + 2, descColor);
    }

    private float lerp(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    public float getWidth() {
        NotificationModule module = (NotificationModule) Myau.moduleManager.modules.get(NotificationModule.class);
        String mode = module != null ? module.notificationMode.getModeString() : "Exhi";

        // Split模式由于左侧是固定宽度的方块(宽度等于高度)，文字起始点不同，需要单独计算
        if (mode.equals("Split")) {
            double maxTextW = Math.max(TITLE_FONT.getStringWidth(title), DESC_FONT.getStringWidth(description));
            return (float) (height + 5 + maxTextW + 8);
        }

        // 紧凑模式间距稍小
        if (mode.equals("Compact")) {
            String iconStr = getIconChar(false);
            double iconW = ICON_FONT.getStringWidth(iconStr);
            double maxTextW = Math.max(TITLE_FONT.getStringWidth(title), DESC_FONT.getStringWidth(description));
            return (float) (6 + iconW + 4 + maxTextW + 6);
        }

        String iconStr = getIconChar(!mode.equals("Clean") && !mode.equals("Outline") && !mode.equals("Glow") && !mode.equals("Sharp"));
        double iconW = ICON_FONT.getStringWidth(iconStr);
        double titleW = TITLE_FONT.getStringWidth(title);
        double descW = DESC_FONT.getStringWidth(description);
        double maxTextW = Math.max(titleW, descW);
        return (float) (8 + iconW + 4 + maxTextW + 8);
    }

    // 根据模式决定图标风格
    private String getIconChar(boolean circleStyle) {
        switch (notificationType) {
            case OKAY:
            case INFO:
                return circleStyle ? "H" : "T";
            case WARNING:
            case ERROR:
                return circleStyle ? "I" : "U";
            default: return "H";
        }
    }
}