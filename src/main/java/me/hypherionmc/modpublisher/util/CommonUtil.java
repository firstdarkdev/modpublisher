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
package me.hypherionmc.modpublisher.util;

import groovy.lang.Closure;
import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;
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

        if (obj instanceof File) {
            return (File) obj;
        }

        if (obj instanceof AbstractArchiveTask) {
            return ((AbstractArchiveTask)obj).getArchiveFile().get().getAsFile();
        }
        return project.file(obj);
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

        if (obj instanceof String)
            return (String) obj;

        if (obj instanceof File) {
            return FileUtils.readFileToString((File) obj, StandardCharsets.UTF_8);
        }

        if (obj instanceof AbstractArchiveTask) {
            return FileUtils.readFileToString(((AbstractArchiveTask)obj).getArchiveFile().get().getAsFile(), StandardCharsets.UTF_8);
        }

        return obj.toString();
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
