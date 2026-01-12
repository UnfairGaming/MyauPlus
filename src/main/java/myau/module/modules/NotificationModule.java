package myau.module.modules;

import myau.module.Category;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.ModeProperty;
import myau.ui.impl.notification.NotificationManager;
import myau.ui.impl.notification.NotificationRenderer;

public class NotificationModule extends Module {
    // 模式选择 - 只保留Exhi模式
    public final ModeProperty notificationMode = new ModeProperty(
            "Mode", 0,
            new String[]{"Exhi"}
    );

    public final BooleanProperty shadow = new BooleanProperty("Shadow", true);

    public final BooleanProperty moduleToggle = new BooleanProperty("Module Toggle", true);

    public NotificationModule() {
        super("Notifications", "Display notifications", Category.RENDER, 0, true, false);
    }

    @Override
    public void onEnabled() {
        if (moduleToggle.getValue()) {
            NotificationRenderer.success("Notifications", "Notification system enabled");
        }
    }

    @Override
    public void onDisabled() {
        NotificationManager.clear();
        if (moduleToggle.getValue()) {
            NotificationRenderer.warning("Notifications", "Notification system disabled");
        }
    }

}