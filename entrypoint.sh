#!/bin/sh
# Starts a module image: the schema is provisioned first, then the module runs with the arguments
# the container was given. Set SKIP_SCHEMA_UPDATE=true to start against a database already
# provisioned by another container.
set -eu

if [ "${MODULE_NAME}" != "database" ] && [ "${SKIP_SCHEMA_UPDATE:-false}" != "true" ]; then
    echo "Provisioning the database schema..."
    "/app/database/bin/database"
fi

exec "/app/${MODULE_NAME}/bin/${MODULE_NAME}" "$@"
