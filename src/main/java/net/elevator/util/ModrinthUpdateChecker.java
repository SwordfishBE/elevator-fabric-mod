package net.elevator.util;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.elevator.ElevatorMod;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ModrinthUpdateChecker {

    private static final String PROJECT_ID = "s5gP8ABG";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .build();
    private static final AtomicBoolean CHECK_STARTED = new AtomicBoolean(false);

    private ModrinthUpdateChecker() {
    }

    public static void checkOnceAsync() {
        if (!CHECK_STARTED.compareAndSet(false, true)) {
            return;
        }

        Thread thread = new Thread(ModrinthUpdateChecker::checkForUpdate, "elevator-modrinth-update-check");
        thread.setDaemon(true);
        thread.start();
    }

    private static void checkForUpdate() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.modrinth.com/v2/project/" + PROJECT_ID + "/version"))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("User-Agent", ElevatorMod.modName() + "/" + currentVersion())
                .GET()
                .build();

        try {
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                ElevatorMod.LOGGER.debug("{} Update check returned HTTP {}.", ElevatorMod.logPrefix(), response.statusCode());
                return;
            }

            Optional<String> latestVersion = extractLatestVersion(response.body());
            if (latestVersion.isEmpty()) {
                ElevatorMod.LOGGER.debug("{} Update check returned no usable versions.", ElevatorMod.logPrefix());
                return;
            }

            String currentVersion = currentVersion();
            String newestVersion = latestVersion.get();
            if (isNewerVersion(newestVersion, currentVersion)) {
                ElevatorMod.LOGGER.info("{} New version available: {} (current: {})",
                        ElevatorMod.logPrefix(),
                        newestVersion, currentVersion);
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            ElevatorMod.LOGGER.debug("{} Update check failed.", ElevatorMod.logPrefix(), e);
        }
    }

    private static Optional<String> extractLatestVersion(String responseBody) {
        JsonElement root = JsonParser.parseString(responseBody);
        if (!root.isJsonArray()) {
            return Optional.empty();
        }

        JsonArray versions = root.getAsJsonArray();
        VersionCandidate newestCompatible = null;
        VersionCandidate newestRelease = null;
        String currentMinecraftVersion = currentMinecraftVersion();

        for (JsonElement versionElement : versions) {
            if (!versionElement.isJsonObject()) {
                continue;
            }

            JsonObject versionObject = versionElement.getAsJsonObject();
            String versionNumber = getString(versionObject, "version_number");
            Instant publishedAt = getPublishedAt(versionObject);
            if (!isValidVersionNumber(versionNumber) || publishedAt == null) {
                continue;
            }

            String versionType = getString(versionObject, "version_type");
            if (!"release".equalsIgnoreCase(versionType)) {
                continue;
            }

            VersionCandidate candidate = new VersionCandidate(versionNumber, publishedAt);
            if (isNewerCandidate(candidate, newestRelease)) {
                newestRelease = candidate;
            }

            if (jsonArrayContains(versionObject, "loaders", "fabric")
                    && jsonArrayContains(versionObject, "game_versions", currentMinecraftVersion)
                    && isNewerCandidate(candidate, newestCompatible)) {
                newestCompatible = candidate;
            }
        }

        return Optional.ofNullable(newestCompatible != null ? newestCompatible : newestRelease)
                .map(VersionCandidate::versionNumber);
    }

    private static String getString(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || value.isJsonNull()) {
            return null;
        }

        return value.getAsString();
    }

    private static boolean jsonArrayContains(JsonObject object, String key, String expectedValue) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonArray()) {
            return false;
        }

        for (JsonElement element : value.getAsJsonArray()) {
            if (element != null && element.isJsonPrimitive() && expectedValue.equalsIgnoreCase(element.getAsString())) {
                return true;
            }
        }

        return false;
    }

    private static Instant getPublishedAt(JsonObject object) {
        String publishedAt = getString(object, "date_published");
        if (publishedAt == null || publishedAt.isBlank()) {
            return null;
        }

        try {
            return Instant.parse(publishedAt);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean isValidVersionNumber(String versionNumber) {
        if (versionNumber == null || versionNumber.isBlank()) {
            return false;
        }

        try {
            Version.parse(versionNumber);
            return true;
        } catch (VersionParsingException ignored) {
            return false;
        }
    }

    private static boolean isNewerCandidate(VersionCandidate candidate, VersionCandidate currentBest) {
        return currentBest == null || candidate.publishedAt().isAfter(currentBest.publishedAt());
    }

    private static String currentVersion() {
        return FabricLoader.getInstance()
                .getModContainer(ElevatorMod.MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse(ElevatorMod.modVersion());
    }

    private static String currentMinecraftVersion() {
        return FabricLoader.getInstance()
                .getModContainer("minecraft")
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }

    private static boolean isNewerVersion(String candidate, String current) {
        try {
            Version candidateVersion = Version.parse(candidate);
            Version currentVersion = Version.parse(current);
            return candidateVersion.compareTo(currentVersion) > 0;
        } catch (VersionParsingException e) {
            ElevatorMod.LOGGER.debug("{} Could not compare versions. candidate='{}', current='{}'.",
                    ElevatorMod.logPrefix(), candidate, current, e);
            return false;
        }
    }

    private record VersionCandidate(String versionNumber, Instant publishedAt) {
    }
}
