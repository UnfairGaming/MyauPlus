package myau.management.altmanager;

import myau.event.EventTarget;
import myau.events.PacketEvent;
import myau.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.util.IChatComponent;

public class BanDetectionHandler {
    private static boolean initialized = false;

    public static void init() {
        if (!initialized) {
            // Register with the custom EventManager instead of Forge EventBus
            myau.event.EventManager.register(new BanDetectionHandler());
            initialized = true;
        }
    }

    @EventTarget
    public void onReceivePacket(PacketEvent event) {
        if (event.getPacket() instanceof S40PacketDisconnect) {
            S40PacketDisconnect disconnectPacket = (S40PacketDisconnect) event.getPacket();
            
            try {
                // Get the disconnect reason/message
                IChatComponent reason = disconnectPacket.getReason();
                if (reason != null) {
                    String message = reason.getUnformattedText().toLowerCase();
                    
                    // Check if the message contains ban-related keywords
                    if (message.contains("suspended") || message.contains("banned") || message.contains("kicked")) {
                        // Get the current account name
                        String currentUsername = Minecraft.getMinecraft().getSession().getUsername();
                        
                        if (currentUsername != null && !currentUsername.isEmpty()) {
                            // Mark the account as banned
                            markAccountAsBanned(currentUsername);
                            ChatUtil.sendFormatted("&cAccount &b" + currentUsername + " &chas been detected as banned/kicked/suspended");
                        }
                    }
                }
            } catch (Exception e) {
                // Silently handle any errors (reflection issues, etc.)
            }
        }
    }

    private void markAccountAsBanned(String username) {
        // Find and mark the account in the alt list
        for (Alt alt : AltManagerGui.alts) {
            if (alt.getName().equalsIgnoreCase(username)) {
                alt.setBanned(true);
                // Save the updated alt list
                myau.management.altmanager.util.AltJsonHandler.saveAlts();
                break;
            }
        }
        
        // Also update AccountData if needed
        AccountData.setBanned(username);
    }
}