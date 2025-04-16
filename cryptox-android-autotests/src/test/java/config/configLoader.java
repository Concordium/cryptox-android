package config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class configLoader {

    private final Properties properties;

    public configLoader() {
        properties = new Properties();
        try (InputStream input = Files.newInputStream(Path.of("setupData.properties"))) {
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }






}
