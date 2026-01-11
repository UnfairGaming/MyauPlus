package myau.module.modules;

import myau.module.Category;
import myau.module.Module;

public class AntiObfuscate extends Module {
    public AntiObfuscate() {
        super("AntiObfuscate", "Removes obfuscation from text.", Category.RENDER, 0, false, true);
    }

    public String stripObfuscated(String input) {
        return input.replaceAll("§k", "");
    }
}
