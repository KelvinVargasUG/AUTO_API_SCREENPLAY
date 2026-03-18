package com.automation.utils;

import io.restassured.specification.RequestSpecification;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;

/**
 * Factory de RequestSpecification reutilizable.
 * Centraliza la configuración de headers (Authorization, Content-Type)
 * para evitar repetición en tasks y questions.
 */
public final class ApiRequestBuilder {

    private ApiRequestBuilder() {}

    /**
     * Petición autenticada con Bearer token extraído de la memoria del actor.
     */
    public static RequestSpecification authenticated(Actor actor) {
        String token = actor.recall(ApiConstants.KEY_TOKEN);
        return SerenityRest.given()
                .header("Authorization", "Bearer " + token);
    }

    /**
     * Petición autenticada con Bearer token + Content-Type: application/json.
     */
    public static RequestSpecification authenticatedJson(Actor actor) {
        return authenticated(actor)
                .contentType("application/json");
    }

    /**
     * Petición sin autenticación con Content-Type: application/json.
     * Usada en register y login.
     */
    public static RequestSpecification asJson() {
        return SerenityRest.given()
                .contentType("application/json");
    }
}
