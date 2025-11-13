import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigUtilTest {

    private Path tempDir;
    private Path configPath;

    @BeforeEach
    void setup() throws Exception {
        tempDir = Files.createTempDirectory("cfg-");
        configPath = tempDir.resolve("config.properties");
        // Point ConfigUtil to isolated config file
        ConfigUtil.setExternalConfigPathForTest(configPath);
    }

    @AfterEach
    void cleanup() throws Exception {
        try { Files.deleteIfExists(configPath); } catch (Exception ignored) {}
        try { Files.deleteIfExists(tempDir); } catch (Exception ignored) {}
    }

    @Test
    void defaultsAreAppliedAndPersisted() throws Exception {
        // After pointing to empty path, load should have set sane defaults
        Path ics = ConfigUtil.getIcsPath();
        assertNotNull(ics);
        assertFalse(ics.toString().isBlank());
        assertFalse(ConfigUtil.isDarkMode());

        // Change values
        Path customIcs = tempDir.resolve("test.ics");
        ConfigUtil.setIcsPath(customIcs);
        ConfigUtil.setDarkMode(true);
        ConfigUtil.save();

        // Re-point and reload to ensure values are read back from file
        ConfigUtil.setExternalConfigPathForTest(configPath);
        assertEquals(customIcs, ConfigUtil.getIcsPath());
        assertTrue(ConfigUtil.isDarkMode());
    }
}

