/*
 * This file is part of modpublisher, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 HypherionSA and Contributors
 *
 */
package me.hypherionmc.modpublisher.tasks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import masecla.modrinth4j.client.agent.UserAgent;
import masecla.modrinth4j.endpoints.version.CreateVersion;
import masecla.modrinth4j.main.ModrinthAPI;
import masecla.modrinth4j.model.version.ProjectVersion;
import me.hypherionmc.modpublisher.Constants;
import me.hypherionmc.modpublisher.plugin.ModPublisherGradleExtension;
import me.hypherionmc.modpublisher.util.CommonUtil;
import me.hypherionmc.modpublisher.util.UploadPreChecks;
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
        UploadPreChecks.checkRequiredValues(project, extension);
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
        modrinthAPI = ModrinthAPI.rateLimited(userAgent.build(), extension.useModrinthStaging ? Constants.MODRINTH_STAGING_API : Constants.MODRINTH_API, extension.apiKeys.modrinth);

        File uploadFile = CommonUtil.resolveFile(project, extension.artifact);

        if (uploadFile == null || !uploadFile.exists())
            throw new FileNotFoundException("Cannot find file " + extension.artifact.toString());

        getLogger().lifecycle("File: " + uploadFile.getAbsolutePath());

        final List<File> uploadFiles = new ArrayList<>();
        CreateVersion.CreateVersionRequest.CreateVersionRequestBuilder builder = CreateVersion.CreateVersionRequest.builder();
        builder.projectId(extension.modrinthID);
        builder.changelog(CommonUtil.resolveString(extension.changelog));
        builder.versionType(ProjectVersion.VersionType.valueOf(extension.versionType.toUpperCase()));
        builder.versionNumber(extension.version);
        uploadFiles.add(uploadFile);

        if (extension.displayName != null && !extension.displayName.isEmpty()) {
            builder.name(extension.displayName);
        } else {
            builder.name(extension.version);
        }

        List<String> finalGameVersions = new ArrayList<>();
        for (String gameVersion : extension.gameVersions) {
            if (gameVersion.endsWith("-snapshot"))
                continue;
            finalGameVersions.add(gameVersion);
        }

        builder.gameVersions(finalGameVersions);

        List<String> finalLoaders = new ArrayList<>();
        for (String loader : extension.loaders) {
            if (loader.equalsIgnoreCase("risugami's modloader")) {
                if (!finalLoaders.contains("modloader"))
                    finalLoaders.add("modloader");
                continue;
            }

            finalLoaders.add(loader);
        }

        if (!finalLoaders.isEmpty())
            builder.loaders(finalLoaders);

        if (extension.modrinthDepends != null) {
            List<ProjectVersion.ProjectDependency> dependencies = new ArrayList<>();
            extension.modrinthDepends.required.forEach(rd -> {
                ProjectVersion.ProjectDependency dependency = new ProjectVersion.ProjectDependency();
                dependency.setProjectId(rd);
                dependency.setDependencyType(ProjectVersion.ProjectDependencyType.REQUIRED);
                dependencies.add(dependency);
            });

            extension.modrinthDepends.optional.forEach(od -> {
                ProjectVersion.ProjectDependency dependency = new ProjectVersion.ProjectDependency();
                dependency.setProjectId(od);
                dependency.setDependencyType(ProjectVersion.ProjectDependencyType.OPTIONAL);
                dependencies.add(dependency);
            });

            extension.modrinthDepends.incompatible.forEach(id -> {
                ProjectVersion.ProjectDependency dependency = new ProjectVersion.ProjectDependency();
                dependency.setProjectId(id);
                dependency.setDependencyType(ProjectVersion.ProjectDependencyType.INCOMPATIBLE);
                dependencies.add(dependency);
            });

            extension.modrinthDepends.embedded.forEach(ed -> {
                ProjectVersion.ProjectDependency dependency = new ProjectVersion.ProjectDependency();
                dependency.setProjectId(ed);
                dependency.setDependencyType(ProjectVersion.ProjectDependencyType.EMBEDDED);
                dependencies.add(dependency);
            });

            if (!dependencies.isEmpty()) {
                builder.dependencies(dependencies);
            }
        }

        if (!extension.additionalFiles.isEmpty()) {
            for (Object file : extension.additionalFiles) {
                uploadFiles.add(CommonUtil.resolveFile(project, file));
            }
        }
        builder.files(uploadFiles);

        // Debug mode, so we do not upload the file
        if (extension.debug) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            project.getLogger().lifecycle("Full data to be sent for upload: {}", gson.toJson(builder.build()));
            return;
        }

        UploadPreChecks.checkEmptyJar(extension, uploadFile, extension.loaders);
        ProjectVersion projectVersion = modrinthAPI.versions().createProjectVersion(builder.build()).join();

        project.getLogger().lifecycle(
                "Successfully uploaded version {} to {} as version ID {}.",
                projectVersion.getVersionNumber(),
                extension.modrinthID,
                projectVersion.getId()
        );
    }

}
