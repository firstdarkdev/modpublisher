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
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSetContainer;
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

        final Task modtaleUploadTask = project.getTasks().create(MODTALE_TASK, ModtaleUploadTask.class, project, extension);
        modtaleUploadTask.setDescription("Upload your mod to Modtale");
        modtaleUploadTask.setGroup(TASK_GROUP);

        project.getPlugins().withId("java", p -> {
            Object maybeContainer = project.getExtensions().getByName("sourceSets");

            if (maybeContainer instanceof SourceSetContainer) {
                SourceSetContainer sourceSets = (SourceSetContainer) maybeContainer;

                sourceSets.configureEach(ss -> {
                    String ssName = ss.getName();
                    if ("main".equals(ssName) || "test".equals(ssName)) return;

                    String extName = EXTENSION_NAME + StringUtils.capitalize(ssName);
                    ModPublisherGradleExtension ssExt = project.getExtensions().create(extName, ModPublisherGradleExtension.class);

                    String aggName = TASK_NAME + StringUtils.capitalize(ssName);
                    final Task ssUploadTask = project.getTasks().create(aggName, UploadModTask.class);
                    ssUploadTask.setDescription("Upload your mod to configured platforms for source set '" + ssName + "'");
                    ssUploadTask.setGroup(INTERNAL_TASK_GROUP);

                    final Task ssCurse = project.getTasks().create(CURSE_TASK + StringUtils.capitalize(ssName), CurseUploadTask.class, project, ssExt);
                    ssCurse.setDescription("Upload '" + ssName + "' to CurseForge");
                    ssCurse.setGroup(INTERNAL_TASK_GROUP);

                    final Task ssGithub = project.getTasks().create(GITHUB_TASK + StringUtils.capitalize(ssName), GithubUploadTask.class, project, ssExt);
                    ssGithub.setDescription("Upload '" + ssName + "' to GitHub");
                    ssGithub.setGroup(INTERNAL_TASK_GROUP);

                    final Task ssModrinth = project.getTasks().create(MODRINTH_TASK + StringUtils.capitalize(ssName), ModrinthPublishTask.class, project, ssExt);
                    ssModrinth.setDescription("Upload '" + ssName + "' to Modrinth");
                    ssModrinth.setGroup(INTERNAL_TASK_GROUP);

                    final Task ssNightbloom = project.getTasks().create(NIGHTBLOOM_TASK + StringUtils.capitalize(ssName), NightBloomUploadTask.class, project, ssExt);
                    ssNightbloom.setDescription("Upload '" + ssName + "' to NightBloom");
                    ssNightbloom.setGroup(INTERNAL_TASK_GROUP);

                    final Task ssmodtaleUploadTask = project.getTasks().create(MODTALE_TASK + StringUtils.capitalize(ssName), ModtaleUploadTask.class, project, extension);
                    ssmodtaleUploadTask.setDescription("Upload '" + ssName + "' to Modtale");
                    ssmodtaleUploadTask.setGroup(TASK_GROUP);

                    project.afterEvaluate(c -> {
                        ssExt.copyFrom(extension, ss);

                        if (ssExt.getProjectName() == null || ssExt.getProjectName().isEmpty()) {
                            ssUploadTask.setEnabled(false);
                            ssCurse.setEnabled(false);
                            ssGithub.setEnabled(false);
                            ssModrinth.setEnabled(false);
                            ssNightbloom.setEnabled(false);
                            ssmodtaleUploadTask.setEnabled(false);
                            return;
                        }

                        doPreChecks(project, ssExt, ssCurse, ssModrinth, ssGithub, ssNightbloom, ssUploadTask, ssmodtaleUploadTask);
                        uploadTask.dependsOn(ssUploadTask);
                    });
                });
            }

        });

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

            doPreChecks(project, extension, curseUploadTask, modrinthUploadTask, gitHubUploadTask, nightbloomUploadTask, uploadTask, modtaleUploadTask);
        });
    }

    private void doPreChecks(Project project, ModPublisherGradleExtension extension, Task curseUploadTask, Task modrinthUploadTask, Task gitHubUploadTask, Task nightbloomUploadTask, Task uploadTask, Task modtaleTask) {
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

            try {
                if (UploadPreChecks.canUploadModtale(project, extension)) {
                    Object artifactObject = CommonUtil.getPlatformArtifact(Platform.MODTALE, extension);
                    resolveInputTask(project, artifactObject, modtaleTask);
                    uploadTask.dependsOn(modtaleTask);
                }
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
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
