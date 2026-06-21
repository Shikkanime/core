#!/bin/sh
set -e

# Applique les droits à TOUT le dossier courant (/app) de manière récursive.
# Cela inclut donc 'data', 'dumps' et tout autre volume monté dans ce dossier.
chown -R appuser:appuser . 2>/dev/null || true

# Démarre Xvfb en tâche de fond (pas besoin de xauth ou de HOME grâce à -ac)
Xvfb :99 -ac -screen 0 1280x1024x24 > /dev/null 2>&1 &

# Exporte le DISPLAY pour que Playwright/Chromium le trouve
export DISPLAY=:99

# Lance la commande en tant que 'appuser'
exec gosu appuser "$@"