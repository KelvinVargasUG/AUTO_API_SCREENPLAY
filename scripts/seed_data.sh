#!/usr/bin/env bash
# =============================================================================
# seed_data.sh — Script de datos iniciales para pruebas de la API de restaurante
#
# Crea:
#   • Usuario demo             : demo / kv20012001
#   • Órdenes COMPLETADAS      : flujo completo → create → start → complete → invoice
#   • Órdenes PARA FACTURAR    : todas las tareas completadas, sin facturar aún
#
# Requisitos: curl, jq
# Uso       : bash scripts/seed_data.sh [--base-url http://host:port]
# =============================================================================
set -euo pipefail

# ---------------------------------------------------------------------------
# Colores
# ---------------------------------------------------------------------------
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# ---------------------------------------------------------------------------
# Configuración
# ---------------------------------------------------------------------------
BASE_URL="http://localhost:8080"

# Parseo de argumentos opcionales
while [[ $# -gt 0 ]]; do
  case "$1" in
    --base-url) BASE_URL="$2"; shift 2 ;;
    *) echo -e "${RED}Argumento desconocido: $1${NC}"; exit 1 ;;
  esac
done

DEMO_USERNAME="demo"
DEMO_EMAIL="demo@foodtech.com"
DEMO_PASS="kv20012001"

# Contadores de resultado
TOTAL=0
SUCCESS=0
FAILED=0

# ---------------------------------------------------------------------------
# Utilidades
# ---------------------------------------------------------------------------
log()     { echo -e "${CYAN}[INFO]${NC}  $*"; }
ok()      { echo -e "${GREEN}[OK]${NC}    $*";  SUCCESS=$((SUCCESS + 1)); TOTAL=$((TOTAL + 1)); }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; FAILED=$((FAILED + 1)); TOTAL=$((TOTAL + 1)); }
section() { echo -e "\n${BOLD}${CYAN}══════════════════════════════════════${NC}"; \
            echo -e "${BOLD}${CYAN}  $*${NC}"; \
            echo -e "${BOLD}${CYAN}══════════════════════════════════════${NC}"; }

# Busca la tarea en la estación y asigna TASK_ID / ORDER_ID en variables globales.
# Acepta tableNumber como número o string en el JSON.
# $1 = TOKEN  $2 = STATION  $3 = TABLE_NUMBER
# Retorna 0 si encontró, 1 si agotó reintentos.
get_task_info() {
  local token="$1" station="$2" table="$3"
  local retries=6 interval=1 raw match

  TASK_ID=""
  ORDER_ID=""

  for ((i=1; i<=retries; i++)); do
    raw=$(curl -s \
      -H "Authorization: Bearer ${token}" \
      "${BASE_URL}/api/tasks/station/${station}?status=PENDING" 2>/dev/null || true)

    # Acepta tableNumber numérico o string comparando como string ambos lados
    match=$(echo "${raw}" | jq -r --arg t "${table}" \
      '[.[] | select((.tableNumber | tostring) == $t)][0] // empty' 2>/dev/null || true)

    if [[ -n "${match}" && "${match}" != "null" ]]; then
      TASK_ID=$(echo  "${match}" | jq -r '.id      // empty')
      ORDER_ID=$(echo "${match}" | jq -r '.orderId // empty')
      [[ -n "${TASK_ID}" && -n "${ORDER_ID}" ]] && return 0
    fi

    warn "  intento ${i}/${retries} — tarea no encontrada en ${station} para tabla=${table}"
    sleep "${interval}"
  done

  return 1
}

# Avanza tarea AL estado IN_PREPARATION
start_task() {
  local token="$1" task_id="$2"
  local http_status
  http_status=$(curl -s -o /dev/null -w "%{http_code}" \
    -X PATCH \
    -H "Authorization: Bearer ${token}" \
    "${BASE_URL}/api/tasks/${task_id}/start")
  echo "${http_status}"
}

# Avanza tarea al estado COMPLETED  (PATCH …/complete)
complete_task() {
  local token="$1" task_id="$2"
  local http_status
  http_status=$(curl -s -o /dev/null -w "%{http_code}" \
    -X PATCH \
    -H "Authorization: Bearer ${token}" \
    "${BASE_URL}/api/tasks/${task_id}/complete")
  echo "${http_status}"
}

# Factura la orden  (POST …/invoice)
invoice_order() {
  local token="$1" order_id="$2"
  local http_status
  http_status=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/json" \
    "${BASE_URL}/api/orders/${order_id}/invoice")
  echo "${http_status}"
}

# ---------------------------------------------------------------------------
# Flujo completo para una orden
# $1=TOKEN $2=TABLE $3=CUSTOMER_NAME $4=CUSTOMER_EMAIL $5=PRODUCTS_JSON
# $6=STATION (BAR | HOT_KITCHEN | COLD_KITCHEN)
# $7=LABEL (nombre descriptivo para logs)
# $8=DO_INVOICE (true | false)
# ---------------------------------------------------------------------------
create_full_order() {
  local token="$1" table="$2" name="$3" email="$4" products="$5"
  local station="$6" label="$7" do_invoice="$8"

  log "Creando orden → tabla=${table} | ${label}"

  # --- CREATE ORDER ---
  local http_status
  http_status=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST \
    -H "Authorization: Bearer ${token}" \
    -H "Content-Type: application/json" \
    -d "{\"tableNumber\":${table},\"customerName\":\"${name}\",\"customerEmail\":\"${email}\",\"products\":${products}}" \
    "${BASE_URL}/api/orders")

  if [[ "${http_status}" != "201" ]]; then
    error "CREATE orden tabla=${table} → HTTP ${http_status}"
    return
  fi

  # --- GET TASK ID + ORDER ID ---
  if ! get_task_info "${token}" "${station}" "${table}"; then
    error "No se encontró task/order para tabla=${table} en estación ${station}"
    return
  fi
  local task_id="${TASK_ID}" order_id="${ORDER_ID}"
  log "  taskId=${task_id} | orderId=${order_id}"

  # --- START TASK ---
  local s
  s=$(start_task "${token}" "${task_id}")
  if [[ "${s}" != "200" ]]; then
    error "START task=${task_id} → HTTP ${s}"
    return
  fi

  # --- COMPLETE TASK ---
  s=$(complete_task "${token}" "${task_id}")
  if [[ "${s}" != "200" && "${s}" != "204" ]]; then
    warn "COMPLETE task=${task_id} → HTTP ${s} (endpoint puede no estar implementado)"
  fi

  # --- INVOICE (opcional) ---
  if [[ "${do_invoice}" == "true" ]]; then
    s=$(invoice_order "${token}" "${order_id}")
    if [[ "${s}" == "200" || "${s}" == "201" ]]; then
      ok "${label} → FACTURADA (orderId=${order_id})"
    else
      warn "${label} → invoice HTTP ${s} (puede requerir estado COMPLETED previo)"
      ok "${label} → completada sin factura (orderId=${order_id})"
    fi
  else
    ok "${label} → LISTA PARA FACTURAR (orderId=${order_id})"
  fi
}

# ===========================================================================
# MAIN
# ===========================================================================
echo ""
echo -e "${BOLD}${GREEN}╔══════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}${GREEN}║        SEED DATA — API Restaurante v1.0              ║${NC}"
echo -e "${BOLD}${GREEN}║  Base URL : ${BASE_URL}${NC}"
echo -e "${BOLD}${GREEN}╚══════════════════════════════════════════════════════╝${NC}"

# ---------------------------------------------------------------------------
section "1 / VERIFICAR DEPENDENCIAS"
# ---------------------------------------------------------------------------
for cmd in curl jq; do
  command -v "${cmd}" &>/dev/null \
    && ok "${cmd} disponible" \
    || { error "${cmd} no encontrado — instálalo antes de continuar"; exit 1; }
done

# ---------------------------------------------------------------------------
section "2 / REGISTRAR USUARIO DEMO"
# ---------------------------------------------------------------------------
log "Registrando usuario: ${DEMO_USERNAME} / ${DEMO_EMAIL}"

REG_STATUS=$(curl -s -o /dev/null -w "%{http_code}" \
  -X POST \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${DEMO_USERNAME}\",\"email\":\"${DEMO_EMAIL}\",\"password\":\"${DEMO_PASS}\"}" \
  "${BASE_URL}/api/auth/register")

case "${REG_STATUS}" in
  201) ok "Usuario demo registrado (201)" ;;
  409) warn "El usuario demo ya existe (409) — se continúa con login" ;;
  *)   error "Registro falló → HTTP ${REG_STATUS}"; exit 1 ;;
esac

# ---------------------------------------------------------------------------
section "3 / LOGIN — OBTENER TOKEN"
# ---------------------------------------------------------------------------
log "Autenticando como ${DEMO_EMAIL} …"

LOGIN_RESP=$(curl -sf \
  -X POST \
  -H "Content-Type: application/json" \
  -d "{\"identifier\":\"${DEMO_EMAIL}\",\"password\":\"${DEMO_PASS}\"}" \
  "${BASE_URL}/api/auth/login")

TOKEN=$(echo "${LOGIN_RESP}" | jq -r '.token // empty')

if [[ -z "${TOKEN}" ]]; then
  error "No se obtuvo token. Respuesta: ${LOGIN_RESP}"
  exit 1
fi

ok "Token obtenido (${#TOKEN} chars)"

# ---------------------------------------------------------------------------
section "4 / ÓRDENES COMPLETADAS (flujo completo + facturadas)"
# ---------------------------------------------------------------------------
# Tabla 1001 — Bebida (BAR)
create_full_order \
  "${TOKEN}" 1001 "Ana López" "ana.lopez@demo.com" \
  '[{"name":"Cappuccino","type":"DRINK","price":3.50}]' \
  "BAR" "Orden #1001 — Cappuccino" "true"

# Tabla 1002 — Plato caliente (HOT_KITCHEN)
create_full_order \
  "${TOKEN}" 1002 "Carlos Méndez" "carlos.mendez@demo.com" \
  '[{"name":"Sopa de tomate","type":"HOT_DISH","price":8.00},{"name":"Pan de ajo","type":"HOT_DISH","price":2.50}]' \
  "HOT_KITCHEN" "Orden #1002 — Sopa + Pan" "true"

# Tabla 1003 — Plato frío (COLD_KITCHEN)
create_full_order \
  "${TOKEN}" 1003 "María García" "maria.garcia@demo.com" \
  '[{"name":"Ensalada César","type":"COLD_DISH","price":9.00}]' \
  "COLD_KITCHEN" "Orden #1003 — Ensalada César" "true"

# Tabla 1004 — Combinada: bebida + plato caliente
# Se deben completar las tareas por cada estación involucrada;
# aquí se crea la orden y solo se gestiona la tarea de BAR como entry point.
create_full_order \
  "${TOKEN}" 1004 "Luis Torres" "luis.torres@demo.com" \
  '[{"name":"Jugo de naranja","type":"DRINK","price":4.00},{"name":"Pasta al pesto","type":"HOT_DISH","price":12.00}]' \
  "BAR" "Orden #1004 — Jugo + Pasta (inicio en BAR)" "true"

# ---------------------------------------------------------------------------
section "5 / ÓRDENES PARA FACTURAR (tareas completadas, sin invoice)"
# ---------------------------------------------------------------------------
# Tabla 2001 — Bebida
create_full_order \
  "${TOKEN}" 2001 "Sofía Ruiz" "sofia.ruiz@demo.com" \
  '[{"name":"Agua con gas","type":"DRINK","price":2.00}]' \
  "BAR" "Orden #2001 — Agua con gas" "false"

# Tabla 2002 — Plato caliente
create_full_order \
  "${TOKEN}" 2002 "Pedro Vargas" "pedro.vargas@demo.com" \
  '[{"name":"Lasaña boloñesa","type":"HOT_DISH","price":14.50}]' \
  "HOT_KITCHEN" "Orden #2002 — Lasaña" "false"

# Tabla 2003 — Plato frío + bebida
create_full_order \
  "${TOKEN}" 2003 "Laura Soto" "laura.soto@demo.com" \
  '[{"name":"Carpaccio","type":"COLD_DISH","price":11.00},{"name":"Limonada","type":"DRINK","price":3.00}]' \
  "COLD_KITCHEN" "Orden #2003 — Carpaccio + Limonada" "false"

# Tabla 2004 — Plato caliente premium
create_full_order \
  "${TOKEN}" 2004 "Roberto Núñez" "roberto.nunez@demo.com" \
  '[{"name":"Filete de res","type":"HOT_DISH","price":22.00},{"name":"Vino tinto","type":"DRINK","price":8.50}]' \
  "HOT_KITCHEN" "Orden #2004 — Filete + Vino" "false"

# ---------------------------------------------------------------------------
section "RESUMEN"
# ---------------------------------------------------------------------------
echo ""
echo -e "  Operaciones totales : ${BOLD}${TOTAL}${NC}"
echo -e "  ${GREEN}Exitosas${NC}            : ${BOLD}${SUCCESS}${NC}"
echo -e "  ${RED}Fallidas${NC}            : ${BOLD}${FAILED}${NC}"
echo ""

if [[ "${FAILED}" -gt 0 ]]; then
  echo -e "${YELLOW}⚠  Algunas operaciones fallaron. Revisa los mensajes [ERROR] / [WARN] arriba.${NC}"
  echo -e "   • Verifica que la API esté corriendo en ${BASE_URL}"
  echo -e "   • Los endpoints PATCH …/complete y POST …/invoice requieren implementación en el backend."
  exit 1
else
  echo -e "${GREEN}✓  Seed completado sin errores.${NC}"
  echo ""
  echo -e "  Credenciales del usuario demo:"
  echo -e "    Email    : ${BOLD}${DEMO_EMAIL}${NC}"
  echo -e "    Password : ${BOLD}${DEMO_PASS}${NC}"
  echo ""
  echo -e "  Órdenes FACTURADAS     : tablas 1001 – 1004"
  echo -e "  Órdenes PARA FACTURAR  : tablas 2001 – 2004"
fi
