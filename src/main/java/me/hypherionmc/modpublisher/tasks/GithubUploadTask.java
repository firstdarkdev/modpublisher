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

import me.hypherionmc.modpublisher.util.CommonUtil;
import me.hypherionmc.modpublisher.util.UploadPreChecks;
import me.hypherionmc.modpublisher.util.UserAgentInterceptor;
import okhttp3.OkHttpClient;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;
import org.kohsuke.github.*;
import org.kohsuke.github.extras.okhttp3.OkHttpGitHubConnector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static me.hypherionmc.modpublisher.plugin.ModPublisherPlugin.extension;
import static me.hypherionmc.modpublisher.plugin.ModPublisherPlugin.project;

/**
 * @author HypherionSA
 * Sub-Task to handle GitHub publishing. This task will only be executed if
 * an GitHub API Key and repo is supplied
 */
public class GithubUploadTask extends DefaultTask {

    // Instance of HUB4J to handle GitHub API communications
    private final GitHub gitHub;

    public GithubUploadTask() throws IOException {
        // Create an HTTP Client with UserAgent and longer timeouts
        OkHttpClient client = new OkHttpClient().newBuilder()
                .callTimeout(1, TimeUnit.MINUTES)
                .connectTimeout(1, TimeUnit.MINUTES)
                .addNetworkInterceptor(new UserAgentInterceptor("modpublisher/v1 (https://github.com/firstdarkdev/modpublisher)"))
                .build();

        // Try to instantiate the GitHub API.
        // Will throw an error if the Token is invalid
        gitHub = new GitHubBuilder()
                .withOAuthToken(extension.apiKeys.github)
                .withConnector(new OkHttpGitHubConnector(client)).build();
    }

    /**
     * Configure the upload and upload it
     */
    @TaskAction
    public void upload() throws Exception {
        project.getLogger().lifecycle("Uploading to GitHub");
        UploadPreChecks.checkRequiredValues();
        boolean canUpload = UploadPreChecks.canUploadGitHub();
        if (!canUpload)
            return;

        File uploadFile = CommonUtil.resolveFile(project, extension.artifact);

        if (uploadFile == null || !uploadFile.exists())
            throw new FileNotFoundException("Cannot find file " + extension.artifact.toString());

        if (gitHub == null)
            return;

        final String uploadRepo = CommonUtil.cleanGithubUrl(extension.githubRepo);

        GHRepository ghRepository = gitHub.getRepository(uploadRepo);

        // Try to find an existing release.
        // If one is found, the file will be added onto it.
        GHRelease ghRelease = ghRepository.getReleaseByTagName(extension.version);

        // Debug Mode. Return early to prevent any API calls that will result in anything
        // being created or uploaded
        if (extension.debug) {
            project.getLogger().lifecycle("Debug mode is enabled. Not uploading to github");
            return;
        }

        // Existing release was not found, so we create a new one
        if (ghRelease == null) {
            GHReleaseBuilder releaseBuilder = new GHReleaseBuilder(ghRepository, extension.version);

            if (extension.displayName != null && !extension.displayName.isEmpty()) {
                releaseBuilder.name(extension.displayName);
            } else {
                releaseBuilder.name(extension.version);
            }

            releaseBuilder.body(CommonUtil.resolveString(extension.changelog));
            releaseBuilder.draft(true);
            releaseBuilder.commitish(ghRepository.getDefaultBranch());
            ghRelease = releaseBuilder.create();
        }

        if (ghRelease == null)
            throw new NullPointerException("Could not get existing or create new Github Release with tag " +  extension.version);

        GHAsset asset = ghRelease.uploadAsset(uploadFile, "application/octet-stream");

        if (asset == null)
            throw new IOException("Failed to upload release to github. No error found");

        // Mark Release as PRE-RELEASE if alpha or beta
        // Actually publish the release if a brand new one was created
        GHReleaseUpdater releaseUpdater = ghRelease.update();
        releaseUpdater.prerelease(extension.versionType.equalsIgnoreCase("beta") || extension.versionType.equalsIgnoreCase("alpha"));
        releaseUpdater.draft(false);
        releaseUpdater.update();

        project.getLogger().lifecycle(
                "Successfully uploaded version {} to {}. {}.",
                extension.version,
                ghRepository.getUrl().toString(),
                ghRelease.getHtmlUrl().toString()
        );
    }
}
