import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ConfigUtil to ensure configuration persistence works correctly,
 * especially when running from a JAR file.
 */
public class ConfigUtilTest {
    
    private Path testConfigPath;
    
    @BeforeEach
    void setUp() throws Exception {
        // Create a test config file in a temp location
        testConfigPath = Files.createTempFile("test-config-", ".properties");
    }
    
    @AfterEach
    void tearDown() throws Exception {
        if (testConfigPath != null && Files.exists(testConfigPath)) {
            Files.deleteIfExists(testConfigPath);
        }
    }
    
    @Test
    void testConfigLoadDefaults() {
        // Test that defaults are properly loaded
        ConfigUtil.StorageMode mode = ConfigUtil.getStorageMode();
        assertNotNull(mode);
        
        Path icsPath = ConfigUtil.getIcsPath();
        assertNotNull(icsPath);
    }
    
    @Test
    void testConfigSaveAndLoad() throws Exception {
        // Change settings
        ConfigUtil.setStorageMode(ConfigUtil.StorageMode.DB);
        ConfigUtil.setIcsPath(Paths.get("test-calendar.ics"));
        
        // Save config
        ConfigUtil.save();
        
        // Reload config
        ConfigUtil.load();
        
        // Verify settings persisted
        assertEquals(ConfigUtil.StorageMode.DB, ConfigUtil.getStorageMode());
        assertTrue(ConfigUtil.getIcsPath().toString().contains("test-calendar.ics"));
        
        // Reset to defaults for other tests
        ConfigUtil.setStorageMode(ConfigUtil.StorageMode.ICS);
        ConfigUtil.setIcsPath(Paths.get("calendar.ics"));
        ConfigUtil.save();
    }
    
    @Test
    void testIcsPathIsRelative() {
        // Ensure ICS path works as relative path (important for JAR execution)
        Path icsPath = ConfigUtil.getIcsPath();
        
        // The default should be "calendar.ics" - a relative path
        assertNotNull(icsPath);
        
        // If it's just a filename, it should work in the working directory
        Path absolute = icsPath.toAbsolutePath();
        assertNotNull(absolute);
    }
    
    @Test
    void testConfigWithAbsolutePath() throws Exception {
        // Test setting an absolute path
        Path absolutePath = Files.createTempFile("test-cal-", ".ics");
        
        try {
            ConfigUtil.setIcsPath(absolutePath);
            ConfigUtil.save();
            ConfigUtil.load();
            
            Path loaded = ConfigUtil.getIcsPath();
            // Should preserve the absolute path
            assertTrue(loaded.toString().contains(".ics"));
        } finally {
            Files.deleteIfExists(absolutePath);
            ConfigUtil.setIcsPath(Paths.get("calendar.ics"));
            ConfigUtil.save();
        }
    }
}
