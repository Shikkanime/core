#!/bin/sh
set -e

# Applique les droits à TOUT le dossier courant (/app) de manière récursive.
# Cela inclut donc 'data', 'dumps' et tout autre volume monté dans ce dossier.
chown -R appuser:appuser . 2>/dev/null || true

# Lance la commande en tant que 'appuser'
exec gosu appuser "$@"