# Decisiones de diseño

Una línea por decisión no obvia. Formato: `[historia] decisión — por qué`.

- [setup] Spring Boot 3.5.7 → 3.5.16 — 3.5.7 trae Testcontainers 1.21.3, que usa la API 1.32 de Docker y Docker Engine 29 la rechaza (mínimo 1.40); 3.5.16 trae Testcontainers 1.21.4 y es el último patch 3.5.x, como pedía el pom.
- [setup] Maven Wrapper 3.3.4 tipo `only-script` con Maven 3.9.11 — no se versiona ningún .jar binario en el repo; el script descarga Maven la primera vez.
- [setup] `.gitattributes` fuerza LF en `mvnw` — con `core.autocrlf=true` en Windows, un `mvnw` con CRLF rompe el CI de Linux.
- [HU-01] Nuevas excepciones de dominio en `shared`: `NoAutenticadoException` (401) y `AccesoBloqueadoException` (423) — HU-01 exige 401/423 y las tres existentes solo cubren 404/409/422; se agregan al `sealed` y al `GlobalExceptionHandler` existente (no se duplica).
- [HU-01] El login usa `@Transactional(noRollbackFor = {401, 423})` — si no, el rollback borraría el intento fallido y el bloqueo nunca llegaría a guardarse.
- [HU-01] Usuario inexistente y usuario desactivado responden igual (401 `CREDENCIALES_INVALIDAS`) y gastan un BCrypt ficticio — para no revelar qué usernames existen, ni por el mensaje ni por el tiempo de respuesta.
- [HU-01] Con el usuario bloqueado no se verifica el PIN ni se cuentan intentos; el 5.º fallo responde 423 (no 401) — así el cliente sabe que debe esperar.
- [HU-01] El `sub` del JWT es el id del usuario y el rol va en el claim `rol` — no viaja nada personal en el token, y los casos de uso posteriores obtienen `creado_por` directo del token.
- [HU-01] El decoder JWT acepta solo HS256, valida el emisor y la expiración sin tolerancia, usando el `Clock` de la app — un token de otro sistema o firmado con otro algoritmo no entra.
- [HU-01] Roles con `@PreAuthorize` en cada endpoint + `RoleHierarchy` (ADMIN > MOZO) también en el `MethodSecurityExpressionHandler` — sin eso, `hasRole('MOZO')` en métodos ignoraría la jerarquía (RN-16).
- [HU-01] Username normalizado a minúsculas (`Mama` = `mama`); en el login no se valida su formato — un login nunca debe responder 422.
- [HU-01] `ADMIN_PIN` mal formado hace fallar el arranque; si faltan las variables solo hay WARN — una configuración incorrecta debe notarse, una ausente es el caso normal tras el primer arranque.
- [HU-01] `JWT_SECRET` ausente o < 32 bytes impide arrancar (validación en `JwtProperties`) — mejor no arrancar que firmar con un secreto débil.
- [HU-01] Mapeo entidad↔dominio a mano (`UsuarioEntity.desde/aDominio`); MapStruct solo para DTO web — el dominio no tiene setters ni constructor público y MapStruct no puede usar `reconstituir` sin configuración extra.
- [HU-01] Pruebas `*IT` heredan de `IntegracionBase`: un solo PostgreSQL para toda la corrida — arrancar uno por clase haría el build cada vez más lento; cada prueba usa datos propios.
- [HU-01] La configuración de pruebas va en `src/test/resources/config/application.yml` — se suma al `application.yml` principal (no lo reemplaza) y aplica también a `@WebMvcTest`.
- [HU-03] Presentaciones (HU-04) y acompañamientos (HU-05) viajan dentro del cuerpo de POST/PUT `/productos`; sin rutas nuevas — decidido con el usuario: la tabla de API solo tiene esas rutas.
- [HU-03] Baja de producto = PUT con `activo=false` (lógica); `GET ?incluirInactivos=true` las muestra — decidido con el usuario: `detalle_pedido` referencia productos y no hay DELETE en la tabla de API.
- [HU-03] La categoría no se puede cambiar (422 `CATEGORIA_NO_MODIFICABLE`) — decidido con el usuario: una BEBIDA con presentaciones no puede volverse PLATO sin precio.
- [HU-03] BEBIDA no lleva precio propio (422 `PRECIO_EN_PRESENTACION`); PLATO y TAPER exigen precio > 0 — así lo modela el CHECK de V1 (`precio` NULL solo en BEBIDA) y HU-04 pone el precio en la presentación.
- [HU-03] `requiereCocina` es opcional; por defecto true en PLATO y false en BEBIDA/TAPER (RN-06) — permite un plato que no pase por cocina sin obligar a enviarlo siempre.
- [HU-03] Nombre único sin importar mayúsculas ni espacios repetidos: validado en el caso de uso (409) y con el índice único `lower(nombre)` en V3 — el chequeo previo no evita la carrera entre dos altas simultáneas; el índice sí.
- [HU-03] `Dinero` (record, escala 2, HALF_UP) en `shared.domain.model`; `equals` por valor porque normaliza la escala al construir — `15.0` y `15.00` son el mismo monto.
- [HU-03] El controller recibe `page`/`size` como `@RequestParam` y arma `Paginacion` (dominio, máx. 100); `Pageable` solo vive en el adaptador de persistencia — `ArchitectureTest` prohíbe `org.springframework.data` en `adapter.in.web`, y la regla 8 (nunca `findAll()` sin límite) se cumple igual.
- [HU-03] Listados devuelven `PaginaResponse` propio, no `Page` de Spring — la serialización de `PageImpl` no es estable entre versiones de Spring Data y no debe filtrarse al contrato de la API.
- [HU-04] Toda BEBIDA necesita al menos una presentación (422 `PRESENTACION_REQUERIDA`); PLATO/TAPER no pueden tener (422 `PRESENTACION_SOLO_EN_BEBIDA`) — sin presentación la bebida no tendría precio y nunca se podría pedir.
- [HU-04] En PUT la lista de presentaciones es la completa deseada: se emparejan por nombre sin mayúsculas (conservan su id), las nuevas se agregan y las ausentes quedan inactivas (V4 agrega `presentacion.activo`) — `detalle_pedido.presentacion_id` las referencia y borrarlas rompería pedidos viejos; conservar el id mantiene válido lo que tenga en pantalla el mozo.
- [HU-04] Toda la lista se valida antes de modificar el producto — un PUT inválido no deja el agregado a medio cambiar.
- [HU-04] `Producto.precioPara(presentacionId)` concentra "pedir una bebida exige presentación" (422 `PRESENTACION_REQUERIDA` / `PRESENTACION_NO_VALIDA` / `PRESENTACION_NO_APLICA`) — HU-07 solo lo llama al congelar el precio (RN-02); la regla vive en el dominio, no en el controller.
- [HU-04] `hibernate.default_batch_fetch_size=100` — al listar una página de productos las presentaciones se cargan en una consulta `IN (...)` y no una por producto (N+1).
- [HU-04] La respuesta solo muestra presentaciones activas, ordenadas por precio — es lo que el mozo puede vender; las inactivas solo importan para el historial.
- [HU-05] Acompañamientos como catálogo compartido resuelto por nombre sin mayúsculas (se crean si no existen) + índice único `lower(nombre)` en V5 — decidido con el usuario (por nombre en el cuerpo del producto); el índice evita duplicados por carrera, como V3.
- [HU-05] Solo un PLATO tiene acompañamientos (422 `ACOMPANAMIENTO_SOLO_EN_PLATO`); nombres repetidos → 422 `ACOMPANAMIENTO_DUPLICADO` — mismas reglas y mismo estilo que las presentaciones de HU-04.
- [HU-05] En PUT la lista es la completa; si se omite, el plato queda sin acompañamientos — semántica de reemplazo total coherente con PUT; se documenta en Swagger.
- [HU-05] Si una regla falla después de crear acompañamientos nuevos, el rollback de la transacción los deshace (probado en `ProductosIT`) — se prefirió eso a duplicar el chequeo de categoría antes de resolver el catálogo.
- [HU-05] `ProductoEntity` recibe los acompañamientos como `getReferenceById` (proxies gestionados), sin cascada — se evita depender de cómo Hibernate trata entidades detached en un `@ManyToMany` al hacer persist/merge.
