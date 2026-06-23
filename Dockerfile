ARG JAVA_VERSION=25
ARG PLAYWRIGHT_VERSION=1.60.0

FROM amazoncorretto:${JAVA_VERSION} AS java
FROM node:25-bookworm-slim
ARG JAVA_VERSION
COPY --from=java /usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto /usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto
COPY --from=java /usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto/lib/security/cacerts /usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto/lib/security/cacerts

ENV LANG=C.UTF-8 \
    JAVA_HOME=/usr/lib/jvm/java-${JAVA_VERSION}-amazon-corretto \
    TZ=Europe/Paris \
    DEBIAN_FRONTEND=noninteractive \
    DISPLAY=:99 \
    MOZ_GMP_PATH=/opt/WidevineCdm/gmp-widevinecdm/latest \
    PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 \
    PLAYWRIGHT_BROWSERS_PATH=/opt/playwright

ARG PLAYWRIGHT_VERSION
# Install necessary packages and set timezone
RUN mkdir -p ${PLAYWRIGHT_BROWSERS_PATH} && \
    npx -y playwright@${PLAYWRIGHT_VERSION} install --with-deps chromium && \
    rm -rf ~/.npm && \
    apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates curl unzip wget fonts-dejavu fontconfig libfreetype6 tzdata webp gosu xvfb xauth dbus x11-utils file && \
    ARCH="$(dpkg --print-architecture)" && \
    if [ "$ARCH" = "amd64" ]; then \
      echo "Downloading Widevine for x86_64..." && \
      wget -q https://dl.google.com/linux/direct/google-chrome-stable_current_amd64.deb -O /tmp/chrome.deb && \
      dpkg-deb -x /tmp/chrome.deb /tmp/chrome && \
      find ${PLAYWRIGHT_BROWSERS_PATH} -type d -name "chrome-linux*" | while read -r dir; do \
        cp -rL /tmp/chrome/opt/google/chrome/WidevineCdm "$dir/"; \
      done && \
      rm -rf /tmp/chrome* ; \
    elif [ "$ARCH" = "arm64" ]; then \
      echo "Installing Raspberry Pi OS libwidevinecdm0 for arm64..." && \
      mkdir -p /etc/apt/keyrings && \
      wget -q https://archive.raspberrypi.com/debian/raspberrypi.gpg.key -O /etc/apt/keyrings/raspberrypi.asc && \
      echo "deb [arch=arm64 signed-by=/etc/apt/keyrings/raspberrypi.asc] http://archive.raspberrypi.com/debian/ bookworm main" > /etc/apt/sources.list.d/raspberrypi.list && \
      apt-get update && \
      apt-get install -y --no-install-recommends libwidevinecdm0 && \
      echo "Patching Raspberry Pi Widevine manifest for Linux arm64..." && \
      node -e "\
const fs=require('fs'),f='/opt/WidevineCdm/manifest.json',m=JSON.parse(fs.readFileSync(f,'utf8'));\
m.platforms=[{os:'linux',arch:'arm64',sub_package_path:'_platform_specific/linux_arm64/'},{os:'linux',arch:'x64',sub_package_path:'_platform_specific/linux_x64/'}];\
m['x-cdm-persistent-license-support']=false;\
fs.writeFileSync(f,JSON.stringify(m,null,2));" && \
      mkdir -p /opt/WidevineCdm/_platform_specific/linux_x64 && \
      touch /opt/WidevineCdm/_platform_specific/linux_x64/libwidevinecdm.so && \
      find ${PLAYWRIGHT_BROWSERS_PATH} -type d -name "chrome-linux*" | while read -r dir; do \
        rm -rf "$dir/WidevineCdm" && \
        ln -s /opt/WidevineCdm "$dir/WidevineCdm" && \
        echo "Linked Raspberry Pi WidevineCdm into $dir"; \
      done && \
      echo "Effective Widevine manifests:" && \
      find -L /opt/WidevineCdm ${PLAYWRIGHT_BROWSERS_PATH} -path "*/WidevineCdm/manifest.json" -exec sh -c 'echo "--- $1"; cat "$1"' sh {} \; && \
      echo "Effective Widevine libraries:" && \
      find -L /opt/WidevineCdm ${PLAYWRIGHT_BROWSERS_PATH} -name "libwidevinecdm.so" -exec sh -c '\
        for file do \
          echo "--- $file"; \
          ls -la "$file"; \
          file "$file"; \
        done' sh {} + && \
      find ${PLAYWRIGHT_BROWSERS_PATH} -type l -name "WidevineCdm" -exec sh -c '\
        for link do \
          echo "--- $link -> $(readlink "$link")"; \
        done' sh {} + && \
      find /opt/WidevineCdm -maxdepth 4 -type l -exec sh -c '\
        for link do \
          echo "--- $link -> $(readlink "$link")"; \
        done' sh {} + ; \
    else \
      echo "Unsupported architecture for Widevine: $ARCH" && \
      exit 1 ; \
    fi && \
    if [ "$ARCH" = "amd64" ]; then \
      echo "Patching WidevineCdm manifest.json for Linux amd64..." && \
      find ${PLAYWRIGHT_BROWSERS_PATH} -path "*/WidevineCdm/manifest.json" | while read -r manifest; do \
        node -e "\
const fs=require('fs'),f=process.argv[1],m=JSON.parse(fs.readFileSync(f,'utf8'));\
m.platforms=[{os:'linux',arch:'x64',sub_package_path:'_platform_specific/linux_x64/'}];\
m['x-cdm-persistent-license-support']=false;\
fs.writeFileSync(f,JSON.stringify(m,null,2));" "$manifest" && \
        echo "Patched: $manifest"; \
      done ; \
    fi && \
    WIDEVINE_FILES="$(find -L ${PLAYWRIGHT_BROWSERS_PATH} /opt/WidevineCdm -path '*/WidevineCdm/manifest.json' -o -path '*/WidevineCdm/_platform_specific/linux_arm64/libwidevinecdm.so' -o -path '*/WidevineCdm/_platform_specific/linux_x64/libwidevinecdm.so' -o -path '*/gmp-widevinecdm/latest/libwidevinecdm.so' 2>/dev/null || true)" && \
    if [ -z "$WIDEVINE_FILES" ]; then \
      echo "Widevine files were not installed into ${PLAYWRIGHT_BROWSERS_PATH} or /opt/WidevineCdm" && \
      exit 1 ; \
    fi && \
    echo "Widevine files installed:" && \
    printf '%s\n' "$WIDEVINE_FILES" && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* && \
    ln -snf /usr/share/zoneinfo/"$TZ" /etc/localtime && \
    echo "$TZ" > /etc/timezone && \
    dpkg-reconfigure --frontend noninteractive tzdata && \
    groupadd -r -g 1001 appuser && useradd -r -u 1001 -g appuser -m -d /home/appuser appuser && \
    (chown -R appuser:appuser ${PLAYWRIGHT_BROWSERS_PATH} /opt/WidevineCdm 2>/dev/null || true)

COPY --chown=appuser:appuser build/install/core hibernate.cfg.xml /app/
COPY --chmod=755 entrypoint.sh /usr/local/bin/entrypoint.sh

WORKDIR /app
EXPOSE 37100
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD ["./bin/core", "--enable-jobs"]
