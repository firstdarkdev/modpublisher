/*
 * This file is part of modpublisher, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 HypherionSA and Contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package me.hypherionmc.modpublisher.tasks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import masecla.modrinth4j.client.agent.UserAgent;
import masecla.modrinth4j.endpoints.version.CreateVersion;
import masecla.modrinth4j.main.ModrinthAPI;
import masecla.modrinth4j.model.version.ProjectVersion;
import me.hypherionmc.modpublisher.util.CommonUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static me.hypherionmc.modpublisher.plugin.ModPublisherPlugin.extension;
import static me.hypherionmc.modpublisher.plugin.ModPublisherPlugin.project;

/**
 * @author HypherionSA
 * Sub-Task to handle Modrinth publishing. This task will only be executed if
 * a Modrinth API Key and Project ID is supplied
 */
public class ModrinthPublishTask {

    // Instance of Modrinth4J that will be used
    private final ModrinthAPI modrinthAPI;

    public ModrinthPublishTask() {
        // Required User Agent
        UserAgent.UserAgentBuilder userAgent = UserAgent.builder();
        userAgent.authorUsername("HypherionSA");
        userAgent.contact("hypherionmc@gmail.com");
        userAgent.projectName("ModPublisher");
        userAgent.projectVersion("v1");

        // Create the API Client
        modrinthAPI = ModrinthAPI.rateLimited(userAgent.build(), extension.apiKeys.modrinth);
    }

    public void upload() throws IOException {
        project.getLogger().lifecycle("Uploading to Modrinth");

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

        builder.gameVersions(extension.gameVersions);
        builder.loaders(extension.loaders);

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

        ProjectVersion projectVersion = modrinthAPI.versions().createProjectVersion(builder.build()).join();

        project.getLogger().lifecycle(
                "Successfully uploaded version {} to {} as version ID {}.",
                projectVersion.getVersionNumber(),
                extension.modrinthID,
                projectVersion.getId()
        );
    }

}
