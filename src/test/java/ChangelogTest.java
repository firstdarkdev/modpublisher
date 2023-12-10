import com.hypherionmc.modpublisher.util.CommonUtil;
import com.hypherionmc.modpublisher.util.changelogs.ChangelogUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ChangelogTest {

    @Test
    public void testValidUrlChecker() {
        String url = "https://gist.githubusercontent.com/hypherionmc/92f825d3c9337964cc77c9c8c9bf65e6/raw/ceeaaee5b98c688a23398864fe480b84796a1651/test_gist.md";
        boolean valid = ChangelogUtil.isValidUploadSite(url);

        assertTrue(valid, "Valid URL should've returned true");
    }

    @Test
    public void testInvalidUrlChecker() {
        String url = "https://gist.github.com/hypherionmc/92f825d3c9337964cc77c9c8c9bf65e6";
        boolean valid = ChangelogUtil.isValidUploadSite(url);

        assertFalse(valid, "Invalid URL should've returned false");
    }

    @Test
    public void testStringResolver() throws IOException {
        String value = CommonUtil.resolveString("Hello World");

        assertEquals("Hello World", value, "String resolver should've returned Hello World");
    }

    @Test
    public void testChangelogResolver() throws IOException {
        String value = CommonUtil.resolveString("https://gist.githubusercontent.com/hypherionmc/92f825d3c9337964cc77c9c8c9bf65e6/raw/ceeaaee5b98c688a23398864fe480b84796a1651/test_gist.md");
        assertEquals("This is a test gist", value, "Changelog Resolver should've returned This is a test gist");
    }
}
