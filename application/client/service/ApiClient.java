package application.client.service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import application.client.dto.ApiModels.AdminUserView;
import application.client.dto.ApiModels.AuthResponse;
import application.client.dto.ApiModels.LessonDetails;
import application.client.dto.ApiModels.ProgressSummary;
import application.client.dto.ApiModels.QuizAttemptResult;
import application.client.dto.ApiModels.QuizQuestionView;
import application.client.dto.ApiModels.TopicSummary;
import application.client.dto.ApiModels.TopicTree;

@Component
public final class ApiClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private final ObjectMapper objectMapper;
    private volatile URI baseUri = URI.create("http://127.0.0.1:8080/api/");
    private volatile String token;
    private volatile AuthResponse currentSession;

    public ApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void setBaseUri(URI baseUri) {
        String value = baseUri.toString();
        this.baseUri = URI.create(value.endsWith("/") ? value : value + "/");
    }

    public URI getBaseUri() {
        return baseUri;
    }

    public void clearSession() {
        token = null;
        currentSession = null;
    }

    public CompletableFuture<AuthResponse> login(String username, String password) {
        return send("POST", "auth/login", Map.of("username", username, "password", password),
                new TypeReference<>() { });
    }

    public CompletableFuture<AuthResponse> register(
            String username, String displayName, String password, String role) {
        return send("POST", "auth/register", Map.of(
                "username", username,
                "displayName", displayName,
                "password", password,
                "role", role), new TypeReference<>() { });
    }

    public CompletableFuture<AuthResponse> googleLogin(String email, String displayName) {
        return send("POST", "auth/google", Map.of(
                "email", email,
                "displayName", displayName == null ? "" : displayName),
                new TypeReference<>() { });
    }

    public CompletableFuture<application.client.dto.ApiModels.AuthMessageResponse> forgotPassword(String emailOrUsername) {
        return send("POST", "auth/forgot-password", Map.of("emailOrUsername", emailOrUsername),
                new TypeReference<>() { });
    }

    public CompletableFuture<application.client.dto.ApiModels.AuthMessageResponse> resetPassword(
            String emailOrUsername, String code, String newPassword) {
        return send("POST", "auth/reset-password", Map.of(
                "emailOrUsername", emailOrUsername,
                "code", code,
                "newPassword", newPassword),
                new TypeReference<>() { });
    }

    public void useSession(AuthResponse response) {
        token = response.token();
        currentSession = response;
    }

    public AuthResponse getCurrentSession() {
        return currentSession;
    }

    public CompletableFuture<List<TopicSummary>> topics() {
        return send("GET", "topics", null, new TypeReference<>() { });
    }

    public CompletableFuture<TopicTree> topicTree(long topicId) {
        return send("GET", "topics/" + topicId + "/tree", null, new TypeReference<>() { });
    }

    public CompletableFuture<LessonDetails> lesson(long lessonId) {
        return send("GET", "lessons/" + lessonId, null, new TypeReference<>() { });
    }

    public CompletableFuture<Void> enroll(long topicId) {
        return send("POST", "enrollments/" + topicId, Map.of(), new TypeReference<>() { });
    }

    public CompletableFuture<ProgressSummary> progress() {
        return send("GET", "progress/me", null, new TypeReference<>() { });
    }

    public CompletableFuture<Void> updateCompletion(long lessonId, boolean completed) {
        return send("PUT", "progress/lessons/" + lessonId, Map.of("completed", completed),
                new TypeReference<>() { });
    }

    public CompletableFuture<List<QuizQuestionView>> quiz(long lessonId) {
        return send("GET", "quizzes/" + lessonId, null, new TypeReference<>() { });
    }

    public CompletableFuture<QuizAttemptResult> submitQuiz(long lessonId, List<Integer> answers) {
        return send("POST", "quizzes/" + lessonId + "/attempts", Map.of("answers", answers),
                new TypeReference<>() { });
    }

    public CompletableFuture<List<AdminUserView>> adminUsers() {
        return send("GET", "admin/users", null, new TypeReference<>() { });
    }

    public CompletableFuture<List<Map<String, Object>>> adminTopics() {
        return send("GET", "admin/content/topics", null, new TypeReference<>() { });
    }

    public CompletableFuture<Map<String, Object>> createAdminTopic(Map<String, Object> topic) {
        return send("POST", "admin/content/topics", topic, new TypeReference<>() { });
    }

    public CompletableFuture<Map<String, Object>> updateAdminTopic(long topicId, Map<String, Object> topic) {
        return send("PUT", "admin/content/topics/" + topicId, topic, new TypeReference<>() { });
    }

    public CompletableFuture<Void> deleteAdminTopic(long topicId) {
        return send("DELETE", "admin/content/topics/" + topicId, null, new TypeReference<>() { });
    }

    private <T> CompletableFuture<T> send(
            String method, String path, Object requestBody, TypeReference<T> responseType) {
        HttpRequest.Builder request = HttpRequest.newBuilder(baseUri.resolve(encodePath(path)))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json");
        if (token != null && !token.isBlank()) {
            request.header("Authorization", "Bearer " + token);
        }

        if (requestBody == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            try {
                request.header("Content-Type", "application/json");
                request.method(method, HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)));
            } catch (Exception exception) {
                return CompletableFuture.failedFuture(exception);
            }
        }

        return httpClient.sendAsync(request.build(), HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> decode(response, responseType));
    }

    private <T> T decode(HttpResponse<String> response, TypeReference<T> responseType) {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new CompletionException(new ApiException(response.statusCode(), errorMessage(response.body())));
        }
        if (response.body() == null || response.body().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(response.body(), responseType);
        } catch (Exception exception) {
            throw new CompletionException(new ApiException(
                    response.statusCode(), "The server returned an unreadable response.", exception));
        }
    }

    private String errorMessage(String body) {
        if (body == null || body.isBlank()) {
            return "The server could not complete the request.";
        }
        try {
            JsonNode error = objectMapper.readTree(body);
            if (error.hasNonNull("message")) {
                return error.get("message").asText();
            }
            if (error.hasNonNull("error")) {
                return error.get("error").asText();
            }
        } catch (Exception ignored) {
            // Fall through to the bounded response text.
        }
        return body.length() > 240 ? body.substring(0, 240) : body;
    }

    private String encodePath(String path) {
        return String.join("/", List.of(path.split("/", -1)).stream()
                .map(segment -> URLEncoder.encode(segment, StandardCharsets.UTF_8))
                .toList());
    }

    public static final class ApiException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private final int statusCode;

        public ApiException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public ApiException(int statusCode, String message, Throwable cause) {
            super(message, cause);
            this.statusCode = statusCode;
        }

        public int statusCode() {
            return statusCode;
        }
    }
}
