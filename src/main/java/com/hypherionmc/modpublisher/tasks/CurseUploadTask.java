/*
 * This file is part of modpublisher, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 HypherionSA and Contributors
 *
 */
package com.hypherionmc.modpublisher.tasks;

import com.hypherionmc.modpublisher.plugin.ModPublisherGradleExtension;
import com.hypherionmc.modpublisher.properties.Platform;
import com.hypherionmc.modpublisher.util.CommonUtil;
import com.hypherionmc.modpublisher.util.UploadPreChecks;
import me.hypherionmc.curseupload.CurseUploadApi;
import me.hypherionmc.curseupload.constants.CurseChangelogType;
import me.hypherionmc.curseupload.constants.CurseReleaseType;
import me.hypherionmc.curseupload.requests.CurseArtifact;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.gradle.api.DefaultTask;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.regex.Pattern;

/**
 * @author HypherionSA
 * Sub-Task to handle Curseforge publishing. This task will only be executed if
 * a Curseforge API Key and Project ID is supplied
 */
public class CurseUploadTask extends DefaultTask {

    // Instance of CurseUpload4J to use
    private CurseUploadApi uploadApi;
    private final Pattern pattern = Pattern.compile("[A-Za-z0-9]+", Pattern.CASE_INSENSITIVE);

    private final Project project;
    private final ModPublisherGradleExtension extension;

    @Inject
    public CurseUploadTask(Project project, ModPublisherGradleExtension extension) {
        this.project = project;
        this.extension = extension;
    }

    /**
     * Configure the upload and upload it
     */
    @TaskAction
    public void upload() throws Exception {
        project.getLogger().lifecycle("Uploading to Curseforge");
        UploadPreChecks.checkRequiredValues(project, Platform.CURSEFORGE, extension);
        boolean canUpload = UploadPreChecks.canUploadCurse(project, extension);
        if (!canUpload)
            return;

        // Create the API Client and pass the Gradle logger as logger
        uploadApi = new CurseUploadApi(extension.getApiKeys().getCurseforge(), project.getLogger());

        // Enable debug mode if required
        uploadApi.setDebug(extension.getDebug().get());
        Object artifactObject = CommonUtil.getPlatformArtifact(Platform.CURSEFORGE, extension);
        File uploadFile = CommonUtil.resolveFile(project, artifactObject);

        if (uploadFile == null || !uploadFile.exists())
            throw new FileNotFoundException("Cannot find file " + artifactObject);

        CurseArtifact artifact = new CurseArtifact(uploadFile, Long.parseLong(extension.getCurseID().get()));
        artifact.changelog(CommonUtil.resolveString(extension.getChangelog().get()));
        artifact.changelogType(CurseChangelogType.MARKDOWN);
        artifact.releaseType(CurseReleaseType.valueOf(extension.getVersionType().get().toUpperCase()));

        // Start super-duper accurate check for CraftPresence... Weirdness xD
        // Just kidding CDA. But seriously, you have way too much free time

        // Compare if MC version is below b1.6.6, as the lowest curse supports is b1.6.6
        for (String gameVersion : extension.getGameVersions().get()) {
            if (pattern.matcher(gameVersion).matches())
                continue;

            if (gameVersion.contains("-pre") || gameVersion.contains("-rc"))
                continue;

            DefaultArtifactVersion min = new DefaultArtifactVersion("b1.6.6");
            DefaultArtifactVersion current = new DefaultArtifactVersion(gameVersion);

            // Version is lower, so default to b1.6.6
            if (current.compareTo(min) < 0) {
                artifact.addGameVersion("beta 1.6.6");
            } else if (gameVersion.contains("b1")) {
                // Convert into curseforge slug format
                String ver = gameVersion.replace("b", "beta ");
                artifact.addGameVersion(ver);
            } else {
                // No change needed, pass game version as-is
                artifact.addGameVersion(gameVersion);
            }
        }

        for (String modLoader : extension.getLoaders().get()) {

            // Replace `modloader` with `risugamis-modloader`
            if (modLoader.equalsIgnoreCase("modloader")) {
                artifact.modLoader("risugami's modloader");
            } else {
                // No changes needed, pass the modloader along
                artifact.modLoader(modLoader);
            }
        }
        // Back to our regularly scheduled code

        // Add Curse Environment tags if they are specified
        if (extension.getCurseEnvironment().isPresent() && !extension.getCurseEnvironment().get().isEmpty()) {
            String env = extension.getCurseEnvironment().get().toLowerCase();

            switch (env) {
                case "client":
                    artifact.addGameVersion("client");
                    break;
                case "server":
                    artifact.addGameVersion("server");
                    break;
                default:
                case "both":
                    artifact.addGameVersion("client");
                    artifact.addGameVersion("server");
                    break;
            }
        }

        if (extension.getJavaVersions().isPresent() && !extension.getJavaVersions().get().isEmpty()) {
            for (JavaVersion javaVersion : extension.getJavaVersions().get()) {
                artifact.javaVersion("Java " + javaVersion.getMajorVersion());
            }
        }

        if (extension.getDisplayName().isPresent() && !extension.getDisplayName().get().isEmpty()) {
            artifact.displayName(extension.getDisplayName().get());
        } else {
            artifact.displayName(extension.getProjectVersion().get());
        }

        if (extension.getIsManualRelease().get()) {
            artifact.manualRelease();
        }

        if (extension.getCurseDepends() != null) {
            extension.getCurseDepends().getRequired().get().forEach(artifact::requirement);
            extension.getCurseDepends().getOptional().get().forEach(artifact::optional);
            extension.getCurseDepends().getIncompatible().get().forEach(artifact::incompatibility);
            extension.getCurseDepends().getEmbedded().get().forEach(artifact::embedded);
        }

        if (extension.getAdditionalFiles().isPresent()) {
            for (ModPublisherGradleExtension.AdditionalFile file : extension.getAdditionalFiles().get()) {
                String changelog = file.getChangelog() == null ? null : CommonUtil.resolveString(file.getChangelog());
               artifact.addAdditionalFile(CommonUtil.resolveFile(project, file.getArtifact()), file.getDisplayName(), changelog);
            }
        }

        UploadPreChecks.checkEmptyJar(extension, uploadFile, extension.getLoaders().get());

        // If debug mode is enabled, this will only log the JSON that will be sent and
        // will not actually upload the file
        uploadApi.upload(artifact);
    }

}
