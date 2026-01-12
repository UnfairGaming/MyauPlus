package myau.module;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.KeyEvent;
import myau.events.TickEvent;
import myau.module.modules.NotificationModule;
import myau.ui.impl.notification.NotificationRenderer;
import myau.util.SoundUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ModuleManager {
    private boolean sound = false;
    public final LinkedHashMap<Class<?>, Module> modules = new LinkedHashMap<>();

    public Module getModule(String string) {
        return this.modules.values().stream().filter(mD -> mD.getName().equalsIgnoreCase(string)).findFirst().orElse(null);
    }

    public void playSound() {
        this.sound = true;
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        for (Module module : this.modules.values()) {
            if (module.getKey() != event.getKey()) {
                continue;
            }
            boolean wasEnabled = module.isEnabled();
            module.toggle();

            // 发送模块切换通知
            NotificationModule notifModule = (NotificationModule) modules.get(NotificationModule.class);
            if (notifModule != null && notifModule.isEnabled() && notifModule.moduleToggle.getValue()) {
                // 排除Notification模块自身的切换通知
                if (!(module instanceof NotificationModule)) {
                    if (module.isEnabled()) {
                        NotificationRenderer.success(module.getName() + " Enabled", "Module has been turned on");
                    } else {
                        NotificationRenderer.warning(module.getName() + " Disabled", "Module has been turned off");
                    }
                }
            }
        }
    }

    @EventTarget
    public void onTick(TickEvent event) {
        if (event.getType() == EventType.PRE) {
            if (this.sound) {
                this.sound = false;
                SoundUtil.playSound("random.click");
            }
        }
    }

    public ArrayList<Module> getModulesInCategory(Category category) {
        ArrayList<Module> modulesInCat = new ArrayList<>();
        for (Module module : this.modules.values()) {
            if (module.getCategory() == category) {
                modulesInCat.add(module);
            }
        }
        return modulesInCat;
    }
}
