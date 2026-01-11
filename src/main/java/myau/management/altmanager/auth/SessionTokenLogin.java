package myau.management.altmanager.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import myau.management.altmanager.AltManagerGui;
import myau.management.altmanager.SessionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Lightweight session-token login adapted from FDP's LoginUtils.
 * GUI is intentionally excluded; this only performs the session swap.
 */
public final class SessionTokenLogin {

    private SessionTokenLogin() {
    }

    public enum LoginResult {
        INVALID_ACCOUNT_DATA,
        LOGGED,
        FAILED_PARSE_TOKEN
    }

    /**
     * Login using a Microsoft session token (JWT style: header.payload.signature).
     * We decode the payload and pull the Minecraft UUID from the "profiles.mc" field.
     */
    public static LoginResult loginWithSessionToken(String sessionToken) {
        if (sessionToken == null || sessionToken.isEmpty()) {
            return LoginResult.FAILED_PARSE_TOKEN;
        }

        String[] parts = sessionToken.split("\\.");
        if (parts.length < 2) {
            return LoginResult.FAILED_PARSE_TOKEN;
        }

        String payloadJson;
        try {
            payloadJson = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return LoginResult.FAILED_PARSE_TOKEN;
        }

        JsonObject payload;
        try {
            payload = new JsonParser().parse(payloadJson).getAsJsonObject();
        } catch (Exception e) {
            return LoginResult.FAILED_PARSE_TOKEN;
        }

        String uuid = null;
        try {
            JsonObject profiles = payload.getAsJsonObject("profiles");
            if (profiles != null && profiles.has("mc")) {
                uuid = profiles.get("mc").getAsString();
            }
        } catch (Exception ignored) {
            // fall through
        }

        if (uuid == null || uuid.isEmpty() || sessionToken.contains(":")) {
            return LoginResult.FAILED_PARSE_TOKEN;
        }

        // Use current username as a fallback; we cannot derive the name without an extra API call.
        String username = Minecraft.getMinecraft().getSession().getUsername();
        if (username == null || username.isEmpty()) {
            username = "Player";
        }

        try {
            SessionUtil.setSession(Minecraft.getMinecraft(), new Session(username, uuid, sessionToken, "mojang"));
        } catch (Exception e) {
            return LoginResult.INVALID_ACCOUNT_DATA;
        }

        // Keep status consistent with existing UI text
        AltManagerGui.status = "§aLogged into §f§l" + username + "§a.";
        return LoginResult.LOGGED;
    }
}

