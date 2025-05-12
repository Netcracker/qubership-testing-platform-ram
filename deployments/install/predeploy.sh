#!/usr/bin/env sh

if [ ! -f ./atp-common-scripts/openshift/common.sh ]; then
  echo "ERROR: Cannot locate ./atp-common-scripts/openshift/common.sh"
  exit 1
fi

# shellcheck source=../atp-common-scripts/openshift/common.sh
. ./atp-common-scripts/openshift/common.sh

case ${PAAS_PLATFORM:-OPENSHIFT} in
  COMPOSE)
    echo "***** Copying SSL certificates *****"
    mkdir -p data -m 777 && cp -r ssl/ data/
    ;;
  OPENSHIFT|KUBERNETES)
    ;;
  *)
    echo "ERROR: Unsupported PAAS_PLATFORM '${PAAS_PLATFORM}'. Expected values: COMPOSE, OPENSHIFT, KUBERNETES"
    exit 1
esac

_ns="${NAMESPACE}"

RAM_DB="$(env_default "${RAM_DB}" "${SERVICE_NAME}" "${_ns}")"
RAM_DB_USER="$(env_default "${RAM_DB_USER}" "${SERVICE_NAME}" "${_ns}")"
RAM_DB_PASSWORD="$(env_default "${RAM_DB_PASSWORD}" "${SERVICE_NAME}" "${_ns}")"

init_mongo "${MONGO_DB_ADDR}" "${RAM_DB}" "${RAM_DB_USER}" "${RAM_DB_PASSWORD}" "${MONGO_DB_PORT}" "${mongo_user}" "${mongo_pass}"

GRIDFS_DB="$(env_default "${GRIDFS_DB}" "atp_gridfs" "${_ns}")"
GRIDFS_DB_USER="$(env_default "${GRIDFS_DB_USER}" "atp_gridfs" "${_ns}")"
GRIDFS_DB_PASSWORD="$(env_default "${GRIDFS_DB_PASSWORD}" "atp_gridfs" "${_ns}")"

init_mongo "${GRIDFS_DB_ADDR}" "${GRIDFS_DB}" "${GRIDFS_DB_USER}" "${GRIDFS_DB_PASSWORD}" "${GRIDFS_DB_PORT}" "${gridfs_user}" "${gridfs_pass}"

EI_GRIDFS_DB="$(env_default "${EI_GRIDFS_DB}" "atp-ei-gridfs" "${_ns}")"
EI_GRIDFS_USER="$(env_default "${EI_GRIDFS_USER}" "atp-ei-gridfs" "${_ns}")"
EI_GRIDFS_PASSWORD="$(env_default "${EI_GRIDFS_PASSWORD}" "atp-ei-gridfs" "${_ns}")"

init_mongo "${EI_GRIDFS_DB_ADDR:-$GRIDFS_DB_ADDR}" "${EI_GRIDFS_DB}" "${EI_GRIDFS_USER}" "${EI_GRIDFS_PASSWORD}" "${EI_GRIDFS_DB_PORT:-$GRIDFS_DB_PORT}" "${ei_gridfs_user:-$gridfs_user}" "${ei_gridfs_pass:-$gridfs_pass}"
