/*
 * This file is part of modpublisher, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 HypherionSA and Contributors
 *
 */
package com.hypherionmc.modpublisher.plugin;

import com.hypherionmc.modpublisher.properties.CurseEnvironment;
import com.hypherionmc.modpublisher.properties.ModLoader;
import com.hypherionmc.modpublisher.properties.Platform;
import com.hypherionmc.modpublisher.properties.ReleaseType;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.JavaVersion;
import org.gradle.api.Project;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.jetbrains.annotations.ApiStatus;

import java.util.HashMap;

/**
 * @author HypherionSA
 * Plugin Settings Extension. This exposes settings to Gradle to allow you
 * to configure the plugin
 */
public class ModPublisherGradleExtension {

    // API Keys for Modrinth/Curseforge/GitHub. Used for publishing
    @Getter private final ApiKeys apiKeys = new ApiKeys();

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
    @Getter Property<Object> artifact;

    // Override Dependencies for Platforms
    @Getter private HashMap<String, Object> artifacts;

    // GitHub options
    @Getter private final GithubConfig github;

    // Curse Dependencies
    @Getter private final Dependencies curseDepends;

    // Modrinth Dependencies
    @Getter private final Dependencies modrinthDepends;

    // Disable Jar Scanning
    @Getter private final Property<Boolean> disableMalwareScanner;

    // Disable Empty Jar Checker
    @Getter private final Property<Boolean> disableEmptyJarCheck;

    // Allow uploads to modrinth staging
    @Getter private final Property<Boolean> useModrinthStaging;

    // Allow uploading additional files
    @Getter private final ListProperty<AdditionalFile> additionalFiles;

    @Getter private final ListProperty<JavaVersion> javaVersions;

    private final Project project;

    public ModPublisherGradleExtension(Project project) {
        this.project = project;
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
        this.artifacts = new HashMap<>();
        this.artifact = project.getObjects().property(Object.class);

        // GitHub config
        this.github = new GithubConfig();

        // Control Curseforge Dependencies
        ListProperty<String> curseRequired = project.getObjects().listProperty(String.class).empty();
        ListProperty<String> curseIncompatible = project.getObjects().listProperty(String.class).empty();
        ListProperty<String> curseOptional = project.getObjects().listProperty(String.class).empty();
        ListProperty<String> curseEmbedded = project.getObjects().listProperty(String.class).empty();
        this.curseDepends = new Dependencies(curseRequired, curseOptional, curseIncompatible, curseEmbedded);
        this.javaVersions = project.getObjects().listProperty(JavaVersion.class).empty();

        // Control Modrinth Dependencies
        ListProperty<String> modrinthRequired = project.getObjects().listProperty(String.class).empty();
        ListProperty<String> modrinthIncompatible = project.getObjects().listProperty(String.class).empty();
        ListProperty<String> modrinthOptional = project.getObjects().listProperty(String.class).empty();
        ListProperty<String> modrinthEmbedded = project.getObjects().listProperty(String.class).empty();
        this.modrinthDepends = new Dependencies(modrinthRequired, modrinthOptional, modrinthIncompatible, modrinthEmbedded);

        this.disableMalwareScanner = project.getObjects().property(Boolean.class).convention(false);
        this.disableEmptyJarCheck = project.getObjects().property(Boolean.class).convention(false);
        this.useModrinthStaging = project.getObjects().property(Boolean.class).convention(false);
        this.additionalFiles = project.getObjects().listProperty(AdditionalFile.class).empty();
    }

    /**
     * Helper method to override an upload artifact for a platform
     * @param platform The {@link Platform} the artifact is for
     * @param artifact The artifact object to resolve
     */
    public void setPlatformArtifact(Platform platform, Object artifact) {
        this.setPlatformArtifact(platform.toString().toLowerCase(), artifact);
    }

    /**
     * Helper method to override an upload artifact for a platform
     * @param platform The platform the override is for
     * @param artifact The artifact object to resolve
     */
    public void setPlatformArtifact(String platform, Object artifact) {
        if (this.artifacts == null) artifacts = new HashMap<>();

        if (this.artifacts.containsKey(platform)) {
            throw new GradleException("Artifact already added for platform " + platform);
        }

        this.artifacts.put(platform, artifact);
    }

    /**
     * Helper Method to create the apiKeys extension with DSL
     * @param action The configured apiKeys DSL to apply
     */
    public void apiKeys(Action<ApiKeys> action) {
        action.execute(apiKeys);
    }

    /**
     * Helper Method to create the github extension with DSL
     * @param action The configured github DSL to apply
     */
    public void github(Action<GithubConfig> action) {
        action.execute(github);
    }

    /**
     * Helper method to create curseDepends with DSL
     * @param action The configured curseDepends DSL to apply
     */
    public void curseDepends(Action<Dependencies> action) {
        action.execute(curseDepends);
    }

    /**
     * Helper method to create modrinthDepends with DSL
     * @param action The configured modrinthDepends DSL to apply
     */
    public void modrinthDepends(Action<Dependencies> action) {
        action.execute(modrinthDepends);
    }

    /**
     * Allow adding a single game version, without a list
     * @param version The game version to add
     */
    public void setGameVersions(String version) {
        this.gameVersions.add(version);
    }

    /**
     * Allow adding a list of game versions
     * @param version The game versions to add
     */
    public void setGameVersions(String... version) {
        this.gameVersions.addAll(version);
    }

    /**
     * Allow adding a single modloader without a list/array
     * @param loader The loader to add
     */
    public void setLoaders(String loader) {
        this.loaders.add(loader);
    }

    /**
     * Allow adding a list of modloaders
     * @param loader The loaders to add
     */
    public void setLoaders(String... loader) {
        this.loaders.addAll(loader);
    }

    /**
     * Allow adding a single loader with the {@link ModLoader} helper enum
     * @param loader The loader to add
     */
    public void setLoaders(ModLoader loader) {
        this.loaders.add(loader.toString());
    }

    /**
     * Allow adding multiple mod loaders with the {@link ModLoader} helper enum
     * @param loader The loaders to add
     */
    public void setLoaders(ModLoader... loader) {
        for (ModLoader l : loader) {
            this.loaders.add(l.toString());
        }
    }

    /**
     * Add supported Java Versions, currently only used for Curseforge.
     * <p>
     * Accepts any object that can be converted to a {@link JavaVersion} using {@link JavaVersion#toVersion(Object)}.
     * <p>
     * Strings will have the optional prefix {@code "Java "} removed before being parsed,
     * to support versions formatted as {@code "Java 8"}.
     * <p>
     * Example: {@code setJavaVersions(8, JavaVersion.VERSION_11, "16", "Java 17")}
     *
     * @param version Version or Versions to add.
     */
    public void setJavaVersions(Object... version) {
        for (Object v : version) {
            if (v instanceof String) {
                // Handle "curseforge format" versions, e.g. "Java 8"
                v = StringUtils.removeStart((String) v, "Java ");
            }
            this.javaVersions.add(JavaVersion.toVersion(v));
        }
    }

    /**
     * Allow settings the Curse Environment with a String
     * @param environment The curse environment
     */
    public void setCurseEnvironment(String environment) {
        this.curseEnvironment.set(environment);
    }

    /**
     * Allow adding Curse Environment with the {@link CurseEnvironment} helper enum
     * @param environment The Curse Environment to add
     */
    public void setCurseEnvironment(CurseEnvironment environment) {
        this.curseEnvironment.set(environment.toString().toLowerCase());
    }

    /**
     * Allows setting the release type with the {@link ReleaseType} helper enum
      * @param type The release type to set
     */
    public void setReleaseType(ReleaseType type) {
        this.versionType.set(type.toString().toLowerCase());
    }

    /**
     * Allow adding a file to be uploaded along with the main artifact
     * @param file The file
     */
    public void addAdditionalFile(Object file) {
        AdditionalFile additionalFile = new AdditionalFile();
        additionalFile.artifact = file;
        this.additionalFiles.add(additionalFile);
    }

    /**
     * Allow adding multiple additional files to be uploaded along with the main artifact
     * @param file The files
     */
    public void addAdditionalFile(Object... file) {
        for (Object f : file) {
            AdditionalFile additionalFile = new AdditionalFile();
            additionalFile.artifact = f;
            this.additionalFiles.add(additionalFile);
        }
    }

    /**
     * DSL support for adding a file with a custom display name and changelog
     * @param file The DSL object containing the file data
     */
    public void addAdditionalFile(Action<AdditionalFile> file) {
        AdditionalFile additionalFile = new AdditionalFile();
        file.execute(additionalFile);
        this.additionalFiles.add(additionalFile);
    }

    @Getter
    public static class ApiKeys {
        private String curseforge = "";
        private String modrinth = "";
        private String github = "";

        /**
         * Mostly for Kotlin support
         * Set the Curseforge API key
         * @param curseforge The api Key
         */
        public void curseforge(String curseforge) {
            this.curseforge = curseforge;
        }

        /**
         * Mostly for Kotlin support
         * Set the Modrinth API key
         * @param modrinth The api key
         */
        public void modrinth(String modrinth) {
            this.modrinth = modrinth;
        }

        /**
         * Mostly for Kotlin support
         * Set the GitHub token
         * @param github The token
         */
        public void github(String github) {
            this.github = github;
        }
    }

    /**
     * A Helper class to add dependencies with DSL
     */
    @Getter
    public static class Dependencies {
        private final ListProperty<String> required;
        private final ListProperty<String> optional;
        private final ListProperty<String> incompatible;
        private final ListProperty<String> embedded;

        /**
         * Constructor helper
         * @param required List of required dependencies
         * @param optional List of optional dependencies
         * @param incompatible List of incompatible dependencies
         * @param embedded List of embedded dependencies
         */
        public Dependencies(ListProperty<String> required, ListProperty<String> optional,
                            ListProperty<String> incompatible, ListProperty<String> embedded) {
            this.required = required;
            this.optional = optional;
            this.incompatible = incompatible;
            this.embedded = embedded;
        }

        /**
         * Mostly for kotlin support
         * Add a single required dependency
         * @param dep The dependency to add
         */
        public void required(String dep) {
            required.add(dep);
        }

        /**
         * Mostly for kotlin support
         * Add multiple required dependencies
         * @param deps The dependencies to add
         */
        public void required(String... deps) {
            required.addAll(deps);
        }

        /**
         * Mostly for kotlin support
         * Add a single optional dependency
         * @param dep The dependency to add
         */
        public void optional(String dep) {
            optional.add(dep);
        }

        /**
         * Mostly for kotlin support
         * Add multiple optional dependencies
         * @param deps The dependencies to add
         */
        public void optional(String... deps) {
            optional.addAll(deps);
        }

        /**
         * Mostly for kotlin support
         * Add a single incompatible dependency
         * @param dep The dependency to add
         */
        public void incompatible(String dep) {
            incompatible.add(dep);
        }

        /**
         * Mostly for kotlin support
         * Add multiple incompatible dependencies
         * @param deps The dependencies to add
         */
        public void incompatible(String... deps) {
            incompatible.addAll(deps);
        }

        /**
         * Mostly for kotlin support
         * Add a single embedded dependency
         * @param dep The dependency to add
         */
        public void embedded(String dep) {
            embedded.add(dep);
        }

        /**
         * Mostly for kotlin support
         * Add multiple embedded dependencies
         * @param deps The dependencies to add
         */
        public void embedded(String... deps) {
            embedded.addAll(deps);
        }
    }

    /**
     * Helper Class to add additional files with a custom Changelog and Display name
     */
    @Getter
    public static class AdditionalFile {
        private Object artifact;
        private String displayName;
        private String changelog;

        public void configure(Action<AdditionalFile> action) {
            action.execute(this);
        }

        /**
         * Set the artifact
         * @param file The File, Task or String
         */
        public void artifact(Object file) {
            this.artifact = file;
        }

        /**
         * Set the artifact Display Name
         * @param displayName The display name
         */
        public void displayName(String displayName) {
            this.displayName = displayName;
        }

        /**
         * Set the artifact changelog
         * @param changelog The changelog
         */
        public void changelog(String changelog) {
            this.changelog = changelog;
        }
    }

    /**
     * Options related to publishing GitHub Releases
     */
    @Getter
    @Setter
    public class GithubConfig {

        /**
         * GitHub Release tag. Defaults to Version
         */
        private String tag = ModPublisherGradleExtension.this.version.getOrNull();

        /**
         * GitHub Repo. username/repo or URL
         * <p>
         * Overrides {@link ModPublisherGradleExtension#getGithubRepo() githubRepo}
         */
        private String repo = ModPublisherGradleExtension.this.githubRepo.getOrNull();

        /**
         * Create GitHub tag if missing
         */
        @ApiStatus.Experimental
        private boolean createTag = true;

        /**
         * Create GitHub release if missing
         */
        @ApiStatus.Experimental
        private boolean createRelease = true;

        /**
         * Update GitHub release if exists
         */
        @ApiStatus.Experimental
        private boolean updateRelease = true;
    }
}

