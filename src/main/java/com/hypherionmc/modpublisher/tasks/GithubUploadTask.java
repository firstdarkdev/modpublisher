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
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskAction;
import org.kohsuke.github.*;
import org.kohsuke.github.extras.okhttp3.OkHttpGitHubConnector;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

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

        final String uploadRepo = CommonUtil.cleanGithubUrl(extension.getGithub().getRepo());

        GHRepository ghRepository = gitHub.getRepository(uploadRepo);

        final String tag = extension.getGithub().getTag();

        // Try to find an existing release.
        // If one is found, the file will be added onto it.
        GHRelease ghRelease = ghRepository.getReleaseByTagName(tag);

        UploadPreChecks.checkEmptyJar(extension, uploadFile, extension.getLoaders().get());

        // We want to know if the existing release was a draft to avoid un-publishing releases
        boolean wasDraft;

        // Existing release was not found, so we create a new one
        if (ghRelease == null) {
            if (!extension.getGithub().isCreateRelease()) {
                project.getLogger().warn("Create GitHub Release is disabled and Github Release with tag {} does not exist", tag);
                return;
            }

            // FIXME this check doesn't make sense when `github.draft` is true,
            //       because draft releases don't create a tag anyway...
            if (!extension.getGithub().isCreateTag() && !hasRef(ghRepository, "refs/tags/" + tag)) {
                project.getLogger().warn("Create tag for GitHub Release is disabled and tag {} does not already exists", tag);
                return;
            }

            GHReleaseBuilder releaseBuilder = new GHReleaseBuilder(ghRepository, tag);

            // Use the first non-empty value in [displayName, version, githubTag]
            String name = Stream.of(extension.getDisplayName(), extension.getProjectVersion())
                    .map(Provider::getOrNull)
                    .filter(StringUtils::isNotBlank)
                    .findFirst()
                    .orElse(tag);

            releaseBuilder.name(name);

            // New releases should begin as drafts while we upload stuff to them
            wasDraft = true;
            releaseBuilder.draft(wasDraft);

            releaseBuilder.body(CommonUtil.resolveString(extension.getChangelog().get()));
            Optional.ofNullable(extension.getGithub().getTarget()).ifPresent(releaseBuilder::commitish);
            ghRelease = releaseBuilder.create();
        } else if (!extension.getGithub().isUpdateRelease()) {
            project.getLogger().warn("Update GitHub Release is disabled and Github Release with tag {} already exists", tag);
            return;
        } else {
            wasDraft = ghRelease.isDraft();
        }

        if (ghRelease == null)
            throw new NullPointerException("Could not get existing or create new Github Release with tag " + tag);

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
        if (wasDraft) {
            releaseUpdater.draft(extension.getGithub().isDraft());
        }
        releaseUpdater.update();

        project.getLogger().lifecycle(
                "Successfully uploaded {} (tag {}) to {}. {}.",
                extension.getProjectVersion().map(v -> "version " + v).getOrElse("(empty version)"),
                tag,
                ghRepository.getUrl().toString(),
                ghRelease.getHtmlUrl().toString()
        );
    }

    /**
     * Lookup whether the {@link GHRepository repo} has the specified {@code ref} or not.
     *
     * @param repo The GitHub repository to check
     * @param ref The fully qualified ref, e.g. {@code "refs/heads/main"} or {@code "refs/tags/v1.0.0"}
     * @return Whether the ref exists
     * @throws IOException if something went wrong. E.g. a network error
     */
    private static boolean hasRef(GHRepository repo, String ref) throws IOException {
        try {
            // Lookup the tag, but ignore the result
            // We only care whether a GHFileNotFoundException gets thrown
            repo.getRef(ref);
            return true;
        } catch (GHFileNotFoundException e) {
            return false;
        } catch (IOException e) {
            throw new IOException("Error checking whether \"" + ref + "\" exists on GitHub repo \"" + repo.getFullName() + "\"", e);
        }
    }
}
