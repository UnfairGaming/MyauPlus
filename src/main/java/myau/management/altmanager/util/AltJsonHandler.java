package myau.management.altmanager.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import myau.management.altmanager.Alt;
import myau.management.altmanager.AltManagerGui;

public class AltJsonHandler {
	public static File ROOT_DIR = new File("config/MyauPLus");
    public static File alts = new File(ROOT_DIR, "alts.json");
    public static Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
    public static JsonParser jsonParser = new JsonParser();
    
    public static void start() {
        if (!ROOT_DIR.exists()) {ROOT_DIR.mkdirs();}
    }
    
    public static void saveAlts() {
        try {
            JsonObject json = new JsonObject();
            JsonArray altArray = new JsonArray();
            for (Alt alt : AltManagerGui.alts) {
                JsonObject altJson = new JsonObject();
                altJson.addProperty("name", alt.getName());
                altJson.addProperty("cracked", alt.isCracked());
                altJson.addProperty("banned", alt.isBanned());

                if (!alt.isCracked()) {
                    altJson.addProperty("email", alt.getEmail());
                    altJson.addProperty("password", alt.getPassword());
                    if (alt.hasRefreshToken()) {
                        altJson.addProperty("refreshToken", alt.getRefreshToken());
                    }
                }
                if (alt.getUuid() != null) {
                    altJson.addProperty("uuid", alt.getUuid());
                }
                altArray.add(altJson);
            }
            json.add("alts", altArray);
            PrintWriter save = new PrintWriter(new FileWriter(alts));
            save.println(prettyGson.toJson(json));
            save.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void loadAlts() {
        try {
            if (!alts.exists()) {
                AltManagerGui.alts.clear();
                return;
            }
            BufferedReader load = new BufferedReader(new FileReader(alts));
            JsonObject json = (JsonObject) jsonParser.parse(load);
            load.close();
            AltManagerGui.alts.clear();
            JsonArray altArray = json.getAsJsonArray("alts");
            for (JsonElement element : altArray) {
                JsonObject altJson = element.getAsJsonObject();
                String name = altJson.get("name").getAsString();
                boolean cracked = altJson.get("cracked").getAsBoolean();

                String email = cracked ? null : altJson.get("email").getAsString();
                String password = cracked ? null : altJson.get("password").getAsString();

                Alt alt = new Alt(email, password, name, cracked);
                if (altJson.has("refreshToken")) {
                    alt.setRefreshToken(altJson.get("refreshToken").getAsString());
                }
                if (altJson.has("uuid")) {
                    alt.setUuid(altJson.get("uuid").getAsString());
                }
                if (altJson.has("banned")) {
                    alt.setBanned(altJson.get("banned").getAsBoolean());
                } else {
                    // Check banned status if not saved
                    alt.setBanned(myau.management.altmanager.AccountData.isBanned(name));
                }
                AltManagerGui.alts.add(alt);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

