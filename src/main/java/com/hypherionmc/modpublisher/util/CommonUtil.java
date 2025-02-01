/*
 * This file is part of modpublisher, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 HypherionSA and Contributors
 *
 */
package com.hypherionmc.modpublisher.util;

import com.hypherionmc.modpublisher.plugin.ModPublisherGradleExtension;
import com.hypherionmc.modpublisher.properties.Platform;
import com.hypherionmc.modpublisher.util.changelogs.ChangelogUtil;
import groovy.lang.Closure;
import org.apache.commons.io.FileUtils;
import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.bundling.AbstractArchiveTask;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author HypherionSA
 * Gradle Utility class
 */
public class CommonUtil {

    /**
     * Try to convert an OBJECT to a file
     */
    public static File resolveFile(Project project, Object obj) {
        if (obj == null) {
            throw new NullPointerException("Null Path");
        }

        if (obj instanceof Provider) {
            Provider<?> p = (Provider<?>) obj;
            obj = p.get();
        }

        if (obj instanceof File) {
            return (File) obj;
        }

        if (obj instanceof AbstractArchiveTask) {
            return ((AbstractArchiveTask)obj).getArchiveFile().get().getAsFile();
        }
        return project.file(obj);
    }

    /**
     * Resolve an override artifact for a specific platform
     * @param platform The {@link Platform} the override is for
     * @param extension The configured gradle extension
     * @return The override artifact, or default artifact
     */
    public static Object getPlatformArtifact(Platform platform, ModPublisherGradleExtension extension) {
        if (!extension.getArtifacts().isEmpty() && extension.getArtifacts().containsKey(platform.toString().toLowerCase())) {
            return extension.getArtifacts().get(platform.toString().toLowerCase());
        }

        if (!extension.getArtifact().isPresent()) {
            throw new GradleException("No artifacts set");
        }

        return extension.getArtifact().get();
    }

    /**
     * Try to convert an OBJECT to a String
     */
    public static String resolveString(Object obj) throws IOException {
        if (obj == null)
            throw new NullPointerException();

        while (obj instanceof Closure<?>) {
            obj = ((Closure<?>)obj).call();
        }

        if (obj instanceof String) {
            String val = (String) obj;

            if (val.startsWith("http://") || val.startsWith("https://")) {
                val = ChangelogUtil.readFromUrl(val);
            }

            if (val == null)
                return (String) obj;

            return val;
        }

        if (obj instanceof File) {
            return FileUtils.readFileToString((File) obj, StandardCharsets.UTF_8);
        }

        if (obj instanceof AbstractArchiveTask) {
            return FileUtils.readFileToString(((AbstractArchiveTask)obj).getArchiveFile().get().getAsFile(), StandardCharsets.UTF_8);
        }

        return obj.toString();
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Cleanup GitHub URLS for use with hub4j
     * @param url The URL as passed to the config
     * @return Just the username/repo section of the URL
     */
    public static String cleanGithubUrl(String url) {
        url = url.replace("https://github.com/", "");
        url = url.replace("http://github.com/", "");
        url = url.replace("git@github.com:", "");
        url = url.replace(".git", "");

        if (url.endsWith("/")) {
            url = url.substring(0, url.length() -1 );
        }

        return url;
    }
}
