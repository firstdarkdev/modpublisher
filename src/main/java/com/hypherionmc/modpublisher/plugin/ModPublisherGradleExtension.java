/*
 * This file is part of modpublisher, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 HypherionSA and Contributors
 *
 */
package com.hypherionmc.modpublisher.plugin;

import lombok.Getter;
import lombok.Setter;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;

import java.util.ArrayList;
import java.util.List;

/**
 * @author HypherionSA
 * Plugin Settings Extension. This exposes settings to Gradle to allow you
 * to configure the plugin
 */
public class ModPublisherGradleExtension {

    // API Keys for Modrinth/Curseforge/GitHub. Used for publishing
    @Getter private final Property<ApiKeys> apiKeys;

    // Enable debug mode. If enabled, no files will actually be uploaded
    @Getter private final Property<Boolean> debug;

    // Curseforge Project ID
    @Getter private final Property<String> curseID;

    // Modrinth Project ID (NOT SLUG)
    @Getter private final Property<String> modrinthID;

    // GitHub Repo. username/repo or URL
    @Getter private final Property<String> githubRepo;

    // Type of release. Valid entries: release, beta, alpha
    @Getter private final Property<String> versionType;

    // Changelog text to apply to the uploaded file
    @Getter private final Property<Object> changelog;

    // Version. Used for Modrinth and GitHub
    @Getter private final Property<String> version;

    // Friendly display name for the files
    @Getter private final Property<String> displayName;

    // Minecraft versions supported by this upload
    @Getter private final ListProperty<String> gameVersions;

    // Modloaders supported by this upload
    @Getter private final ListProperty<String> loaders;

    // New Curseforge Environment Tag. CLIENT, SERVER or BOTH
    @Getter private final Property<String> curseEnvironment;

    // The file, or string location of the file that will be uploaded
    @Getter private final Property<Object> artifact;

    // Control Curseforge Dependencies
    @Getter private final Property<Dependencies> curseDepends;

    // Control Modrinth Dependencies
    @Getter private final Property<Dependencies> modrinthDepends;

    // Disable Jar Scanning
    @Getter private final Property<Boolean> disableMalwareScanner;

    // Disable Empty Jar Checker
    @Getter private final Property<Boolean> disableEmptyJarCheck;

    // Allow uploads to modrinth staging
    @Getter private final Property<Boolean> useModrinthStaging;

    // Allow uploading additional files
    @Getter private final ListProperty<Object> additionalFiles;

    private final Project project;

    public ModPublisherGradleExtension(Project project) {
        this.project = project;
        this.apiKeys = project.getObjects().property(ApiKeys.class).convention(new ApiKeys());
        this.debug = project.getObjects().property(Boolean.class).convention(false);
        this.curseID = project.getObjects().property(String.class);
        this.modrinthID = project.getObjects().property(String.class);
        this.githubRepo = project.getObjects().property(String.class);
        this.versionType = project.getObjects().property(String.class).convention("release");
        this.changelog = project.getObjects().property(Object.class);
        this.version = project.getObjects().property(String.class);
        this.displayName = project.getObjects().property(String.class);
        this.gameVersions = project.getObjects().listProperty(String.class).empty();
        this.loaders = project.getObjects().listProperty(String.class).empty();
        this.curseEnvironment = project.getObjects().property(String.class).convention("both");
        this.artifact = project.getObjects().property(Object.class);
        this.curseDepends = project.getObjects().property(Dependencies.class).convention(new Dependencies());
        this.modrinthDepends = project.getObjects().property(Dependencies.class).convention(new Dependencies());
        this.disableMalwareScanner = project.getObjects().property(Boolean.class).convention(false);
        this.disableEmptyJarCheck = project.getObjects().property(Boolean.class).convention(false);
        this.useModrinthStaging = project.getObjects().property(Boolean.class).convention(false);
        this.additionalFiles = project.getObjects().listProperty(Object.class).empty();
    }

    public void apiKeys(Action<ApiKeys> action) {
        action.execute(apiKeys.get());
    }

    public void curseDepends(Action<Dependencies> action) {
        action.execute(curseDepends.get());
    }

    public void modrinthDepends(Action<Dependencies> action) {
        action.execute(modrinthDepends.get());
    }

    @Setter
    public static class ApiKeys {
        public String curseforge;
        public String modrinth;
        public String github;
    }

    public static class Dependencies {
        public List<String> required = new ArrayList<>();
        public List<String> optional = new ArrayList<>();
        public List<String> incompatible = new ArrayList<>();
        public List<String> embedded = new ArrayList<>();
    }
}

