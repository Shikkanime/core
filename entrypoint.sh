#!/bin/sh
set -e

echo "=== Entrypoint starting ==="
echo "Architecture: $(uname -m)"
echo "Kernel: $(uname -r)"

chown -R appuser:appuser . 2>/dev/null || true

# Clean up leftover socket and lock files from previous runs
echo "--- Cleaning up leftover lock files and sockets ---"
rm -f /tmp/.X99-lock /tmp/.X11-unix/X99 2>/dev/null || true
rm -f /run/dbus/pid /run/dbus/system_bus_socket /var/run/dbus/pid /var/run/dbus/system_bus_socket 2>/dev/null || true

# Ensure X11 socket directory exists with world-accessible permissions
echo "--- Setting up X11 socket directory ---"
mkdir -p /tmp/.X11-unix
chmod 1777 /tmp/.X11-unix
ls -ld /tmp/.X11-unix

# Start dbus system bus (Chromium requires it, especially on ARM)
echo "--- Starting D-Bus ---"
mkdir -p /run/dbus
if dbus-daemon --system --fork 2>/dev/null; then
    echo "D-Bus started successfully"
    echo "D-Bus socket exists: $(ls -la /run/dbus/system_bus_socket 2>&1)"
else
    echo "WARNING: D-Bus failed to start (may cause issues on ARM)"
fi

# Check Xvfb binary
echo "--- Checking Xvfb ---"
which Xvfb && Xvfb -version 2>&1 || echo "WARNING: Xvfb version check failed"

echo "--- Starting Xvfb on display :99 ---"
Xvfb :99 -ac -screen 0 1280x1024x24 -nolisten tcp > /tmp/xvfb.log 2>&1 &
Xvfb_PID=$!
echo "Xvfb started with PID=$Xvfb_PID"

WAIT_COUNT=0
for _ in $(seq 1 100); do
    WAIT_COUNT=$((WAIT_COUNT + 1))
    if [ -S /tmp/.X11-unix/X99 ]; then
        echo "Xvfb socket detected after ${WAIT_COUNT} iterations"
        break
    fi

    if ! kill -0 "$Xvfb_PID" 2>/dev/null; then
        echo "Xvfb process died (PID=$Xvfb_PID) after ${WAIT_COUNT} iterations:" >&2
        echo "--- Xvfb log ---" >&2
        cat /tmp/xvfb.log >&2
        echo "--- End Xvfb log ---" >&2
        exit 1
    fi

    sleep 0.1
done

if [ ! -S /tmp/.X11-unix/X99 ]; then
    echo "Xvfb did not become ready after ${WAIT_COUNT} iterations:" >&2
    echo "--- Xvfb log ---" >&2
    cat /tmp/xvfb.log >&2
    echo "--- End Xvfb log ---" >&2
    echo "--- /tmp/.X11-unix contents ---" >&2
    ls -la /tmp/.X11-unix/ >&2
    exit 1
fi

# Make the X11 socket accessible to appuser
echo "--- X11 socket permissions (before chmod) ---"
ls -la /tmp/.X11-unix/
chmod 777 /tmp/.X11-unix/X99 2>/dev/null || true
echo "--- X11 socket permissions (after chmod) ---"
ls -la /tmp/.X11-unix/

# Verify Xvfb is still alive
if kill -0 "$Xvfb_PID" 2>/dev/null; then
    echo "Xvfb is running (PID=$Xvfb_PID)"
else
    echo "ERROR: Xvfb is no longer running!" >&2
    cat /tmp/xvfb.log >&2
    exit 1
fi

# Print Xvfb log for diagnostics
echo "--- Xvfb log ---"
cat /tmp/xvfb.log
echo "--- End Xvfb log ---"

export DISPLAY=:99
echo "DISPLAY=$DISPLAY"

# Quick X11 connectivity test
echo "--- Testing X11 connectivity ---"
if command -v xdpyinfo > /dev/null 2>&1; then
    if xdpyinfo -display :99 > /dev/null 2>&1; then
        echo "xdpyinfo: X11 connection OK"
    else
        echo "WARNING: xdpyinfo failed to connect to :99"
    fi
else
    echo "xdpyinfo not available, skipping X11 test"
fi

# Test as appuser
echo "--- Testing X11 connectivity as appuser ---"
if command -v xdpyinfo > /dev/null 2>&1; then
    if gosu appuser xdpyinfo -display :99 > /dev/null 2>&1; then
        echo "xdpyinfo as appuser: X11 connection OK"
    else
        echo "WARNING: xdpyinfo as appuser FAILED to connect to :99"
    fi
else
    echo "xdpyinfo not available, skipping appuser X11 test"
fi

echo "--- Environment for appuser ---"
echo "  DISPLAY=$DISPLAY"
echo "  XAUTHORITY=${XAUTHORITY:-<not set>}"
echo "  HOME=$(gosu appuser sh -c 'echo $HOME')"
echo "  USER=$(gosu appuser id)"

# Widevine CDM diagnostics
echo "--- Widevine CDM diagnostics ---"
PLAYWRIGHT_PATH="${PLAYWRIGHT_BROWSERS_PATH:-/opt/playwright}"
echo "Playwright browsers path: $PLAYWRIGHT_PATH"

echo "--- WidevineCdm directory structure ---"
find "$PLAYWRIGHT_PATH" -path "*/WidevineCdm*" -exec ls -la {} \; 2>/dev/null || echo "No WidevineCdm files found!"

echo "--- WidevineCdm manifest.json content ---"
find "$PLAYWRIGHT_PATH" -path "*/WidevineCdm/manifest.json" -exec cat {} \; 2>/dev/null || echo "No manifest.json found!"

echo "--- libwidevinecdm.so details ---"
WIDEVINE_SO=$(find "$PLAYWRIGHT_PATH" -name "libwidevinecdm.so" 2>/dev/null | head -1)
if [ -n "$WIDEVINE_SO" ]; then
    echo "Found: $WIDEVINE_SO"
    ls -la "$WIDEVINE_SO"
    echo "File type: $(file "$WIDEVINE_SO" 2>/dev/null)"
    echo "Shared library dependencies:"
    ldd "$WIDEVINE_SO" 2>&1 || echo "ldd failed (might be static or cross-arch)"
else
    echo "ERROR: libwidevinecdm.so NOT FOUND in $PLAYWRIGHT_PATH"
fi

echo "--- Chromium version ---"
CHROME_BIN=$(find "$PLAYWRIGHT_PATH" -name "chrome" -type f 2>/dev/null | head -1)
if [ -n "$CHROME_BIN" ]; then
    echo "Chrome binary: $CHROME_BIN"
    echo "File type: $(file "$CHROME_BIN" 2>/dev/null)"
    "$CHROME_BIN" --version 2>&1 || echo "Could not get Chrome version"
    echo "Chrome directory contents (WidevineCdm):"
    CHROME_DIR=$(dirname "$CHROME_BIN")
    ls -la "$CHROME_DIR/WidevineCdm/" 2>/dev/null || echo "No WidevineCdm in Chrome dir"
    ls -laR "$CHROME_DIR/WidevineCdm/" 2>/dev/null || true
else
    echo "Chrome binary not found"
fi

echo "--- End Widevine CDM diagnostics ---"

echo "=== Starting application with DISPLAY=$DISPLAY ==="
exec gosu appuser "$@"