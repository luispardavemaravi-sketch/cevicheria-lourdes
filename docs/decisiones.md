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
