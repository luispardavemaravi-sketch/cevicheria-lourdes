# Cevichería Lourdes — Sistema de Pedidos y Caja

Pedidos de mesa y para llevar, pantalla de cocina en tiempo real, avisos de reposición, caja y reportes para una cevichería de 14 mesas.

| Carpeta | Contenido |
| --- | --- |
| `api/` | Backend Java 21 + Spring Boot 3, arquitectura hexagonal, PostgreSQL, Flyway |
| `web/` | Frontend Angular + Tailwind (PWA) — desde el Sprint 2 |
| `infra/` | Docker Compose para desarrollo y despliegue |
| `docs/` | Especificación de requisitos y diseño (historias, RF, RN, diagramas) |

## Requisitos

- Java 21, Maven 3.9+, Docker Desktop.

## Primer arranque

```bash
cp .env.example .env                                   # y cambia DB_PASSWORD
cd infra && docker compose --env-file ../.env up -d db # levanta PostgreSQL 17
cd ../api
set -a && source ../.env && set +a                     # carga las variables (PowerShell: ver abajo)
./mvnw spring-boot:run                                 # Flyway crea las 15 tablas al arrancar
```

En PowerShell: `Get-Content ..\.env | ForEach-Object { $k,$v = $_ -split '=',2; if ($k -and $k -notmatch '^#') { Set-Item "env:$k" $v } }`

- Health: http://localhost:8080/actuator/health
- Swagger: http://localhost:8080/swagger-ui.html

## Pruebas

```bash
cd api && ./mvnw verify   # unitarias + integración (necesita Docker abierto) + cobertura en target/site/jacoco
```

## Convenciones

- Una rama por historia: `feature/HU-01-login-pin`. Nada entra a `main` sin pull request y CI en verde.
- Commits: `feat(pedidos): crear pedido de mesa [HU-07]` (Conventional Commits).
- `domain` no importa Spring ni JPA (lo verifica `ArchitectureTest`).
- Errores siempre en formato ProblemDetail con `code` estable.
