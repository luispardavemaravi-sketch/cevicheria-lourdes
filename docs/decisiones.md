# Decisiones de diseño

Una línea por decisión no obvia. Formato: `[historia] decisión — por qué`.

- [setup] Spring Boot 3.5.7 → 3.5.16 — 3.5.7 trae Testcontainers 1.21.3, que usa la API 1.32 de Docker y Docker Engine 29 la rechaza (mínimo 1.40); 3.5.16 trae Testcontainers 1.21.4 y es el último patch 3.5.x, como pedía el pom.
- [setup] Maven Wrapper 3.3.4 tipo `only-script` con Maven 3.9.11 — no se versiona ningún .jar binario en el repo; el script descarga Maven la primera vez.
- [setup] `.gitattributes` fuerza LF en `mvnw` — con `core.autocrlf=true` en Windows, un `mvnw` con CRLF rompe el CI de Linux.
