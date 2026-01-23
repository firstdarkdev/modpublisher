package com.hypherionmc.modpublisher.util.modtale;

import com.google.gson.JsonObject;
import com.hypherionmc.modpublisher.util.modtale.meta.ModtaleMetadata;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import me.hypherionmc.curseupload.CurseUploadApi;
import me.hypherionmc.curseupload.util.HTTPUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.gradle.api.logging.Logger;

import java.io.File;

@RequiredArgsConstructor(staticName = "of")
public class ModtaleApiClient {

    private final String apiKey;
    private final Logger logger;
    private static final String MODTALE_API = "https://api.modtale.net/api/v1/projects/%s/versions";

    @Setter @Getter
    private boolean debugMode = false;

    public void upload(String id, ModtaleMetadata metadata, File artifact) {
        final HttpClient client = HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                .setUserAgent("ModPublisher")
                .build();

        MultipartEntityBuilder requestBody = MultipartEntityBuilder.create().setMode(HttpMultipartMode.STRICT).setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

        requestBody.addPart(
                "file",
                new FileBody(artifact, ContentType.APPLICATION_OCTET_STREAM, artifact.getName())
        );

        requestBody.addTextBody(
                "versionNumber",
                metadata.getVersionNumber(),
                ContentType.TEXT_PLAIN
        );

        requestBody.addTextBody(
                "channel",
                metadata.getChannel(),
                ContentType.TEXT_PLAIN
        );

        for (String gv : metadata.getGameVersions()) {
            requestBody.addTextBody("gameVersions", gv, ContentType.TEXT_PLAIN);
        }

        requestBody.addTextBody(
                "changelog",
                metadata.getChangelog().isEmpty() ? "Coming Soon!" : metadata.getChangelog(),
                ContentType.TEXT_PLAIN
        );

        final HttpPost request = new HttpPost(String.format(MODTALE_API, id));
        request.addHeader("X-MODTALE-KEY", apiKey);
        request.setEntity(requestBody.build());

        if (!debugMode) {
            try {
                final HttpResponse response = client.execute(request);

                int status = response.getStatusLine().getStatusCode();

                String body = response.getEntity() != null
                        ? EntityUtils.toString(response.getEntity())
                        : "<no body>";

                if (status == 200) {
                    logger.lifecycle("Successfully uploaded artifact {}", artifact.getName());
                } else {
                    int errorCode = response.getStatusLine().getStatusCode();
                    String errorMessage = response.getStatusLine().getReasonPhrase();
                    logger.error("Failed to Upload artifact to Modtale. Code: {}, Error: {}, Output: {}", errorCode, errorMessage, body);
                }
            } catch (Exception e) {
                CurseUploadApi.INSTANCE.getLogger().error("Failed to Upload artifact to Modtale.", e);
            }
        } else {
            // Do not upload the file. Instead, write the JSON that will be sent to the console
            JsonObject object = new JsonObject();
            object.add("metadata", HTTPUtils.gson.toJsonTree(metadata));
            object.addProperty("file", artifact.getName());

           logger.lifecycle(HTTPUtils.gson.toJson(object));
        }
    }

}
