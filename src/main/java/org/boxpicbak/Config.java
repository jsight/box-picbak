package org.boxpicbak;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import java.util.prefs.Preferences;

public class Config {
    private static final String KEY_API = "api_key";
    private static final String KEY_TICKET = "ticket";
    private static final String KEY_TOKEN = "token";
    
    private static final Config config = new Config();
    
    private Properties properties;
    
    private Config() {
        try {
            properties = new Properties();
            InputStream is = Config.class.getResourceAsStream("/config.properties");
            properties.load(is);
            is.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static Config getInstance() {
        return config;
    }
    
    public String getApiKey() {
        return properties.getProperty(KEY_API);
    }
    public String getTicket() {
        return getPrefs().get(KEY_TICKET, null);
    }
    public void setTicket(String ticket) {
        getPrefs().put(KEY_TICKET, ticket);
    }
    public String getToken() {
        return getPrefs().get(KEY_TOKEN, null);
    }
    public void setToken(String token) {
        getPrefs().put(KEY_TICKET, token);
    }
    
    private Preferences getPrefs() {
        return Preferences.userNodeForPackage(Config.class);
    }
}
