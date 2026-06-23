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
which Xvfb || echo "WARNING: Xvfb binary not found"

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
echo "  MOZ_GMP_PATH=${MOZ_GMP_PATH:-<not set>}"
echo "  SHIKKANIME_BROWSER=${SHIKKANIME_BROWSER:-<not set>}"
echo "  XAUTHORITY=${XAUTHORITY:-<not set>}"
echo "  HOME=$(gosu appuser sh -c 'echo $HOME')"
echo "  USER=$(gosu appuser id)"

# Widevine CDM diagnostics
echo "--- Widevine CDM diagnostics ---"
PLAYWRIGHT_PATH="${PLAYWRIGHT_BROWSERS_PATH:-/opt/playwright}"
echo "Playwright browsers path: $PLAYWRIGHT_PATH"
echo "Raspberry Pi Widevine path: /opt/WidevineCdm"
echo "Asahi Widevine fallback path: /var/lib/widevine"

for WIDEVINE_ROOT in /opt/WidevineCdm /var/lib/widevine; do
if [ -d "$WIDEVINE_ROOT" ]; then
    echo "--- $WIDEVINE_ROOT structure ---"
    find "$WIDEVINE_ROOT" -maxdepth 5 -exec ls -ld {} \; 2>/dev/null || true

    echo "--- $WIDEVINE_ROOT symlinks ---"
    find "$WIDEVINE_ROOT" -maxdepth 5 -type l -exec sh -c '
        for link do
            echo "$link -> $(readlink "$link")"
        done
    ' sh {} + 2>/dev/null || true

    echo "--- $WIDEVINE_ROOT manifest.json content ---"
    cat "$WIDEVINE_ROOT/manifest.json" 2>/dev/null || echo "No $WIDEVINE_ROOT/manifest.json found"
else
    echo "No $WIDEVINE_ROOT directory found"
fi
done

echo "--- Firefox/GMP Widevine path ---"
ls -la "${MOZ_GMP_PATH:-/opt/WidevineCdm/gmp-widevinecdm/latest}" 2>/dev/null || echo "No GMP Widevine path found"

echo "--- WidevineCdm directory structure ---"
find -L "$PLAYWRIGHT_PATH" -path "*/WidevineCdm*" -exec ls -ld {} \; 2>/dev/null || echo "No WidevineCdm files found!"

echo "--- Playwright WidevineCdm symlinks ---"
find "$PLAYWRIGHT_PATH" -type l -name "WidevineCdm" -exec sh -c '
    for link do
        echo "$link -> $(readlink "$link")"
    done
' sh {} + 2>/dev/null || echo "No Playwright WidevineCdm symlink found"

echo "--- WidevineCdm manifest.json content ---"
find -L "$PLAYWRIGHT_PATH" -path "*/WidevineCdm/manifest.json" -exec sh -c '
    for manifest do
        echo "--- $manifest"
        cat "$manifest"
    done
' sh {} + 2>/dev/null || echo "No manifest.json found!"

echo "--- libwidevinecdm.so details ---"
WIDEVINE_SO_COUNT=0
find -L "$PLAYWRIGHT_PATH" /opt/WidevineCdm /var/lib/widevine -name "libwidevinecdm.so" 2>/dev/null | sort | while read -r WIDEVINE_SO; do
    WIDEVINE_SO_COUNT=$((WIDEVINE_SO_COUNT + 1))
    echo "--- $WIDEVINE_SO"
    ls -la "$WIDEVINE_SO"
    echo "File type: $(file "$WIDEVINE_SO" 2>/dev/null)"
    if gosu appuser test -r "$WIDEVINE_SO"; then
        echo "Readable by appuser: yes"
    else
        echo "Readable by appuser: no"
    fi
    if [ -s "$WIDEVINE_SO" ]; then
        echo "Shared library dependencies:"
        ldd "$WIDEVINE_SO" 2>&1 || echo "ldd failed (might be static or cross-arch)"
    else
        echo "Skipping ldd: file is empty"
    fi
done
if ! find -L "$PLAYWRIGHT_PATH" /opt/WidevineCdm /var/lib/widevine -name "libwidevinecdm.so" -print -quit 2>/dev/null | grep -q .; then
    echo "ERROR: libwidevinecdm.so NOT FOUND in $PLAYWRIGHT_PATH, /opt/WidevineCdm or /var/lib/widevine"
fi

echo "--- Chromium version ---"
echo "--- Chromium binaries found ---"
find "$PLAYWRIGHT_PATH" -path "*/chrome-linux/chrome" -type f 2>/dev/null | sort | while read -r chrome; do
    echo "--- $chrome"
    echo "File type: $(file "$chrome" 2>/dev/null)"
    "$chrome" --version 2>&1 || echo "Could not get Chrome version"
done

CHROME_BIN=$(find "$PLAYWRIGHT_PATH" -name "chrome" -type f 2>/dev/null | sort | head -1)
if [ -n "$CHROME_BIN" ]; then
    echo "Chrome binary: $CHROME_BIN"
    echo "File type: $(file "$CHROME_BIN" 2>/dev/null)"
    "$CHROME_BIN" --version 2>&1 || echo "Could not get Chrome version"
    echo "Chrome directory contents (WidevineCdm):"
    CHROME_DIR=$(dirname "$CHROME_BIN")
    ls -la "$CHROME_DIR" 2>/dev/null || true
    if [ -L "$CHROME_DIR/WidevineCdm" ]; then
        echo "$CHROME_DIR/WidevineCdm -> $(readlink "$CHROME_DIR/WidevineCdm")"
    fi
    ls -la "$CHROME_DIR/WidevineCdm/" 2>/dev/null || echo "No WidevineCdm in Chrome dir"
    find -L "$CHROME_DIR/WidevineCdm" -maxdepth 5 -exec ls -ld {} \; 2>/dev/null || true
else
    echo "Chrome binary not found"
fi

echo "--- Firefox binaries found ---"
find "$PLAYWRIGHT_PATH" -path "*/firefox/firefox" -type f 2>/dev/null | sort | while read -r firefox; do
    echo "--- $firefox"
    echo "File type: $(file "$firefox" 2>/dev/null)"
    "$firefox" --version 2>&1 || echo "Could not get Firefox version"
done

echo "--- End Widevine CDM diagnostics ---"

echo "=== Starting application with DISPLAY=$DISPLAY ==="
exec gosu appuser "$@"