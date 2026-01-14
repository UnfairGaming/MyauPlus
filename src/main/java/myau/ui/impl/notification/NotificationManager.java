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

        // 2. 基础 Y 坐标 (根据位置设置)
        float startY;
        NotificationModule module = (NotificationModule) Myau.moduleManager.modules.get(NotificationModule.class);
        boolean isTop = module != null && module.positionY.getValue() == 0; // 0 表示 TOP
        
        if (isTop) {
            startY = 50; // 距离顶部 50px
        } else {
            startY = sr.getScaledHeight() - 50; // 距离底部 50px，留出空间给 ArmorHUD 等
        }

        // 如果聊天栏打开，相应调整位置
        if (mc.currentScreen instanceof GuiChat) {
            if (isTop) {
                startY += 14; // 如果在顶部，向下移动
            } else {
                startY -= 14; // 如果在底部，向上移动
            }
        }

        // 3. 遍历渲染
        float currentY = startY;
        
        // 获取间距设置
        float padding = (module != null) ? module.spacing.getValue() : 6.0f; // 通知之间的垂直间距

        for (Notification notification : notifications) {
            // 将目标 Y 坐标传给 Notification，让它自己 Lerp 过去
            notification.render(currentY);

            // 根据位置决定堆叠方向
            if (isTop) {
                // 如果在顶部，通知向下堆叠
                currentY += (notification.getHeight() + padding);
            } else {
                // 如果在底部，通知向上堆叠
                currentY -= (notification.getHeight() + padding);
            }
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