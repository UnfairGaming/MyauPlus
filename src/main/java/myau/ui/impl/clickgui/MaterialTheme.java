package myau.ui.impl.clickgui;

import java.awt.*;

public class MaterialTheme {
    // --- 现代深色模式基调 ---

    // 背景色：极深灰/黑
    public static final Color BACKGROUND_COLOR = new Color(15, 15, 19);

    // 主色调：赛博紫
    public static final Color PRIMARY_COLOR = new Color(110, 80, 255);

    // 容器背景 (极深色，半透明)
    public static final Color SURFACE_CONTAINER_LOW = new Color(20, 20, 24, 220);
    public static final Color SURFACE_CONTAINER_HIGH = new Color(45, 45, 50, 220); // 悬停色

    // 文本颜色
    public static final Color TEXT_COLOR = new Color(240, 240, 240);
    public static final Color TEXT_COLOR_SECONDARY = new Color(160, 160, 170);

    // 装饰性颜色
    public static final Color OUTLINE_COLOR = new Color(80, 80, 90, 100);
    public static final Color SHADOW_COLOR = new Color(0, 0, 0, 150);
    public static final Color PRIMARY_CONTAINER_COLOR = new Color(40, 35, 60);
    public static final Color ON_PRIMARY_CONTAINER_COLOR = new Color(220, 210, 255);

    // 布局参数
    public static final float CORNER_RADIUS_SMALL = 4.0f;
    public static final float CORNER_RADIUS_FRAME = 8.0f;

    /**
     * 获取 RGB 颜色值
     */
    public static int getRGB(Color color) {
        return color.getRGB();
    }

    /**
     * 获取带透明度的 RGB 颜色值
     */
    public static int getRGBWithAlpha(Color color, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha).getRGB();
    }
}