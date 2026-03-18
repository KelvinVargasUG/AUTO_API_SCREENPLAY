package com.automation.questions;

import com.automation.utils.ApiConstants;
import com.automation.utils.ApiRequestBuilder;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;

public class OrderStatus implements Question<Integer> {

    private final String orderId;

    private OrderStatus(String orderId) {
        this.orderId = orderId;
    }

    public static OrderStatus forCurrentActor() {
        return new OrderStatus(null);
    }

    public static OrderStatus forOrderId(String orderId) {
        return new OrderStatus(orderId);
    }

    @Override
    public Integer answeredBy(Actor actor) {
        String id = (orderId != null) ? orderId : actor.recall(ApiConstants.KEY_ORDER_ID);

        String url = ApiConstants.BASE_URL + ApiConstants.ORDERS_STATUS.replace("{orderId}", id);

        return ApiRequestBuilder.authenticated(actor)
                .when()
                .get(url)
                .statusCode();
    }
}
