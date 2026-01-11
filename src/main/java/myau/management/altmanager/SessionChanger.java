package myau.management.altmanager;

import myau.management.altmanager.auth.MicrosoftAuthResult;
import myau.management.altmanager.auth.MicrosoftAuthenticationException;
import myau.management.altmanager.auth.MicrosoftAuthenticator;
import myau.management.altmanager.microsoft.MicrosoftOAuthTranslation;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

public class SessionChanger {
	public static String username = null;
	private MicrosoftAuthenticator auth;
	private static SessionChanger instance;
	private final Minecraft mc = Minecraft.getMinecraft();
	public long timeSinceFail;
	
	public void loginCracked(String n) {
		SessionUtil.setSession(mc, new Session(n, n, "0", "legacy"));
		username = n;
    }
	
	public void loginMicrosoft(String email, String password) {
		new Thread(() -> {
	        MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
	        AltManagerGui.status = "§6Logging in";
	        try {
	            MicrosoftAuthResult acc = authenticator.loginWithCredentials(email, password);
	            if (acc != null) {
	                SessionUtil.setSession(
	                        Minecraft.getMinecraft(),
		                    new Session(
		                        acc.getProfile().getName(),
		                        acc.getProfile().getId(),
		                        acc.getAccessToken(),
		                        "mojang"
		                    )
		                );
	                username = acc.getProfile().getName();
	                System.out.println("Login successful");
	            } else {
	                System.out.println("Failed login");
	                timeSinceFail = System.currentTimeMillis();
	            }
	            AltManagerGui.status = "§aIdle";
	        } catch (MicrosoftAuthenticationException e) {
	            e.printStackTrace();
	            System.out.println("Failed login");
                timeSinceFail = System.currentTimeMillis();
	        }
	    }).start();
    }
	
	public void loginWithRefreshToken(String refreshToken) {
	    new Thread(() -> {
	        AltManagerGui.status = "§6Logging in with OAuth...";
	        MicrosoftOAuthTranslation.LoginData loginData = MicrosoftOAuthTranslation.login(refreshToken);
	        
	        if (loginData.isGood()) {
	            setSessionWithData(loginData);
	            AltManagerGui.status = "§aLogged in as " + loginData.username;
	        } else {
	            System.out.println("OAuth login failed");
	            timeSinceFail = System.currentTimeMillis();
	            AltManagerGui.status = "§cOAuth login failed";
	        }
	    }).start();
	}
	
	public void setSessionWithData(MicrosoftOAuthTranslation.LoginData loginData) {
	    SessionUtil.setSession(mc, new Session(loginData.username, loginData.uuid, loginData.mcToken, "mojang"));
	    username = loginData.username;
	    System.out.println("OAuth login successful: " + loginData.username);
	}
	
	public String getUser(String email, String password) {
		MicrosoftAuthenticator authenticator = new MicrosoftAuthenticator();
		try {
			MicrosoftAuthResult acc = authenticator.loginWithCredentials(email, password);
			return acc.getProfile().getName();
		} catch (MicrosoftAuthenticationException e) {

		}
		return "";
	}
	
	public static SessionChanger instance() {
		if (instance == null) {
			instance = new SessionChanger();
		}

		return instance;
	}
}
