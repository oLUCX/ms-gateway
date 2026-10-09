package com.bodeganube.gateway.filter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Autenticacion machine-to-machine (M2M) para el webhook de ventas.
 *
 * La AWS Lambda que reenvia los avisos de venta no es un usuario: no tiene sesion ni JWT. Por eso no pasa
 * por la validacion de JWT de las rutas /api/**, sino que se identifica con una API key compartida en la
 * cabecera X-Api-Key. En application.yml este filtro se usa con el nombre "ApiKey".
 */
@Component
public class ApiKeyGatewayFilterFactory extends AbstractGatewayFilterFactory<ApiKeyGatewayFilterFactory.Config> {

    public static final String CABECERA = "X-Api-Key";

    private static final byte[] CUERPO_401 =
            "{\"status\":401,\"error\":\"Unauthorized\",\"mensaje\":\"API key ausente o invalida\"}"
                    .getBytes(StandardCharsets.UTF_8);

    private final byte[] apiKeyEsperada;

    public ApiKeyGatewayFilterFactory(@Value("${app.webhook.api-key}") String apiKey) {
        super(Config.class);
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("app.webhook.api-key no puede estar vacia");
        }
        this.apiKeyEsperada = apiKey.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String recibida = exchange.getRequest().getHeaders().getFirst(CABECERA);
            // MessageDigest.isEqual compara en tiempo constante: el tiempo de respuesta no delata
            // cuantos caracteres de la clave se acertaron.
            if (recibida != null && MessageDigest.isEqual(apiKeyEsperada, recibida.getBytes(StandardCharsets.UTF_8))) {
                return chain.filter(exchange);
            }
            return rechazar(exchange.getResponse());
        };
    }

    private Mono<Void> rechazar(ServerHttpResponse response) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(CUERPO_401)));
    }

    /** El filtro no recibe parametros desde la ruta; la clave viene de la configuracion. */
    public static class Config {
    }
}
