package dadi;
import java.util.*;

public class ConfigurationManager {
    private Map<String, List<FTPConfiguration>> userConfigs;

    public ConfigurationManager() {
        this.userConfigs = new HashMap<>();
    }

    public void addConfiguration(String username, FTPConfiguration config) {
        userConfigs.putIfAbsent(username, new ArrayList<>());
        userConfigs.get(username).add(config);
    }

    public List<FTPConfiguration> getConfigurations(String username) {
        return userConfigs.getOrDefault(username, new ArrayList<>());
    }
}
