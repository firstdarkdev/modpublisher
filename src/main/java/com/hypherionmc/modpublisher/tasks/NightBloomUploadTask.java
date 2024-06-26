/*
 * This file is part of modpublisher, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2024 HypherionSA and Contributors
 *
 */
package com.hypherionmc.modpublisher.tasks;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypherionmc.modpublisher.plugin.ModPublisherGradleExtension;
import com.hypherionmc.modpublisher.properties.Platform;
import com.hypherionmc.modpublisher.util.CommonUtil;
import com.hypherionmc.modpublisher.util.UploadPreChecks;
import com.hypherionmc.nightbloom.NightBloom4J;
import com.hypherionmc.nightbloom.client.agent.UserAgent;
import com.hypherionmc.nightbloom.model.ProjectMeta;
import com.hypherionmc.nightbloom.model.StandardResponse;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskAction;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NightBloomUploadTask extends DefaultTask {

    private final Project project;
    private final ModPublisherGradleExtension extension;

    @Inject
    public NightBloomUploadTask(Project project, ModPublisherGradleExtension extension) {
        this.project = project;
        this.extension = extension;
    }

    /**
     * Configure the upload and upload it
     */
    @TaskAction
    public void upload() throws Exception {
        project.getLogger().lifecycle("Uploading to NightBloom");
        UploadPreChecks.checkRequiredValues(project, Platform.NIGHTBLOOM, extension);
        boolean canUpload = UploadPreChecks.canUploadNightbloom(project, extension);
        if (!canUpload)
            return;

        // Required User Agent
        UserAgent.UserAgentBuilder userAgent = UserAgent.builder();
        userAgent.authorName("HypherionSA");
        userAgent.contact("hypherionmc@gmail.com");
        userAgent.projectName("ModPublisher");
        userAgent.projectVersion("v1");

        // Create the API Client
        // Instance of NightBloom4J that will be used
        NightBloom4J bloomApi = NightBloom4J.v1(userAgent.build(), extension.getApiKeys().getNightbloom());
        //bloomApi.setUrl("http://127.0.0.1:8787");

        Object artifactObject = CommonUtil.getPlatformArtifact(Platform.NIGHTBLOOM, extension);
        File uploadFile = CommonUtil.resolveFile(project, artifactObject);

        if (uploadFile == null || !uploadFile.exists())
            throw new FileNotFoundException("Cannot find file " + artifactObject);

        ProjectMeta.ProjectMetaBuilder metab = ProjectMeta.builder();
        metab.changelog(CommonUtil.resolveString(extension.getChangelog().get()));
        metab.type(extension.getVersionType().get().toLowerCase());
        metab.version(extension.getProjectVersion().get());

        if (extension.getNightbloomDepends().isPresent())
            metab.dependsOn(extension.getNightbloomDepends().get());

        if (extension.getDisplayName().isPresent() && !extension.getDisplayName().get().isEmpty()) {
            metab.displayName(extension.getDisplayName().get());
        } else {
            metab.displayName(extension.getProjectVersion().get());
        }

        List<String> finalGameVersions = new ArrayList<>();
        for (String gameVersion : extension.getGameVersions().get()) {
            if (gameVersion.endsWith("-snapshot"))
                continue;
            finalGameVersions.add(gameVersion.toLowerCase());
        }

        ProjectMeta meta = metab.build();
        finalGameVersions.forEach(meta::addMinecraft);

        List<String> finalLoaders = new ArrayList<>();
        for (String loader : extension.getLoaders().get()) {
            if (loader.equalsIgnoreCase("risugami's modloader")) {
                if (!finalLoaders.contains("modloader"))
                    finalLoaders.add("modloader");
                continue;
            }

            finalLoaders.add(loader.toLowerCase());
        }

        if (!finalLoaders.isEmpty())
            finalLoaders.forEach(meta::addModloader);

        // Debug mode, so we do not upload the file
        if (extension.getDebug().get()) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            project.getLogger().lifecycle("Full data to be sent for upload: {}", gson.toJson(meta));
            project.getLogger().lifecycle("File to be uploaded: {}", uploadFile.getName());
            return;
        }

        UploadPreChecks.checkEmptyJar(extension, uploadFile, extension.getLoaders().get());
        StandardResponse r = bloomApi.projects().uploadFile(extension.getNightbloomID().get(), meta, uploadFile);

        if (r.isError()) {
            project.getLogger().error("Failed to upload to Nightbloom: {}", r.getMessage());
            return;
        }

        Matcher matcher = Pattern.compile("\\bID\\s*(\\d+)\\b").matcher(r.getMessage());
        matcher.find();

        project.getLogger().lifecycle(
                "Successfully uploaded version {} to {} as version ID {}.",
                extension.getProjectVersion().get(),
                extension.getNightbloomID().get(),
                matcher.group(1)
        );
    }

}