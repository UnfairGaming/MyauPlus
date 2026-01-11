package myau.util;

public class AnimationUtil {
    // 简单的线性插值
    public static float animate(float target, float current, float speed) {
        if (Math.abs(target - current) < 0.0001f) return target;
        return current + (target - current) * speed;
    }
    
    // 颜色插值
    public static int interpolateColor(int color1, int color2, float fraction) {
        fraction = Math.min(1, Math.max(0, fraction));
        int f1 = color1 >> 24 & 0xFF;
        int f2 = color1 >> 16 & 0xFF;
        int f3 = color1 >> 8 & 0xFF;
        int f4 = color1 & 0xFF;
        int s1 = color2 >> 24 & 0xFF;
        int s2 = color2 >> 16 & 0xFF;
        int s3 = color2 >> 8 & 0xFF;
        int s4 = color2 & 0xFF;
        int a = (int)(f1 + (s1 - f1) * fraction);
        int r = (int)(f2 + (s2 - f2) * fraction);
        int g = (int)(f3 + (s3 - f3) * fraction);
        int b = (int)(f4 + (s4 - f4) * fraction);
        return a << 24 | r << 16 | g << 8 | b;
    }
}