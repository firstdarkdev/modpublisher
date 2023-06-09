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

import javax.annotation.Nonnull;

import static me.hypherionmc.modpublisher.Constants.*;

/**
 * @author HypherionSA
 * Main Gradle Plugin Class.
 * This plugin is mainly intended for use with MultiLoader Projects
 */
public class ModPublisherPlugin implements Plugin<Project> {

    // The project this plugin is applied to
    public static Project project;

    // Configuration of the plugin
    public static ModPublisherGradleExtension extension;

    @Override
    public void apply(@Nonnull Project project) {
        ModPublisherPlugin.project = project;

        // Create the configuration extension
        extension = project.getExtensions().create(EXTENSION_NAME, ModPublisherGradleExtension.class);

        // Create the upload tasks
        final Task uploadTask = project.getTasks().create(TASK_NAME, UploadModTask.class);
        uploadTask.setDescription("Upload your mod to configured platforms");
        uploadTask.setGroup(TASK_GROUP);

        final Task curseUploadTask = project.getTasks().create(CURSE_TASK, CurseUploadTask.class);
        curseUploadTask.setDescription("Upload your mod to Curseforge");
        curseUploadTask.setGroup(TASK_GROUP);

        final Task gitHubUploadTask = project.getTasks().create(GITHUB_TASK, GithubUploadTask.class);
        gitHubUploadTask.setDescription("Upload your mod to GitHub");
        gitHubUploadTask.setGroup(TASK_GROUP);

        final Task modrinthUploadTask = project.getTasks().create(MODRINTH_TASK, ModrinthPublishTask.class);
        modrinthUploadTask.setDescription("Upload your mod to Modrinth");
        modrinthUploadTask.setGroup(TASK_GROUP);

        project.afterEvaluate(c -> {
            try {
                if (UploadPreChecks.canUploadCurse()) {
                    uploadTask.dependsOn(curseUploadTask);
                }
            } catch (Exception ignored) {}

            try {
                if (UploadPreChecks.canUploadModrinth()) {
                    uploadTask.dependsOn(modrinthUploadTask);
                }
            } catch (Exception ignored) {}

            try {
                if (UploadPreChecks.canUploadGitHub()) {
                    uploadTask.dependsOn(gitHubUploadTask);
                }
            } catch (Exception ignored) {}
        });
    }
}
