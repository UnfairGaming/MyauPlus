package myau.module.modules;

import myau.module.Category;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.ModeProperty;
import myau.ui.impl.notification.NotificationManager;
import myau.ui.impl.notification.NotificationRenderer;

public class NotificationModule extends Module {
    public final ModeProperty notificationMode = new ModeProperty(
            "Mode", 0,
            new String[]{
                    "Idk",
                    "Modern",
                    "Clean",
                    "Split",
                    "Glow",
                    "Compact",
                    "Dark",
                    "Outline",
                    "Glass",
                    "Sharp"
            }
    );

    public final BooleanProperty shadow = new BooleanProperty("Shadow", true);
    public final BooleanProperty moduleToggle = new BooleanProperty("Module Toggle", true);
    public final FloatProperty spacing = new FloatProperty("Spacing", 0.0f, 0.0f, 100.0f);
    
    // 添加位置调节选项
    public final ModeProperty positionX = new ModeProperty("Position X", 1, new String[]{"LEFT", "RIGHT"});
    public final ModeProperty positionY = new ModeProperty("Position Y", 1, new String[]{"TOP", "BOTTOM"});

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