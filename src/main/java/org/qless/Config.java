package org.qless;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileInputStream;
import java.io.InputStream;

public class Config {
    private static JsonNode config;
    
    static {
        loadConfig();
    }
    
    private static void loadConfig() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            InputStream configFile;
            
            if (Main.isRunningFromJar()) {
                configFile = Config.class.getResourceAsStream("/config.json");
            } else {
                configFile = new FileInputStream("src/main/resources/config.json");
            }
            
            if (configFile != null) {
                config = objectMapper.readTree(configFile);
            } else {
                // Fallback to default values if config file is not found
                config = null;
            }
        } catch (Exception e) {
            System.err.println("Error loading config: " + e.getMessage());
            config = null;
        }
    }
    
    public static String getDeliverySupport() {
        if (config != null && config.has("support") && config.get("support").has("delivery_issues")) {
            return config.get("support").get("delivery_issues").asText();
        }
        return "@your_delivery_support";
    }
    
    public static String getTechSupport() {
        if (config != null && config.has("support") && config.get("support").has("bot_issues")) {
            return config.get("support").get("bot_issues").asText();
        }
        return "@your_tech_support";
    }
    
    public static String getProjectUpdates() {
        if (config != null && config.has("support") && config.get("support").has("project_updates")) {
            return config.get("support").get("project_updates").asText();
        }
        return "@your_project_channel";
    }
    
    public static String getDatabaseName() {
        if (config != null && config.has("system") && config.get("system").has("database_name")) {
            return config.get("system").get("database_name").asText();
        }
        return "delivery-agents";
    }
    
    public static int getPercentOfIncome() {
        if (config != null && config.has("system") && config.get("system").has("percent_of_income")) {
            return config.get("system").get("percent_of_income").asInt();
        }
        return 25;
    }
    
    public static int getFirstOrdersForFree() {
        if (config != null && config.has("system") && config.get("system").has("first_orders_for_free")) {
            return config.get("system").get("first_orders_for_free").asInt();
        }
        return 0;
    }
    
    public static boolean isOneMoreTokenForFirstOrder() {
        if (config != null && config.has("system") && config.get("system").has("one_more_token_for_first_order")) {
            return config.get("system").get("one_more_token_for_first_order").asBoolean();
        }
        return true;
    }
}
