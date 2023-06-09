/*
 * This file is part of modpublisher, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 HypherionSA and Contributors
 *
 */
package me.hypherionmc.modpublisher.tasks;

import me.hypherionmc.curseupload.CurseUploadApi;
import me.hypherionmc.curseupload.constants.CurseChangelogType;
import me.hypherionmc.curseupload.constants.CurseReleaseType;
import me.hypherionmc.curseupload.requests.CurseArtifact;
import me.hypherionmc.modpublisher.util.CommonUtil;
import me.hypherionmc.modpublisher.util.UploadPreChecks;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.regex.Pattern;

import static me.hypherionmc.modpublisher.plugin.ModPublisherPlugin.extension;
import static me.hypherionmc.modpublisher.plugin.ModPublisherPlugin.project;

/**
 * @author HypherionSA
 * Sub-Task to handle Curseforge publishing. This task will only be executed if
 * a Curseforge API Key and Project ID is supplied
 */
public class CurseUploadTask extends DefaultTask {

    // Instance of CurseUpload4J to use
    private CurseUploadApi uploadApi;
    private final Pattern pattern = Pattern.compile("[A-Za-z0-9]+", Pattern.CASE_INSENSITIVE);

    /**
     * Configure the upload and upload it
     */
    @TaskAction
    public void upload() throws Exception {
        project.getLogger().lifecycle("Uploading to Curseforge");
        UploadPreChecks.checkRequiredValues();
        boolean canUpload = UploadPreChecks.canUploadCurse();
        if (!canUpload)
            return;

        // Create the API Client and pass the Gradle logger as logger
        uploadApi = new CurseUploadApi(extension.apiKeys.curseforge, project.getLogger());

        // Enable debug mode if required
        uploadApi.setDebug(extension.debug);

        File uploadFile = CommonUtil.resolveFile(project, extension.artifact);

        if (uploadFile == null || !uploadFile.exists())
            throw new FileNotFoundException("Cannot find file " + extension.artifact.toString());

        CurseArtifact artifact = new CurseArtifact(uploadFile, Long.parseLong(extension.curseID));
        artifact.changelog(CommonUtil.resolveString(extension.changelog));
        artifact.changelogType(CurseChangelogType.MARKDOWN);
        artifact.releaseType(CurseReleaseType.valueOf(extension.versionType.toUpperCase()));

        // Start super-duper accurate check for CraftPresence... Weirdness xD
        // Just kidding CDA. But seriously, you have way too much free time

        // Compare if MC version is below b1.6.6, as the lowest curse supports is b1.6.6
        for (String gameVersion : extension.gameVersions) {
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

        for (String modLoader : extension.loaders) {

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
        if (extension.curseEnvironment != null && !extension.curseEnvironment.isEmpty()) {
            String env = extension.curseEnvironment.toLowerCase();

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

        if (extension.displayName != null && !extension.displayName.isEmpty()) {
            artifact.displayName(extension.displayName);
        } else {
            artifact.displayName(extension.version);
        }

        if (extension.curseDepends != null) {
            extension.curseDepends.required.forEach(artifact::requirement);
            extension.curseDepends.optional.forEach(artifact::optional);
            extension.curseDepends.incompatible.forEach(artifact::incompatibility);
            extension.curseDepends.embedded.forEach(artifact::embedded);
        }

        if (!extension.additionalFiles.isEmpty()) {
            for (Object file : extension.additionalFiles) {
                artifact.addAdditionalFile(CommonUtil.resolveFile(project, file));
            }
        }

        UploadPreChecks.checkEmptyJar(uploadFile, extension.loaders);

        // If debug mode is enabled, this will only log the JSON that will be sent and
        // will not actually upload the file
        uploadApi.upload(artifact);
    }

}
