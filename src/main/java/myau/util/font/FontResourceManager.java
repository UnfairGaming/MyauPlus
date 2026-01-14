package myau.util.font;

import myau.util.font.impl.FontRenderer;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

/**
 * 字体资源管理器，用于管理字体资源的生命周期，防止内存泄漏
 */
public class FontResourceManager {
    private static final Set<WeakReference<FontRenderer>> fontReferences = new HashSet<>();
    
    /**
     * 注册字体渲染器引用，以便在需要时可以统一清理
     */
    public static void registerFont(FontRenderer fontRenderer) {
        if (fontRenderer != null) {
            fontReferences.add(new WeakReference<>(fontRenderer));
        }
    }
    
    /**
     * 清理所有注册的字体资源
     */
    public static void cleanupAllFonts() {
        fontReferences.removeIf(ref -> {
            FontRenderer font = ref.get();
            if (font != null) {
                font.destroy();
                return true;
            }
            // 如果引用为空，则从集合中移除该引用
            return true;
        });
    }
    
    /**
     * 清理无效的引用（垃圾回收后的弱引用）
     */
    public static void cleanupInvalidReferences() {
        fontReferences.removeIf(ref -> ref.get() == null);
    }
}