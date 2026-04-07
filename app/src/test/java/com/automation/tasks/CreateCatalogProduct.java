package com.automation.tasks;

import com.automation.models.CatalogProductRequest;
import com.automation.models.CatalogProductResponse;
import com.automation.utils.ApiConstants;
import com.automation.utils.ApiRequestBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class CreateCatalogProduct implements Performable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String name;
    private final String type;
    private final String category;
    private final double price;

    public CreateCatalogProduct(String name, String type, String category, double price) {
        this.name     = name;
        this.type     = type;
        this.category = category;
        this.price    = price;
    }

    public static CreateCatalogProduct withDetails(String name, String type, String category, double price) {
        return instrumented(CreateCatalogProduct.class, name, type, category, price);
    }

    @Override
    @Step("{0} creates catalog product '{name}'")
    public <T extends Actor> void performAs(T actor) {

        String uniqueName = name + "_" + System.currentTimeMillis();
        CatalogProductRequest body = new CatalogProductRequest(uniqueName, type, category, price);

        String json;
        try {
            json = MAPPER.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize product body", e);
        }

        io.restassured.response.Response response = ApiRequestBuilder.authenticatedJson(actor)
                .body(json)
                .when()
                .post(ApiConstants.BASE_URL + ApiConstants.PRODUCTS);

        response.then().statusCode(201);

        CatalogProductResponse created;
        try {
            created = MAPPER.readValue(response.getBody().asString(), CatalogProductResponse.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize product response", e);
        }

        actor.remember(ApiConstants.KEY_PRODUCT_ID, created.getId());
    }
}
