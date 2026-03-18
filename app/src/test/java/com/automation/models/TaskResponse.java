package com.automation.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskResponse {

    private String id;
    private String orderId;
    private String station;
    private String tableNumber;
    private String status;
    private String createdAt;
    private String updatedAt;
}
