package com.hypherionmc.modpublisher.tasks;

import com.hypherionmc.modpublisher.plugin.ModPublisherGradleExtension;
import com.hypherionmc.modpublisher.properties.Platform;
import com.hypherionmc.modpublisher.util.CommonUtil;
import com.hypherionmc.modpublisher.util.UploadPreChecks;
import com.hypherionmc.modpublisher.util.modtale.ModtaleApiClient;
import com.hypherionmc.modpublisher.util.modtale.meta.ModtaleMetadata;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author HypherionSA
 * Sub-Task to handle Modtale publishing. This task will only be executed if
 * a Modtale API Key and Project ID is supplied
 */
public class ModtaleUploadTask extends DefaultTask {

    private final Project project;
    private final ModPublisherGradleExtension extension;

    @Inject
    public ModtaleUploadTask(Project project, ModPublisherGradleExtension extension) {
        this.project = project;
        this.extension = extension;
    }

    @TaskAction
    public void upload() throws Exception {
        if (!extension.getGameType().get().equalsIgnoreCase("hytale"))
            return;

        if (extension.getSourceSet() == null) {
            project.getLogger().lifecycle("Uploading to Modtale");
        } else {
            project.getLogger().lifecycle("Uploading {} to Modtale", extension.getProjectName());
        }

        UploadPreChecks.checkRequiredValues(project, Platform.MODTALE, extension);
        boolean canUpload = UploadPreChecks.canUploadCurse(project, extension);
        if (!canUpload)
            return;

        // Modtale enforces x.x.x versioning
        if (!extension.getProjectVersion().get().matches("^\\d+\\.\\d+\\.\\d+$")) {
            throw new GradleException(extension.getProjectVersion().get() + " is not a valid file version for Modtale. Version needs to be in format X.Y.Z");
        }

        // Create the API Client and pass the Gradle logger as logger
        ModtaleApiClient apiClient = ModtaleApiClient.of(extension.getApiKeys().getModtale(), getLogger());

        // Enable debug mode if required
        apiClient.setDebugMode(extension.getDebug().get());

        Object artifactObject = CommonUtil.getPlatformArtifact(Platform.MODTALE, extension);
        File uploadFile = CommonUtil.resolveFile(project, artifactObject);

        if (uploadFile == null || !uploadFile.exists())
            throw new FileNotFoundException("Cannot find file " + artifactObject);

        ModtaleMetadata metadata = ModtaleMetadata.builder()
                .setChangelog(CommonUtil.resolveString(extension.getChangelog().get()))
                .setChannel(extension.getVersionType().get().toUpperCase())
                .setVersionNumber(extension.getProjectVersion().get())
                .setGameVersions(extension.getGameVersions().get());

        // If debug mode is enabled, this will only log the JSON that will be sent and
        // will not actually upload the file
        apiClient.upload(extension.getModtaleID().get(), metadata, uploadFile);
    }
}
