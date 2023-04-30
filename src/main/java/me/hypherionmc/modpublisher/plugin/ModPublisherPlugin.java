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
