package pl.errordustin;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class ErrorDustinConfig {
    public static boolean webEnabled = true;
    public static boolean waterEnabled = true;
    public static boolean leverEnabled = true;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("errordustin.properties");
    }

    public static void load() {
        try (var in = Files.newInputStream(file())) {
            Properties p = new Properties();
            p.load(in);
            webEnabled = Boolean.parseBoolean(p.getProperty("web", "true"));
            waterEnabled = Boolean.parseBoolean(p.getProperty("water", "true"));
            leverEnabled = Boolean.parseBoolean(p.getProperty("lever", "true"));
        } catch (IOException ignored) {
        }
    }

    public static void save() {
        Properties p = new Properties();
        p.setProperty("web", String.valueOf(webEnabled));
        p.setProperty("water", String.valueOf(waterEnabled));
        p.setProperty("lever", String.valueOf(leverEnabled));
        try (var out = Files.newOutputStream(file())) {
            p.store(out, "errordustin");
        } catch (IOException ignored) {
        }
    }
}
