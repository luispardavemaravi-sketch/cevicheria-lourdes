# CLAUDE.md — Cevichería Lourdes · Sistema de Pedidos y Caja

Lee este archivo completo antes de escribir código. Es la especificación y la ley del proyecto.

## Contexto

Cevichería de 14 mesas en Lima. Atiende 7:30–15:00. Lunes a viernes trabajan 2 personas; sábado y domingo, 5 (100–200 pedidos el domingo).
La cocina está en el 2.º piso: pedidos y avisos le llegan por pantalla (WebSocket). Se cobra al final en mesa; para llevar se cobra ANTES de cocinar.
Pagos: efectivo y Yape (sin integración por API). Moneda: soles (PEN).

## Stack (no se cambia)

Java 21 · Spring Boot 3.5.x · Spring Web, Validation, Security, Data JPA, WebSocket (STOMP) · PostgreSQL 17 · Flyway · MapStruct · springdoc-openapi · JUnit 5, Mockito, AssertJ, Testcontainers, ArchUnit · Maven wrapper (`./mvnw`) · Docker.
Para JWT usa `spring-boot-starter-oauth2-resource-server` (Nimbus, HS256, secreto en `JWT_SECRET`, ≥ 32 bytes). No agregues otras librerías sin justificarlo en el PR.

## Comandos

- Pruebas completas: `cd api && ./mvnw verify` (necesita Docker para Testcontainers).
- Base local: `cd infra && docker compose --env-file ../.env up -d db`.

## Arquitectura hexagonal (violación = rechazo)

```
pe.cevicherialourdes.<contexto>/
├── domain/model/        entidades y value objects puros
├── domain/exception/    (usa las de shared: RecursoNoEncontrado 404, ConflictoNegocio 409, ReglaNegocio 422)
├── domain/port/in/      interfaces de casos de uso
├── domain/port/out/     interfaces de repositorios y notificadores
├── application/usecase/ implementación de port/in; @Transactional SOLO aquí
└── infrastructure/
    ├── adapter/in/web/        controllers, DTO request/response (record + Bean Validation), mappers
    ├── adapter/out/persistence/ @Entity, Spring Data repos, adaptadores que implementan port/out
    ├── adapter/out/ws/        publicación STOMP
    └── config/
```

Contextos: `identidad`, `carta`, `pedidos`, `cocina`, `caja`, `gastos`, `reportes`, `avisos`, `shared` (ya existe).

1. `domain` no importa `org.springframework.*`, `jakarta.persistence.*`, `org.hibernate.*` ni Jackson. `ArchitectureTest` lo verifica: nunca lo debilites.
2. Controller: recibe DTO con `@Valid`, llama al puerto in, devuelve DTO. Cero lógica, cero repositorios.
3. Nunca expongas `@Entity` en la API.
4. Invariantes dentro del dominio con métodos con intención (`cuenta.pagar(...)`, `pedido.marcarListo()`); sin setters públicos.
5. Inyección por constructor. Prohibido `@Autowired` en campo.
6. Dinero: value object `Dinero` sobre `BigDecimal` escala 2, `RoundingMode.HALF_UP`. Nunca `double`/`float`. Totales siempre calculados en backend.
7. Fechas: siempre desde el bean `Clock` (America/Lima). Nunca `LocalDate.now()` sin `Clock`.
8. Listados paginados (`Pageable`); nunca `findAll()` sin límite.
9. Errores: lanza las excepciones de `shared.domain.exception` con código `MAYUSCULAS_CON_GUION`. El `GlobalExceptionHandler` existente devuelve ProblemDetail. No lo dupliques.
10. Eventos a WebSocket solo DESPUÉS del commit: `@TransactionalEventListener(phase = AFTER_COMMIT)`.
11. Concurrencia de cobro: `@Version` en `cuenta` (bloqueo optimista).
12. Logs con SLF4J, sin `System.out`, sin PIN, JWT ni datos personales.
13. Esquema: las migraciones V1 y V2 YA están aplicadas y NUNCA se editan. Cambios = nueva migración `V3__...sql`, `V4__...sql`. `ddl-auto=validate`.

## Definition of Done de cada historia

- [ ] Criterios de aceptación cubiertos por pruebas.
- [ ] Unitarias del dominio y del caso de uso (puertos out con Mockito).
- [ ] `@WebMvcTest` del controller: éxito, 400 por validación, 401/403 por rol, 404/409/422 por negocio.
- [ ] Si toca persistencia: prueba `*IT` con Testcontainers.
- [ ] `./mvnw verify` en verde.
- [ ] Endpoint visible en Swagger con su rol documentado.
- [ ] Un commit por historia: `feat(<contexto>): <qué> [HU-XX]`.

## Roles

`ADMIN` (mamá) hereda todo lo de `MOZO` (RN-16, usa `RoleHierarchy`). `COCINA` (papá). `MOZO` (hijos los fines de semana).

## Reglas de negocio

| ID | Regla |
| --- | --- |
| RN-01 | Precio fijo: quitar acompañamientos no cambia el precio |
| RN-02 | El precio se congela en `detalle_pedido.precio_unitario` al registrar |
| RN-03 | Para llevar se paga antes de entrar a la cola de cocina |
| RN-04 | Mesa se paga al final; máximo una cuenta abierta por mesa |
| RN-05 | Táper S/ 1.00 solo en cuentas de MESA; en PARA_LLEVAR → 422 `TAPER_SOLO_EN_MESA` |
| RN-06 | Productos con `requiere_cocina = false` (bebidas, táper) no van a la cola |
| RN-07 | Pago EFECTIVO o YAPE; en pago mixto la suma == total |
| RN-08 | Efectivo: recibido ≥ monto; vuelto lo calcula el backend |
| RN-09 | Cuenta PAGADA es inmutable; solo pagadas suman en reportes |
| RN-10 | Solo ADMIN anula ítems, con motivo; anulación lógica y auditada |
| RN-11 | Una caja por día; sin caja abierta no hay cobro (409 `CAJA_NO_ABIERTA`); no se cierra con cuentas abiertas; no se reabre |
| RN-12 | Ganancia mes = ventas pagadas − gastos diarios − gastos fijos del mes |
| RN-13 | 14 mesas (1–14); mesa ocupada no admite otra cuenta (409 `MESA_OCUPADA`) |
| RN-14 | Día operativo en America/Lima |
| RN-15 | No se duplica un aviso de insumo mientras haya uno no LISTO del mismo insumo |
| RN-16 | ADMIN tiene todos los permisos de MOZO |

## Historias por sprint (orden obligatorio)

**Sprint 1**
- HU-01 Login con usuario + PIN de 4 dígitos → JWT (8 h). 5 fallos seguidos bloquean 5 min (423 `USUARIO_BLOQUEADO`). Usuario desactivado no entra. Crea el primer ADMIN al arrancar si no existe ninguno, desde `ADMIN_USERNAME`/`ADMIN_PIN` (si faltan, no lo crea y lo registra en log como WARN).
- HU-03 CRUD de productos con categoría y precio; nombre único; precio > 0.
- HU-04 Presentaciones de bebidas con precio propio; pedir una bebida exige presentación.
- HU-05 Acompañamientos por plato (sin precio).

**Sprint 2**
- HU-07 Pedido de mesa: abre la cuenta si la mesa está libre; publica comanda en `/topic/cocina`.
- HU-08 Pedido para llevar: nombre obligatorio (máx. 30); queda pendiente de pago y NO va a cocina hasta pagarse.
- HU-09 Quitar acompañamientos y nota (máx. 80) por ítem.
- HU-10 Agregar ítems a una cuenta de mesa abierta (nuevo pedido, misma cuenta).

**Sprint 3**
- HU-14 Cola de cocina: pedidos PENDIENTE en orden de llegada con minutos de espera.
- HU-15 Marcar LISTO (solo desde PENDIENTE, si no 409); se puede deshacer en 10 s.
- HU-28 Enviar aviso de insumo (NORMAL/URGENTE) → `/topic/avisos`.
- HU-29 Atender aviso: PENDIENTE → PREPARANDO → LISTO.
- HU-30 CRUD de insumos avisables (reordenar, desactivar).

**Sprint 4**
- HU-17 Porciones vendidas y pendientes del día por plato.
- HU-18 Abrir caja con sencillo inicial.
- HU-19 Cobrar cuenta (efectivo con vuelto / Yape con nro. de operación opcional); libera la mesa; publica `/topic/mesas`.
- HU-21 Cerrar caja: ventas por método, efectivo esperado vs contado, diferencia con observación obligatoria si ≠ 0.
- HU-13 Agregar táper a cuenta de mesa.

**Sprint 5**
- HU-22 Gasto diario (categorías: PESCADO_MARISCOS, POTA, VERDURAS, ABARROTES, BEBIDAS, OTROS).
- HU-23 Gasto fijo mensual (LOCAL, LUZ, AGUA, GAS, OTROS_FIJOS).
- HU-24 Reporte de ventas del día (total, por método, por plato, n.º de pedidos mesa/para llevar).
- HU-25 Ganancia del mes + lista de días con ventas y sin gasto registrado.
- HU-02 Gestionar usuarios (crear, desactivar, cambiar PIN; nunca desactivar al último ADMIN).
- HU-06 Marcar plato agotado/disponible (ADMIN, COCINA).

**Sprint 6**
- HU-11 Estado de las 14 mesas (libre, ocupada, pedido listo).
- HU-12 Anular ítem (ADMIN, motivo obligatorio).
- HU-16 Marcar ENTREGADO + aviso al mozo en `/topic/pedidos-listos`.
- HU-26 Ranking de platos por rango de fechas.

**Backlog (no implementar salvo pedido):** HU-20 pago mixto, HU-27 pedidos por hora.

## API (`/api/v1`)

| Método | Ruta | Rol |
| --- | --- | --- |
| POST | `/auth/login` | público |
| GET/POST | `/usuarios` · PATCH `/usuarios/{id}` | ADMIN |
| GET | `/productos` | autenticado |
| POST/PUT | `/productos`, `/productos/{id}` | ADMIN |
| PATCH | `/productos/{id}/disponibilidad` | ADMIN, COCINA |
| GET | `/mesas` | MOZO |
| POST | `/pedidos/mesa`, `/pedidos/para-llevar` | MOZO |
| POST | `/cuentas/{id}/pedidos`, `/cuentas/{id}/taper` | MOZO |
| DELETE | `/pedidos/{id}/items/{itemId}` | ADMIN |
| GET | `/cocina/cola`, `/cocina/porciones` | COCINA |
| PATCH | `/pedidos/{id}/estado` | COCINA, MOZO |
| POST | `/cajas/apertura` · `/cajas/{id}/cierre` · `/cuentas/{id}/pagos` | ADMIN |
| POST | `/gastos` | ADMIN |
| GET | `/reportes/ventas-dia`, `/reportes/ganancia-mes`, `/reportes/ranking` | ADMIN |
| POST | `/avisos` | MOZO |
| PATCH | `/avisos/{id}/estado` | COCINA |
| GET/PUT | `/insumos` | ADMIN |

WebSocket: endpoint `/ws`, tópicos `/topic/cocina`, `/topic/avisos`, `/topic/mesas`, `/topic/pedidos-listos`. El handshake exige JWT.

## Prohibido

- Editar V1/V2, desactivar o debilitar pruebas para que pasen, `@Disabled` sin motivo escrito.
- Subir secretos. Variables nuevas van a `.env.example` con valor de ejemplo.
- Implementar frontend, delivery, facturación SUNAT o integración con Yape.
