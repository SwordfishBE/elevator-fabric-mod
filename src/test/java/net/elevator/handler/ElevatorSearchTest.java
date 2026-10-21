package net.elevator.handler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(5)
class ElevatorSearchTest {
    @Test
    void extremeUpwardDistanceOnlyVisitsBlocksInsideWorld() {
        var visited = new ArrayList<Integer>();
        assertTrue(ElevatorSearch.find(64, -64, 319, Integer.MAX_VALUE, true, y -> {
            visited.add(y);
            return false;
        }).isEmpty());
        assertEquals(255, visited.size());
        assertEquals(65, visited.getFirst());
        assertEquals(319, visited.getLast());
    }

    @Test
    void extremeDownwardDistanceStopsAtWorldBottom() {
        var visited = new ArrayList<Integer>();
        assertTrue(ElevatorSearch.find(64, -64, 319, Integer.MAX_VALUE, false, y -> {
            visited.add(y);
            return false;
        }).isEmpty());
        assertEquals(128, visited.size());
        assertEquals(63, visited.getFirst());
        assertEquals(-64, visited.getLast());
    }

    @Test
    void findsNearestMatchingDestinationInEachDirection() {
        assertEquals(OptionalInt.of(70), ElevatorSearch.find(64, -64, 319, 50, true,
            y -> y == 70 || y == 80));
        assertEquals(OptionalInt.of(60), ElevatorSearch.find(64, -64, 319, 50, false,
            y -> y == 50 || y == 60));
    }

    @Test
    void includesConfiguredDistanceAndSkipsDestinationsBeyondIt() {
        assertEquals(OptionalInt.of(114), ElevatorSearch.find(64, -64, 319, 50, true, y -> y == 114));
        assertTrue(ElevatorSearch.find(64, -64, 319, 50, true, y -> y == 115).isEmpty());
        assertEquals(OptionalInt.of(14), ElevatorSearch.find(64, -64, 319, 50, false, y -> y == 14));
        assertTrue(ElevatorSearch.find(64, -64, 319, 50, false, y -> y == 13).isEmpty());
    }

    @Test
    void worldEdgeDoesNotVisitOutOfBoundsOrCurrentPlatform() {
        assertTrue(ElevatorSearch.find(319, -64, 319, Integer.MAX_VALUE, true, y -> {
            fail("Must not search above world ceiling");
            return true;
        }).isEmpty());
        assertTrue(ElevatorSearch.find(-64, -64, 319, Integer.MAX_VALUE, false, y -> {
            fail("Must not search below world floor");
            return true;
        }).isEmpty());
    }

    @Test
    void arithmeticDoesNotWrapNearIntegerLimits() {
        var visited = new ArrayList<Integer>();
        ElevatorSearch.find(Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 5, Integer.MAX_VALUE,
            Integer.MAX_VALUE, true, y -> { visited.add(y); return false; });
        assertEquals(List.of(Integer.MAX_VALUE - 1, Integer.MAX_VALUE), visited);
        visited.clear();
        ElevatorSearch.find(Integer.MIN_VALUE + 2, Integer.MIN_VALUE, Integer.MIN_VALUE + 5,
            Integer.MAX_VALUE, false, y -> { visited.add(y); return false; });
        assertEquals(List.of(Integer.MIN_VALUE + 1, Integer.MIN_VALUE), visited);
    }
}
