package myau.property.properties;

import com.google.gson.JsonObject;
import myau.property.Property;
import java.util.function.BooleanSupplier;

public class ColorProperty extends Property<Integer> {
    public ColorProperty(String name, Integer color) {
        this(name, color, null);
    }

    public ColorProperty(String string, Integer color, BooleanSupplier check) {
        super(string, color, c -> true, check);
    }

    @Override
    public String getValuePrompt() {
        return "RGB";
    }

    @Override
    public String formatValue() {
        return String.format("%06X", (0xFFFFFF & this.getValue()));
    }

    @Override
    public boolean parseString(String string) {
        try {
            return this.setValue((int)Long.parseLong(string.replace("#", ""), 16) | 0xFF000000);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public boolean read(JsonObject jsonObject) {
        if (jsonObject.has(this.getName())) {
            return this.parseString(jsonObject.get(this.getName()).getAsString());
        }
        return false;
    }

    @Override
    public void write(JsonObject jsonObject) {
        jsonObject.addProperty(this.getName(), String.format("%06X", (0xFFFFFF & this.getValue())));
    }
}