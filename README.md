# ms-gateway

**Spring Cloud Gateway** del caso BodegaNube (JVY0101): el punto de entrada único hacia los 4
microservicios dentro de la VPC privada. Enruta cada petición y autentica el webhook de ventas que llega
desde la AWS Lambda.

| | |
|---|---|
| Puerto | `8080` |
| Base de datos | No usa |
| Stack | Java 17, Spring Boot 3.2, Spring Cloud Gateway 2023.0 (WebFlux + Netty), Maven Wrapper |

## Rutas
Definidas en `src/main/resources/application.yml`:

| Ruta | Destino | Autenticación |
|---|---|---|
| `POST /webhooks/ordenes` | ms-ordenes `POST /api/ordenes` (puerto 8082) | **API key** en la cabecera `X-Api-Key` (M2M) |
| `/api/auth/**` | ms-autenticacion (puerto 8081) | pública (login) |
| `/api/ordenes/**` | ms-ordenes (puerto 8082) | JWT de usuario (siguiente paso) |
| `/api/productos/**` | ms-inventario (puerto 8083) | JWT de usuario (siguiente paso) |
| `/api/picking/**` | ms-picking-despacho (puerto 8084) | JWT de usuario (siguiente paso) |

## Autenticación machine-to-machine del webhook
En la Evaluación 1 el profesor observó que la Lambda no tiene una sesión JWT de usuario final. Por eso el
webhook entra por una ruta propia, `/webhooks/ordenes`, que no usa JWT: el filtro
`ApiKeyGatewayFilterFactory` exige la cabecera `X-Api-Key` con la clave compartida con la Lambda.

- Sin cabecera o con una clave incorrecta responde **401** y la petición nunca llega a ms-ordenes.
- Con la clave correcta reescribe la ruta a `/api/ordenes` y la reenvía.
- La comparación se hace en tiempo constante (`MessageDigest.isEqual`) y el gateway no arranca si la clave
  está vacía.
- La clave se configura con `WEBHOOK_API_KEY`. En AWS vendría de Secrets Manager.

```bash
curl -i -X POST http://localhost:8080/webhooks/ordenes \
  -H "Content-Type: application/json" \
  -H "X-Api-Key: clave-local-solo-para-desarrollo" \
  -d '{"externalOrderId":"shopify-001","comercioId":"comercio-123","items":[{"productoId":"SKU-1","cantidad":1}]}'
```

## Estructura
```
src/main/java/com/bodeganube/gateway
├── MsGatewayApplication.java
└── filter/   ApiKeyGatewayFilterFactory   filtro "ApiKey" que se usa en application.yml
```

## Levantar el gateway desde cero

### 1. Requisitos
- JDK 17 o superior (`java -version`) y Git.
- **No hace falta instalar Maven**: el repositorio trae el Maven Wrapper (`mvnw` y `mvnw.cmd`).
- Para que las rutas respondan, los 4 microservicios tienen que estar corriendo (ver el README de cada uno).

### 2. Clonar, compilar y ejecutar
```bash
git clone https://github.com/oLUCX/ms-gateway.git
cd ms-gateway
.\mvnw.cmd clean package        # Windows (PowerShell); en Linux, macOS o Git Bash: ./mvnw clean package
java -jar target/ms-gateway.jar
```
Queda escuchando en `http://localhost:8080`. Desde ahí el cliente habla con todo el sistema, por ejemplo
`GET http://localhost:8080/api/productos`.

### 3. Probar con Postman
Importa `postman/ms-gateway.postman_collection.json` y usa *Run collection* con los 4 microservicios
arriba: el webhook sin clave y con una clave incorrecta da 401, con la clave correcta crea la orden (201) y
las rutas `/api/**` llegan a cada servicio.

## Configuración

| Variable | Valor por defecto |
|---|---|
| `PORT` | `8080` |
| `WEBHOOK_API_KEY` | `clave-local-solo-para-desarrollo` |
| `MS_AUTENTICACION_URL` | `http://localhost:8081` |
| `MS_ORDENES_URL` | `http://localhost:8082` |
| `MS_INVENTARIO_URL` | `http://localhost:8083` |
| `MS_PICKING_URL` | `http://localhost:8084` |

## Pruebas automatizadas
`.\mvnw.cmd test` corre 6 pruebas:
- `ApiKeyGatewayFilterFactoryTest`: sin clave, con clave incorrecta y con clave correcta.
- `RutasGatewayTest`: levanta el gateway con el `application.yml` real y comprueba que `/webhooks/ordenes`
  exige la API key y solo acepta POST.

## Ramas
`main` tiene la versión entregada, `develop` integra el trabajo en curso y cada cambio entra desde una
rama `feature/...` con un Pull Request hacia `develop`.

## Próximos pasos
- `GlobalFilter` que valide la firma y la expiración del JWT emitido por ms-autenticacion y aplique RBAC
  por rol (`OPERARIO` / `COMERCIO`) en las rutas `/api/**`.
- Circuit Breaker en las rutas hacia cada microservicio.
