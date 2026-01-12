package myau.ui.impl.notification;

import myau.event.EventTarget;
import myau.events.Render2DEvent;
import myau.events.TickEvent;

public class NotificationRenderer {

    @EventTarget
    public void onTick(TickEvent event) {
        NotificationManager.update();
    }

    @EventTarget
    public void onRender2D(Render2DEvent event) {
        NotificationManager.render();
    }

    public static void success(String title, String description) {
        NotificationManager.post(NotificationType.OKAY, title, description);
    }

    public static void warning(String title, String description) {
        NotificationManager.post(NotificationType.WARNING, title, description);
    }

}