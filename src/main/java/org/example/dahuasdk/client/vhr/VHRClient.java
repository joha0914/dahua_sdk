package org.example.dahuasdk.client.vhr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.dahuasdk.client.vhr.entity.load.Commands;
import org.example.dahuasdk.client.vhr.entity.load.FaceImage;
import org.example.dahuasdk.client.vhr.entity.load.Photo;
import org.example.dahuasdk.client.vhr.entity.save.CommandsResult;
import org.example.dahuasdk.config.VhrProperties;
import org.example.dahuasdk.dto.EventDTO;
import org.example.dahuasdk.entity.Middleware;
import okhttp3.*;
import org.example.dahuasdk.schedule.ApplicationScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@RequiredArgsConstructor
@Component

public class VHRClient {
    private static final Logger log = LoggerFactory.getLogger(VHRClient.class);
    private final MediaType JSON = MediaType.get("application/json");
    private final OkHttpClient vhrHttpClient;
    private final VhrProperties properties;
    private final ObjectMapper objectMapper;

    private RequestBody createRequestBody(Object body) throws JsonProcessingException {
        return RequestBody.create(objectMapper.writeValueAsString(body), JSON);
    }

    record LoadDeviceRequestBody(long device_id) {
    }

    public Commands loadCommands(Middleware middleware, long deviceId) {
        try {
            RequestBody body = createRequestBody(new LoadDeviceRequestBody(deviceId));

            Request request = new Request.Builder()
                    .url(properties.loadCommandsUrl(middleware.getHost()))
                    .header("Authorization", "Basic " + middleware.getCredentials())
                    .header(properties.middlewareRequestHeaderName(), middleware.getToken())
                    .post(body)
                    .build();

            try (Response response = vhrHttpClient.newCall(request).execute()) {
                assert response.body() != null;

                if (!response.isSuccessful())
                    log.error("Error occurred while loading commands from VHR. middlewareId: {}, host: {}, code: {}, message: {}",
                            middleware.getId(), middleware.getHost(), response.code(), response.body().string());

                return objectMapper.readValue(response.body().string(), Commands.class);
            }
        } catch (Exception e) {
            log.error("Error occurred while loading commands from VHR. middlewareId: {}, host: {}", middleware.getId(), middleware.getHost(), e);
        }

        return new Commands();
    }

    public void saveCommands(Middleware middleware, CommandsResult commandsResult) {
        try {
            RequestBody body = createRequestBody(commandsResult);

            Request request = new Request.Builder()
                    .url(properties.saveCommandsResultUrl(middleware.getHost()))
                    .header("Authorization", "Basic " + Base64.getEncoder()
                            .encodeToString(("admin@head:greenwhite").getBytes(StandardCharsets.UTF_8)))
                    .header(properties.middlewareRequestHeaderName(), middleware.getToken())
                    .post(body)
                    .build();

            try (Response response = vhrHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    log.error("Error occurred while saving commands to VHR. middlewareId: {}, host: {}, code: {}, message: {}",
                            middleware.getId(), middleware.getHost(), response.code(), responseBody);
                }
            }
        } catch (Exception e) {
            log.error("Error occurred while saving commands to VHR. middlewareId: {}, host: {}", middleware.getId(), middleware.getHost(), e);
        }
    }

    public void sendDeviceStatuses(Middleware middleware, ApplicationScheduler.DeviceStatuses deviceStatuses) {
        try {
            RequestBody body = createRequestBody(deviceStatuses);

            Request request = new Request.Builder()
                    .url(properties.updateDevicesStatusUri(middleware.getHost()))
                    .header("Authorization", "Basic " + middleware.getCredentials())
                    .header(properties.middlewareRequestHeaderName(), middleware.getToken())
                    .post(body)
                    .build();

            try (Response response = vhrHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    assert response.body() != null;
                    log.warn("Device statuses request to VHR failed. middlewareId: {}, host: {}, code: {}, message: {}",
                            middleware.getId(), middleware.getHost(), response.code(), response.body().string());
                }
            }
        } catch (Exception e) {
            log.error("Error occurred while making device status update request to VHR. middlewareId: {}, host: {}", middleware.getId(), middleware.getHost(), e);
        }
    }

    public void sendHealthCheck(Middleware middleware) {
        try {
            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("middleware", Map.of("token", middleware.getToken()));

            RequestBody body = createRequestBody(bodyMap);

            Request request = new Request.Builder()
                    .url(properties.middlewareHealthCheckUri(middleware.getHost()))
                    .header("Authorization", "Basic " + middleware.getCredentials())
                    .header(properties.middlewareRequestHeaderName(), middleware.getToken())
                    .post(body)
                    .build();

            try (Response response = vhrHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    assert response.body() != null;
                    log.warn("Health check request to VHR failed. middlewareId: {}, host: {}, code: {}, message: {}",
                            middleware.getId(), middleware.getHost(), response.code(), response.body().string());
                }
            }
        } catch (Exception e) {
            log.error("Error occurred while making health check request to VHR. middlewareId: {}, host: {}", middleware.getId(), middleware.getHost(), e);
        }
    }

    public Photo loadPhoto(Middleware middleware, FaceImage faceImage) {
        try {
            Request request = new Request.Builder()
                    .url(properties.loadPhotoUri(middleware.getHost(), faceImage.getPhotoSha(), faceImage.getWidth(), faceImage.getHeight(), faceImage.getFormat(), faceImage.getQuality()))
                    .header("Authorization", "Basic " + middleware.getCredentials())
                    .header(properties.middlewareRequestHeaderName(), middleware.getToken())
                    .get()
                    .build();

            try (Response response = vhrHttpClient.newCall(request).execute()) {
                assert response.body() != null;

                if (!response.isSuccessful())
                    log.error("Error occurred while loading photo from VHR. middlewareId: {}, host: {}, sha: {}, code: {}, message: {}",
                            middleware.getId(), middleware.getHost(), faceImage.getPhotoSha(), response.code(), response.body().string());
                else
                    return new Photo(extractFilename(response.header("Content-Disposition")), response.body().bytes());
            }
        } catch (Exception e) {
            log.error("Error occurred while loading photo from VHR. middlewareId: {}, host: {}, sha: {}", middleware.getId(), middleware.getHost(), faceImage.getPhotoSha(), e);
        }

        return null;
    }

    private String extractFilename(String contentDisposition) {
        try {
            return contentDisposition.substring(contentDisposition.indexOf("filename=") + 10, contentDisposition.length() - 1);
        } catch (Exception e) {
            return "photo.jpeg";
        }
    }

    public void sendEvents(Middleware middleware, List<EventDTO> events) {
        try {
            Map<String, Object> bodyMap = new HashMap<>();
            bodyMap.put("middleware", Map.of("token", middleware.getToken()));

            RequestBody body = createRequestBody(bodyMap);

            Request request = new Request.Builder()
                    .url(properties.saveDeviceEventsUri(middleware.getHost()))
                    .header("Authorization", "Basic " + middleware.getCredentials())
                    .header(properties.middlewareRequestHeaderName(), middleware.getToken())
                    .post(body)
                    .build();

            try (Response response = vhrHttpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    assert response.body() != null;
                    log.warn("Send device event request to VHR failed. middlewareId: {}, host: {}, code: {}, message: {}",
                            middleware.getId(), middleware.getHost(), response.code(), response.body().string());
                }
            }
        } catch (Exception e) {
            log.error("Error occurred while sending events to VHR. middlewareId: {}, host: {}", middleware.getId(), middleware.getHost(), e);
        }
    }
}
