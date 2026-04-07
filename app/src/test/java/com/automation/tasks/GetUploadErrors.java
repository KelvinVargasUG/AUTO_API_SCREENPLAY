package com.automation.tasks;

import com.automation.utils.ApiConstants;
import com.automation.utils.ApiRequestBuilder;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class GetUploadErrors implements Performable {

    public static GetUploadErrors forCurrentSession() {
        return instrumented(GetUploadErrors.class);
    }

    @Override
    @Step("{0} retrieves the error report for the current upload session")
    public <T extends Actor> void performAs(T actor) {
        String uploadId = actor.recall(ApiConstants.KEY_UPLOAD_ID);

        String url = ApiConstants.BASE_URL
                + ApiConstants.UPLOAD_ERRORS.replace("{uploadId}", uploadId);

        int status = ApiRequestBuilder.authenticated(actor)
                .when()
                .get(url)
                .statusCode();

        actor.remember(ApiConstants.KEY_LAST_STATUS, status);
    }
}
