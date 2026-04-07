package com.automation.tasks;

import com.automation.utils.ApiConstants;
import net.serenitybdd.annotations.Step;
import net.serenitybdd.screenplay.Actor;
import net.serenitybdd.screenplay.Performable;

import java.util.Arrays;

import static net.serenitybdd.screenplay.Tasks.instrumented;

public class UploadOversizedChunk implements Performable {

    private static final int CHUNK_SIZE_BYTES = 6 * 1024 * 1024;

    public static UploadOversizedChunk forCurrentSession() {
        return instrumented(UploadOversizedChunk.class);
    }

    @Override
    @Step("{0} sube 2 chunks de 6 MB c/u — el segundo supera el límite acumulado de 10 MB y retorna 413")
    public <T extends Actor> void performAs(T actor) {
        String uploadId = actor.recall(ApiConstants.KEY_UPLOAD_ID);
        String token    = actor.recall(ApiConstants.KEY_TOKEN);

        String url = ApiConstants.BASE_URL
                + ApiConstants.UPLOAD_CHUNK.replace("{uploadId}", uploadId);

        byte[] chunkData = buildChunkContent();

        uploadChunk(url, token, 0, chunkData);

        int status = uploadChunk(url, token, 1, chunkData);
        actor.remember(ApiConstants.KEY_LAST_STATUS, status);
    }

    private byte[] buildChunkContent() {

        byte[] header = "nombre,precio,categoria,estacion,descripcion,estado\n".getBytes();
        byte[] chunk = Arrays.copyOf(header, CHUNK_SIZE_BYTES);

        Arrays.fill(chunk, header.length, chunk.length, (byte) ' ');
        return chunk;
    }

    private int uploadChunk(String baseUrl, String token, int index, byte[] data) {
        return io.restassured.RestAssured.given()
                .header("Authorization", "Bearer " + token)
                .multiPart("file", "chunk_" + index + ".csv", data, "text/csv")
                .formParam("chunkIndex", index)
                .formParam("checksum", "test-checksum-" + index)
                .when()
                .post(baseUrl)
                .getStatusCode();
    }
}
