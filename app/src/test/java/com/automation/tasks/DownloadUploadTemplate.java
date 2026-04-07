package com.automation.tasks;

import com.automation.utils.ApiConstants;
import com.automation.utils.ApiRequestBuilder;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class DownloadUploadTemplate implements Performable {

    public static DownloadUploadTemplate asCurrentUser() {
        return instrumented(DownloadUploadTemplate.class);
    }

    @Override
    @Step("{0} downloads the CSV upload template")
    public <T extends Actor> void performAs(T actor) {
        int status = ApiRequestBuilder.authenticated(actor)
                .when()
                .get(ApiConstants.BASE_URL + ApiConstants.UPLOAD_TEMPLATE)
                .statusCode();

        actor.remember(ApiConstants.KEY_LAST_STATUS, status);
    }
}
