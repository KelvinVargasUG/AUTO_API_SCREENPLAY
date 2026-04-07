package com.automation.tasks;

import com.automation.models.UpdateCatalogProductRequest;
import com.automation.utils.ApiConstants;
import com.automation.utils.ApiRequestBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class UpdateCatalogProduct implements Performable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String name;
    private final String type;
    private final String category;
    private final double price;

    public UpdateCatalogProduct(String name, String type, String category, double price) {
        this.name     = name;
        this.type     = type;
        this.category = category;
        this.price    = price;
    }

    public static UpdateCatalogProduct withDetails(String name, String type, String category, double price) {
        return instrumented(UpdateCatalogProduct.class, name, type, category, price);
    }

    @Override
    @Step("{0} updates catalog product to '{name}'")
    public <T extends Actor> void performAs(T actor) {
        String productId = actor.recall(ApiConstants.KEY_PRODUCT_ID);

        UpdateCatalogProductRequest body = new UpdateCatalogProductRequest(
                name, type, category, price, "ACTIVE", null
        );

        String json;
        try {
            json = MAPPER.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize update body", e);
        }

        String url = ApiConstants.BASE_URL
                + ApiConstants.PRODUCT_UPDATE.replace("{productId}", productId);

        int status = ApiRequestBuilder.authenticatedJson(actor)
                .body(json)
                .when()
                .put(url)
                .statusCode();

        actor.remember(ApiConstants.KEY_LAST_STATUS, status);
    }
}
