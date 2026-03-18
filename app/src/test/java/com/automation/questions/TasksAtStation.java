package com.automation.questions;

import com.automation.models.TaskResponse;
import com.automation.utils.ApiConstants;
import com.automation.utils.ApiRequestBuilder;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Question;

import java.util.List;
import java.util.Optional;

public class TasksAtStation implements Question<TaskResponse> {

    private static final int    MAX_RETRIES       = 5;
    private static final long   POLL_INTERVAL_MS  = 1000L;

    private final String station;

    private TasksAtStation(String station) {
        this.station = station;
    }

    public static TasksAtStation atStation(String station) {
        return new TasksAtStation(station);
    }

    @Override
    public TaskResponse answeredBy(Actor actor) {
        int tableNumber = actor.recall(ApiConstants.KEY_TABLE_NUMBER);

        String url = ApiConstants.BASE_URL + ApiConstants.TASKS_STATION.replace("{station}", station);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            ApiRequestBuilder.authenticated(actor)
                    .queryParam("status", "PENDING")
                    .when()
                    .get(url);

            List<TaskResponse> tasks =
                    SerenityRest.lastResponse().jsonPath().getList("$", TaskResponse.class);

            if (tasks != null) {
                Optional<TaskResponse> match = tasks.stream()
                        .filter(t -> t.getTableNumber() != null
                                && String.valueOf(tableNumber).equals(t.getTableNumber()))
                        .findFirst();

                if (match.isPresent()) {
                    TaskResponse found = match.get();
                    actor.remember(ApiConstants.KEY_ORDER_ID, found.getOrderId());
                    actor.remember(ApiConstants.KEY_TASK_ID,  found.getId());
                    return found;
                }
            }

            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Polling interrupted while waiting for task at station " + station, e);
                }
            }
        }

        throw new AssertionError(
                "No task found for tableNumber=" + tableNumber
                        + " at station=" + station
                        + " after " + MAX_RETRIES + " retries"
        );
    }
}
