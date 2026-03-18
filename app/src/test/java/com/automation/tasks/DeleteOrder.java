package com.automation.tasks;

import com.automation.utils.ApiConstants;
import com.automation.utils.ApiRequestBuilder;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class DeleteOrder implements Performable {

    private final String orderId;
    private final boolean assertSuccess;

    public DeleteOrder(String orderId, boolean assertSuccess) {
        this.orderId = orderId;
        this.assertSuccess = assertSuccess;
    }

    public static DeleteOrder forCurrentActor() {
        return instrumented(DeleteOrder.class, (Object) null, true);
    }

    public static DeleteOrder withId(String orderId) {
        return instrumented(DeleteOrder.class, orderId, false);
    }

    @Override
    @Step("{0} deletes order {orderId}")
    public <T extends Actor> void performAs(T actor) {
        String id = (orderId != null) ? orderId : actor.recall(ApiConstants.KEY_ORDER_ID);

        String url = ApiConstants.BASE_URL + ApiConstants.ORDERS + "/" + id;

        if (assertSuccess) {
            ApiRequestBuilder.authenticated(actor)
                    .when()
                    .delete(url)
                    .then()
                    .statusCode(204);
        } else {
            int status = ApiRequestBuilder.authenticated(actor)
                    .when()
                    .delete(url)
                    .statusCode();
            actor.remember(ApiConstants.KEY_LAST_STATUS, status);
        }
    }
}
