/*
 * This file is part of modpublisher, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2023 HypherionSA and Contributors
 *
 */
package com.hypherionmc.modpublisher.util.changelogs;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChangelogUtil {

    private static final Set<String> allowedUrls = new HashSet<>();

    static {
        allowedUrls.add("https://gist.githubusercontent.com");
        allowedUrls.add("https://raw.githubusercontent.com");
        allowedUrls.add("https://paste.firstdark.dev/raw");
    }

    public static boolean isValidUploadSite(String url) {
        AtomicBoolean isValid = new AtomicBoolean(false);

        allowedUrls.forEach(v -> {
            if (url.startsWith(v) && !isValid.get()) {
                isValid.set(true);
            }
        });

        if (!isValid.get()) {
            System.err.println(url + " is an unsupported site");
            return false;
        }

        return true;
    }

    @Nullable
    public static String readFromUrl(String url) {
        if (!isValidUploadSite(url))
            return null;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(url)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String responseBody = response.body().string();

                if (responseBody != null && !responseBody.isEmpty()) {
                    return responseBody;
                }

                return null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return null;
    }
}
