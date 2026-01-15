package myau.util;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.GL20;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class ShaderUtil {
    private int programID;
    private final Map<String, Integer> uniforms = new HashMap<>();

    public ShaderUtil(String fragmentShaderPath) {
        int program = 0;
        try {
            // 1. 创建顶点着色器 (尝试读取文件，如果不存在则使用内置的默认顶点着色器)
            int vertexShaderID = createShader("myau/shader/vertex.vsh", GL20.GL_VERTEX_SHADER);

            // 2. 创建片元着色器 (尝试读取文件，如果不存在则检查是否有内置代码)
            int fragmentShaderID = createShader(fragmentShaderPath, GL20.GL_FRAGMENT_SHADER);

            // 如果编译失败，停止
            if (vertexShaderID == 0 || fragmentShaderID == 0) {
                this.programID = 0;
                return;
            }

            // 3. 链接程序
            program = GL20.glCreateProgram();
            GL20.glAttachShader(program, vertexShaderID);
            GL20.glAttachShader(program, fragmentShaderID);
            GL20.glLinkProgram(program);

            // 4. 检查链接状态
            int status = GL20.glGetProgrami(program, GL20.GL_LINK_STATUS);
            if (status == 0) {
                System.err.println("Shader Link Failed: " + GL20.glGetProgramInfoLog(program, 1024));
                program = 0;
            }

            // 5. 清理 Shader 对象 (Link 后即可删除)
            GL20.glDeleteShader(vertexShaderID);
            GL20.glDeleteShader(fragmentShaderID);

        } catch (Exception e) {
            e.printStackTrace();
            program = 0;
        }

        this.programID = program;
    }

    private int createShader(String shaderPath, int shaderType) {
        int shaderID = 0;
        try {
            // 获取源码 (文件 -> 后缀替换 -> 内置回退)
            String shaderSource = getShaderSource(shaderPath);

            if (shaderSource == null) {
                // 后缀回退逻辑
                if (shaderPath.endsWith(".fsh")) {
                    shaderSource = getShaderSource(shaderPath.replace(".fsh", ".frag"));
                } else if (shaderPath.endsWith(".frag")) {
                    shaderSource = getShaderSource(shaderPath.replace(".frag", ".fsh"));
                }
            }

            // 最后检查内置代码逻辑 (防止资源文件丢失导致崩溃)
            if (shaderSource == null) {
                if (shaderPath.contains("vertex")) {
                    shaderSource = DEFAULT_VERTEX_SHADER;
                } else if (shaderPath.contains("roundedRect")) {
                    shaderSource = ROUNDED_RECT_SHADER;
                } else {
                    System.err.println("Shader file not found: " + shaderPath);
                    return 0;
                }
            }

            shaderID = GL20.glCreateShader(shaderType);
            GL20.glShaderSource(shaderID, shaderSource);
            GL20.glCompileShader(shaderID);

            if (GL20.glGetShaderi(shaderID, GL20.GL_COMPILE_STATUS) == 0) {
                System.err.println("Error compiling shader: " + shaderPath);
                System.err.println(GL20.glGetShaderInfoLog(shaderID, 1024));
                GL20.glDeleteShader(shaderID);
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
        return shaderID;
    }

    private String getShaderSource(String path) {
        try {
            ResourceLocation loc = new ResourceLocation(path);
            try (InputStream inputStream = Minecraft.getMinecraft().getResourceManager().getResource(loc).getInputStream()) {
                return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            return null; // 返回 null 以触发回退逻辑
        }
    }

    public void init() {
        if (programID != 0) {
            GL20.glUseProgram(programID);
        }
    }

    public void unload() {
        if (programID != 0) {
            GL20.glUseProgram(0);
        }
    }

    public int getUniform(String name) {
        if (programID == 0) return -1;
        if (uniforms.containsKey(name)) {
            return uniforms.get(name);
        }
        int uniform = GL20.glGetUniformLocation(programID, name);
        uniforms.put(name, uniform);
        return uniform;
    }

    public void setUniformf(String name, float... args) {
        if (programID == 0) return;
        int uniform = getUniform(name);
        if (uniform == -1) return;

        switch (args.length) {
            case 1: GL20.glUniform1f(uniform, args[0]); break;
            case 2: GL20.glUniform2f(uniform, args[0], args[1]); break;
            case 3: GL20.glUniform3f(uniform, args[0], args[1], args[2]); break;
            case 4: GL20.glUniform4f(uniform, args[0], args[1], args[2], args[3]); break;
        }
    }

    public void setUniformi(String name, int... args) {
        if (programID == 0) return;
        int uniform = getUniform(name);
        if (uniform == -1) return;

        switch (args.length) {
            case 1: GL20.glUniform1i(uniform, args[0]); break;
            case 2: GL20.glUniform2i(uniform, args[0], args[1]); break;
            case 3: GL20.glUniform3i(uniform, args[0], args[1], args[2]); break;
            case 4: GL20.glUniform4i(uniform, args[0], args[1], args[2], args[3]); break;
        }
    }

    public int getProgramID() {
        return this.programID;
    }

    // =========================================================================
    //                            INTERNAL SHADERS
    // =========================================================================

    // 默认顶点着色器 (处理坐标和纹理映射)
    private static final String DEFAULT_VERTEX_SHADER =
            "#version 120\n" +
                    "\n" +
                    "void main() {\n" +
                    "    gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
                    "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
                    "}";

    // 圆角矩形片元着色器 (RenderUtil.drawRound 使用)
    private static final String ROUNDED_RECT_SHADER =
            "#version 120\n" +
                    "\n" +
                    "uniform vec2 location, rectSize;\n" +
                    "uniform vec4 color;\n" +
                    "uniform float radius;\n" +
                    "uniform bool blur;\n" +
                    "\n" +
                    "float roundSDF(vec2 p, vec2 b, float r) {\n" +
                    "    return length(max(abs(p) - b, 0.0)) - r;\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec2 rectHalf = rectSize * .5;\n" +
                    "    // Smooth the result (free antialiasing).\n" +
                    "    float smoothedAlpha =  (1.0-smoothstep(0.0, 1.0, roundSDF(rectHalf - (gl_TexCoord[0].st * rectSize), rectHalf - radius - 1., radius))) * color.a;\n" +
                    "    gl_FragColor = vec4(color.rgb, smoothedAlpha);\n" +
                    "}";
}