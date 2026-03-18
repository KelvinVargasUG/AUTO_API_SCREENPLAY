package com.automation.utils;

/**
 * Central registry of API endpoint templates and Actor memory key constants.
 * All endpoint paths use URI template syntax — callers are responsible for
 * substituting placeholders (e.g. {orderId}, {station}, {taskId}).
 */
public final class ApiConstants {

    private ApiConstants() {
        // utility class — no instances
    }

    // -------------------------------------------------------------------------
    // Base URL
    // -------------------------------------------------------------------------
    public static final String BASE_URL = "http://localhost:8080";

    // -------------------------------------------------------------------------
    // Auth endpoints
    // -------------------------------------------------------------------------
    public static final String AUTH_REGISTER = "/api/auth/register";
    public static final String AUTH_LOGIN    = "/api/auth/login";

    // -------------------------------------------------------------------------
    // Order endpoints
    // -------------------------------------------------------------------------
    public static final String ORDERS        = "/api/orders";
    public static final String ORDERS_STATUS = "/api/orders/{orderId}/status";

    // -------------------------------------------------------------------------
    // Task endpoints
    // -------------------------------------------------------------------------
    public static final String TASKS_STATION = "/api/tasks/station/{station}";
    public static final String TASKS_START   = "/api/tasks/{taskId}/start";

    // -------------------------------------------------------------------------
    // Actor memory keys (used with actor.remember / actor.recall)
    // -------------------------------------------------------------------------
    public static final String KEY_TOKEN        = "authToken";
    public static final String KEY_TABLE_NUMBER = "tableNumber";
    public static final String KEY_ORDER_ID     = "orderId";
    public static final String KEY_TASK_ID      = "taskId";
    public static final String KEY_EMAIL        = "email";
    public static final String KEY_PASSWORD     = "password";
    public static final String KEY_LAST_STATUS  = "lastStatusCode";
}
