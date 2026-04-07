package com.automation.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadSummaryResponse {

    private String uploadId;
    private String uploadStatus;
    private String processingStatus;
    private int    totalRecords;
    private int    processedRecords;
    private int    failedRecords;
}
