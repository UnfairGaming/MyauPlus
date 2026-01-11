package myau.management.altmanager.gui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import myau.management.altmanager.Alt;
import myau.management.altmanager.AltManagerGui;
import myau.management.altmanager.SessionUtil;
import myau.management.altmanager.util.AltJsonHandler;
import myau.ui.impl.gui.BackgroundRenderer;
import myau.util.font.FontManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.Session;

public class MicrosoftLoginGui extends GuiScreen {
    private final AltManagerGui parent;
    private GuiButton loginButton, backButton;
    private GuiTextField tokenField;

    public MicrosoftLoginGui(AltManagerGui parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        
        int centerX = this.width / 2;
        int fieldWidth = 150;
        int fieldHeight = 20;
        int buttonWidth = 150;
        int buttonHeight = 20;
        int baseY = this.height / 2 - 20;

        this.buttonList.clear();
        this.tokenField = new GuiTextField(0, this.fontRendererObj, centerX - (fieldWidth / 2), baseY, fieldWidth, fieldHeight);
        this.tokenField.setMaxStringLength(32767);
        // 使用 .trim() 去除可能的首尾空格
        this.loginButton = new GuiButton(0, centerX - (buttonWidth / 2), baseY + fieldHeight + 10, buttonWidth, buttonHeight, "Login");
        this.backButton = new GuiButton(1, centerX - (buttonWidth / 2), baseY + fieldHeight + 40, buttonWidth, buttonHeight, "Back");

        this.buttonList.add(loginButton);
        this.buttonList.add(backButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        BackgroundRenderer.draw(this.width, this.height, mouseX, mouseY);
        if (FontManager.productSans20 != null) {
            FontManager.productSans20.drawCenteredString("Token Login", this.width / 2.0f, 20, 0xFFFFFF);
            FontManager.productSans20.drawString("Current Alt: §a" + mc.getSession().getUsername(), 5, 5, 0xAAAAAA);
            FontManager.productSans20.drawString("Status: " + AltManagerGui.status, 5, 20, 0xAAAAAA);
        } else {
            drawCenteredString(this.fontRendererObj, "Token Login", this.width / 2, 20, 0xFFFFFF);
            this.fontRendererObj.drawStringWithShadow("Current Alt: §a" + mc.getSession().getUsername(), 5, 5, 0xAAAAAA);
            this.fontRendererObj.drawStringWithShadow("Status: " + AltManagerGui.status, 5, 20, 0xAAAAAA);
        }
        this.tokenField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
        
        // Draw status text again to ensure it's on top of everything
        if (FontManager.productSans20 != null) {
            FontManager.productSans20.drawString("Current Alt: §a" + mc.getSession().getUsername(), 5, 5, 0xAAAAAA);
            FontManager.productSans20.drawString("Status: " + AltManagerGui.status, 5, 20, 0xAAAAAA);
        } else {
            this.fontRendererObj.drawStringWithShadow("Current Alt: §a" + mc.getSession().getUsername(), 5, 5, 0xAAAAAA);
            this.fontRendererObj.drawStringWithShadow("Status: " + AltManagerGui.status, 5, 20, 0xAAAAAA);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            // 添加非空检查
            String token = tokenField.getText().trim();
            if (!token.isEmpty()) {
                loginWithToken(token);
            }
        } else if (button.id == 1) {
            this.mc.displayGuiScreen(parent);
        }
        super.actionPerformed(button);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        this.tokenField.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.tokenField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void loginWithToken(String token) {
        // 设置状态为正在登录
        AltManagerGui.status = "§eLogging in...";

        new Thread(() -> {
            try {
                // 这一步可能会抛出异常（如果 Token 无效）
                String[] playerInfo = getProfileInfo(token);

                String username = playerInfo[0];
                String uuid = playerInfo[1];

                Session newSession = new Session(username, uuid, token, "mojang");

                try {
                    SessionUtil.setSession(mc, newSession);

                    mc.addScheduledTask(() -> {
                        // 登录成功逻辑
                        AltManagerGui.status = "§aLogged in as " + newSession.getUsername();

                        // 更新或添加账号逻辑
                        Alt existingAlt = null;
                        for (Alt alt : AltManagerGui.alts) {
                            if (alt.getName().equals(newSession.getUsername()) ||
                                    (alt.getUuid() != null && alt.getUuid().equals(newSession.getPlayerID()))) {
                                existingAlt = alt;
                                break;
                            }
                        }

                        if (existingAlt != null) {
                            existingAlt.setUuid(newSession.getPlayerID());
                            existingAlt.setRefreshToken(token);
                            existingAlt.setBanned(myau.management.altmanager.AccountData.isBanned(newSession.getUsername()));
                        } else {
                            Alt alt = new Alt(newSession.getUsername(), "", newSession.getUsername(), false);
                            alt.setUuid(newSession.getPlayerID());
                            alt.setRefreshToken(token);
                            alt.setBanned(myau.management.altmanager.AccountData.isBanned(newSession.getUsername()));
                            AltManagerGui.alts.add(alt);
                        }

                        AltJsonHandler.start();
                        AltJsonHandler.saveAlts();
                        AltJsonHandler.loadAlts();

                        this.mc.displayGuiScreen(parent);
                    });
                } catch (Exception e) {
                    mc.addScheduledTask(() -> AltManagerGui.status = "§cSession Error");
                    e.printStackTrace();
                }
            } catch (Exception e) {
                // 捕获 getProfileInfo 抛出的异常，提示 Token 无效
                mc.addScheduledTask(() -> AltManagerGui.status = "§cInvalid Token / API Error");
                e.printStackTrace();
            }
        }).start();
    }

    // --- 重点修复部分 ---
    private String[] getProfileInfo(String token) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet("https://api.minecraftservices.com/minecraft/profile");
            request.setHeader("Authorization", "Bearer " + token);

            try (CloseableHttpResponse response = client.execute(request)) {
                // 1. 获取 HTTP 状态码
                int statusCode = response.getStatusLine().getStatusCode();
                String jsonString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                // 2. 如果不是 200 OK，说明 Token 无效
                if (statusCode != 200) {
                    throw new IOException("API returned " + statusCode + ": " + jsonString);
                }

                JsonParser parser = new JsonParser();
                JsonObject json = parser.parse(jsonString).getAsJsonObject();

                // 3. 安全检查：确保字段存在
                if (!json.has("name") || !json.has("id")) {
                    throw new IOException("Invalid JSON response (Missing name/id): " + jsonString);
                }

                String username = json.get("name").getAsString();
                String uuid = json.get("id").getAsString();
                return new String[]{username, uuid};
            }
        }
    }
}