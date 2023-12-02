/*
 * This file is part of modpublisher, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 HypherionSA and Contributors
 *
 */
package me.hypherionmc.modpublisher.plugin;

import me.hypherionmc.modpublisher.tasks.CurseUploadTask;
import me.hypherionmc.modpublisher.tasks.GithubUploadTask;
import me.hypherionmc.modpublisher.tasks.ModrinthPublishTask;
import me.hypherionmc.modpublisher.tasks.UploadModTask;
import me.hypherionmc.modpublisher.util.UploadPreChecks;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;

import javax.annotation.Nonnull;

import static me.hypherionmc.modpublisher.Constants.*;

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

        project.afterEvaluate(c -> {
            try {
                if (UploadPreChecks.canUploadCurse(project, extension)) {
                    resolveInputTask(project, extension.artifact, curseUploadTask);
                    uploadTask.dependsOn(curseUploadTask);
                }
            } catch (Exception ignored) {}

            try {
                if (UploadPreChecks.canUploadModrinth(project, extension)) {
                    resolveInputTask(project, extension.artifact, modrinthUploadTask);
                    uploadTask.dependsOn(modrinthUploadTask);
                }
            } catch (Exception ignored) {}

            try {
                if (UploadPreChecks.canUploadGitHub(project, extension)) {
                    resolveInputTask(project, extension.artifact, gitHubUploadTask);
                    uploadTask.dependsOn(gitHubUploadTask);
                }
            } catch (Exception ignored) {}
        });
    }

    private void resolveInputTask(Project project, Object inTask, Task mainTask) {
        if (project == null || inTask == null || mainTask == null)
            return;

        Task task = null;

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
