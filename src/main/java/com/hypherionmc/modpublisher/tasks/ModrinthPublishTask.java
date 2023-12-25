/*
 * This file is part of modpublisher, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 HypherionSA and Contributors
 *
 */
package com.hypherionmc.modpublisher.tasks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypherionmc.modpublisher.Constants;
import com.hypherionmc.modpublisher.plugin.ModPublisherGradleExtension;
import com.hypherionmc.modpublisher.properties.Platform;
import com.hypherionmc.modpublisher.util.CommonUtil;
import com.hypherionmc.modpublisher.util.UploadPreChecks;
import masecla.modrinth4j.client.agent.UserAgent;
import masecla.modrinth4j.endpoints.version.CreateVersion;
import masecla.modrinth4j.main.ModrinthAPI;
import masecla.modrinth4j.model.version.ProjectVersion;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author HypherionSA
 * Sub-Task to handle Modrinth publishing. This task will only be executed if
 * a Modrinth API Key and Project ID is supplied
 */
public class ModrinthPublishTask extends DefaultTask {

    // Instance of Modrinth4J that will be used
    private ModrinthAPI modrinthAPI;

    private final Project project;
    private final ModPublisherGradleExtension extension;

    @Inject
    public ModrinthPublishTask(Project project, ModPublisherGradleExtension extension) {
        this.project = project;
        this.extension = extension;
    }

    /**
     * Configure the upload and upload it
     */
    @TaskAction
    public void upload() throws Exception {
        project.getLogger().lifecycle("Uploading to Modrinth");
        UploadPreChecks.checkRequiredValues(project, Platform.MODRINTH, extension);
        boolean canUpload = UploadPreChecks.canUploadModrinth(project, extension);
        if (!canUpload)
            return;

        // Required User Agent
        UserAgent.UserAgentBuilder userAgent = UserAgent.builder();
        userAgent.authorUsername("HypherionSA");
        userAgent.contact("hypherionmc@gmail.com");
        userAgent.projectName("ModPublisher");
        userAgent.projectVersion("v1");

        // Create the API Client
        modrinthAPI = ModrinthAPI.rateLimited(userAgent.build(), extension.getUseModrinthStaging().get() ? Constants.MODRINTH_STAGING_API : Constants.MODRINTH_API, extension.getApiKeys().getModrinth());

        Object artifactObject = CommonUtil.getPlatformArtifact(Platform.MODRINTH, extension);
        File uploadFile = CommonUtil.resolveFile(project, artifactObject);

        if (uploadFile == null || !uploadFile.exists())
            throw new FileNotFoundException("Cannot find file " + artifactObject);

        final List<File> uploadFiles = new ArrayList<>();
        CreateVersion.CreateVersionRequest.CreateVersionRequestBuilder builder = CreateVersion.CreateVersionRequest.builder();
        builder.projectId(extension.getModrinthID().get());
        builder.changelog(CommonUtil.resolveString(extension.getChangelog().get()));
        builder.versionType(ProjectVersion.VersionType.valueOf(extension.getVersionType().get().toUpperCase()));
        builder.versionNumber(extension.getVersion().get());
        uploadFiles.add(uploadFile);

        if (extension.getDisplayName().isPresent() && !extension.getDisplayName().get().isEmpty()) {
            builder.name(extension.getDisplayName().get());
        } else {
            builder.name(extension.getVersion().get());
        }

        List<String> finalGameVersions = new ArrayList<>();
        for (String gameVersion : extension.getGameVersions().get()) {
            if (gameVersion.endsWith("-snapshot"))
                continue;
            finalGameVersions.add(gameVersion);
        }

        builder.gameVersions(finalGameVersions);

        List<String> finalLoaders = new ArrayList<>();
        for (String loader : extension.getLoaders().get()) {
            if (loader.equalsIgnoreCase("risugami's modloader")) {
                if (!finalLoaders.contains("modloader"))
                    finalLoaders.add("modloader");
                continue;
            }

            finalLoaders.add(loader);
        }

        if (!finalLoaders.isEmpty())
            builder.loaders(finalLoaders);

        if (extension.getModrinthDepends() != null) {
            List<ProjectVersion.ProjectDependency> dependencies = new ArrayList<>();
            extension.getModrinthDepends().getRequired().get().forEach(rd -> {
                ProjectVersion.ProjectDependency dependency = new ProjectVersion.ProjectDependency();
                dependency.setProjectId(rd);
                dependency.setDependencyType(ProjectVersion.ProjectDependencyType.REQUIRED);
                dependencies.add(dependency);
            });

            extension.getModrinthDepends().getOptional().get().forEach(od -> {
                ProjectVersion.ProjectDependency dependency = new ProjectVersion.ProjectDependency();
                dependency.setProjectId(od);
                dependency.setDependencyType(ProjectVersion.ProjectDependencyType.OPTIONAL);
                dependencies.add(dependency);
            });

            extension.getModrinthDepends().getIncompatible().get().forEach(id -> {
                ProjectVersion.ProjectDependency dependency = new ProjectVersion.ProjectDependency();
                dependency.setProjectId(id);
                dependency.setDependencyType(ProjectVersion.ProjectDependencyType.INCOMPATIBLE);
                dependencies.add(dependency);
            });

            extension.getModrinthDepends().getEmbedded().get().forEach(ed -> {
                ProjectVersion.ProjectDependency dependency = new ProjectVersion.ProjectDependency();
                dependency.setProjectId(ed);
                dependency.setDependencyType(ProjectVersion.ProjectDependencyType.EMBEDDED);
                dependencies.add(dependency);
            });

            if (!dependencies.isEmpty()) {
                builder.dependencies(dependencies);
            }
        }

        if (extension.getAdditionalFiles().isPresent()) {
            for (Object file : extension.getAdditionalFiles().get()) {
                uploadFiles.add(CommonUtil.resolveFile(project, file));
            }
        }
        builder.files(uploadFiles);

        // Debug mode, so we do not upload the file
        if (extension.getDebug().get()) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            project.getLogger().lifecycle("Full data to be sent for upload: {}", gson.toJson(builder.build()));
            return;
        }

        UploadPreChecks.checkEmptyJar(extension, uploadFile, extension.getLoaders().get());
        ProjectVersion projectVersion = modrinthAPI.versions().createProjectVersion(builder.build()).join();

        project.getLogger().lifecycle(
                "Successfully uploaded version {} to {} as version ID {}.",
                projectVersion.getVersionNumber(),
                extension.getModrinthID().get(),
                projectVersion.getId()
        );
    }

}
