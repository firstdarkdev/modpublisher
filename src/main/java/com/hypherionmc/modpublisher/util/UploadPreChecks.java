/*
 * This file is part of modpublisher, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 HypherionSA and Contributors
 *
 */
package com.hypherionmc.modpublisher.util;

import com.hypherionmc.modpublisher.plugin.ModPublisherGradleExtension;
import com.hypherionmc.modpublisher.properties.Platform;
import com.hypherionmc.modpublisher.util.scanner.JarInfectionScanner;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.provider.Property;

import java.io.File;
import java.nio.file.*;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class UploadPreChecks {

    public static void checkRequiredValues(Project project, Platform platform, ModPublisherGradleExtension extension) throws Exception {
        // Check if API Keys are configured
        if (extension.getApiKeys() == null) {
            throw new Exception("Missing apiKeys config. Artifacts cannot be uploaded without this");
        }

        Object artifactObject = CommonUtil.getPlatformArtifact(platform, extension);

        if (artifactObject == null) {
            throw new Exception("Missing artifact. Cannot continue");
        }

        if (!extension.getGameVersions().isPresent() || extension.getGameVersions().get().isEmpty()) {
            throw new Exception("gameVersions is not defined. This is required");
        }

        if (!extension.getDisableMalwareScanner().get()) {
            JarInfectionScanner.scan(project, artifactObject);
        }
    }

    public static boolean canUploadCurse(Project project, ModPublisherGradleExtension extension) throws Exception {
        if (extension == null)
            return false;

        // Check that both the Curseforge API key and Project ID is defined
        if (extension.getApiKeys() != null && !extension.getApiKeys().getCurseforge().isEmpty()) {
            if (!extension.getCurseID().isPresent() || extension.getCurseID().get().isEmpty()) {
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

        if (StringUtils.isBlank(extension.getVersion().getOrNull())) {
            throw new Exception("Version is not defined. This is REQUIRED by modrinth");
        }

        // Check that both the Modrinth API key and Project ID is defined
        if (extension.getApiKeys() != null && !extension.getApiKeys().getModrinth().isEmpty()) {
            if (!extension.getModrinthID().isPresent() || extension.getModrinthID().get().isEmpty()) {
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

        if (StringUtils.isBlank(extension.getGithub().getTag())) {
            // tag defaults to version; if tag is missing, so is version
            throw new Exception("Neither Version or GitHub Tag are defined. At least one is REQUIRED by github");
        }

        if (!(extension.getGithub().isCreateRelease() || extension.getGithub().isUpdateRelease())) {
            throw new Exception("Github options createRelease and updateRelease are both disabled, at least one must be enabled");
        }

        if (extension.getApiKeys() != null && !extension.getApiKeys().getGithub().isEmpty()) {
            if (StringUtils.isBlank(extension.getGithub().getRepo())) {
                throw new Exception("Found GitHub token, but github repo is not defined");
            } else {
                return true;
            }
        }
        return false;
    }

    public static void checkEmptyJar(ModPublisherGradleExtension extension, File file, List<String> loaderVersions) throws Exception {
        if (extension.getDisableEmptyJarCheck().get())
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

    public static boolean isModrinthID(String testInput) {
        return Pattern.compile("^[0-9a-zA-Z]+$").matcher(testInput).matches();
    }
}
