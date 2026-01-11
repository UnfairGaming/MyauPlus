package myau.management.altmanager.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NameGenerator {
    private static final Random random = new Random();
    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 16; // Minecraft username limit
    
    // Default word banks
    private static final String[] DEFAULT_ADJECTIVES = {
        "Cool", "Fast", "Swift", "Bright", "Dark", "Sharp", "Smooth", "Bold", "Wild", "Crazy",
        "Epic", "Lucky", "Magic", "Golden", "Silver", "Brave", "Fierce", "Gentle", "Quick", "Smart",
        "Tough", "Strong", "Clever", "Wise", "Calm", "Happy", "Sad", "Angry", "Proud", "Noble",
        "Quiet", "Loud", "Big", "Small", "Tall", "Short", "Wide", "Thin", "Thick", "Slim",
        "Mighty", "Ancient", "Frozen", "Burning", "Silent", "Rapid", "Stealth", "Divine", "Mystic", "Legend",
        "Deadly", "Savage", "Furious", "Vicious", "Brutal", "Ruthless", "Fierce", "Wicked", "Sinister", "Dark",
        "Radiant", "Brilliant", "Glorious", "Majestic", "Splendid", "Magnificent", "Elegant", "Graceful", "Regal", "Royal",
        "Mysterious", "Shadowy", "Hidden", "Secret", "Covert", "Elusive", "Ethereal", "Ghostly", "Phantom", "Wraith",
        "Thunderous", "Stormy", "Electric", "Blazing", "Fiery", "Flaming", "Scorching", "Searing", "Molten", "Volcanic",
        "Frosty", "Icy", "Glacial", "Arctic", "Polar", "Freezing", "Chilled", "Crystal", "Diamond", "Ice",
        "Swift", "Rapid", "Lightning", "Blitz", "Turbo", "Sonic", "Supersonic", "Hyper", "Ultra", "Extreme"
    };
    
    private static final String[] DEFAULT_ADVERBS = {
        "Swiftly", "Boldly", "Quietly", "Fiercely", "Calmly", "Rapidly", "Stealthily", "Bravely",
        "Wildly", "Smoothly", "Sharply", "Brightly", "Darkly", "Epically", "Magically", "Divinely",
        "Mightily", "Cleverly", "Wisely", "Proudly", "Nobly", "Gently", "Quickly", "Strongly",
        "Silently", "Frozenly", "Burning", "Mystically", "Legendary", "Anciently",
        "Deadly", "Savagely", "Furiously", "Viciously", "Brutally", "Ruthlessly", "Wickedly", "Sinisterly",
        "Radiantly", "Brilliantly", "Gloriously", "Majestically", "Splendidly", "Magnificently", "Elegantly", "Gracefully",
        "Mysteriously", "Shadowily", "Secretly", "Covertly", "Elusively", "Ethereally", "Ghostly", "Phantomly",
        "Thunderously", "Stormily", "Electrically", "Blazingly", "Fiery", "Flamingly", "Scorchingly", "Searingly",
        "Frostily", "Icily", "Glacially", "Arctically", "Polar", "Freezingly", "Chilled", "Crystal",
        "Lightning", "Blitz", "Turbo", "Sonic", "Supersonic", "Hyper", "Ultra", "Extreme"
    };
    
    private static final String[] DEFAULT_VERBS = {
        "Strike", "Dash", "Leap", "Soar", "Roar", "Hunt", "Stalk", "Pounce", "Charge", "Rush",
        "Blaze", "Freeze", "Shock", "Crush", "Slice", "Pierce", "Guard", "Defend", "Attack", "Retreat",
        "Emerge", "Vanish", "Glide", "Dive", "Climb", "Fall", "Rise", "Sink", "Float", "Drift",
        "Conquer", "Dominate", "Rule", "Command", "Lead", "Follow", "Track", "Trace", "Seek", "Find",
        "Slash", "Cut", "Rend", "Tear", "Rip", "Shred", "Break", "Shatter", "Smash", "Destroy",
        "Blast", "Explode", "Burst", "Erupt", "Ignite", "Burn", "Melt", "Vaporize", "Annihilate", "Obliterate",
        "Escape", "Flee", "Evade", "Dodge", "Duck", "Weave", "Dart", "Sprint", "Race", "Zoom",
        "Ambush", "Surprise", "Assault", "Raid", "Invade", "Storm", "Besiege", "Overwhelm", "Overpower", "Overcome",
        "Protect", "Shield", "Block", "Parry", "Counter", "Repel", "Resist", "Endure", "Survive", "Persist",
        "Observe", "Watch", "Monitor", "Survey", "Scout", "Recon", "Investigate", "Examine", "Analyze", "Study"
    };
    
    private static final String[] DEFAULT_NOUNS = {
        "Wolf", "Eagle", "Tiger", "Lion", "Bear", "Fox", "Hawk", "Shark", "Falcon", "Dragon",
        "Warrior", "Knight", "Mage", "Ranger", "Assassin", "Ninja", "Samurai", "Viking", "Pirate", "Wizard",
        "Storm", "Fire", "Ice", "Thunder", "Shadow", "Light", "Blade", "Sword", "Arrow", "Bow",
        "Star", "Moon", "Sun", "Sky", "Ocean", "Mountain", "River", "Forest", "Desert", "Valley",
        "Phoenix", "Griffin", "Demon", "Angel", "Spirit", "Ghost", "Beast", "Monster", "Guardian", "Champion",
        "Hunter", "Predator", "Killer", "Slayer", "Destroyer", "Reaper", "Executioner", "Butcher", "Murderer", "Assassin",
        "Hero", "Legend", "Myth", "Tale", "Saga", "Epic", "Story", "Chronicle", "Account", "Narrative",
        "Rogue", "Thief", "Bandit", "Outlaw", "Criminal", "Villain", "Scoundrel", "Rascal", "Rebel", "Renegade",
        "Sorcerer", "Warlock", "Necromancer", "Enchanter", "Alchemist", "Shaman", "Druid", "Priest", "Cleric", "Paladin",
        "Blade", "Axe", "Hammer", "Spear", "Dagger", "Mace", "Whip", "Chain", "Claw", "Fang",
        "Flame", "Frost", "Lightning", "Thunder", "Wind", "Earth", "Water", "Void", "Chaos", "Order",
        "Reaper", "Specter", "Wraith", "Banshee", "Vampire", "Werewolf", "Zombie", "Skeleton", "Golem", "Titan"
    };
    
    // Loaded word banks (can be customized via files)
    private static List<String> ADJECTIVES = new ArrayList<>();
    private static List<String> ADVERBS = new ArrayList<>();
    private static List<String> VERBS = new ArrayList<>();
    private static List<String> NOUNS = new ArrayList<>();
    
    // File directory for word bank files (uses same root as AltJsonHandler)
    private static File WORD_BANK_DIR = new File(AltJsonHandler.ROOT_DIR, "nameGenerator");
    
    static {
        loadWordBanks();
    }
    
    /**
     * Loads word banks from files, or uses defaults if files don't exist
     */
    private static void loadWordBanks() {
        // Initialize with defaults
        ADJECTIVES.clear();
        ADVERBS.clear();
        VERBS.clear();
        NOUNS.clear();
        
        for (String word : DEFAULT_ADJECTIVES) {
            ADJECTIVES.add(word);
        }
        for (String word : DEFAULT_ADVERBS) {
            ADVERBS.add(word);
        }
        for (String word : DEFAULT_VERBS) {
            VERBS.add(word);
        }
        for (String word : DEFAULT_NOUNS) {
            NOUNS.add(word);
        }
        
        // Create directory if it doesn't exist
        if (!WORD_BANK_DIR.exists()) {
            WORD_BANK_DIR.mkdirs();
        }
        
        // Try to load from files
        loadWordBankFromFile("adjectives.txt", ADJECTIVES);
        loadWordBankFromFile("adverbs.txt", ADVERBS);
        loadWordBankFromFile("verbs.txt", VERBS);
        loadWordBankFromFile("nouns.txt", NOUNS);
        
        // Ensure we have at least some words
        if (ADJECTIVES.isEmpty()) {
            for (String word : DEFAULT_ADJECTIVES) {
                ADJECTIVES.add(word);
            }
        }
        if (ADVERBS.isEmpty()) {
            for (String word : DEFAULT_ADVERBS) {
                ADVERBS.add(word);
            }
        }
        if (VERBS.isEmpty()) {
            for (String word : DEFAULT_VERBS) {
                VERBS.add(word);
            }
        }
        if (NOUNS.isEmpty()) {
            for (String word : DEFAULT_NOUNS) {
                NOUNS.add(word);
            }
        }
    }
    
    /**
     * Loads words from a file into a list
     */
    private static void loadWordBankFromFile(String filename, List<String> wordList) {
        File file = new File(WORD_BANK_DIR, filename);
        if (!file.exists()) {
            // Create default file
            try {
                file.createNewFile();
                PrintWriter writer = new PrintWriter(new FileWriter(file));
                // Write default words based on filename
                String[] defaults = null;
                if (filename.equals("adjectives.txt")) {
                    defaults = DEFAULT_ADJECTIVES;
                } else if (filename.equals("adverbs.txt")) {
                    defaults = DEFAULT_ADVERBS;
                } else if (filename.equals("verbs.txt")) {
                    defaults = DEFAULT_VERBS;
                } else if (filename.equals("nouns.txt")) {
                    defaults = DEFAULT_NOUNS;
                }
                
                if (defaults != null) {
                    for (String word : defaults) {
                        writer.println(word);
                    }
                }
                writer.close();
            } catch (IOException e) {
                // If we can't create the file, just use defaults
                return;
            }
        }
        
        // Read from file
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            wordList.clear();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && line.length() <= 20) { // Reasonable word length limit
                    wordList.add(line);
                }
            }
            reader.close();
        } catch (IOException e) {
            // If we can't read, keep defaults
        }
    }
    
    /**
     * Generates a random username using the best method first, falls back to backup method if needed.
     * Best method: Grabs an existing minecraft username and changes at least 3 characters
     * Backup method: Various patterns with word combinations
     */
    public static String generateRandomUsername() {
        String username = null;
        int attempts = 0;
        int maxAttempts = 10;
        
        // Try to generate a valid username
        while ((username == null || username.length() < MIN_NAME_LENGTH || username.length() > MAX_NAME_LENGTH) && attempts < maxAttempts) {
            attempts++;
            
            try {
                // Try to get a random minecraft username from AccountData
                String existingUsername = getRandomMinecraftUsername();
                if (existingUsername != null && !existingUsername.isEmpty()) {
                    username = modifyUsername(existingUsername);
                } else {
                    // Backup method: Use various patterns
                    username = generateBackupUsername();
                }
            } catch (Exception e) {
                // Fall back to backup method if fails
                username = generateBackupUsername();
            }
            
            // Validate length
            if (username != null) {
                if (username.length() < MIN_NAME_LENGTH) {
                    // Too short, pad with numbers
                    while (username.length() < MIN_NAME_LENGTH && username.length() < MAX_NAME_LENGTH) {
                        username = username + random.nextInt(10);
                    }
                } else if (username.length() > MAX_NAME_LENGTH) {
                    // Too long, truncate
                    username = username.substring(0, MAX_NAME_LENGTH);
                }
            }
        }
        
        // Final fallback if all attempts failed
        if (username == null || username.length() < MIN_NAME_LENGTH) {
            username = "Player" + (random.nextInt(9000) + 1000);
        }
        
        return username;
    }
    
    /**
     * Fetches a random minecraft username from AccountData
     */
    private static String getRandomMinecraftUsername() {
        try {
            Map<String, Boolean> names = myau.management.altmanager.AccountData.getNames();
            if (names != null && !names.isEmpty()) {
                List<String> nameList = new ArrayList<>(names.keySet());
                if (!nameList.isEmpty()) {
                    return nameList.get(random.nextInt(nameList.size()));
                }
            }
        } catch (Exception e) {
            // Continue to backup method
        }
        return null;
    }
    
    /**
     * Modifies a username by changing at least 3 characters (any characters, not just digits)
     */
    private static String modifyUsername(String username) {
        if (username == null || username.isEmpty()) {
            return generateBackupUsername();
        }
        
        // Ensure username is within valid length before modifying
        if (username.length() > MAX_NAME_LENGTH) {
            username = username.substring(0, MAX_NAME_LENGTH);
        }
        if (username.length() < MIN_NAME_LENGTH) {
            // If too short, pad it first
            while (username.length() < MIN_NAME_LENGTH && username.length() < MAX_NAME_LENGTH) {
                username = username + random.nextInt(10);
            }
        }
        
        char[] chars = username.toCharArray();
        int minChanges = 3;
        
        // Collect all character positions that can be changed
        List<Integer> changeablePositions = new ArrayList<>();
        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            // Skip underscores and hyphens as they're common in usernames
            if (c != '_' && c != '-') {
                changeablePositions.add(i);
            }
        }
        
        // If not enough positions, include underscores/hyphens
        if (changeablePositions.size() < minChanges) {
            changeablePositions.clear();
            for (int i = 0; i < chars.length; i++) {
                changeablePositions.add(i);
            }
        }
        
        // Randomly shuffle positions to change
        List<Integer> positionsToChange = new ArrayList<>();
        while (positionsToChange.size() < minChanges && !changeablePositions.isEmpty()) {
            int randomIndex = random.nextInt(changeablePositions.size());
            positionsToChange.add(changeablePositions.remove(randomIndex));
        }
        
        // Change the selected characters
        for (Integer pos : positionsToChange) {
            char originalChar = chars[pos];
            char newChar;
            
            if (Character.isDigit(originalChar)) {
                // Change to a different digit
                do {
                    newChar = (char) ('0' + random.nextInt(10));
                } while (newChar == originalChar);
            } else if (Character.isLetter(originalChar)) {
                // Change to a different letter (same case)
                if (Character.isUpperCase(originalChar)) {
                    do {
                        newChar = (char) ('A' + random.nextInt(26));
                    } while (newChar == originalChar);
                } else {
                    do {
                        newChar = (char) ('a' + random.nextInt(26));
                    } while (newChar == originalChar);
                }
            } else {
                // For other characters, change to letter or digit
                if (random.nextBoolean()) {
                    newChar = (char) ('a' + random.nextInt(26));
                } else {
                    newChar = (char) ('0' + random.nextInt(10));
                }
            }
            
            chars[pos] = newChar;
        }
        
        return new String(chars);
    }
    
    /**
     * Backup method: Generates username using various patterns
     * Patterns: (adv)(adj)(num), (adv)(v)(num), (v)(adv)(num), (adj)(noun)(num)
     */
    private static String generateBackupUsername() {
        int pattern = random.nextInt(4); // 0-3 for 4 patterns
        String result = "";
        int maxAttempts = 20; // Try multiple times to get valid length
        int attempts = 0;
        
        while ((result.isEmpty() || result.length() < MIN_NAME_LENGTH || result.length() > MAX_NAME_LENGTH) && attempts < maxAttempts) {
            attempts++;
            result = "";
            
            switch (pattern) {
                case 0:
                    // Pattern: (adv)(adj)(num)
                    if (!ADVERBS.isEmpty() && !ADJECTIVES.isEmpty()) {
                        String adv = ADVERBS.get(random.nextInt(ADVERBS.size()));
                        String adj = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
                        // Calculate how many digits we can add while staying within limits
                        int baseLength = adv.length() + adj.length();
                        int maxDigits = Math.min(4, MAX_NAME_LENGTH - baseLength);
                        int minDigits = Math.max(1, MIN_NAME_LENGTH - baseLength);
                        if (minDigits <= maxDigits && maxDigits > 0) {
                            int numDigits = minDigits < maxDigits ? random.nextInt(maxDigits - minDigits + 1) + minDigits : minDigits;
                            StringBuilder digits = new StringBuilder();
                            for (int i = 0; i < numDigits; i++) {
                                digits.append(random.nextInt(10));
                            }
                            result = adv + adj + digits.toString();
                        }
                    }
                    break;
                case 1:
                    // Pattern: (adv)(v)(num)
                    if (!ADVERBS.isEmpty() && !VERBS.isEmpty()) {
                        String adv = ADVERBS.get(random.nextInt(ADVERBS.size()));
                        String verb = VERBS.get(random.nextInt(VERBS.size()));
                        int baseLength = adv.length() + verb.length();
                        int maxDigits = Math.min(4, MAX_NAME_LENGTH - baseLength);
                        int minDigits = Math.max(1, MIN_NAME_LENGTH - baseLength);
                        if (minDigits <= maxDigits && maxDigits > 0) {
                            int numDigits = minDigits < maxDigits ? random.nextInt(maxDigits - minDigits + 1) + minDigits : minDigits;
                            StringBuilder digits = new StringBuilder();
                            for (int i = 0; i < numDigits; i++) {
                                digits.append(random.nextInt(10));
                            }
                            result = adv + verb + digits.toString();
                        }
                    }
                    break;
                case 2:
                    // Pattern: (v)(adv)(num)
                    if (!VERBS.isEmpty() && !ADVERBS.isEmpty()) {
                        String verb = VERBS.get(random.nextInt(VERBS.size()));
                        String adv = ADVERBS.get(random.nextInt(ADVERBS.size()));
                        int baseLength = verb.length() + adv.length();
                        int maxDigits = Math.min(4, MAX_NAME_LENGTH - baseLength);
                        int minDigits = Math.max(1, MIN_NAME_LENGTH - baseLength);
                        if (minDigits <= maxDigits && maxDigits > 0) {
                            int numDigits = minDigits < maxDigits ? random.nextInt(maxDigits - minDigits + 1) + minDigits : minDigits;
                            StringBuilder digits = new StringBuilder();
                            for (int i = 0; i < numDigits; i++) {
                                digits.append(random.nextInt(10));
                            }
                            result = verb + adv + digits.toString();
                        }
                    }
                    break;
                case 3:
                default:
                    // Pattern: (adj)(noun)(num) - original pattern
                    if (!ADJECTIVES.isEmpty() && !NOUNS.isEmpty()) {
                        String adj = ADJECTIVES.get(random.nextInt(ADJECTIVES.size()));
                        String noun = NOUNS.get(random.nextInt(NOUNS.size()));
                        int baseLength = adj.length() + noun.length();
                        int maxDigits = Math.min(4, MAX_NAME_LENGTH - baseLength);
                        int minDigits = Math.max(1, MIN_NAME_LENGTH - baseLength);
                        if (minDigits <= maxDigits && maxDigits > 0) {
                            int numDigits = minDigits < maxDigits ? random.nextInt(maxDigits - minDigits + 1) + minDigits : minDigits;
                            StringBuilder digits = new StringBuilder();
                            for (int i = 0; i < numDigits; i++) {
                                digits.append(random.nextInt(10));
                            }
                            result = adj + noun + digits.toString();
                        }
                    }
                    break;
            }
            
            // If result is still invalid, try a different pattern
            if (result.isEmpty() || result.length() < MIN_NAME_LENGTH || result.length() > MAX_NAME_LENGTH) {
                pattern = random.nextInt(4);
            }
        }
        
        // Final validation and adjustment
        if (result.length() < MIN_NAME_LENGTH) {
            // Too short, pad with numbers
            while (result.length() < MIN_NAME_LENGTH && result.length() < MAX_NAME_LENGTH) {
                result = result + random.nextInt(10);
            }
        } else if (result.length() > MAX_NAME_LENGTH) {
            // Too long, truncate
            result = result.substring(0, MAX_NAME_LENGTH);
        }
        
        // Fallback if pattern generation failed
        if (result.isEmpty() || result.length() < MIN_NAME_LENGTH) {
            result = "Player" + (random.nextInt(9000) + 1000);
            // Ensure fallback is within limits
            if (result.length() > MAX_NAME_LENGTH) {
                result = result.substring(0, MAX_NAME_LENGTH);
            }
        }
        
        return result;
    }
}

