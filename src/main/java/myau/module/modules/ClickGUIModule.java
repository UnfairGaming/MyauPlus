package myau.module.modules;

import myau.module.Category;
import myau.module.Module;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.IntProperty;
import myau.ui.impl.clickgui.ClickGuiScreen;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class ClickGUIModule extends Module {

    public BooleanProperty saveGuiState = new BooleanProperty("Save GUI State", true);
    // --- 新增：阴影开关 ---
    public BooleanProperty shadow = new BooleanProperty("Shadow", true);

    public IntProperty windowWidth = new IntProperty("Window Width", 600, 300, 1200);
    public IntProperty windowHeight = new IntProperty("Window Height", 400, 200, 800);
    public FloatProperty cornerRadius = new FloatProperty("Corner Radius", 8.0f, 0.0f, 20.0f);

    public ClickGUIModule() {
        super("ClickGUI", "Material Design 3 based ClickGUI", Category.RENDER, Keyboard.KEY_RSHIFT, false, true);
        // Properties are automatically added via reflection in Myau.java
    }

    @Override
    public void onEnabled() {
        super.onEnabled();
        // If not in a world, immediately disable the module
        if (Minecraft.getMinecraft().theWorld == null) {
            this.setEnabled(false); // This will call onDisabled
            return;
        }
        ClickGuiScreen gui = ClickGuiScreen.getInstance();
        if (gui != null) {
            Minecraft.getMinecraft().displayGuiScreen(gui);
        }
    }

    @Override
    public void onDisabled() {
        super.onDisabled();
        Minecraft.getMinecraft().displayGuiScreen(null);
        if (Minecraft.getMinecraft().currentScreen == null) {
            Minecraft.getMinecraft().setIngameFocus();
        }
    }
}