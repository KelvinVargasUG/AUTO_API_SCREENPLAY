package com.automation.tasks;

import com.automation.utils.ApiConstants;
import com.automation.utils.ApiRequestBuilder;
import com.automation.utils.TestDataGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

import java.util.HashMap;
import java.util.Map;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class RegisterUser implements Performable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static RegisterUser withGeneratedData() {
        return instrumented(RegisterUser.class);
    }

    @Override
    @Step("{0} registers a new user with generated data")
    public <T extends Actor> void performAs(T actor) {
        String email    = TestDataGenerator.generateUniqueEmail();
        String username = TestDataGenerator.generateUniqueUsername();
        String password = "Test@1234";

        actor.remember(ApiConstants.KEY_EMAIL, email);
        actor.remember(ApiConstants.KEY_PASSWORD, password);

        Map<String, String> body = new HashMap<>();
        body.put("username", username);
        body.put("email", email);
        body.put("password", password);

        String json;
        try {
            json = MAPPER.writeValueAsString(body);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize register body", e);
        }

        ApiRequestBuilder.asJson()
                .body(json)
                .when()
                .post(ApiConstants.BASE_URL + ApiConstants.AUTH_REGISTER)
                .then()
                .statusCode(201);
    }
}
