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
    PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1 \
    PLAYWRIGHT_BROWSERS_PATH=/opt/playwright

ARG PLAYWRIGHT_VERSION
# Install necessary packages and set timezone
RUN mkdir -p ${PLAYWRIGHT_BROWSERS_PATH} && \
    npx -y playwright@${PLAYWRIGHT_VERSION} install --with-deps chromium && \
    rm -rf ~/.npm && \
    apt-get update && \
    apt-get install -y --no-install-recommends ca-certificates curl unzip wget fonts-dejavu fontconfig libfreetype6 tzdata webp gosu xvfb xauth && \
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
      echo "Downloading and patching Widevine for arm64..." && \
      apt-get install -y --no-install-recommends git squashfs-tools python3 && \
      git clone https://github.com/AsahiLinux/widevine-installer.git /tmp/widevine-installer && \
      printf "\n\n" | sh /tmp/widevine-installer/widevine-installer && \
      find ${PLAYWRIGHT_BROWSERS_PATH} -type d -name "chrome-linux*" | while read -r dir; do \
        cp -rL /var/lib/widevine/WidevineCdm "$dir/"; \
      done && \
      apt-get purge -y git squashfs-tools && \
      apt-get autoremove -y && \
      rm -rf /tmp/widevine-installer /var/lib/widevine ; \
    else \
      echo "Unsupported architecture for Widevine: $ARCH" && \
      exit 1 ; \
    fi && \
    WIDEVINE_FILES="$(find ${PLAYWRIGHT_BROWSERS_PATH} -path '*/WidevineCdm/manifest.json' -o -path '*/WidevineCdm/_platform_specific/linux_arm64/libwidevinecdm.so' -o -path '*/WidevineCdm/_platform_specific/linux_x64/libwidevinecdm.so')" && \
    if [ -z "$WIDEVINE_FILES" ]; then \
      echo "Widevine files were not installed into ${PLAYWRIGHT_BROWSERS_PATH}" && \
      exit 1 ; \
    fi && \
    echo "Widevine files installed:" && \
    printf '%s\n' "$WIDEVINE_FILES" && \
    rm -rf /var/lib/apt/lists/* /var/cache/apt/archives/* && \
    ln -snf /usr/share/zoneinfo/"$TZ" /etc/localtime && \
    echo "$TZ" > /etc/timezone && \
    dpkg-reconfigure --frontend noninteractive tzdata && \
    groupadd -r -g 1001 appuser && useradd -r -u 1001 -g appuser -m -d /home/appuser appuser && \
    chown -R appuser:appuser ${PLAYWRIGHT_BROWSERS_PATH}

COPY --chown=appuser:appuser build/install/core hibernate.cfg.xml /app/
COPY --chmod=755 entrypoint.sh /usr/local/bin/entrypoint.sh

WORKDIR /app
EXPOSE 37100
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
CMD ["./bin/core", "--enable-jobs"]
