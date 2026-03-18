package com.automation.utils;

/**
 * Generates unique test data values for each test run using System.currentTimeMillis()
 * as the uniqueness seed.  All methods are static — the class is not instantiable.
 *
 * tableNumber uses (millis % 9000) + 1000 to stay in the range 1000–9999,
 * which is the correlation key linking POST /api/orders to
 * GET /api/tasks/station/{station} responses.
 */
public final class TestDataGenerator {

    private TestDataGenerator() {
        // utility class — no instances
    }

    /**
     * Returns a unique e-mail address of the form {@code qa_auto_<timestamp>@test.com}.
     * Each call ≥1 ms apart is guaranteed to return a different value.
     */
    public static String generateUniqueEmail() {
        return "qa_auto_" + System.currentTimeMillis() + "@test.com";
    }

    /**
     * Returns a unique username of the form {@code qa_auto_<timestamp>}.
     * Each call ≥1 ms apart is guaranteed to return a different value.
     */
    public static String generateUniqueUsername() {
        return "qa_auto_" + System.currentTimeMillis();
    }

    /**
     * Returns a unique table number in the inclusive range [1000, 9999].
     * <p>
     * Formula: {@code (int)(System.currentTimeMillis() % 9000) + 1000}
     * <ul>
     *   <li>millis % 9000 → [0, 8999]</li>
     *   <li>+ 1000        → [1000, 9999]</li>
     * </ul>
     */
    public static int generateUniqueTableNumber() {
        return (int) (System.currentTimeMillis() % 9000) + 1000;
    }
}
