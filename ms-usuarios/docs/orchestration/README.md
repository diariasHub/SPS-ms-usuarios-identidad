# Snippets de orquestación para ms-usuarios-identidad

Archivos pensados para copiar/pegar al repo donde vive el `docker-compose.yaml`
(el orquestador). NO se ejecutan desde este repo.

## Contenido

- `.env.example` — variables que el orquestador debe definir (Postgres, FHIR, IdP).
- `initdb/01-create-databases.sh` — montar en el contenedor de Postgres en
  `/docker-entrypoint-initdb.d/` para crear una BD por microservicio en el primer
  arranque (database-per-service).
- `docker-compose.snippet.yaml` — bloques sugeridos para reemplazar/agregar en
  tu compose. NO es un compose completo.

## Pasos en el repo orquestador

1. Copiar `.env.example` como `.env` y rellenar credenciales reales.
2. Copiar la carpeta `initdb/` a la raíz del repo orquestador (junto al
   `docker-compose.yaml`).
3. Hacer ejecutable el script: `chmod +x initdb/01-create-databases.sh`.
4. Aplicar los bloques de `docker-compose.snippet.yaml` sobre el compose
   existente (reemplazar `postgres`, `hapi-fhir`, `ms-usuarios-identidad`
   y agregar el ajuste de `ms-api-gateway`).
5. Si ya hay un volumen `postgres_data` con datos previos, eliminarlo para
   que el script de init corra:
   `docker compose down -v && docker compose up --build`.

## Notas

- El script de init **solo corre en el primer arranque** (cuando el volumen
  está vacío). Para forzarlo, hay que borrar el volumen.
- Si vas a usar IdP real (Keycloak/Azure AD), agrega su servicio al compose
  y apunta `IDP_ISSUER_URI` / `IDP_JWK_SET_URI` al realm correspondiente.
- Mientras no haya IdP, ms-usuarios-identidad puede correr en perfil `dev`
  (cambiar `SPRING_PROFILES_ACTIVE: dev` y omitir las vars `IDP_*` y
  `SPRING_DATASOURCE_*`). Pierdes persistencia (H2 in-memory) y aislamiento
  de roles, pero ganas un arranque standalone sin dependencias.
