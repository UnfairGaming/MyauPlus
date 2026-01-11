package myau.module.modules;

import myau.module.Module;
import myau.property.properties.*;

public class Animations extends Module {
    public static Animations INSTANCE;

    public final ModeProperty swordMode = new ModeProperty("Sword", 9, new String[]{"1.8", "Swing", "Old", "Push", "Dash", "Slash", "Slide", "Scale", "Swank", "Swang", "Swonk", "Stella", "Small", "Edit", "Rhys", "Stab", "Float", "Remix", "Avatar", "Xiv", "Winter", "Yamato", "SlideSwing", "SmallPush", "Reverse", "Invent", "Leaked", "Aqua", "Astro", "Fadeaway", "Astolfo", "AstolfoSpin", "Moon", "MoonPush", "Smooth", "Jigsaw", "Tap1", "Tap2", "Sigma3", "Sigma4"});
    public final FloatProperty blockPosX = new FloatProperty("BlockPos-X", 0f, -1f, 1f);
    public final FloatProperty blockPosY = new FloatProperty("BlockPos-Y", 0f, -1f, 1f);
    public final FloatProperty blockPosZ = new FloatProperty("BlockPos-Z", 0f, -1f, 1f);
    public final FloatProperty scale = new FloatProperty("Item-Size", 0f, -0.5f, 0.5f);

    public Animations() {
        super("Animations", true);
        INSTANCE = this;
    }

    public static Animations getInstance() {
        return INSTANCE;
    }
}