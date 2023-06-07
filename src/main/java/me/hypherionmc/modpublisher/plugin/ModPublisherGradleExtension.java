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

import groovy.lang.Closure;

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

    /**
     * Configure API Keys for this Project
     */
    public ApiKeys apiKeys(Closure<ApiKeys> closure) {
        apiKeys = new ApiKeys();
        ModPublisherPlugin.project.configure(apiKeys, closure);
        return apiKeys;
    }

    /**
     * Configure Curseforge Dependencies for this project
     */
    public Dependencies curseDepends(Closure<Dependencies> closure) {
        curseDepends = new Dependencies();
        ModPublisherPlugin.project.configure(curseDepends, closure);
        return curseDepends;
    }

    /**
     * Configure Modrinth dependencies for this project
     */
    public Dependencies modrinthDepends(Closure<Dependencies> closure) {
        modrinthDepends = new Dependencies();
        ModPublisherPlugin.project.configure(modrinthDepends, closure);
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
