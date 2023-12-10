/*
 * This file is part of modpublisher, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 HypherionSA and Contributors
 *
 */
package com.hypherionmc.modpublisher.tasks;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

/**
 * @author HypherionSA
 * The main Upload Task. This will check that all required values are present
 * and execute the appropriate upload sub-task
 */
public class UploadModTask extends DefaultTask {

    @TaskAction
    void uploadArtifacts() throws Exception {
        getProject().getLogger().lifecycle("Published mod to all configured platforms");
    }

}
