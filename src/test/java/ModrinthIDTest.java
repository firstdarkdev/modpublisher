import com.hypherionmc.modpublisher.util.UploadPreChecks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ModrinthIDTest {

    @Test
    public void testModrinthId() {
        String id = "P7dR8mSH";
        boolean valid = UploadPreChecks.isModrinthID(id);

        assertTrue(valid, "Input was not detected as a valid BASE62 String");
    }

    @Test
    public void testModrinthSlug() {
        String slug = "fabric-api";
        boolean valid = UploadPreChecks.isModrinthID(slug);

        assertFalse(valid, "Slug was detected as a valid BASE62 String");
    }

}
