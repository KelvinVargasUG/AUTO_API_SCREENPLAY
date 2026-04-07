package com.automation.utils;

import io.restassured.specification.RequestSpecification;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;

public final class ApiRequestBuilder {

    private ApiRequestBuilder() {}

    public static RequestSpecification authenticated(Actor actor) {
        String token = actor.recall(ApiConstants.KEY_TOKEN);
        return SerenityRest.given()
                .header("Authorization", "Bearer " + token);
    }

    public static RequestSpecification authenticatedJson(Actor actor) {
        return authenticated(actor)
                .contentType("application/json");
    }

    public static RequestSpecification asJson() {
        return SerenityRest.given()
                .contentType("application/json");
    }
}
