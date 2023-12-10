/*
 * This file is part of modpublisher, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 HypherionSA and Contributors
 *
 */
package com.hypherionmc.modpublisher.util;

import com.hypherionmc.modpublisher.plugin.ModPublisherGradleExtension;
import com.hypherionmc.modpublisher.util.scanner.JarInfectionScanner;
import org.gradle.api.GradleException;
import org.gradle.api.Project;

import java.io.File;
import java.nio.file.*;
import java.util.List;

public class UploadPreChecks {

    public static void checkRequiredValues(Project project, ModPublisherGradleExtension extension) throws Exception {
        // Check if API Keys are configured
        if (extension.apiKeys == null) {
            throw new Exception("Missing apiKeys config. Artifacts cannot be uploaded without this");
        }

        if (extension.artifact == null) {
            throw new Exception("Missing artifact. Cannot continue");
        }

        if (extension.gameVersions.isEmpty()) {
            throw new Exception("gameVersions is not defined. This is required");
        }

        if (!extension.disableMalwareScanner) {
            JarInfectionScanner.scan(project, extension.artifact);
        }
    }

    public static void checkVersion(Project project, ModPublisherGradleExtension extension) throws Exception {
        if (extension.version == null || extension.version.isEmpty()) {
            throw new Exception("Version is not defined. This is REQUIRED by modrinth/github");
        }
    }

    public static boolean canUploadCurse(Project project, ModPublisherGradleExtension extension) throws Exception {
        if (extension == null)
            return false;

        // Check that both the Curseforge API key and Project ID is defined
        if (extension.apiKeys.curseforge != null && !extension.apiKeys.curseforge.isEmpty()) {
            if (extension.curseID == null || extension.curseID.isEmpty()) {
                throw new Exception("Found Curseforge API token, but curseID is not defined");
            } else {
                return true;
            }
        }
        return false;
    }

    public static boolean canUploadModrinth(Project project, ModPublisherGradleExtension extension) throws Exception {
        if (extension == null)
            return false;

        checkVersion(project, extension);
        // Check that both the Modrinth API key and Project ID is defined
        if (extension.apiKeys.modrinth != null && !extension.apiKeys.modrinth.isEmpty()) {
            if (extension.modrinthID == null || extension.modrinthID.isEmpty()) {
                throw new Exception("Found Modrinth API token, but modrinthID is not defined");
            } else {
                return true;
            }
        }
        return false;
    }

    public static boolean canUploadGitHub(Project project, ModPublisherGradleExtension extension) throws Exception {
        if (extension == null)
            return false;

        checkVersion(project, extension);
        if (extension.apiKeys.github != null && !extension.apiKeys.github.isEmpty()) {
            if (extension.githubRepo == null || extension.githubRepo.isEmpty()) {
                throw new Exception("Found GitHub token, but githubRepo is not defined");
            } else {
                return true;
            }
        }
        return false;
    }

    public static void checkEmptyJar(ModPublisherGradleExtension extension, File file, List<String> loaderVersions) throws Exception {
        if (extension.disableEmptyJarCheck)
            return;

        if (loaderVersions.isEmpty())
            return;

        FileSystem system = FileSystems.newFileSystem(Paths.get(file.getPath()), null);
        Path quiltJson = system.getPath("quilt.mod.json");
        Path fabricJson = system.getPath("fabric.mod.json");
        Path forgeToml = system.getPath("META-INF/mods.toml");
        Path forgeMc = system.getPath("mcmod.info");

        if (loaderVersions.contains("forge") || loaderVersions.contains("neoforge")) {
            // Check for either mods.toml or mcmod.info (for older version support)
            if (!Files.exists(forgeToml) && !Files.exists(forgeMc))
                throw new GradleException("File marked as forge/neoforge, but no mods.toml or mcmod.info file was found");
        }

        if (loaderVersions.contains("fabric")) {
            if (!Files.exists(fabricJson))
                throw new GradleException("File marked as fabric, but no fabric.mod.json file was found");
        }

        if (loaderVersions.contains("quilt")) {
            // Fabric mods can run on quilt, so we check for either of the files to be present
            if (!Files.exists(quiltJson) && !Files.exists(fabricJson))
                throw new GradleException("File marked as quilt, but no quilt.mod.json OR fabric.mod.json file was found");
        }

        system.close();
    }
}
