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

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import static me.hypherionmc.modpublisher.plugin.ModPublisherPlugin.extension;

/**
 * @author HypherionSA
 * The main Upload Task. This will check that all required values are present
 * and execute the appropriate upload sub-task
 */
public class UploadModTask extends DefaultTask {

    @TaskAction
    void uploadArtifacts() throws Exception {
        // Used later to check if the sub-task must be run
        boolean canUploadCurse = true;
        boolean canUploadModrinth = true;

        // Check if API Keys are configured
        if (extension.apiKeys == null) {
            throw new Exception("Missing apiKeys config. Artifacts cannot be uploaded without this");
        }

        if (extension.artifact == null) {
            throw new Exception("Missing artifact. Cannot continue");
        }

        // Check that both the Curseforge API key and Project ID is defined
        if (extension.apiKeys.curseforge != null && !extension.apiKeys.curseforge.isEmpty()) {
            if (extension.curseID == null || extension.curseID.isEmpty()) {
                throw new Exception("Found Curseforge API token, but curseID is not defined");
            }
        } else {
            canUploadCurse = false;
        }

        // Check that both the Modrinth API key and Project ID is defined
        if (extension.apiKeys.modrinth != null && !extension.apiKeys.modrinth.isEmpty()) {
            if (extension.modrinthID == null || extension.modrinthID.isEmpty()) {
                throw new Exception("Found Modrinth API token, but modrinthID is not defined");
            }
        } else {
            canUploadModrinth = false;
        }

        if (extension.version == null || extension.version.isEmpty()) {
            throw new Exception("Version is not defined. This is REQUIRED by modrinth");
        }

        if (extension.gameVersions.isEmpty()) {
            throw new Exception("gameVersions is not defined. This is required");
        }

        if (extension.loaders.isEmpty()) {
            throw new Exception("loaders is not defined. This is required");
        }

        // All checks passed, run Modrinth upload task
        if (canUploadModrinth) {
            ModrinthPublishTask modrinthPublishTask = new ModrinthPublishTask();
            modrinthPublishTask.upload();
        }

        // All checks passed, run Curseforge upload task
        if (canUploadCurse) {
            CurseUploadTask curseUploadTask = new CurseUploadTask();
            curseUploadTask.upload();
        }
    }

}
