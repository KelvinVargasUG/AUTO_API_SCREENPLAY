package com.automation.tasks;

import com.automation.utils.ApiConstants;
import com.automation.utils.ApiRequestBuilder;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

import java.util.Map;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class AttemptLoginWith implements Performable {

    private final String password;

    public AttemptLoginWith(String password) {
        this.password = password;
    }

    public static AttemptLoginWith wrongPassword(String password) {
        return instrumented(AttemptLoginWith.class, password);
    }

    @Override
    @Step("{0} attempts login with invalid password")
    public <T extends Actor> void performAs(T actor) {
        String email = actor.recall(ApiConstants.KEY_EMAIL);

        int status = ApiRequestBuilder.asJson()
                .body(Map.of("identifier", email, "password", password))
                .when()
                .post(ApiConstants.BASE_URL + ApiConstants.AUTH_LOGIN)
                .statusCode();

        actor.remember(ApiConstants.KEY_LAST_STATUS, status);
    }
}
