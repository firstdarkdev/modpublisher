/*
 * This file is part of modpublisher, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 HypherionSA and Contributors
 *
 */
package com.hypherionmc.modpublisher.tasks;

import com.hypherionmc.modpublisher.plugin.ModPublisherGradleExtension;
import com.hypherionmc.modpublisher.properties.Platform;
import com.hypherionmc.modpublisher.util.CommonUtil;
import com.hypherionmc.modpublisher.util.UploadPreChecks;
import com.hypherionmc.modpublisher.util.UserAgentInterceptor;
import okhttp3.OkHttpClient;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;
import org.kohsuke.github.*;
import org.kohsuke.github.extras.okhttp3.OkHttpGitHubConnector;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
/**
 * @author HypherionSA
 * Sub-Task to handle GitHub publishing. This task will only be executed if
 * an GitHub API Key and repo is supplied
 */
public class GithubUploadTask extends DefaultTask {

    // Instance of HUB4J to handle GitHub API communications
    private GitHub gitHub;

    private final Project project;
    private final ModPublisherGradleExtension extension;

    @Inject
    public GithubUploadTask(Project project, ModPublisherGradleExtension extension) {
        this.project = project;
        this.extension = extension;
    }

    /**
     * Configure the upload and upload it
     */
    @TaskAction
    public void upload() throws Exception {
        project.getLogger().lifecycle("Uploading to GitHub");
        UploadPreChecks.checkRequiredValues(project, Platform.GITHUB, extension);
        boolean canUpload = UploadPreChecks.canUploadGitHub(project, extension);
        if (!canUpload)
            return;

        // Create an HTTP Client with UserAgent and longer timeouts
        OkHttpClient client = new OkHttpClient().newBuilder()
                .callTimeout(1, TimeUnit.MINUTES)
                .connectTimeout(1, TimeUnit.MINUTES)
                .addNetworkInterceptor(new UserAgentInterceptor("modpublisher/v1 (https://github.com/firstdarkdev/modpublisher)"))
                .build();

        // Try to instantiate the GitHub API.
        // Will throw an error if the Token is invalid
        gitHub = new GitHubBuilder()
                .withOAuthToken(extension.getApiKeys().getGithub())
                .withConnector(new OkHttpGitHubConnector(client)).build();

        Object artifactObject = CommonUtil.getPlatformArtifact(Platform.GITHUB, extension);
        File uploadFile = CommonUtil.resolveFile(project, artifactObject);

        if (uploadFile == null || !uploadFile.exists())
            throw new FileNotFoundException("Cannot find file " + artifactObject);

        if (gitHub == null)
            return;

        // Debug Mode. Return early to prevent any API calls that will result in anything
        // being created or uploaded
        if (extension.getDebug().get()) {
            project.getLogger().lifecycle("Debug mode is enabled. Not uploading to github");
            return;
        }

        final String uploadRepo = CommonUtil.cleanGithubUrl(extension.getGithubRepo().get());

        GHRepository ghRepository = gitHub.getRepository(uploadRepo);

        // Try to find an existing release.
        // If one is found, the file will be added onto it.
        GHRelease ghRelease = ghRepository.getReleaseByTagName(extension.getVersion().get());

        UploadPreChecks.checkEmptyJar(extension, uploadFile, extension.getLoaders().get());

        // Existing release was not found, so we create a new one
        if (ghRelease == null) {
            GHReleaseBuilder releaseBuilder = new GHReleaseBuilder(ghRepository, extension.getVersion().get());

            if (extension.getDisplayName().isPresent() && !extension.getDisplayName().get().isEmpty()) {
                releaseBuilder.name(extension.getDisplayName().get());
            } else {
                releaseBuilder.name(extension.getVersion().get());
            }

            releaseBuilder.body(CommonUtil.resolveString(extension.getChangelog().get()));
            releaseBuilder.draft(true);
            releaseBuilder.commitish(ghRepository.getDefaultBranch());
            ghRelease = releaseBuilder.create();
        }

        if (ghRelease == null)
            throw new NullPointerException("Could not get existing or create new Github Release with tag " +  extension.getVersion().get());

        GHAsset asset = ghRelease.uploadAsset(uploadFile, "application/octet-stream");

        if (asset == null)
            throw new IOException("Failed to upload release to github. No error found");

        if (extension.getAdditionalFiles().isPresent()) {
            for (ModPublisherGradleExtension.AdditionalFile file : extension.getAdditionalFiles().get()) {
                ghRelease.uploadAsset(CommonUtil.resolveFile(project, file.getArtifact()), "application/octet-stream");
            }
        }

        // Mark Release as PRE-RELEASE if alpha or beta
        // Actually publish the release if a brand new one was created
        GHReleaseUpdater releaseUpdater = ghRelease.update();
        releaseUpdater.prerelease(extension.getVersionType().get().equalsIgnoreCase("beta") || extension.getVersionType().get().equalsIgnoreCase("alpha"));
        releaseUpdater.draft(false);
        releaseUpdater.update();

        project.getLogger().lifecycle(
                "Successfully uploaded version {} to {}. {}.",
                extension.getVersion().get(),
                ghRepository.getUrl().toString(),
                ghRelease.getHtmlUrl().toString()
        );
    }
}
