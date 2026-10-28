FROM wtv-agent-base:2

USER node
# The native installer keeps Claude Code updatable by node; an npm global install is root-owned.
RUN curl -fsSL https://claude.ai/install.sh | bash
ENV PATH=/home/node/.local/bin:$PATH

USER root
# Each pin sits just above its first use, since a changed ARG reruns every RUN after it. Keep this
# one at the playwright version that PLAYWRIGHT_MCP_VERSION depends on, so that its Chromium build
# is the one installed.
ARG PLAYWRIGHT_VERSION=1.64.0-alpha-1790635538000
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
# The base already has Chromium's libraries, and the cached lists keep apt-get update incremental.
# The base's no-gpg ran before this apt step, so the build fails if a pin's dependencies bring
# gpgconf back.
RUN --mount=type=cache,target=/var/lib/apt/lists,sharing=locked \
  npx -y playwright@${PLAYWRIGHT_VERSION} install --with-deps chromium \
  && chown -R node:node /ms-playwright \
  && ! command -v gpgconf

# A new named volume takes its mount point's owner, so this lets node write the workspace volume.
RUN install -d -o node -g node /workspace

USER node
ARG PLAYWRIGHT_MCP_VERSION=0.0.83
# Warms the npx cache so .mcp.json's pinned version starts without a download.
RUN npx -y @playwright/mcp@${PLAYWRIGHT_MCP_VERSION} --version

# Last, so that editing them rebuilds only these layers. COPY makes them root-owned whatever USER
# is, so node cannot change them.
COPY managed-settings.json /etc/claude-code/managed-settings.json
COPY --chmod=755 app-token.sh /usr/local/bin/wtv-app-token
COPY --chmod=755 gh-wrapper.sh /usr/local/bin/gh
