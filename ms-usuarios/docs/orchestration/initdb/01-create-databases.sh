#!/usr/bin/env bash
# =========================================================================
# Script ejecutado UNA VEZ por el contenedor de Postgres en su primer arranque
# (se monta en /docker-entrypoint-initdb.d/). Crea una BD por microservicio
# para mantener aislamiento de schemas y permitir backups/restores granulares.
# =========================================================================
set -euo pipefail

create_database() {
    local db="$1"
    echo "  Creando base de datos '$db' si no existe..."
    psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        SELECT 'CREATE DATABASE $db'
        WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db')\gexec
EOSQL
}

# Una BD por servicio. Agregar líneas a medida que se sumen MS al ecosistema.
create_database hapi_fhir
create_database db_ms_usuarios
create_database db_ms_red_centros
create_database db_ms_agenda
create_database db_ms_ficha_clinica
create_database db_ms_urgencias_flujo
