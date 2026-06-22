#!/bin/sh
set -e

chown -R appuser:appuser . 2>/dev/null || true

Xvfb_PID=$(Xvfb :99 -ac -screen 0 1280x1024x24 > /tmp/xvfb.log 2>&1 & echo $!)
timeout=50; current=0
while [ $current -lt $timeout ]; do
    if [ -S /tmp/.X11-unix/X99 ]; then break; fi
    if ! kill -0 "$Xvfb_PID" 2>/dev/null; then
        echo "Xvfb failed to start:" >&2
        cat /tmp/xvfb.log >>/dev/stderr
        exit 1
    fi
    sleep 0.1
    current=$((current + 1))
done

if ! [ -S /tmp/.X11-unix/X99 ]; then
    echo "Xvfb did not become ready in time:" >&2
    cat /tmp/xvfb.log >>/dev/stderr
    exit 1
fi

export DISPLAY=:99
exec gosu appuser "$@"