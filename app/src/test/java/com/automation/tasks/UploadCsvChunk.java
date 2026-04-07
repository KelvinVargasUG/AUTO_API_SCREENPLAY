package com.automation.tasks;

import com.automation.utils.ApiConstants;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

import java.nio.charset.StandardCharsets;import java.security.MessageDigest;
import static net.serenitybdd.screenplay.Tasks.instrumented;

public class UploadCsvChunk implements Performable {

    private static final String CSV_CONTENT =
            "nombre,precio,categoria,estacion,descripcion,estado\n" +
            "Café de Prueba,3.50,Bebidas Calientes,BAR,Café de prueba QA,Activo\n" +
            "Sopa de Prueba,5.00,Platos Calientes,HOT_KITCHEN,Sopa de prueba QA,Activo\n" +
            "Ensalada Prueba,4.00,Ensaladas,COLD_KITCHEN,Ensalada de prueba QA,Activo\n";

    public static UploadCsvChunk forCurrentSession() {
        return instrumented(UploadCsvChunk.class);
    }

    @Override
    @Step("{0} uploads CSV chunk 0 for the current upload session")
    public <T extends Actor> void performAs(T actor) {
        String uploadId = actor.recall(ApiConstants.KEY_UPLOAD_ID);
        String token    = actor.recall(ApiConstants.KEY_TOKEN);

        byte[] csvBytes = CSV_CONTENT.getBytes(StandardCharsets.UTF_8);
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

        int status = response.getStatusCode();
        if (status >= 400) {
            throw new RuntimeException(
                "Chunk upload failed — status " + status + ": " + response.getBody().asString()
            );
        }

        actor.remember(ApiConstants.KEY_LAST_STATUS, status);
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
