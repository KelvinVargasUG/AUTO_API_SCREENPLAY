package com.automation.stepdefinitions;

import com.automation.models.TaskResponse;
import com.automation.questions.OrderStatus;
import com.automation.questions.TasksAtStation;
import com.automation.tasks.AttemptLoginWith;
import com.automation.tasks.CreateOrder;
import com.automation.tasks.DeleteOrder;
import com.automation.tasks.LoginUser;
import com.automation.tasks.RegisterUser;
import com.automation.tasks.StartTask;
import com.automation.utils.ApiConstants;
import com.automation.utils.TestDataGenerator;
import io.cucumber.java.Before;
import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;
import io.cucumber.java.es.Entonces;
import net.serenitybdd.rest.SerenityRest;
import net.serenitybdd.screenplay.Actor;

import static org.assertj.core.api.Assertions.assertThat;

public class CrudFlowStepDefs {

    private Actor actor;
    private int tableNumber;

    @Before
    public void setUp() {
        actor = Actor.named("QA Tester");
        tableNumber = TestDataGenerator.generateUniqueTableNumber();
    }

    @Dado("que se registra una nueva cuenta de usuario en el sistema")
    public void registrarUsuario() {
        actor.attemptsTo(RegisterUser.withGeneratedData());
    }

    @Dado("el usuario se autentica con credenciales válidas")
    public void autenticarUsuario() {
        actor.attemptsTo(LoginUser.withCredentialsFromActor());
    }

    @Cuando("el usuario crea una nueva orden con una bebida para la mesa actual")
    public void crearOrden() {
        actor.attemptsTo(CreateOrder.withTableNumber(tableNumber));
    }

    @Entonces("las tareas de la orden deben aparecer como PENDIENTES en la estación BAR")
    public void verificarTareasPendientesEnBAR() {
        TaskResponse task = actor.asksFor(TasksAtStation.atStation("BAR"));
        assertThat(task.getStatus()).isEqualTo("PENDING");
    }

    @Cuando("el sistema inicia la preparación de la tarea")
    public void iniciarPreparacion() {
        actor.attemptsTo(StartTask.forCurrentActor());
    }

    @Entonces("el estado de la orden debe ser EN_PREPARACION")
    public void verificarEstadoEnPreparacion() {
        Integer statusCode = actor.asksFor(OrderStatus.forCurrentActor());
        assertThat(statusCode).isEqualTo(200);
        String bodyStatus = SerenityRest.lastResponse().jsonPath().getString("status");
        assertThat(bodyStatus).isEqualTo("IN_PREPARATION");
    }

    @Cuando("se elimina la orden del sistema")
    public void eliminarOrden() {
        actor.attemptsTo(DeleteOrder.forCurrentActor());
    }

    @Entonces("consultar el estado de la orden debe retornar un error 404")
    public void verificar404TrasDelecion() {
        Integer statusCode = actor.asksFor(OrderStatus.forCurrentActor());
        assertThat(statusCode).isEqualTo(404);
    }

    @Cuando("el usuario intenta autenticarse con una contraseña incorrecta")
    public void loginConPasswordIncorrecta() {
        actor.attemptsTo(AttemptLoginWith.wrongPassword("WrongPassword999!"));
    }

    @Entonces("el sistema debe rechazar el intento de autenticación")
    public void verificarRechazoDeAutenticacion() {
        Integer status = actor.recall(ApiConstants.KEY_LAST_STATUS);
        assertThat(status).isEqualTo(400);
    }

    @Cuando("el usuario consulta el estado de una orden que no existe")
    public void consultarOrdenInexistente() {
        Integer status = actor.asksFor(OrderStatus.forOrderId("99999"));
        actor.remember(ApiConstants.KEY_LAST_STATUS, status);
    }

    @Entonces("el sistema debe responder con un error de recurso no encontrado")
    public void verificarError404() {
        Integer status = actor.recall(ApiConstants.KEY_LAST_STATUS);
        assertThat(status).isEqualTo(404);
    }

    @Cuando("el usuario intenta eliminar una orden que no existe")
    public void eliminarOrdenInexistente() {
        actor.attemptsTo(DeleteOrder.withId("99999"));
    }
}
