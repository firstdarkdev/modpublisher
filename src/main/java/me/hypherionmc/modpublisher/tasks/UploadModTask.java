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

import me.hypherionmc.modpublisher.util.UploadPreChecks;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * @author HypherionSA
 * The main Upload Task. This will check that all required values are present
 * and execute the appropriate upload sub-task
 */
public class UploadModTask extends DefaultTask {

    @TaskAction
    void uploadArtifacts() throws Exception {
        if (UploadPreChecks.canUploadModrinth()) {
            ModrinthPublishTask modrinthPublishTask = new ModrinthPublishTask();
            modrinthPublishTask.upload();
        }

        if (UploadPreChecks.canUploadCurse()) {
            CurseUploadTask curseUploadTask = new CurseUploadTask();
            curseUploadTask.upload();
        }

        if (UploadPreChecks.canUploadGitHub()) {
            GithubUploadTask githubUploadTask = new GithubUploadTask();
            githubUploadTask.upload();
        }
    }

}
