package com.automation.tasks;

import com.automation.models.LoginRequest;
import com.automation.utils.ApiConstants;
import com.automation.utils.ApiRequestBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class LoginUser implements Performable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static LoginUser withCredentialsFromActor() {
        return instrumented(LoginUser.class);
    }

    @Override
    @Step("{0} logs in with remembered credentials")
    public <T extends Actor> void performAs(T actor) {
        String email    = actor.recall(ApiConstants.KEY_EMAIL);
        String password = actor.recall(ApiConstants.KEY_PASSWORD);

        LoginRequest loginRequest = new LoginRequest(email, password);

        String json;
        try {
            json = MAPPER.writeValueAsString(loginRequest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize login body", e);
        }

        io.restassured.response.Response response = ApiRequestBuilder.asJson()
                .body(json)
                .when()
                .post(ApiConstants.BASE_URL + ApiConstants.AUTH_LOGIN);

        response.then().statusCode(200);

        String token = response.jsonPath().getString("token");
        actor.remember(ApiConstants.KEY_TOKEN, token);
    }
}
