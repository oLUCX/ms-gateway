package com.bodeganube.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Levanta el gateway completo (con el application.yml real) y comprueba que la ruta del webhook exige
 * la API key antes de intentar reenviar a ms-ordenes. No necesita que los microservicios esten corriendo.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.webhook.api-key=clave-de-prueba")
class RutasGatewayTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void webhookSinApiKeyEsRechazadoCon401() {
        webTestClient.post().uri("/webhooks/ordenes")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.mensaje").isEqualTo("API key ausente o invalida");
    }

    @Test
    void webhookSoloAceptaPost() {
        webTestClient.get().uri("/webhooks/ordenes")
                .exchange()
                .expectStatus().isNotFound();
    }
}
