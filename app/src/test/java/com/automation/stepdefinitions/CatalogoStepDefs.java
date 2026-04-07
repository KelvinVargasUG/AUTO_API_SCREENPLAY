package com.automation.stepdefinitions;

import com.automation.tasks.CreateCatalogProduct;
import com.automation.tasks.DeactivateCatalogProduct;
import com.automation.tasks.GetCatalogProducts;
import com.automation.tasks.LoginUser;
import com.automation.tasks.RegisterUser;
import com.automation.tasks.UpdateCatalogProduct;
import com.automation.utils.ApiConstants;
import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;
import io.restassured.response.Response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CatalogoStepDefs {

    private Actor actor;

    @Before
    public void setUp() {
        actor = Actor.named("Admin QA");
    }

    @Dado("que se registra un nuevo usuario para gestionar el catálogo")
    public void registrarUsuarioCatalogo() {
        actor.attemptsTo(RegisterUser.withGeneratedData());
    }

    @Dado("el usuario del catálogo se autentica con credenciales válidas")
    public void autenticarUsuarioCatalogo() {
        actor.attemptsTo(LoginUser.withCredentialsFromActor());
    }

    @Cuando("el usuario crea un producto {string} de tipo {string} en categoría {string} con precio {double}")
    public void crearProducto(String name, String type, String category, double price) {
        actor.attemptsTo(CreateCatalogProduct.withDetails(name, type, category, price));
    }

    @Entonces("el producto debe crearse exitosamente con status {int}")
    public void verificarCreacion(int expectedStatus) {
        int actual = SerenityRest.lastResponse().getStatusCode();
        assertThat(actual).isEqualTo(expectedStatus);
    }

    @Cuando("el usuario consulta el listado del catálogo")
    public void listarCatalogo() {
        actor.attemptsTo(GetCatalogProducts.fromBackend());
    }

    @Entonces("el catálogo debe retornar status {int} con al menos un producto")
    public void verificarListado(int expectedStatus) {
        int actual = actor.recall(ApiConstants.KEY_LAST_STATUS);
        assertThat(actual).isEqualTo(expectedStatus);

        int count = SerenityRest.lastResponse().jsonPath().getList("$").size();
        assertThat(count).isGreaterThanOrEqualTo(1);
    }

    @Cuando("el usuario actualiza el producto con nombre {string} de tipo {string} en categoría {string} con precio {double}")
    public void actualizarProducto(String name, String type, String category, double price) {
        actor.attemptsTo(UpdateCatalogProduct.withDetails(name, type, category, price));
    }

    @Entonces("el producto debe actualizarse con status {int}")
    public void verificarActualizacion(int expectedStatus) {
        int actual = actor.recall(ApiConstants.KEY_LAST_STATUS);
        assertThat(actual).isEqualTo(expectedStatus);
    }

    @Cuando("el usuario desactiva el producto creado del catálogo")
    public void desactivarProducto() {
        actor.attemptsTo(DeactivateCatalogProduct.forCurrentActor());
    }

    @Entonces("el producto debe desactivarse con status {int}")
    public void verificarDesactivacion(int expectedStatus) {
        int actual = actor.recall(ApiConstants.KEY_LAST_STATUS);
        assertThat(actual).isEqualTo(expectedStatus);
    }

    @Cuando("el usuario intenta crear un producto con nombre vacío")
    public void crearProductoNombreVacio() {
        String token = actor.recall(ApiConstants.KEY_TOKEN);
        Response response = net.serenitybdd.rest.SerenityRest.given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("{\"name\":\"\",\"type\":\"DRINK\",\"category\":\"Bebidas\",\"price\":2.00}")
                .when()
                .post(ApiConstants.BASE_URL + ApiConstants.PRODUCTS);
        actor.remember(ApiConstants.KEY_LAST_STATUS, response.getStatusCode());
    }

    @Entonces("el sistema debe rechazar la creación con un error 4xx")
    public void verificarRechazoCreacion() {
        int actual = actor.recall(ApiConstants.KEY_LAST_STATUS);
        assertThat(actual).isBetween(400, 499);
    }

    @Entonces("el producto recién creado debe aparecer en el catálogo")
    public void verificarProductoEnCatalogo() {
        int status = actor.recall(ApiConstants.KEY_LAST_STATUS);
        assertThat(status).isEqualTo(200);
        String productId = actor.recall(ApiConstants.KEY_PRODUCT_ID);
        List<Map<String, Object>> products = SerenityRest.lastResponse().jsonPath().getList("$");
        boolean found = products.stream()
                .anyMatch(p -> productId.equals(String.valueOf(p.get("id"))));
        assertThat(found).as("el producto creado debe estar en el catálogo").isTrue();
    }

    @Cuando("el usuario intenta actualizar un producto que no existe")
    public void actualizarProductoInexistente() {
        String fakeId = UUID.randomUUID().toString();
        String token  = actor.recall(ApiConstants.KEY_TOKEN);
        String url    = ApiConstants.BASE_URL + ApiConstants.PRODUCT_UPDATE.replace("{productId}", fakeId);
        Response response = net.serenitybdd.rest.SerenityRest.given()
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .body("{\"name\":\"NoExiste\",\"type\":\"DRINK\",\"category\":\"X\",\"price\":1.00}")
                .when()
                .put(url);
        actor.remember(ApiConstants.KEY_LAST_STATUS, response.getStatusCode());
    }

    @Entonces("el sistema debe responder con 404 al actualizar")
    public void verificar404Actualizar() {
        int actual = actor.recall(ApiConstants.KEY_LAST_STATUS);
        assertThat(actual).isEqualTo(404);
    }

    @Entonces("el producto desactivado no debe aparecer en el listado del catálogo")
    public void verificarProductoInactivoExcluido() {
        actor.attemptsTo(GetCatalogProducts.fromBackend());
        int status = actor.recall(ApiConstants.KEY_LAST_STATUS);
        assertThat(status).isEqualTo(200);
        String productId = actor.recall(ApiConstants.KEY_PRODUCT_ID);
        List<Map<String, Object>> products = SerenityRest.lastResponse().jsonPath().getList("$");
        boolean found = products.stream()
                .anyMatch(p -> productId.equals(String.valueOf(p.get("id"))));
        assertThat(found).as("el producto desactivado NO debe estar en el catálogo").isFalse();
    }

}
