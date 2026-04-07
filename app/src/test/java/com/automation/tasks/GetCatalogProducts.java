package com.automation.tasks;

import com.automation.utils.ApiConstants;
import com.automation.utils.ApiRequestBuilder;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class GetCatalogProducts implements Performable {

    public static GetCatalogProducts fromBackend() {
        return instrumented(GetCatalogProducts.class);
    }

    @Override
    @Step("{0} retrieves the catalog product list")
    public <T extends Actor> void performAs(T actor) {
        int status = ApiRequestBuilder.authenticated(actor)
                .when()
                .get(ApiConstants.BASE_URL + ApiConstants.PRODUCTS)
                .statusCode();

        actor.remember(ApiConstants.KEY_LAST_STATUS, status);
    }
}
