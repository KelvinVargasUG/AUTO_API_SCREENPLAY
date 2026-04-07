package com.automation.tasks;

import com.automation.models.UploadSessionResponse;
import com.automation.utils.ApiConstants;
import com.automation.utils.ApiRequestBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

import java.util.Map;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class InitUploadSession implements Performable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String fileName;

    public InitUploadSession(String fileName) {
        this.fileName = fileName;
    }

    public static InitUploadSession withFileName(String fileName) {
        return instrumented(InitUploadSession.class, fileName);
    }

    public static InitUploadSession withGeneratedFileName() {
        return instrumented(InitUploadSession.class, "productos_" + System.currentTimeMillis() + ".csv");
    }

    @Override
    @Step("{0} initialises an upload session for file '{fileName}'")
    public <T extends Actor> void performAs(T actor) {
        String json;
        try {
            json = MAPPER.writeValueAsString(Map.of("fileName", fileName));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize init body", e);
        }

        io.restassured.response.Response response = ApiRequestBuilder.authenticatedJson(actor)
                .body(json)
                .when()
                .post(ApiConstants.BASE_URL + ApiConstants.UPLOAD_INIT);

        int status = response.getStatusCode();
        actor.remember(ApiConstants.KEY_LAST_STATUS, status);

        UploadSessionResponse session;
        try {
            session = MAPPER.readValue(response.getBody().asString(), UploadSessionResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize upload session response", e);
        }

        actor.remember(ApiConstants.KEY_UPLOAD_ID, session.getUploadId());
    }
}
