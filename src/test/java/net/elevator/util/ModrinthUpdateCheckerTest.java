package net.elevator.util;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModrinthUpdateCheckerTest {
    @Test
    void matchesSnapshotGameIdAndIgnoresNewerIncompatibleReleases() {
        String response = """
            [
              {"version_number":"1.2.1","version_type":"release","date_published":"2026-10-31T00:00:00Z",
               "loaders":["fabric"],"game_versions":["26.4-snapshot-6"]},
              {"version_number":"1.3.0","version_type":"release","date_published":"2026-11-01T00:00:00Z",
               "loaders":["fabric"],"game_versions":["26.4-alpha.6"]},
              {"version_number":"1.4.0","version_type":"release","date_published":"2026-11-02T00:00:00Z",
               "loaders":["neoforge"],"game_versions":["26.4-snapshot-6"]}
            ]
            """;
        assertEquals(Optional.of("1.2.1"),
            ModrinthUpdateChecker.extractLatestVersion(response, "26.4-snapshot-6"));
    }

    @Test
    void doesNotFallBackToAnotherMinecraftVersion() {
        String response = """
            [{"version_number":"1.2.1","version_type":"release","date_published":"2026-10-31T00:00:00Z",
              "loaders":["fabric"],"game_versions":["26.3"]}]
            """;
        assertTrue(ModrinthUpdateChecker.extractLatestVersion(response, "26.4-snapshot-6").isEmpty());
    }

    @Test
    void stillMatchesStableGameVersions() {
        String response = """
            [{"version_number":"1.2.1","version_type":"release","date_published":"2026-10-31T00:00:00Z",
              "loaders":["fabric"],"game_versions":["26.4"]}]
            """;
        assertEquals(Optional.of("1.2.1"), ModrinthUpdateChecker.extractLatestVersion(response, "26.4"));
    }
}
