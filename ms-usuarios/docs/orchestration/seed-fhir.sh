#!/usr/bin/env bash
# =========================================================================
# Siembra recursos FHIR mínimos de demo en el servidor HAPI central.
# Útil para validar el endpoint GET /Patient/{id} expuesto por ms-usuarios.
#
# Uso:
#   ./seed-fhir.sh                  # default: http://localhost:8085/fhir
#   FHIR_URL=http://hapi:8080/fhir ./seed-fhir.sh
# =========================================================================
set -euo pipefail

FHIR_URL="${FHIR_URL:-http://localhost:8085/fhir}"

echo "Creando Patient demo en $FHIR_URL ..."

curl -fsS -X POST "$FHIR_URL/Patient" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Patient",
    "identifier": [{
      "system": "http://rednorte.cl/fhir/identifier/run",
      "value": "12345678-9"
    }],
    "active": true,
    "name": [{
      "given": ["Juan"],
      "family": "Pérez"
    }],
    "gender": "male",
    "birthDate": "1980-05-12",
    "telecom": [{
      "system": "phone",
      "value": "+56912345678"
    }],
    "address": [{
      "use": "home",
      "line": ["Av. Principal 123"],
      "city": "Antofagasta",
      "country": "CL"
    }]
  }' | tee /tmp/seed-patient.json

PATIENT_ID=$(jq -r '.id' /tmp/seed-patient.json 2>/dev/null || echo "?")
echo
echo "Patient creado con id=$PATIENT_ID"
echo "Probar: curl $FHIR_URL/Patient/$PATIENT_ID"
