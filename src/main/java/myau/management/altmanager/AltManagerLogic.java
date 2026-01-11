package myau.management.altmanager;

import myau.management.altmanager.util.AltJsonHandler;
import myau.util.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Non-GUI helpers for working with the alt list. Mirrors the behaviour of FDP's
 * AccountsConfig without bringing over UI or name generation.
 */
public final class AltManagerLogic {

    private AltManagerLogic() {
    }

    /**
     * Return the live alt list.
     */
    public static List<Alt> getAccounts() {
        return AltManagerGui.alts;
    }

    /**
     * Adds a cracked account if it does not already exist.
     *
     * @return the created Alt or null if it already existed.
     */
    public static Alt addCrackedAccount(String name) {
        if (name == null || name.trim().isEmpty()) return null;
        Alt alt = new Alt(null, null, name.trim(), true);
        if (accountExists(alt)) return null;
        AltManagerGui.alts.add(alt);
        AltJsonHandler.saveAlts();
        return alt;
    }

    /**
     * Adds a premium/Microsoft account if it does not already exist.
     *
     * @return the created Alt or null if it already existed.
     */
    public static Alt addPremiumAccount(String email, String password, String displayName, String refreshToken, String uuid) {
        String cleanEmail = email == null ? null : email.trim();
        String cleanName = displayName == null ? null : displayName.trim();
        Alt alt = new Alt(cleanEmail, password, cleanName, false);
        if (refreshToken != null && !refreshToken.isEmpty()) {
            alt.setRefreshToken(refreshToken);
        }
        if (uuid != null && !uuid.isEmpty()) {
            alt.setUuid(uuid);
        }
        if (accountExists(alt)) return null;
        AltManagerGui.alts.add(alt);
        AltJsonHandler.saveAlts();
        return alt;
    }

    public static boolean removeAccount(Alt alt) {
        if (alt == null) return false;
        boolean removed = AltManagerGui.alts.remove(alt);
        if (removed) AltJsonHandler.saveAlts();
        return removed;
    }

    public static boolean accountExists(Alt alt) {
        if (alt == null) return false;
        return AltManagerGui.alts.stream().anyMatch(existing -> sameAccount(existing, alt));
    }

    private static boolean sameAccount(Alt a, Alt b) {
        // Cracked: compare names (case-insensitive)
        if (a.isCracked() && b.isCracked()) {
            return safeEqualsIgnoreCase(a.getName(), b.getName());
        }

        // Premium: prefer uuid, then email, then name
        if (!a.isCracked() && !b.isCracked()) {
            if (safeEqualsIgnoreCase(a.getUuid(), b.getUuid())) return true;
            if (safeEqualsIgnoreCase(a.getEmail(), b.getEmail())) return true;
            return safeEqualsIgnoreCase(a.getName(), b.getName());
        }

        return false;
    }

    private static boolean safeEqualsIgnoreCase(String a, String b) {
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }

    /**
     * Move an account in the backing list.
     */
    public static void swapAccounts(int firstIndex, int secondIndex) {
        List<Alt> list = AltManagerGui.alts;
        if (firstIndex < 0 || secondIndex < 0 || firstIndex >= list.size() || secondIndex >= list.size()) return;
        Collections.swap(list, firstIndex, secondIndex);
        AltJsonHandler.saveAlts();
    }

    /**
     * Picks a random account from the list.
     */
    public static Alt randomAccount() {
        List<Alt> list = AltManagerGui.alts;
        if (list.isEmpty()) return null;
        return list.get(RandomUtil.nextInt(0, list.size() - 1));
    }

    /**
     * Import accounts from a simple text file. Supports:
     * <p>
     * email:password (premium) or plain names (cracked).
     */
    public static int importFromFile(File file) throws IOException {
        if (file == null || !file.exists() || file.isDirectory()) return 0;

        int added = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] split = line.split(":", 2);
                if (split.length == 2) {
                    if (addPremiumAccount(split[0], split[1], split[0], null, null) != null) added++;
                } else {
                    if (addCrackedAccount(line) != null) added++;
                }
            }
        }
        if (added > 0) AltJsonHandler.saveAlts();
        return added;
    }

    /**
     * Export accounts to a text file in a format compatible with importFromFile.
     */
    public static void exportToFile(File file) throws IOException {
        if (file == null) return;
        if (file.isDirectory()) throw new IOException("Target is a directory");
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            file.createNewFile();
        }

        List<String> lines = AltManagerGui.alts.stream().map(alt -> {
            if (alt.isCracked()) return alt.getName();
            if (alt.getEmail() != null && alt.getPassword() != null) return alt.getEmail() + ":" + alt.getPassword();
            return alt.getName();
        }).collect(Collectors.toList());

        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    /**
     * Marks an account as banned based on name.
     */
    public static void markBanned(String name) {
        if (name == null) return;
        for (Alt alt : AltManagerGui.alts) {
            if (safeEqualsIgnoreCase(alt.getName(), name)) {
                alt.setBanned(true);
            }
        }
        AltJsonHandler.saveAlts();
    }
}