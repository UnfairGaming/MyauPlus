package myau.config;

import com.google.gson.*;
import myau.Myau;
import myau.mixin.IAccessorMinecraft;
import myau.module.Module;
import myau.property.Property;
import myau.ui.impl.gui.BackgroundRenderer;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;

import java.io.*;
import java.util.ArrayList;

public class Config {
    public static Minecraft mc = Minecraft.getMinecraft();
    public static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    public String name;
    public File file;

    public Config(String name, boolean newConfig) {
        this.name = name;
        this.file = new File("./config/Myau/", String.format("%s.json", this.name));
        try {
            file.getParentFile().mkdirs();
            if (newConfig) {
                ((IAccessorMinecraft) mc).getLogger().info(String.format("Created: %s", this.file.getName()));
            }
        } catch (Exception e) {
            ((IAccessorMinecraft) mc).getLogger().error(e.getMessage());
        }
    }

    public void load() {
        try {
            JsonElement parsed = new JsonParser().parse(new BufferedReader(new FileReader(file)));
            JsonObject jsonObject = parsed.getAsJsonObject();
            // Loading GUI state
            
            // Load background shader index if it exists
            JsonElement backgroundIndexElement = jsonObject.get("background_index");
            if (backgroundIndexElement != null) {
                BackgroundRenderer.currentBackgroundIndex = backgroundIndexElement.getAsInt();
                BackgroundRenderer.reloadShader(BackgroundRenderer.currentBackgroundIndex);
            }
            
            // Properties
            JsonObject propertiesObject = jsonObject.getAsJsonObject("properties");

            for (Module module : Myau.moduleManager.modules.values()) {
                JsonElement moduleObj = jsonObject.get(module.getName());
                if (moduleObj != null) {
                    JsonObject object = moduleObj.getAsJsonObject();
                    JsonElement toggled = object.get("toggled");
                    module.setEnabled(toggled.getAsBoolean());
                    JsonElement key = object.get("key");
                    module.setKey(key.getAsInt());
                    JsonElement hidden = object.get("hidden");
                    module.setHidden(hidden.getAsBoolean());
                    ArrayList<Property<?>> list = Myau.propertyManager.properties.get(module.getClass());
                    if (list != null) {
                        for (Property<?> property : list) {
                            if (object.has(property.getName())) {
                                property.read(object);
                            }
                        }
                    }
                }
            }
            ChatUtil.sendFormatted(String.format("%sConfig has been loaded (&a&o%s&r)&r", Myau.clientName, file.getName()));
        } catch (Exception e) {
            ((IAccessorMinecraft) mc).getLogger().error(e.getMessage());
            ChatUtil.sendFormatted(String.format("%sConfig couldn't be loaded (&c&o%s&r)&r", Myau.clientName, file.getName()));
        }
    }

    public void save() {
        try {
            JsonObject object = new JsonObject();
            // Saving GUI state
            
            // Save background shader index
            object.addProperty("background_index", BackgroundRenderer.currentBackgroundIndex);
            
            // Properties
            JsonObject propertiesObject = new JsonObject();

            for (Module module : Myau.moduleManager.modules.values()) {
                JsonObject moduleObject = new JsonObject();
                moduleObject.addProperty("toggled", module.isEnabled());
                moduleObject.addProperty("key", module.getKey());
                moduleObject.addProperty("hidden", module.isHidden());
                ArrayList<Property<?>> list = Myau.propertyManager.properties.get(module.getClass());
                if (list != null) {
                    for (Property<?> property : list) {
                        property.write(moduleObject);
                    }
                }
                object.add(module.getName(), moduleObject);
            }
            PrintWriter printWriter = new PrintWriter(new FileWriter(file));
            printWriter.println(gson.toJson(object));
            printWriter.close();
            ChatUtil.sendFormatted(String.format("%sConfig has been saved (&a&o%s&r)&r", Myau.clientName, file.getName()));
        } catch (IOException e) {
            ChatUtil.sendFormatted(String.format("%sConfig couldn't be saved (&c&o%s&r)&r", Myau.clientName, file.getName()));
        }
    }
}
