import com.hypherionmc.modpublisher.util.CommonUtil;
import com.hypherionmc.modpublisher.util.modtale.ModtaleApiClient;
import com.hypherionmc.modpublisher.util.modtale.meta.ModtaleMetadata;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.gradle.internal.cc.base.LoggingKt.getLogger;

public class ModtaleTest {

    public static void main(String[] args) throws IOException {
        ModtaleApiClient apiClient = ModtaleApiClient.of("", getLogger());
        //apiClient.setDebugMode(true);

        ModtaleMetadata metadata = ModtaleMetadata.builder()
                .setChangelog("Testing")
                .setChannel("RELEASE")
                .setVersionNumber("1.0.0")
                .setGameVersions(Collections.singletonList("2026.01.17-4b0f30090"));

        // If debug mode is enabled, this will only log the JSON that will be sent and
        // will not actually upload the file
        apiClient.upload("67a31f5e-255e-41b4-80ca-94eb58328695", metadata, new File("dummy.jar"));
    }

}
