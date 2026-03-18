package com.automation.tasks;

import com.automation.models.CreateOrderRequest;
import com.automation.models.ProductRequest;
import com.automation.utils.ApiConstants;
import com.automation.utils.ApiRequestBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

import java.util.Collections;

import static net.serenitybdd.screenplay.Tasks.instrumented;
import static org.hamcrest.Matchers.equalTo;

public class CreateOrder implements Performable {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final int tableNumber;

    public CreateOrder(int tableNumber) {
        this.tableNumber = tableNumber;
    }

    public static CreateOrder withTableNumber(int tableNumber) {
        return instrumented(CreateOrder.class, tableNumber);
    }

    @Override
    @Step("{0} creates an order for table #tableNumber")
    public <T extends Actor> void performAs(T actor) {
        String email        = actor.recall(ApiConstants.KEY_EMAIL);
        String customerName = "QA Tester";

        ProductRequest product = new ProductRequest("Coffee", "DRINK", 3.50);

        CreateOrderRequest request = new CreateOrderRequest(
                tableNumber,
                customerName,
                email,
                Collections.singletonList(product)
        );

        String json;
        try {
            json = MAPPER.writeValueAsString(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize order body", e);
        }

        ApiRequestBuilder.authenticatedJson(actor)
                .body(json)
                .when()
                .post(ApiConstants.BASE_URL + ApiConstants.ORDERS)
                .then()
                .statusCode(201)
                .body("tasksCreated", equalTo(1));

        actor.remember(ApiConstants.KEY_TABLE_NUMBER, tableNumber);
    }
}
