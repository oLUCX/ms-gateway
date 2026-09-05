# ms-gateway

Spring Cloud Gateway del caso BodegaNube (asignatura JVY0101): punto único de enrutamiento
interno hacia los 4 microservicios, dentro de la VPC privada según el diagrama de arquitectura.

## Responsabilidad
Enrutar cada petición al microservicio correspondiente y (en la versión funcional completa)
validar el JWT emitido por `ms-autenticacion` antes de reenviarla (RBAC).

## Stack
Java 17, Spring Boot 3.2, Spring Cloud Gateway.

## Rutas configuradas (`application.yml`)
| Path                  | Destino                         |
|------------------------|----------------------------------|
| `/api/auth/**`         | ms-autenticacion (puerto 8081)  |
| `/api/ordenes/**`      | ms-ordenes (puerto 8082)        |
| `/api/productos/**`    | ms-inventario (puerto 8083)     |
| `/api/picking/**`      | ms-picking-despacho (puerto 8084) |

## Cómo correrlo localmente
1. Levantar los 4 microservicios en sus puertos por defecto.
2. `mvn spring-boot:run` (queda escuchando en el puerto 8080).
3. Todas las peticiones del cliente deberían apuntar a `http://localhost:8080/...` en vez de
   hablar directamente con cada microservicio.

## Próximos pasos (fuera del alcance de este esqueleto)
- Implementar un `GlobalFilter` que valide la firma y expiración del JWT, y rechace con 401/403
  según el rol (`ROLE_OPERARIO` / `ROLE_COMERCIO`) antes de enrutar la petición — este es el
  control de seguridad marcado en verde en el diagrama de arquitectura.
