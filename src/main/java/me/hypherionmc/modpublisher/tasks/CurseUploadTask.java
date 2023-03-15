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
package me.hypherionmc.modpublisher.tasks;

import me.hypherionmc.curseupload.CurseUploadApi;
import me.hypherionmc.curseupload.constants.CurseChangelogType;
import me.hypherionmc.curseupload.constants.CurseReleaseType;
import me.hypherionmc.curseupload.requests.CurseArtifact;
import me.hypherionmc.modpublisher.util.CommonUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static me.hypherionmc.modpublisher.plugin.ModPublisherPlugin.extension;
import static me.hypherionmc.modpublisher.plugin.ModPublisherPlugin.project;

/**
 * @author HypherionSA
 * Sub-Task to handle Curseforge publishing. This task will only be executed if
 * a Curseforge API Key and Project ID is supplied
 */
public class CurseUploadTask {

    // Instance of CurseUpload4J to use
    private final CurseUploadApi uploadApi;

    public CurseUploadTask() {
        // Create the API Client and pass the Gradle logger as logger
        uploadApi = new CurseUploadApi(extension.apiKeys.curseforge, project.getLogger());

        // Enable debug mode if required
        uploadApi.setDebug(extension.debug);
    }

    /**
     * Configure the upload and upload it
     */
    public void upload() throws IOException {
        project.getLogger().lifecycle("Uploading to Curseforge");
        File uploadFile = CommonUtil.resolveFile(project, extension.artifact);

        if (uploadFile == null || !uploadFile.exists())
            throw new FileNotFoundException("Cannot find file " + extension.artifact.toString());

        CurseArtifact artifact = new CurseArtifact(uploadFile, Long.parseLong(extension.curseID));
        artifact.changelog(CommonUtil.resolveString(extension.changelog));
        artifact.changelogType(CurseChangelogType.MARKDOWN);
        artifact.releaseType(CurseReleaseType.valueOf(extension.versionType.toUpperCase()));

        extension.gameVersions.forEach(artifact::addGameVersion);
        extension.loaders.forEach(artifact::modLoader);

        if (extension.displayName != null && !extension.displayName.isEmpty()) {
            artifact.displayName(extension.displayName);
        } else {
            artifact.displayName(extension.version);
        }

        if (extension.curseDepends != null) {
            extension.curseDepends.required.forEach(artifact::requirement);
            extension.curseDepends.optional.forEach(artifact::optional);
            extension.curseDepends.incompatible.forEach(artifact::incompatibility);
            extension.curseDepends.embedded.forEach(artifact::embedded);
        }

        // If debug mode is enabled, this will only log the JSON that will be sent and
        // will not actually upload the file
        uploadApi.upload(artifact);
    }

}
