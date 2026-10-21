package net.elevator.handler;

import java.util.OptionalInt;
import java.util.function.IntPredicate;

final class ElevatorSearch {
    private ElevatorSearch() {
    }

    static OptionalInt find(int currentY, int minY, int maxY, int maxDistance,
                            boolean upward, IntPredicate matches) {
        if (maxDistance < 1) {
            return OptionalInt.empty();
        }
        // Use long arithmetic so even extreme configured distances cannot wrap.
        long start = upward ? Math.max((long) currentY + 1, minY) : Math.min((long) currentY - 1, maxY);
        long end = upward ? Math.min((long) currentY + maxDistance, maxY)
                          : Math.max((long) currentY - maxDistance, minY);
        for (long y = start; upward ? y <= end : y >= end; y += upward ? 1 : -1) {
            if (matches.test((int) y)) {
                return OptionalInt.of((int) y);
            }
        }
        return OptionalInt.empty();
    }
}
