package com.automation.tasks;

import com.automation.utils.ApiConstants;
import com.automation.utils.ApiRequestBuilder;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class CompleteUpload implements Performable {

    public static CompleteUpload forCurrentSession() {
        return instrumented(CompleteUpload.class);
    }

    @Override
    @Step("{0} completes the upload session and triggers CSV processing")
    public <T extends Actor> void performAs(T actor) {
        String uploadId = actor.recall(ApiConstants.KEY_UPLOAD_ID);

        String url = ApiConstants.BASE_URL
                + ApiConstants.UPLOAD_COMPLETE.replace("{uploadId}", uploadId);

        io.restassured.response.Response response = ApiRequestBuilder.authenticatedJson(actor)
                .body("{}")
                .when()
                .post(url);

        int status = response.getStatusCode();
        actor.remember(ApiConstants.KEY_LAST_STATUS, status);
    }
}
