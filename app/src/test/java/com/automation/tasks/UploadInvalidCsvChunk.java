package com.automation.tasks;

import com.automation.utils.ApiConstants;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class UploadInvalidCsvChunk implements Performable {

    private static final String INVALID_CSV =
            "col1,col2,col3\n" +
            "valor1,valor2,valor3\n";

    public static UploadInvalidCsvChunk forCurrentSession() {
        return instrumented(UploadInvalidCsvChunk.class);
    }

    @Override
    @Step("{0} uploads a CSV chunk with incorrect headers to trigger validation error")
    public <T extends Actor> void performAs(T actor) {
        String uploadId = actor.recall(ApiConstants.KEY_UPLOAD_ID);
        String token    = actor.recall(ApiConstants.KEY_TOKEN);

        byte[] csvBytes = INVALID_CSV.getBytes(StandardCharsets.UTF_8);
        String checksum = computeMd5Hex(csvBytes);

        String url = ApiConstants.BASE_URL
                + ApiConstants.UPLOAD_CHUNK.replace("{uploadId}", uploadId);

        io.restassured.response.Response response = io.restassured.RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .multiPart("file", "chunk_0", csvBytes, "text/csv")
                .formParam("chunkIndex", 0)
                .formParam("checksum", checksum)
                .when()
                .post(url);

        actor.remember(ApiConstants.KEY_LAST_STATUS, response.getStatusCode());
    }

    private static String computeMd5Hex(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "00000000000000000000000000000000";
        }
    }
}
