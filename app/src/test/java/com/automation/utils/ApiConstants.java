package com.automation.utils;

public final class ApiConstants {

    private ApiConstants() {

    }

    public static final String BASE_URL = "http://localhost:8080";

    public static final String AUTH_REGISTER = "/api/auth/register";
    public static final String AUTH_LOGIN    = "/api/auth/login";

    public static final String ORDERS        = "/api/orders";
    public static final String ORDERS_STATUS = "/api/orders/{orderId}/status";

    public static final String TASKS_STATION = "/api/tasks/station/{station}";
    public static final String TASKS_START   = "/api/tasks/{taskId}/start";

    public static final String PRODUCTS            = "/api/products";
    public static final String PRODUCT_UPDATE      = "/api/products/{productId}";
    public static final String PRODUCT_DEACTIVATE  = "/api/products/{productId}/deactivate";
    public static final String PRODUCT_ACTIVATE    = "/api/products/{productId}/activate";

    public static final String UPLOAD_INIT     = "/api/upload/init";
    public static final String UPLOAD_CHUNK    = "/api/upload/{uploadId}/chunk";
    public static final String UPLOAD_COMPLETE = "/api/upload/{uploadId}/complete";
    public static final String UPLOAD_STATUS   = "/api/upload/{uploadId}/status";
    public static final String UPLOAD_TEMPLATE = "/api/upload/template";
    public static final String UPLOAD_ERRORS   = "/api/upload/{uploadId}/errors";

    public static final String KEY_TOKEN        = "authToken";
    public static final String KEY_TABLE_NUMBER = "tableNumber";
    public static final String KEY_ORDER_ID     = "orderId";
    public static final String KEY_TASK_ID      = "taskId";
    public static final String KEY_EMAIL        = "email";
    public static final String KEY_PASSWORD     = "password";
    public static final String KEY_LAST_STATUS  = "lastStatusCode";
    public static final String KEY_PRODUCT_ID   = "productId";
    public static final String KEY_UPLOAD_ID    = "uploadId";
}
