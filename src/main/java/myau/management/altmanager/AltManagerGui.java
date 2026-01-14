package myau.management.altmanager;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import myau.management.altmanager.gui.CrackedLoginGui;
import myau.management.altmanager.gui.MicrosoftLoginGui;
import myau.management.altmanager.microsoft.MicrosoftOAuthTranslation;
import myau.management.altmanager.util.AltJsonHandler;
import myau.util.*;
import myau.ui.impl.gui.BackgroundRenderer;
import myau.util.font.FontManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.FontRenderer;
import org.lwjgl.opengl.GL11;
import org.lwjgl.input.Mouse;

public class AltManagerGui extends GuiScreen {
    private GuiMainMenu parent;
    private GuiButton loginButton, loginButton2, oauthButton, backButton, deleteBannedButton;
    public static List<Alt> alts = new ArrayList<>();
    public static String status = "§aIdle";

    // Scrolling variables
    private int scrollOffset = 0;
    private double smoothScrollOffset = 0.0;
    // Hover value tracking for account buttons (key: account index)
    private final Map<Integer, Integer> accountHoverValues = new HashMap<>();
    // Hover value tracking for delete buttons (key: account index)
    private final Map<Integer, Integer> deleteHoverValues = new HashMap<>();
    private static final int ACCOUNT_BUTTON_HEIGHT = 40;
    private static final int ACCOUNT_BUTTON_WIDTH = 180;
    private static final int DELETE_BUTTON_WIDTH = 24;
    private static final int DELETE_BUTTON_HEIGHT = 24;
    private static final int COLUMNS = 3;
    private static final int COLUMN_SPACING = 15; // Increased spacing
    private static final int ROW_SPACING = 10; // Added row spacing
    private static final int SCROLL_AREA_TOP = 50;

    private int getScrollAreaBottom() {
        // Leave space for buttons at bottom (adjusted for better layout)
        return this.height - 140;
    }

    private int getVisibleRows() {
        return (getScrollAreaBottom() - SCROLL_AREA_TOP) / (ACCOUNT_BUTTON_HEIGHT + ROW_SPACING);
    }

    private int getTotalVisibleAccounts() {
        return getVisibleRows() * COLUMNS;
    }

    public AltManagerGui(GuiMainMenu parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        // Initialize smooth scroll offset to match current scroll offset
        smoothScrollOffset = scrollOffset;
        
        // Load alts from file when initializing the GUI
        AltJsonHandler.loadAlts();
        
        // Initialize background renderer
        BackgroundRenderer.init();
        
        int centerX = this.width / 2;
        int buttonHeight = 20;
        int buttonSpacing = 25;
        int buttonWidth = 95;
        int baseY = this.height - 75; // Adjusted to fit all buttons on screen

        this.buttonList.clear();

        // Bottom action buttons - arranged in a more compact layout
        // Row 1: Four buttons side by side (dividing the width evenly)
        int totalButtonWidth = 400; // Total width for all buttons in row 1
        int singleButtonWidth = totalButtonWidth / 4; // ~100 pixels per button
        int startX = (this.width - totalButtonWidth) / 2; // Center the group of buttons
        
        this.loginButton = new GuiButton(0, startX, baseY, singleButtonWidth - 5, buttonHeight,
                "Add Cracked");
        this.loginButton2 = new GuiButton(1, startX + singleButtonWidth, baseY, singleButtonWidth - 5, buttonHeight,
                "Token Login");
        this.oauthButton = new GuiButton(3, startX + singleButtonWidth * 2, baseY, singleButtonWidth - 5, buttonHeight, "OAuth Login");
        GuiButton randomCrackedButton = new GuiButton(5, startX + singleButtonWidth * 3, baseY, singleButtonWidth - 5, buttonHeight, "Random Cracked");

        // Row 2: Two buttons side by side
        int midX = this.width / 2;
        this.deleteBannedButton = new GuiButton(4, midX - buttonWidth - 5, baseY + buttonSpacing, buttonWidth,
                buttonHeight, "Delete Banned");
        this.backButton = new GuiButton(2, midX + 5, baseY + buttonSpacing, buttonWidth, buttonHeight, "Back");

        this.buttonList.add(loginButton);
        this.buttonList.add(loginButton2);
        this.buttonList.add(oauthButton);
        this.buttonList.add(randomCrackedButton);
        this.buttonList.add(deleteBannedButton);
        this.buttonList.add(backButton);

        // Update banned status for all accounts
        updateBannedStatus();
    }

    private void updateBannedStatus() {
        for (Alt alt : alts) {
            boolean isBanned = AccountData.isBanned(alt.getName());
            alt.setBanned(isBanned);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Update smooth scroll offset with interpolation
        double targetScroll = scrollOffset;
        double diff = targetScroll - smoothScrollOffset;
        double scrollSpeed = 0.2; // Adjust this value to change scroll speed (higher = faster)
        if (Math.abs(diff) > 0.001) {
            // Smooth exponential interpolation
            smoothScrollOffset += diff * scrollSpeed * (1.0 + partialTicks * 0.5);
            // Clamp to prevent overshooting
            if (diff > 0 && smoothScrollOffset > targetScroll) {
                smoothScrollOffset = targetScroll;
            } else if (diff < 0 && smoothScrollOffset < targetScroll) {
                smoothScrollOffset = targetScroll;
            }
        } else {
            smoothScrollOffset = targetScroll;
        }
        
        // Draw background first using BackgroundRenderer
        BackgroundRenderer.draw(this.width, this.height, mouseX, mouseY);

        if (System.currentTimeMillis() - SessionChanger.instance().timeSinceFail < 5000
                && SessionChanger.instance().timeSinceFail != 0) {
            status = "§cFailed login";
        } else {
            if (status.equalsIgnoreCase("§cFailed login")) {
                SessionChanger.instance().timeSinceFail = 0;
                status = "§aIdle";
            }
        }

        // Use custom font renderer if available
        if (mc.fontRendererObj != null) {
            mc.fontRendererObj.drawString("Alt Manager", (int) (this.width / 2.0f - FontManager.productSans20.getStringWidth("Alt Manager") / 2.0f), 20, 0xFFFFFF);
            mc.fontRendererObj.drawString("Current Alt: §a" + mc.getSession().getUsername(), 5, 5, 0xAAAAAA);
            mc.fontRendererObj.drawString("Status: " + status, 5, 20, 0xAAAAAA);
        } else {
            // Fallback to standard Minecraft font renderer
            FontRenderer fontRenderer = mc.fontRendererObj;
            mc.fontRendererObj.drawStringWithShadow("Alt Manager", this.width / 2.0f - fontRenderer.getStringWidth("Alt Manager") / 2.0f, 20, 0xFFFFFF);
            mc.fontRendererObj.drawStringWithShadow("Current Alt: §a" + mc.getSession().getUsername(), 5, 5, 0xAAAAAA);
            mc.fontRendererObj.drawStringWithShadow("Status: " + status, 5, 20, 0xAAAAAA);
        }

        // Draw scrollable account list (draw after background to ensure text is
        // visible)
        drawAccountList(mouseX, mouseY);

        // Draw buttons (this will be drawn on top)
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawAccountList(int mouseX, int mouseY) {
        // Calculate center position for account list
        int totalWidth = (ACCOUNT_BUTTON_WIDTH * COLUMNS) + (COLUMN_SPACING * (COLUMNS - 1));
        int startX = (this.width - totalWidth) / 2;
        // Use smooth scroll offset for rendering
        int startIndex = Math.max(0, (int) Math.floor(smoothScrollOffset * COLUMNS));
        int endIndex = Math.min(alts.size(), startIndex + getTotalVisibleAccounts() + COLUMNS); // Add extra row for smooth scrolling

        // Removed account list background - no longer drawing solid background

        int visibleRows = getVisibleRows();
        String currentUsername = mc.getSession().getUsername();

        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_DEPTH_TEST);

        for (int i = startIndex; i < endIndex; i++) {
            Alt alt = alts.get(i);

            // Calculate which column and row this account belongs to (left-to-right, top-to-bottom)
            // Use absolute index for column to ensure proper alignment
            int accountRow = i / COLUMNS; // Row number based on account index
            int column = i % COLUMNS; // Column number based on account index (NOT relativeIndex!)
            int relativeIndex = i - startIndex;
            int relativeRow = relativeIndex / COLUMNS;

            if (relativeRow >= visibleRows + 1) // +1 for smooth scrolling extra row
                break; // Safety check

            // Apply smooth scroll offset for vertical position - use accountRow for consistent alignment
            double smoothRow = accountRow - smoothScrollOffset;
            int accountX = startX + column * (ACCOUNT_BUTTON_WIDTH + COLUMN_SPACING);
            int yPos = (int) (SCROLL_AREA_TOP + smoothRow * (ACCOUNT_BUTTON_HEIGHT + ROW_SPACING));
            
            // Skip rendering if outside visible area (for smooth scrolling)
            if (yPos + ACCOUNT_BUTTON_HEIGHT < SCROLL_AREA_TOP || yPos > getScrollAreaBottom()) {
                continue;
            }

            // Calculate delete button position (right center, inside account button)
            int deleteX = accountX + ACCOUNT_BUTTON_WIDTH - DELETE_BUTTON_WIDTH - 4;
            int deleteY = yPos + (ACCOUNT_BUTTON_HEIGHT - DELETE_BUTTON_HEIGHT) / 2;

            // Check hover states
            boolean hoveredAccount = mouseX >= accountX && mouseX < deleteX &&
                    mouseY >= yPos && mouseY <= yPos + ACCOUNT_BUTTON_HEIGHT;
            boolean hoveredDelete = mouseX >= deleteX && mouseX <= accountX + ACCOUNT_BUTTON_WIDTH &&
                    mouseY >= deleteY && mouseY <= deleteY + DELETE_BUTTON_HEIGHT;

            // Update hover value for fancy button style
            int currentHoverValue = accountHoverValues.getOrDefault(i, 102);
            int fps = Minecraft.getDebugFPS();
            if (fps <= 0) fps = 60;
            fps = Math.max(10, Math.min(240, fps));
            double step = Math.min(4.0 * 150.0 / fps, 10.0);
            if (hoveredAccount) {
                currentHoverValue = (int) Math.min(currentHoverValue + step, 200);
            } else {
                currentHoverValue = (int) Math.max(currentHoverValue - step, 102);
            }
            accountHoverValues.put(i, currentHoverValue);

            // Update hover value for delete button
            int deleteHoverValue = deleteHoverValues.getOrDefault(i, 102);
            if (hoveredDelete) {
                deleteHoverValue = (int) Math.min(deleteHoverValue + step, 200);
            } else {
                deleteHoverValue = (int) Math.max(deleteHoverValue - step, 102);
            }
            deleteHoverValues.put(i, deleteHoverValue);

            // Use fancy button style
            Color rectColor = new Color(35, 37, 43, currentHoverValue);
            rectColor = interpolateColor(rectColor, brighter(rectColor, 0.4f), -1);
            // Removed blur shadow around account buttons
        }

        // Second pass: Draw visible outlines and delete buttons
        for (int i = startIndex; i < endIndex; i++) {
            Alt alt = alts.get(i);

            // Calculate which column and row this account belongs to
            int accountRow = i / COLUMNS; // Row number based on account index
            int column = i % COLUMNS; // Column number based on account index
            int relativeIndex = i - startIndex;
            int relativeRow = relativeIndex / COLUMNS;

            if (relativeRow >= visibleRows + 1) // +1 for smooth scrolling extra row
                break;

            // Apply smooth scroll offset for vertical position - use accountRow for consistent alignment
            double smoothRow = accountRow - smoothScrollOffset;
            int accountX = startX + column * (ACCOUNT_BUTTON_WIDTH + COLUMN_SPACING);
            int yPos = (int) (SCROLL_AREA_TOP + smoothRow * (ACCOUNT_BUTTON_HEIGHT + ROW_SPACING));
            
            // Skip rendering if outside visible area (for smooth scrolling)
            if (yPos + ACCOUNT_BUTTON_HEIGHT < SCROLL_AREA_TOP || yPos > getScrollAreaBottom()) {
                continue;
            }
            
            int deleteX = accountX + ACCOUNT_BUTTON_WIDTH - DELETE_BUTTON_WIDTH - 4;
            int deleteY = yPos + (ACCOUNT_BUTTON_HEIGHT - DELETE_BUTTON_HEIGHT) / 2;

            // Get hover values
            int currentHoverValue = accountHoverValues.getOrDefault(i, 102);
            int deleteHoverValue = deleteHoverValues.getOrDefault(i, 102);

            // Use fancy button style
            Color rectColor = new Color(35, 37, 43, currentHoverValue);
            rectColor = interpolateColor(rectColor, brighter(rectColor, 0.4f), -1);
            
            // Draw visible outline
            RenderUtil.drawRoundedRectOutline(accountX, yPos, ACCOUNT_BUTTON_WIDTH, ACCOUNT_BUTTON_HEIGHT, 3.5F, 0.0015f,
                    rectColor.getRGB(), true, true, true, true);

            // Draw delete button with fancy rounded style
            Color deleteBaseColor = new Color(74, 0, 0, 180);
            Color deleteHoverColor = new Color(204, 0, 0, 220);
            float deleteHoverAmount = (deleteHoverValue - 102) / 98.0f;
            Color deleteColor = interpolateColor(deleteBaseColor, deleteHoverColor, deleteHoverAmount);
            RenderUtil.drawRoundedRect(deleteX, deleteY, DELETE_BUTTON_WIDTH, DELETE_BUTTON_HEIGHT, 4.0F, deleteColor.getRGB(), true, true, true, true);
        }

        // Restore GL state before drawing text
        GL11.glPopMatrix();
        
        // Draw all text in a separate pass
        for (int i = startIndex; i < endIndex; i++) {
            Alt alt = alts.get(i);

            // Calculate which column and row this account belongs to (left-to-right, top-to-bottom)
            int accountRow = i / COLUMNS; // Row number based on account index
            int column = i % COLUMNS; // Column number based on account index
            int relativeIndex = i - startIndex;
            int relativeRow = relativeIndex / COLUMNS;

            if (relativeRow >= visibleRows + 1) // +1 for smooth scrolling extra row
                break; // Safety check

            // Apply smooth scroll offset for vertical position - use accountRow for consistent alignment
            double smoothRow = accountRow - smoothScrollOffset;
            int accountX = startX + column * (ACCOUNT_BUTTON_WIDTH + COLUMN_SPACING);
            int yPos = (int) (SCROLL_AREA_TOP + smoothRow * (ACCOUNT_BUTTON_HEIGHT + ROW_SPACING));
            
            // Skip rendering if outside visible area (for smooth scrolling)
            if (yPos + ACCOUNT_BUTTON_HEIGHT < SCROLL_AREA_TOP || yPos > getScrollAreaBottom()) {
                continue;
            }

            // Calculate delete button position (right center, inside account button)
            int deleteX = accountX + ACCOUNT_BUTTON_WIDTH - DELETE_BUTTON_WIDTH - 4;
            int deleteY = yPos + (ACCOUNT_BUTTON_HEIGHT - DELETE_BUTTON_HEIGHT) / 2;

            // Check if this is the active account
            boolean isActive = alt.getName().equals(currentUsername);

            if (FontManager.productSans20 != null) {
                // Prepare text content - just the name, no [BANNED] text
                String nameText = alt.getName();
                
                // Truncate if too long to avoid delete button area
                // Account for delete button width + padding
                int availableWidth = ACCOUNT_BUTTON_WIDTH - DELETE_BUTTON_WIDTH - 20;
                if (FontManager.productSans20.getStringWidth(nameText) > availableWidth) {
                    String truncated = nameText;
                    while (FontManager.productSans20.getStringWidth(truncated + "...") > availableWidth && truncated.length() > 0) {
                        truncated = truncated.substring(0, truncated.length() - 1);
                    }
                    nameText = truncated + "...";
                }

                // Line 1: name (centered like normal GUI buttons)
                // Center horizontally, position vertically in upper portion of button
                double textX = accountX + ACCOUNT_BUTTON_WIDTH / 2.0;
                double textY = yPos + ACCOUNT_BUTTON_HEIGHT / 2.0 - FontManager.productSans20.getHeight() / 2.0 - 6;
                // Use green color for active account, red for banned, white for others
                int nameColor;
                if (isActive) {
                    nameColor = 0xFF00FF00; // Green for active
                } else if (alt.isBanned()) {
                    nameColor = 0xFFFF0000; // Red for banned
                } else {
                    nameColor = -1; // White for normal
                }
                
                // Center the text by calculating the horizontal center position
                int centeredX = (int) (textX - FontManager.productSans20.getStringWidth(nameText) / 2.0);
                FontManager.productSans20.drawString(nameText, centeredX, (float) textY, nameColor);

                // Line 2: account type (centered below name)
                String accountType = alt.isCracked() ? "Cracked" : "Premium";
                double accountTypeY = yPos + ACCOUNT_BUTTON_HEIGHT / 2.0 - FontManager.productSans20.getHeight() / 2.0 + 6;
                // Use brighter colors: green for Premium, orange for Cracked
                int accountTypeColor = alt.isCracked() ? 0xFFFFAA00 : 0xFF00FF00; // Orange for Cracked, Green for Premium (ARGB format with alpha)
                int accountTypeCenteredX = (int) (textX - FontManager.productSans20.getStringWidth(accountType) / 2.0);
                FontManager.productSans20.drawString(accountType, accountTypeCenteredX, (float) accountTypeY, accountTypeColor);

                // Draw delete button text (centered like normal GUI buttons)
                double deleteTextX = deleteX + DELETE_BUTTON_WIDTH / 2.0;
                double deleteTextY = deleteY + DELETE_BUTTON_HEIGHT / 2.0 - FontManager.productSans20.getHeight() / 2.0;
                String deleteText = "X";
                int deleteTextCenteredX = (int) (deleteTextX - FontManager.productSans20.getStringWidth(deleteText) / 2.0);
                FontManager.productSans20.drawString(deleteText, deleteTextCenteredX, (float) deleteTextY, 0xFFFFFFFF); // White X symbol
            } else {
                // Fallback to standard Minecraft font renderer
                FontRenderer fontRenderer = mc.fontRendererObj;
                
                // Prepare text content - just the name, no [BANNED] text
                String nameText = alt.getName();
                
                // Truncate if too long to avoid delete button area
                // Account for delete button width + padding
                int availableWidth = ACCOUNT_BUTTON_WIDTH - DELETE_BUTTON_WIDTH - 20;
                if (fontRenderer.getStringWidth(nameText) > availableWidth) {
                    String truncated = nameText;
                    while (fontRenderer.getStringWidth(truncated + "...") > availableWidth && truncated.length() > 0) {
                        truncated = truncated.substring(0, truncated.length() - 1);
                    }
                    nameText = truncated + "...";
                }

                // Line 1: name (centered like normal GUI buttons)
                // Center horizontally, position vertically in upper portion of button
                double textX = accountX + ACCOUNT_BUTTON_WIDTH / 2.0;
                double textY = yPos + ACCOUNT_BUTTON_HEIGHT / 2.0 - fontRenderer.FONT_HEIGHT / 2.0 - 6;
                // Use green color for active account, red for banned, white for others
                int nameColor;
                if (isActive) {
                    nameColor = 0xFF00FF00; // Green for active
                } else if (alt.isBanned()) {
                    nameColor = 0xFFFF0000; // Red for banned
                } else {
                    nameColor = -1; // White for normal
                }
                
                // Center the text by calculating the horizontal center position
                int centeredX = (int) (textX - fontRenderer.getStringWidth(nameText) / 2.0);
                fontRenderer.drawStringWithShadow(nameText, centeredX, (float) textY, nameColor);

                // Line 2: account type (centered below name)
                String accountType = alt.isCracked() ? "Cracked" : "Premium";
                double accountTypeY = yPos + ACCOUNT_BUTTON_HEIGHT / 2.0 - fontRenderer.FONT_HEIGHT / 2.0 + 6;
                // Use brighter colors: green for Premium, orange for Cracked
                int accountTypeColor = alt.isCracked() ? 0xFFFFAA00 : 0xFF00FF00; // Orange for Cracked, Green for Premium (ARGB format with alpha)
                int accountTypeCenteredX = (int) (textX - fontRenderer.getStringWidth(accountType) / 2.0);
                fontRenderer.drawStringWithShadow(accountType, accountTypeCenteredX, (float) accountTypeY, accountTypeColor);

                // Draw delete button text (centered like normal GUI buttons)
                double deleteTextX = deleteX + DELETE_BUTTON_WIDTH / 2.0;
                double deleteTextY = deleteY + DELETE_BUTTON_HEIGHT / 2.0 - fontRenderer.FONT_HEIGHT / 2.0;
                String deleteText = "X";
                int deleteTextCenteredX = (int) (deleteTextX - fontRenderer.getStringWidth(deleteText) / 2.0);
                fontRenderer.drawStringWithShadow(deleteText, deleteTextCenteredX, (float) deleteTextY, 0xFFFFFFFF); // White X symbol
            }
        }

        // Restore depth test after text rendering
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            this.mc.displayGuiScreen(new CrackedLoginGui(this));
        } else if (button.id == 1) {
            this.mc.displayGuiScreen(new MicrosoftLoginGui(this));
        } else if (button.id == 2) {
            AltJsonHandler.saveAlts();
            this.mc.displayGuiScreen(parent);
        } else if (button.id == 3) {
            startOAuthLogin();
        } else if (button.id == 4) {
            // Delete all banned accounts
            Iterator<Alt> iterator = alts.iterator();
            int deletedCount = 0;
            while (iterator.hasNext()) {
                Alt alt = iterator.next();
                if (alt.isBanned()) {
                    iterator.remove();
                    deletedCount++;
                }
            }
            AltJsonHandler.saveAlts();
            this.initGui();
        } else if (button.id == 5) {
            // Random Cracked button clicked
            String randomUsername = myau.management.altmanager.util.NameGenerator.generateRandomUsername();
            if (randomUsername != null && !randomUsername.isEmpty()) {
                // Create a new cracked alt with the random username
                Alt alt = new Alt(null, null, randomUsername, true);
                boolean isBanned = AccountData.isBanned(randomUsername);
                alt.setBanned(isBanned);
                alts.add(alt);
                SessionChanger.instance().loginCracked(randomUsername);
                AltJsonHandler.start();
                AltJsonHandler.saveAlts();
                AltJsonHandler.loadAlts();
                AltManagerGui.status = "§aAdded and logged in with random cracked alt: " + randomUsername;
            } else {
                AltManagerGui.status = "§cFailed to generate random username";
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            // Check if mouse is over scroll area
            int totalWidth = (ACCOUNT_BUTTON_WIDTH * COLUMNS) + (COLUMN_SPACING * (COLUMNS - 1));
            int startX = (this.width - totalWidth) / 2;
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

            int scrollAreaBottom = getScrollAreaBottom();
            int scrollAreaRight = startX + totalWidth + 5;
            if (mouseX >= startX - 5 && mouseX <= scrollAreaRight &&
                    mouseY >= SCROLL_AREA_TOP - 5 && mouseY <= scrollAreaBottom + 5 + ROW_SPACING) {

                if (dWheel > 0) {
                    // Scroll up
                    scrollOffset = Math.max(0, scrollOffset - 1);
                } else {
                    // Scroll down
                    int totalRows = (int) Math.ceil((double) alts.size() / COLUMNS);
                    int visibleRows = getVisibleRows();
                    int maxScroll = Math.max(0, totalRows - visibleRows);
                    scrollOffset = Math.min(maxScroll, scrollOffset + 1);
                }
                // Don't call initGui() - smooth scrolling handles the update
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        // Handle clicks on account buttons
        if (mouseButton == 0) {
            // Calculate center position for account list
            int totalWidth = (ACCOUNT_BUTTON_WIDTH * COLUMNS) + (COLUMN_SPACING * (COLUMNS - 1));
            int startX = (this.width - totalWidth) / 2;
            // Use smooth scroll offset for click detection to match visual position
            int startIndex = Math.max(0, (int) Math.floor(smoothScrollOffset * COLUMNS));
            int endIndex = Math.min(alts.size(), startIndex + getTotalVisibleAccounts() + COLUMNS);

            int visibleRows = getVisibleRows();

            for (int i = startIndex; i < endIndex; i++) {
                // Calculate which column and row this account belongs to (left-to-right, top-to-bottom)
                // Use absolute index for column to ensure proper alignment
                int accountRow = i / COLUMNS;
                int column = i % COLUMNS; // Column number based on account index (NOT relativeIndex!)
                int relativeIndex = i - startIndex;
                int relativeRow = relativeIndex / COLUMNS;

                if (relativeRow >= visibleRows + 1)
                    break;

                // Use smooth scroll offset for accurate click detection
                double smoothRow = accountRow - smoothScrollOffset;
                int accountX = startX + column * (ACCOUNT_BUTTON_WIDTH + COLUMN_SPACING);
                int yPos = (int) (SCROLL_AREA_TOP + smoothRow * (ACCOUNT_BUTTON_HEIGHT + ROW_SPACING));
                int deleteX = accountX + ACCOUNT_BUTTON_WIDTH - DELETE_BUTTON_WIDTH - 4;
                int deleteY = yPos + (ACCOUNT_BUTTON_HEIGHT - DELETE_BUTTON_HEIGHT) / 2;

                // Check if clicked on delete button area first
                if (mouseX >= deleteX && mouseX <= accountX + ACCOUNT_BUTTON_WIDTH &&
                        mouseY >= deleteY && mouseY <= deleteY + DELETE_BUTTON_HEIGHT) {
                    // Delete button clicked
                    alts.remove(i);
                    AltJsonHandler.saveAlts();
                    // Adjust scroll if needed
                    int totalRows = (int) Math.ceil((double) alts.size() / COLUMNS);
                    int maxScroll = Math.max(0, totalRows - visibleRows);
                    if (scrollOffset > maxScroll) {
                        scrollOffset = maxScroll;
                    }
                    this.initGui();
                    return;
                }

                // Check if clicked on account button area (excluding delete button)
                if (mouseX >= accountX && mouseX < deleteX &&
                        mouseY >= yPos && mouseY <= yPos + ACCOUNT_BUTTON_HEIGHT) {

                    Alt alt = alts.get(i);
                    if (alt.isCracked()) {
                        SessionChanger.instance().loginCracked(alt.getName());
                    } else {
                        if (alt.hasRefreshToken()) {
                            SessionChanger.instance().loginWithRefreshToken(alt.getRefreshToken());
                        } else {
                            SessionChanger.instance().loginMicrosoft(alt.getEmail(), alt.getPassword());
                        }
                    }
                    return;
                }
            }
        }
    }

    private void startOAuthLogin() {
        status = "§6Opening browser...";
        MicrosoftOAuthTranslation.getRefreshToken(refreshToken -> mc.addScheduledTask(() -> {
            if (refreshToken == null || refreshToken.isEmpty()) {
                status = "§cOAuth cancelled";
                return;
            }

            status = "§6Logging in...";

            new Thread(() -> {
                MicrosoftOAuthTranslation.LoginData loginData = MicrosoftOAuthTranslation.login(refreshToken);

                mc.addScheduledTask(() -> {
                    if (loginData.isGood()) {
                        // Update session immediately
                        SessionChanger.instance().setSessionWithData(loginData);

                        // Upsert account based on username or UUID
                        Alt existingAlt = null;
                        for (Alt alt : alts) {
                            if (alt.getName().equals(loginData.username) ||
                                    (loginData.uuid != null && loginData.uuid.equals(alt.getUuid()))) {
                                existingAlt = alt;
                                break;
                            }
                        }

                        String tokenToStore = (loginData.newRefreshToken != null && !loginData.newRefreshToken.isEmpty())
                                ? loginData.newRefreshToken
                                : refreshToken;

                        if (existingAlt != null) {
                            existingAlt.setRefreshToken(tokenToStore);
                            existingAlt.setUuid(loginData.uuid);
                            existingAlt.setBanned(AccountData.isBanned(loginData.username));
                        } else {
                            Alt alt = new Alt(loginData.username, "", loginData.username, false);
                            alt.setRefreshToken(tokenToStore);
                            alt.setUuid(loginData.uuid);
                            alt.setBanned(AccountData.isBanned(loginData.username));
                            alts.add(alt);
                        }

                        // Persist and refresh list from disk
                        AltJsonHandler.start();
                        AltJsonHandler.saveAlts();
                        AltJsonHandler.loadAlts();

                        status = "§aLogged in as " + loginData.username;
                        this.initGui();
                        mc.displayGuiScreen(this);
                    } else {
                        status = "§cOAuth login failed" +
                                (loginData.errorMessage != null ? ": " + loginData.errorMessage : "");
                    }
                });
            }).start();
        }));
    }

    // Helper method for color interpolation (similar to MixinGuiButton)
    private Color interpolateColor(Color color1, Color color2, float amount) {
        amount = Math.min(1.0f, Math.max(0.0f, amount));
        return new Color(
                interpolateInt(color1.getRed(), color2.getRed(), amount),
                interpolateInt(color1.getGreen(), color2.getGreen(), amount),
                interpolateInt(color1.getBlue(), color2.getBlue(), amount),
                interpolateInt(color1.getAlpha(), color2.getAlpha(), amount));
    }
    
    // Helper method to brighten a color
    private Color brighter(Color color, float factor) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int alpha = color.getAlpha();
        
        r = Math.min(255, (int)(r * (1 + factor)));
        g = Math.min(255, (int)(g * (1 + factor)));
        b = Math.min(255, (int)(b * (1 + factor)));
        
        return new Color(r, g, b, alpha);
    }
    
    // Helper method to interpolate integer values
    private int interpolateInt(int start, int end, float amount) {
        return (int) (start + amount * (end - start));
    }
}