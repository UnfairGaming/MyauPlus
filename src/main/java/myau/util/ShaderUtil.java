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
            // 1. 创建并编译顶点着色器 (通常不需要变动后缀，但也加了防护)
            int vertexShaderID = createShader("myau/shader/vertex.vsh", GL20.GL_VERTEX_SHADER);

            // 2. 创建并编译片元着色器 (这里会处理 .fsh -> .frag 的回退)
            int fragmentShaderID = createShader(fragmentShaderPath, GL20.GL_FRAGMENT_SHADER);

            // 如果任意一个 Shader ID 为 0，说明编译失败，停止后续操作
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
                // 仅在链接真正失败时打印错误
                System.err.println("Shader Link Failed: " + GL20.glGetProgramInfoLog(program, 1024));
                program = 0;
            }

            // 5. 删除中间对象 (即使 Program 失败也应该删除 Shader 对象)
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
            // --- 核心修改：尝试读取源码，支持后缀自动替换 ---
            String shaderSource = getShaderSource(shaderPath);

            if (shaderSource == null) {
                // 如果是片元着色器，尝试替换后缀
                if (shaderPath.endsWith(".fsh")) {
                    String altPath = shaderPath.replace(".fsh", ".frag");
                    // System.out.println("Switching from .fsh to .frag: " + altPath); // 调试用
                    shaderSource = getShaderSource(altPath);
                } else if (shaderPath.endsWith(".frag")) {
                    String altPath = shaderPath.replace(".frag", ".fsh");
                    shaderSource = getShaderSource(altPath);
                }
            }

            // 如果还是 null，说明文件彻底找不到
            if (shaderSource == null) {
                System.err.println("Shader file not found (checked .fsh and .frag): " + shaderPath);
                return 0;
            }
            // ------------------------------------------------

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

    /**
     * 尝试读取 Shader 文件内容。
     * 如果文件不存在，返回 null，而不是抛出异常。
     */
    private String getShaderSource(String path) {
        try {
            ResourceLocation loc = new ResourceLocation(path);
            // 使用 try-with-resources 自动关闭流
            try (InputStream inputStream = Minecraft.getMinecraft().getResourceManager().getResource(loc).getInputStream()) {
                return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            // 这里捕获异常但不打印，返回 null 触发 createShader 中的回退逻辑
            return null;
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

    // 设置浮点数 Uniform (float)
    public void setUniformf(String name, float... args) {
        if (programID == 0) return;
        int uniform = getUniform(name);
        if (uniform == -1) return; // 找不到变量就不设置，防止报错

        switch (args.length) {
            case 1: GL20.glUniform1f(uniform, args[0]); break;
            case 2: GL20.glUniform2f(uniform, args[0], args[1]); break;
            case 3: GL20.glUniform3f(uniform, args[0], args[1], args[2]); break;
            case 4: GL20.glUniform4f(uniform, args[0], args[1], args[2], args[3]); break;
        }
    }

    // 设置整数 Uniform (int)
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
}