package myau.ui.impl.notification;

import lombok.Getter;

import java.awt.Color;

@Getter
public enum NotificationType {
    OKAY("okay", new Color(65, 252, 65)),
    INFO("info", new Color(127, 174, 210)),
    NOTIFY("notify", new Color(255, 255, 94)),
    WARNING("warning", new Color(226, 87, 76)),
    ERROR("error", new Color(226, 87, 76));

    private final String name;
    private final Color color;

    NotificationType(String name, Color color) {
        this.name = name;
        this.color = color;
    }

    public boolean is(String name) {
        return this.name.equalsIgnoreCase(name);
    }
}