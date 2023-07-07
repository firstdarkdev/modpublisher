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
import me.hypherionmc.modpublisher.util.CommonUtil;
import me.hypherionmc.modpublisher.util.UploadPreChecks;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static me.hypherionmc.modpublisher.plugin.ModPublisherPlugin.extension;
import static me.hypherionmc.modpublisher.plugin.ModPublisherPlugin.project;

/**
 * @author HypherionSA
 * Sub-Task to handle Modrinth publishing. This task will only be executed if
 * a Modrinth API Key and Project ID is supplied
 */
public class ModrinthPublishTask extends DefaultTask {

    // Instance of Modrinth4J that will be used
    private ModrinthAPI modrinthAPI;

    /**
     * Configure the upload and upload it
     */
    @TaskAction
    public void upload() throws Exception {
        project.getLogger().lifecycle("Uploading to Modrinth");
        UploadPreChecks.checkRequiredValues();
        boolean canUpload = UploadPreChecks.canUploadModrinth();
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

        CreateVersion.CreateVersionRequest.CreateVersionRequestBuilder builder = CreateVersion.CreateVersionRequest.builder();
        builder.projectId(extension.modrinthID);
        builder.changelog(CommonUtil.resolveString(extension.changelog));
        builder.versionType(ProjectVersion.VersionType.valueOf(extension.versionType.toUpperCase()));
        builder.versionNumber(extension.version);
        builder.files(uploadFile);

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

        // Debug mode, so we do not upload the file
        if (extension.debug) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            project.getLogger().lifecycle("Full data to be sent for upload: {}", gson.toJson(builder.build()));
            return;
        }

        UploadPreChecks.checkEmptyJar(uploadFile, extension.loaders);

        ProjectVersion projectVersion = modrinthAPI.versions().createProjectVersion(builder.build()).join();

        project.getLogger().lifecycle(
                "Successfully uploaded version {} to {} as version ID {}.",
                projectVersion.getVersionNumber(),
                extension.modrinthID,
                projectVersion.getId()
        );
    }

}
