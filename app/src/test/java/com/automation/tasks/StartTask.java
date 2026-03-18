package com.automation.tasks;

import com.automation.utils.ApiConstants;
import com.automation.utils.ApiRequestBuilder;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

import static net.serenitybdd.screenplay.Tasks.instrumented;
import static org.hamcrest.Matchers.equalTo;

public class StartTask implements Performable {

    public static StartTask forCurrentActor() {
        return instrumented(StartTask.class);
    }

    @Override
    @Step("{0} starts the task recalled from actor memory")
    public <T extends Actor> void performAs(T actor) {
        String taskId = actor.recall(ApiConstants.KEY_TASK_ID);

        String url = ApiConstants.BASE_URL + ApiConstants.TASKS_START.replace("{taskId}", taskId);

        ApiRequestBuilder.authenticated(actor)
                .when()
                .patch(url)
                .then()
                .statusCode(200)
                .body("status", equalTo("IN_PREPARATION"));
    }
}
