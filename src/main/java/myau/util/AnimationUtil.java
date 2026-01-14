package myau.util;

public class AnimationUtil {
    public static float animateSmooth(float target, float current, float speed, float deltaTime) {
        float diff = target - current;
        if (Math.abs(diff) < 0.0001f) return target;
        float factor = (float) (1.0 - Math.exp(-speed * deltaTime));
        factor = Math.max(0, Math.min(1, factor));
        return current + diff * factor;
    }
    public static float animate(float target, float current, float speed, float deltaTime) {
        if (Math.abs(target - current) < 0.0001f) return target;
        float change = speed * deltaTime;
        if (current < target) {
            return Math.min(target, current + change);
        } else {
            return Math.max(target, current - change);
        }
    }
    public static int interpolateColor(int color1, int color2, float fraction) {
        fraction = Math.min(1, Math.max(0, fraction));
        int c1 = color1;
        int c2 = color2;
        int a1 = (c1 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;
        int a2 = (c2 >> 24) & 0xFF;
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;
        int a = (int) (a1 + (a2 - a1) * fraction);
        int r = (int) (r1 + (r2 - r1) * fraction);
        int g = (int) (g1 + (g2 - g1) * fraction);
        int b = (int) (b1 + (b2 - b1) * fraction);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}