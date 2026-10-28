FROM node:24-bookworm

# First, so that every apt run reads it, the features' and Playwright's included: a stalled request
# is retried after 15 seconds instead of waiting out the default timeout.
RUN printf '%s\n' 'Acquire::Retries "10";' 'Acquire::http::Timeout "15";' \
    'Acquire::https::Timeout "15";' 'Acquire::ForceIPv4 "true";' > /etc/apt/apt.conf.d/80-flaky-network

# sudo is only for desktop-lite's init script, which runs as root; node has no sudoers entry.
RUN mkdir -p -m 755 /etc/apt/keyrings \
  && curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg -o /etc/apt/keyrings/githubcli-archive-keyring.gpg \
  && chmod go+r /etc/apt/keyrings/githubcli-archive-keyring.gpg \
  && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" > /etc/apt/sources.list.d/github-cli.list \
  && apt-get update \
  && apt-get install -y --no-install-recommends curl gh git jq less openssl procps python3 ripgrep sudo \
  && apt-get clean && rm -rf /var/lib/apt/lists/*

# Only seeds Chromium's libraries, so that the agent image's install --with-deps finds them in
# place. It may lag the agent Dockerfile's pin.
ARG PLAYWRIGHT_VERSION=1.64.0-alpha-1790635538000
RUN npx -y playwright@${PLAYWRIGHT_VERSION} install-deps chromium \
  && apt-get clean && rm -rf /var/lib/apt/lists/*

# The Android build's JDK. Keep the major version at the one AGENTS.md pins for jvmToolchain.
RUN curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public -o /etc/apt/keyrings/adoptium.asc \
  && chmod go+r /etc/apt/keyrings/adoptium.asc \
  && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/adoptium.asc] https://packages.adoptium.net/artifactory/deb bookworm main" > /etc/apt/sources.list.d/adoptium.list \
  && apt-get update \
  && apt-get install -y --no-install-recommends temurin-26-jdk \
  && apt-get clean && rm -rf /var/lib/apt/lists/*

# Owned by node, so that AGP can install a missing SDK package on demand.
# Held at cmdline-tools 22.0: from 23.0 on, sdkmanager is a shim over the native `android` CLI,
# which Google ships for linux-x86_64 only, so it fails on arm64. 22.0's sdkmanager is still Java.
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
ARG CMDLINE_TOOLS_BUILD=15859902
ARG CMDLINE_TOOLS_SHA1=040d3996a65543d22ec4bf73e4c37aa37a8d4af4
ARG ANDROID_PLATFORM=android-36
ARG ANDROID_BUILD_TOOLS=36.1.0
RUN curl -fsSL "https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_BUILD}_latest.zip" -o /tmp/cmdline-tools.zip \
  && echo "${CMDLINE_TOOLS_SHA1}  /tmp/cmdline-tools.zip" | sha1sum -c - \
  && mkdir -p "$ANDROID_HOME/cmdline-tools" \
  && unzip -q /tmp/cmdline-tools.zip -d "$ANDROID_HOME/cmdline-tools" \
  && mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest" \
  && rm /tmp/cmdline-tools.zip \
  && yes | sdkmanager --licenses > /dev/null \
  && sdkmanager "platforms;${ANDROID_PLATFORM}" "build-tools;${ANDROID_BUILD_TOOLS}" > /dev/null \
  && chown -R node:node "$ANDROID_HOME"

# Google ships the build-tools' native binaries for linux-x86_64 only, and AGP 9 fetches an x86_64
# aapt2 from Maven. On arm64, swap in Commit451/android-arm-build-tools' builds (from AOSP source)
# and point AGP at their aapt2. Its release must match ANDROID_BUILD_TOOLS. On amd64 nothing changes.
ARG ARM_BUILD_TOOLS_ASSET=android-build-tools-36.1.0-linux-arm64-20260522.tar.xz
ARG ARM_BUILD_TOOLS_SHA256=59ddd7893c48777fd146b9e62cece29545ed58520b02a76422a1f105f4d551ce
RUN mkdir -p /home/node/.gradle \
  && if [ "$(dpkg --print-architecture)" = arm64 ]; then \
    curl -fsSL "https://github.com/Commit451/android-arm-build-tools/releases/download/platform-tools-${ANDROID_BUILD_TOOLS}/${ARM_BUILD_TOOLS_ASSET}" -o /tmp/arm-build-tools.tar.xz \
    && echo "${ARM_BUILD_TOOLS_SHA256}  /tmp/arm-build-tools.tar.xz" | sha256sum -c - \
    && tar -xJf /tmp/arm-build-tools.tar.xz -C "$ANDROID_HOME/build-tools/${ANDROID_BUILD_TOOLS}" ./aapt2 ./aidl ./zipalign ./split-select \
    && rm /tmp/arm-build-tools.tar.xz \
    && chown node:node "$ANDROID_HOME/build-tools/${ANDROID_BUILD_TOOLS}"/* \
    && echo "android.aapt2FromMavenOverride=$ANDROID_HOME/build-tools/${ANDROID_BUILD_TOOLS}/aapt2" > /home/node/.gradle/gradle.properties; \
  fi

RUN mkdir -p /home/node/.claude /home/node/.config/gh /home/node/.config/git /home/node/.config/wtv-app /home/node/.browser-profile \
  && chown -R node:node /home/node

USER node
