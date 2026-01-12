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

    // --- 极限紧凑配置 ---
    // 高度压缩至 26px，几乎只够放两行字，没有多余留白
    private static final float HEIGHT = 26.0f;
    private static final float RADIUS = 4.0f;
    private static final float SHADOW_SOFTNESS = 6.0f; // 阴影收紧

    private final NotificationType notificationType;
    private final String title;
    private final String description;
    private final float maxTime;
    private final long startTime;

    private float animationX;
    private float animationY;
    private boolean showing = true;

    // 字体：大字体，撑满空间
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
        boolean shadowEnabled = module != null && module.shadow.getValue();

        long elapsed = System.currentTimeMillis() - startTime;
        boolean timeUp = elapsed > maxTime;

        float targetAnimX = timeUp ? getWidth() + 20 : 0;
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
        float renderX = sr.getScaledWidth() - width - 5 + this.animationX; // 距离屏幕边缘仅 5px
        float renderY = this.animationY;

        GL11.glPushMatrix();

        // 1. 阴影 (更深、更实)
        if (shadowEnabled) {
            ShadowUtil.drawShadow(renderX, renderY, width, HEIGHT, RADIUS, SHADOW_SOFTNESS, new Color(0, 0, 0, 120).getRGB());
        }

        // 2. 背景 (深色高透)
        int bgColor = new Color(20, 20, 20, 245).getRGB();
        RenderUtil.drawRoundedRect(renderX, renderY, width, HEIGHT, RADIUS, bgColor, true, true, true, true);

        // 3. 强调色
        int accentColor = notificationType.getColor().getRGB();

        // 4. 左侧色条 (紧贴左边缘，撑满高度)
        RenderUtil.drawRoundedRect(renderX, renderY, 2.5f, HEIGHT, RADIUS, accentColor, true, false, true, false);

        // 5. 图标 (紧贴色条)
        String iconStr = getNotiIcon();
        // 垂直居中
        double iconY = renderY + (HEIGHT - ICON_FONT.getHeight()) / 2.0;
        // 色条宽度2.5 + 间距2 = 4.5
        ICON_FONT.drawString(iconStr, renderX + 5, iconY + 1, accentColor);

        // 6. 文字布局 (核心修改：消除垂直空隙)
        // 文本X起点：图标右侧紧贴
        double textStartX = renderX + 5 + ICON_FONT.getStringWidth(iconStr) + 3;

        // 标题：顶格画 (Y+2)
        // regular16 的高度大约 8px，画在 2px 处
        TITLE_FONT.drawString(title, textStartX, renderY + 2, -1);

        // 描述：紧贴标题下方 (Y+13)
        // 2 + 11 = 13，几乎没有行距
        DESC_FONT.drawString(description, textStartX, renderY + 13.5, new Color(180, 180, 180).getRGB());

        // 7. 进度条 (底部 1px)
        if (!timeUp) {
            float progress = 1.0f - (float)elapsed / maxTime;
            float barWidth = (width - RADIUS) * progress; // 从色条右侧开始算
            if (barWidth > 0) {
                // 画在最底部
                RenderUtil.drawRect(renderX + 2.5f, renderY + HEIGHT - 1f, renderX + 2.5f + barWidth, renderY + HEIGHT, accentColor);
            }
        }

        GL11.glPopMatrix();
    }

    private float lerp(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    public float getWidth() {
        String iconStr = getNotiIcon();
        double iconW = ICON_FONT.getStringWidth(iconStr);
        double titleW = TITLE_FONT.getStringWidth(title);
        double descW = DESC_FONT.getStringWidth(description);

        double maxTextW = Math.max(titleW, descW);

        // 极限宽度计算：
        // 5(Icon左边距) + IconW + 3(文字间距) + TextW + 5(右侧Padding)
        return (float) (5 + iconW + 3 + maxTextW + 5);
    }

    private String getNotiIcon() {
        switch (notificationType) {
            case OKAY: return "H";
            case INFO: return "M";
            case WARNING: return "I";
            case ERROR: return "J";
            default: return "M";
        }
    }
}