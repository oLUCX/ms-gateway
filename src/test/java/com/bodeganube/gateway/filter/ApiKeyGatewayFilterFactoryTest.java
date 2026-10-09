package com.bodeganube.gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

/** Prueba unitaria del filtro: se le pasa una peticion simulada y se revisa si la deja pasar o no. */
class ApiKeyGatewayFilterFactoryTest {

    private final GatewayFilter filtro =
            new ApiKeyGatewayFilterFactory("clave-de-prueba").apply(new ApiKeyGatewayFilterFactory.Config());

    @Test
    void sinApiKeyResponde401YNoReenviaLaPeticion() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/webhooks/ordenes"));
        AtomicBoolean reenviada = new AtomicBoolean(false);

        filtro.filter(exchange, cadena(reenviada)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(reenviada).isFalse();
    }

    @Test
    void conApiKeyIncorrectaResponde401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/webhooks/ordenes")
                .header(ApiKeyGatewayFilterFactory.CABECERA, "clave-adivinada"));
        AtomicBoolean reenviada = new AtomicBoolean(false);

        filtro.filter(exchange, cadena(reenviada)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(reenviada).isFalse();
    }

    @Test
    void conApiKeyCorrectaReenviaLaPeticion() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.post("/webhooks/ordenes")
                .header(ApiKeyGatewayFilterFactory.CABECERA, "clave-de-prueba"));
        AtomicBoolean reenviada = new AtomicBoolean(false);

        filtro.filter(exchange, cadena(reenviada)).block();

        assertThat(reenviada).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void noPermiteArrancarConUnaApiKeyVacia() {
        assertThatThrownBy(() -> new ApiKeyGatewayFilterFactory(" "))
                .isInstanceOf(IllegalStateException.class);
    }

    /** Cadena falsa: en vez de llamar a ms-ordenes, solo anota que la peticion llego hasta aqui. */
    private static GatewayFilterChain cadena(AtomicBoolean reenviada) {
        return exchange -> {
            reenviada.set(true);
            return Mono.empty();
        };
    }
}
