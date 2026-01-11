package myau.module;

import myau.Myau;
import myau.module.modules.HUD;
import myau.util.KeyBindUtil;
import net.minecraft.client.Minecraft;

public abstract class Module {
    protected static final Minecraft mc = Minecraft.getMinecraft();

    protected final String name;
    protected final String description;
    protected final Category category;
    protected final boolean defaultEnabled;
    protected final int defaultKey;
    protected final boolean defaultHidden;

    protected boolean enabled;
    protected int key;
    protected boolean hidden;

    public Module(String name, String description, Category category, int key, boolean enabled, boolean hidden) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.key = this.defaultKey = key;
        this.enabled = this.defaultEnabled = enabled;
        this.hidden = this.defaultHidden = hidden;
    }

    public Module(String name, boolean enabled, boolean hidden) {
        this(name, "", Category.MISC, 0, enabled, hidden);
    }

    public Module(String name, boolean enabled) {
        this(name, enabled, false);
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public String formatModule() {
        return String.format(
                "%s%s &r(%s&r)",
                this.key == 0 ? "" : String.format("&l[%s] &r", KeyBindUtil.getKeyName(this.key)),
                this.name,
                this.enabled ? "&a&lON" : "&c&lOFF"
        );
    }

    public String[] getSuffix() {
        return new String[0];
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                this.onEnabled();
            } else {
                this.onDisabled();
            }
        }
    }

    public boolean toggle() {
        boolean enabled = !this.enabled;
        this.setEnabled(enabled);
        if (this.enabled == enabled) {
            if (((HUD) Myau.moduleManager.modules.get(HUD.class)).toggleSound.getValue()) {
                Myau.moduleManager.playSound();
            }
            return true;
        } else {
            return false;
        }
    }

    public int getKey() {
        return this.key;
    }

    public void setKey(int integer) {
        this.key = integer;
    }

    public boolean isHidden() {
        return this.hidden;
    }

    public void setHidden(boolean boolean1) {
        this.hidden = boolean1;
    }

    public void onEnabled() {
    }

    public void onDisabled() {
    }

    public void verifyValue(String string) {
    }
}