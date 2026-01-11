package myau.util.font;

import myau.util.font.impl.FontRenderer;
import myau.util.font.impl.FontUtil;
import myau.util.font.impl.MinecraftFontRenderer;
import net.minecraft.client.gui.ScaledResolution;

import java.util.HashMap;
import java.util.Map;

import static myau.config.Config.mc;

public class FontManager {
    public static FontRenderer
            regular12, regular16, regular22,
            icon20,
            productSans12, productSans16, productSans18, productSans20, productSans24, productSans28, productSans32, productSansLight, productSansMedium,
            tenacity12, tenacity16, tenacity20, tenacity24, tenacity28, tenacity32, tenacity80,
            vision12, vision16, vision20, vision24, vision28, vision32,
            nbpInforma12, nbpInforma16, nbpInforma20, nbpInforma24, nbpInforma28, nbpInforma32,
            tahomaBold12, tahomaBold16, tahomaBold20, tahomaBold24, tahomaBold28, tahomaBold32;

    private static int prevScale;

    static {
        initializeFonts();
    }
    
    public static void initializeFonts() {
        Map<String, java.awt.Font> locationMap = new HashMap<>();

        ScaledResolution sr = new ScaledResolution(mc);

        int scale = sr.getScaleFactor();

        if (scale != prevScale) {
            prevScale = scale;

            regular12 = new FontRenderer(FontUtil.getResource(locationMap, "regular.ttf", 12));
            regular16 = new FontRenderer(FontUtil.getResource(locationMap, "regular.ttf", 16));
            regular22 = new FontRenderer(FontUtil.getResource(locationMap, "regular.ttf", 22));
            icon20 = new FontRenderer(FontUtil.getResource(locationMap, "icon.ttf", 20));
            productSans12 = new FontRenderer(FontUtil.getResource(locationMap, "product_sans_regular.ttf", 12));
            productSans16 = new FontRenderer(FontUtil.getResource(locationMap, "product_sans_regular.ttf", 16));
            productSans18 = new FontRenderer(FontUtil.getResource(locationMap, "product_sans_regular.ttf", 18));
            productSans20 = new FontRenderer(FontUtil.getResource(locationMap, "product_sans_regular.ttf", 20));
            productSans24 = new FontRenderer(FontUtil.getResource(locationMap, "product_sans_regular.ttf", 24));
            productSans28 = new FontRenderer(FontUtil.getResource(locationMap, "product_sans_regular.ttf", 28));
            productSans32 = new FontRenderer(FontUtil.getResource(locationMap, "product_sans_regular.ttf", 32));
            productSansLight = new FontRenderer(FontUtil.getResource(locationMap, "product_sans_light.ttf", 22));
            productSansMedium = new FontRenderer(FontUtil.getResource(locationMap, "product_sans_medium.ttf", 22));
            tenacity12 = new FontRenderer(FontUtil.getResource(locationMap, "tenacity.ttf", 12));
            tenacity16 = new FontRenderer(FontUtil.getResource(locationMap, "tenacity.ttf", 16));
            tenacity20 = new FontRenderer(FontUtil.getResource(locationMap, "tenacity.ttf", 20));
            tenacity24 = new FontRenderer(FontUtil.getResource(locationMap, "tenacity.ttf", 24));
            tenacity28 = new FontRenderer(FontUtil.getResource(locationMap, "tenacity.ttf", 28));
            tenacity32 = new FontRenderer(FontUtil.getResource(locationMap, "tenacity.ttf", 32));
            tenacity80 = new FontRenderer(FontUtil.getResource(locationMap, "tenacity.ttf", 80));
            
            // Additional fonts from the assets directory
            vision12 = new FontRenderer(FontUtil.getResource(locationMap, "Vision.otf", 12));
            vision16 = new FontRenderer(FontUtil.getResource(locationMap, "Vision.otf", 16));
            vision20 = new FontRenderer(FontUtil.getResource(locationMap, "Vision.otf", 20));
            vision24 = new FontRenderer(FontUtil.getResource(locationMap, "Vision.otf", 24));
            vision28 = new FontRenderer(FontUtil.getResource(locationMap, "Vision.otf", 28));
            vision32 = new FontRenderer(FontUtil.getResource(locationMap, "Vision.otf", 32));
            
            nbpInforma12 = new FontRenderer(FontUtil.getResource(locationMap, "nbp-informa-fivesix.ttf", 12));
            nbpInforma16 = new FontRenderer(FontUtil.getResource(locationMap, "nbp-informa-fivesix.ttf", 16));
            nbpInforma20 = new FontRenderer(FontUtil.getResource(locationMap, "nbp-informa-fivesix.ttf", 20));
            nbpInforma24 = new FontRenderer(FontUtil.getResource(locationMap, "nbp-informa-fivesix.ttf", 24));
            nbpInforma28 = new FontRenderer(FontUtil.getResource(locationMap, "nbp-informa-fivesix.ttf", 28));
            nbpInforma32 = new FontRenderer(FontUtil.getResource(locationMap, "nbp-informa-fivesix.ttf", 32));
            
            tahomaBold12 = new FontRenderer(FontUtil.getResource(locationMap, "tahomabold.ttf", 12));
            tahomaBold16 = new FontRenderer(FontUtil.getResource(locationMap, "tahomabold.ttf", 16));
            tahomaBold20 = new FontRenderer(FontUtil.getResource(locationMap, "tahomabold.ttf", 20));
            tahomaBold24 = new FontRenderer(FontUtil.getResource(locationMap, "tahomabold.ttf", 24));
            tahomaBold28 = new FontRenderer(FontUtil.getResource(locationMap, "tahomabold.ttf", 28));
            tahomaBold32 = new FontRenderer(FontUtil.getResource(locationMap, "tahomabold.ttf", 32));
        }
    }
    
    public static void forceInitialize() {
        initializeFonts();
    }
    
    public static MinecraftFontRenderer getMinecraft() {
        return MinecraftFontRenderer.INSTANCE;
    }

}