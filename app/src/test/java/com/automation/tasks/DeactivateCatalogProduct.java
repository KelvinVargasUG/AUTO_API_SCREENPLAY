package com.automation.tasks;

import com.automation.utils.ApiConstants;
import com.automation.utils.ApiRequestBuilder;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class DeactivateCatalogProduct implements Performable {

    public static DeactivateCatalogProduct forCurrentActor() {
        return instrumented(DeactivateCatalogProduct.class);
    }

    @Override
    @Step("{0} deactivates the remembered catalog product")
    public <T extends Actor> void performAs(T actor) {
        String productId = actor.recall(ApiConstants.KEY_PRODUCT_ID);

        String url = ApiConstants.BASE_URL
                + ApiConstants.PRODUCT_DEACTIVATE.replace("{productId}", productId);

        int status = ApiRequestBuilder.authenticated(actor)
                .when()
                .patch(url)
                .statusCode();

        actor.remember(ApiConstants.KEY_LAST_STATUS, status);
    }
}
