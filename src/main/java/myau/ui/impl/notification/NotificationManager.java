package myau.ui.impl.notification;

import myau.Myau;
import myau.module.modules.NotificationModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;

import java.util.concurrent.ConcurrentLinkedDeque;

public class NotificationManager {
    private static final Minecraft mc = Minecraft.getMinecraft();

    // 使用并发队列，线程安全
    public static final ConcurrentLinkedDeque<Notification> notifications = new ConcurrentLinkedDeque<>();

    public static void post(NotificationType type, String title, String description) {
        // 默认 2000ms
        post(new Notification(type, title, description, 2000L));
    }

    public static void post(NotificationType type, String title, String description, long time) {
        post(new Notification(type, title, description, time));
    }

    public static void post(String title, String description) {
        post(NotificationType.INFO, title, description);
    }

    public static void post(Notification notification) {
        if (isNotificationsEnabled()) {
            notifications.add(notification);
        }
    }

    public static void render() {
        if (notifications.isEmpty() || !isNotificationsEnabled()) return;

        // 1. 清理已完全消失的通知
        notifications.removeIf(notification -> !notification.isShowing());

        ScaledResolution sr = new ScaledResolution(mc);

        // 2. 基础 Y 坐标 (屏幕右下角)
        // 距离底部 50px，留出空间给 ArmorHUD 等
        float startY = sr.getScaledHeight() - 50;

        // 如果聊天栏打开，整体上移以免遮挡
        if (mc.currentScreen instanceof GuiChat) {
            startY -= 14;
        }

        // 3. 遍历渲染
        float currentY = startY;
        float padding = 6.0f; // 通知之间的垂直间距

        for (Notification notification : notifications) {
            // 将目标 Y 坐标传给 Notification，让它自己 Lerp 过去
            notification.render(currentY);

            // 只有当前高度减去 (通知高度 + 间距)
            // 这样会形成从下往上堆叠的效果
            // Notification.HEIGHT 为 34.0f
            currentY -= (34.0f + padding);
        }
    }

    // 更新逻辑已移至 render 实现插值动画，此方法留空或移除
    public static void update() {
    }

    public static void clear() {
        notifications.clear();
    }

    private static boolean isNotificationsEnabled() {
        // 确保你的 ModuleManager 能正确获取到 NotificationModule
        if (Myau.moduleManager == null) return true; // 防止空指针，默认开启
        NotificationModule module = (NotificationModule) Myau.moduleManager.modules.get(NotificationModule.class);
        return module == null || module.isEnabled();
    }
}