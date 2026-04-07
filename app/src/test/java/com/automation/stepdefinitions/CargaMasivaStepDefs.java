package com.automation.stepdefinitions;

import com.automation.tasks.CompleteUpload;
import com.automation.tasks.DownloadUploadTemplate;
import com.automation.tasks.GetCatalogProducts;
import com.automation.tasks.GetUploadErrors;
import com.automation.tasks.InitUploadSession;
import com.automation.tasks.LoginUser;
import com.automation.tasks.RegisterUser;
import com.automation.tasks.UploadCsvChunk;
import com.automation.tasks.UploadInvalidCsvChunk;
import com.automation.tasks.UploadOversizedChunk;
import com.automation.utils.ApiConstants;
import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;

import static org.assertj.core.api.Assertions.assertThat;

public class CargaMasivaStepDefs {

    private Actor actor;

    @Before
    public void setUp() {
        actor = Actor.named("Admin Carga");
    }

    @Dado("que se registra un nuevo usuario para carga masiva")
    public void registrarUsuarioCarga() {
        actor.attemptsTo(RegisterUser.withGeneratedData());
    }

    @Dado("el usuario de carga masiva se autentica con credenciales válidas")
    public void autenticarUsuarioCarga() {
        actor.attemptsTo(LoginUser.withCredentialsFromActor());
    }

    @Cuando("el usuario inicia una sesi\u00f3n de carga masiva")
    public void iniciarSesion() {
        actor.attemptsTo(InitUploadSession.withGeneratedFileName());
    }

    @Entonces("el sistema debe retornar un uploadId válido con status {int}")
    public void verificarInitSession(int expectedStatus) {
        int actual = actor.recall(ApiConstants.KEY_LAST_STATUS);

        assertThat(actual).isIn(expectedStatus, 200, 201);

        String uploadId = actor.recall(ApiConstants.KEY_UPLOAD_ID);
        assertThat(uploadId).isNotNull().isNotBlank();
    }

    @Cuando("el usuario sube el contenido CSV al chunk {int}")
    public void subirChunk(int chunkIndex) {
        actor.attemptsTo(UploadCsvChunk.forCurrentSession());
    }

    @Entonces("el sistema debe confirmar la recepción del chunk con status {int}")
    public void verificarChunk(int expectedStatus) {
        int actual = actor.recall(ApiConstants.KEY_LAST_STATUS);
        assertThat(actual).isEqualTo(expectedStatus);
    }

    @Cuando("el usuario completa la sesión de carga")
    public void completarSesion() {
        actor.attemptsTo(CompleteUpload.forCurrentSession());
    }

    @Entonces("el sistema debe retornar el resumen del procesamiento con status {int}")
    public void verificarCompletado(int expectedStatus) {
        int actual = actor.recall(ApiConstants.KEY_LAST_STATUS);

        assertThat(actual).isIn(expectedStatus, 200, 202);
    }

    @Cuando("el usuario descarga la plantilla de carga masiva")
    public void descargarPlantilla() {
        actor.attemptsTo(DownloadUploadTemplate.asCurrentUser());
    }

    @Entonces("el sistema debe retornar la plantilla CSV con status {int}")
    public void verificarPlantilla(int expectedStatus) {
        int actual = actor.recall(ApiConstants.KEY_LAST_STATUS);
        assertThat(actual).isEqualTo(expectedStatus);
    }

    @Cuando("el usuario sube un CSV con cabeceras incorrectas al chunk {int}")
    public void subirCsvInvalido(int chunkIndex) {
        actor.attemptsTo(UploadInvalidCsvChunk.forCurrentSession());
    }

    @Entonces("el sistema debe rechazar el CSV con un error de validación")
    public void verificarRechazoInvalido() {
        int actual = actor.recall(ApiConstants.KEY_LAST_STATUS);
        assertThat(actual).isBetween(400, 499);
    }

    @Cuando("el usuario sube un chunk que supera el tamaño máximo permitido")
    public void subirChunkSobredimensionado() {
        actor.attemptsTo(UploadOversizedChunk.forCurrentSession());
    }

    @Entonces("el sistema debe rechazar el archivo con status 413")
    public void verificarRechazo413() {
        int actual = actor.recall(ApiConstants.KEY_LAST_STATUS);
        assertThat(actual).as("El servidor debe responder 413 Payload Too Large").isEqualTo(413);
    }

    @Entonces("el sistema debe exponer el endpoint de errores con status {int}")
    public void verificarEndpointErrores(int expectedStatus) {
        actor.attemptsTo(GetUploadErrors.forCurrentSession());
        int actual = actor.recall(ApiConstants.KEY_LAST_STATUS);
        assertThat(actual).isEqualTo(expectedStatus);
    }

    @Entonces("los productos cargados deben aparecer en el listado del catálogo")
    public void verificarProductosCargadosEnCatalogo() {
        actor.attemptsTo(GetCatalogProducts.fromBackend());
        int status = actor.recall(ApiConstants.KEY_LAST_STATUS);
        assertThat(status).isEqualTo(200);
        int count = SerenityRest.lastResponse().jsonPath().getList("$").size();
        assertThat(count).as("el catálogo debe tener al menos un producto tras la carga").isGreaterThanOrEqualTo(1);
    }
}
