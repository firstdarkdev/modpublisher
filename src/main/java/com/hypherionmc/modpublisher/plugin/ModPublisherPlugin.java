/*
 * This file is part of modpublisher, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 HypherionSA and Contributors
 *
 */
package com.hypherionmc.modpublisher.plugin;

import com.hypherionmc.modpublisher.properties.Platform;
import com.hypherionmc.modpublisher.tasks.*;
import com.hypherionmc.modpublisher.util.CommonUtil;
import com.hypherionmc.modpublisher.util.UploadPreChecks;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;

import javax.annotation.Nonnull;

import static com.hypherionmc.modpublisher.Constants.*;
import static com.hypherionmc.modpublisher.util.CommonUtil.isNullOrEmpty;

/**
 * @author HypherionSA
 * Main Gradle Plugin Class.
 * This plugin is mainly intended for use with MultiLoader Projects
 */
public class ModPublisherPlugin implements Plugin<Project> {

    @Override
    public void apply(@Nonnull Project project) {

        // Create the configuration extension
        ModPublisherGradleExtension extension = project.getExtensions().create(EXTENSION_NAME, ModPublisherGradleExtension.class);

        // Create the upload tasks
        final Task uploadTask = project.getTasks().create(TASK_NAME, UploadModTask.class);
        uploadTask.setDescription("Upload your mod to configured platforms");
        uploadTask.setGroup(TASK_GROUP);

        final Task curseUploadTask = project.getTasks().create(CURSE_TASK, CurseUploadTask.class, project, extension);
        curseUploadTask.setDescription("Upload your mod to Curseforge");
        curseUploadTask.setGroup(TASK_GROUP);

        final Task gitHubUploadTask = project.getTasks().create(GITHUB_TASK, GithubUploadTask.class, project, extension);
        gitHubUploadTask.setDescription("Upload your mod to GitHub");
        gitHubUploadTask.setGroup(TASK_GROUP);

        final Task modrinthUploadTask = project.getTasks().create(MODRINTH_TASK, ModrinthPublishTask.class, project, extension);
        modrinthUploadTask.setDescription("Upload your mod to Modrinth");
        modrinthUploadTask.setGroup(TASK_GROUP);

        final Task nightbloomUploadTask = project.getTasks().create(NIGHTBLOOM_TASK, NightBloomUploadTask.class, project, extension);
        nightbloomUploadTask.setDescription("Upload your mod to NightBloom");
        nightbloomUploadTask.setGroup(TASK_GROUP);

        project.afterEvaluate(c -> {
            if (!isNullOrEmpty(extension.getProxyConfig().getHttpHost()) || !isNullOrEmpty(extension.getProxyConfig().getHttpsHost())) {
                if (!isNullOrEmpty(extension.getProxyConfig().getHttpHost())) {
                    System.setProperty("http.proxyHost", extension.getProxyConfig().getHttpHost());
                    System.setProperty("http.proxyPort", String.valueOf(extension.getProxyConfig().getHttpPort()));
                }

                if (!isNullOrEmpty(extension.getProxyConfig().getHttpsHost())) {
                    System.setProperty("https.proxyHost", extension.getProxyConfig().getHttpsHost());
                    System.setProperty("https.proxyPort", String.valueOf(extension.getProxyConfig().getHttpsPort()));
                }
                project.getLogger().lifecycle("Added Proxy Information");
            }

            try {
                if (UploadPreChecks.canUploadCurse(project, extension)) {
                    Object artifactObject = CommonUtil.getPlatformArtifact(Platform.CURSEFORGE, extension);
                    resolveInputTask(project, artifactObject, curseUploadTask);
                    uploadTask.dependsOn(curseUploadTask);
                }
            } catch (Exception ignored) {}

            try {
                if (UploadPreChecks.canUploadModrinth(project, extension)) {
                    Object artifactObject = CommonUtil.getPlatformArtifact(Platform.MODRINTH, extension);
                    resolveInputTask(project, artifactObject, modrinthUploadTask);
                    uploadTask.dependsOn(modrinthUploadTask);
                }
            } catch (Exception ignored) {}

            try {
                if (UploadPreChecks.canUploadGitHub(project, extension)) {
                    Object artifactObject = CommonUtil.getPlatformArtifact(Platform.GITHUB, extension);
                    resolveInputTask(project, artifactObject, gitHubUploadTask);
                    uploadTask.dependsOn(gitHubUploadTask);
                }
            } catch (Exception ignored) {}

            try {
                if (UploadPreChecks.canUploadNightbloom(project, extension)) {
                    Object artifactObject = CommonUtil.getPlatformArtifact(Platform.NIGHTBLOOM, extension);
                    resolveInputTask(project, artifactObject, nightbloomUploadTask);
                    uploadTask.dependsOn(nightbloomUploadTask);
                }
            } catch (Exception ignored) {}
        });
    }

    private void resolveInputTask(Project project, Object inTask, Task mainTask) {
        if (project == null || inTask == null || mainTask == null)
            return;

        Task task = null;

        if (inTask instanceof Provider) {
            Provider<?> p = (Provider<?>) inTask;
            task = (Task) p.get();
        }

        if (inTask instanceof String) {
            task = project.getTasks().getByName((String) inTask);
        }

        if (inTask instanceof Task) {
            task = (Task) inTask;
        }

        if (!(task instanceof AbstractArchiveTask))
            return;

        if (task == null)
            return;

        String taskName = "prepare" + mainTask.getName() + "upload" + project.getName();
        project.task(taskName).dependsOn(task);
        mainTask.dependsOn(taskName);
    }
}
