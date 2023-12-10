/*
 * This file is part of modpublisher, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 HypherionSA and Contributors
 *
 */
package com.hypherionmc.modpublisher.plugin;

import groovy.lang.Closure;
import org.gradle.api.Project;

import java.util.ArrayList;
import java.util.List;

/**
 * @author HypherionSA
 * Plugin Settings Extension. This exposes settings to Gradle to allow you
 * to configure the plugin
 */
public class ModPublisherGradleExtension {

    // API Keys for Modrinth/Curseforge/GitHub. Used for publishing
    public ApiKeys apiKeys;

    // Enable debug mode. If enabled, no files will actually be uploaded
    public boolean debug = false;

    // Curseforge Project ID
    public String curseID;

    // Modrinth Project ID (NOT SLUG)
    public String modrinthID;

    // GitHub Repo. username/repo or URL
    public String githubRepo;

    // Type of release. Valid entries: release, beta, alpha
    public String versionType = "release";

    // Changelog text to apply to the uploaded file
    public Object changelog;

    // Version. Used for Modrinth and GitHub
    public String version;

    // Friendly display name for the files
    public String displayName;

    // Minecraft versions supported by this upload
    public List<String> gameVersions = new ArrayList<>();

    // Modloaders supported by this upload
    public List<String> loaders = new ArrayList<>();

    // New Curseforge Environment Tag. CLIENT, SERVER or BOTH
    public String curseEnvironment;

    // The file, or string location of the file that will be uploaded
    public Object artifact;

    // Control Curseforge Dependencies
    public Dependencies curseDepends;

    // Control Modrinth Dependencies
    public Dependencies modrinthDepends;

    // Disable Jar Scanning
    public boolean disableMalwareScanner = false;

    // Disable Empty Jar Checker
    public boolean disableEmptyJarCheck = false;

    // Allow uploads to modrinth staging
    public boolean useModrinthStaging = false;

    // Allow uploading additional files
    public List<Object> additionalFiles = new ArrayList<>();

    private final Project project;

    public ModPublisherGradleExtension(Project project) {
        this.project = project;
    }

    /**
     * Configure API Keys for this Project
     */
    public ApiKeys apiKeys(Closure<ApiKeys> closure) {
        apiKeys = new ApiKeys();
        project.configure(apiKeys, closure);
        return apiKeys;
    }

    /**
     * Configure Curseforge Dependencies for this project
     */
    public Dependencies curseDepends(Closure<Dependencies> closure) {
        curseDepends = new Dependencies();
        project.configure(curseDepends, closure);
        return curseDepends;
    }

    /**
     * Configure Modrinth dependencies for this project
     */
    public Dependencies modrinthDepends(Closure<Dependencies> closure) {
        modrinthDepends = new Dependencies();
        project.configure(modrinthDepends, closure);
        return modrinthDepends;
    }

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
