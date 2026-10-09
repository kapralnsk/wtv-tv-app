FROM node:24-bookworm

ARG PLAYWRIGHT_MCP_VERSION=0.0.83
ARG PLAYWRIGHT_VERSION=1.64.0-alpha-1790635538000

# sudo is only for desktop-lite's init script, which runs as root; node has no sudoers entry.
RUN mkdir -p -m 755 /etc/apt/keyrings \
  && curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg -o /etc/apt/keyrings/githubcli-archive-keyring.gpg \
  && chmod go+r /etc/apt/keyrings/githubcli-archive-keyring.gpg \
  && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/githubcli-archive-keyring.gpg] https://cli.github.com/packages stable main" > /etc/apt/sources.list.d/github-cli.list \
  && apt-get update \
  && apt-get install -y --no-install-recommends gh git jq less procps python3 ripgrep sudo \
  && apt-get clean && rm -rf /var/lib/apt/lists/*

COPY managed-settings.json /etc/claude-code/managed-settings.json

ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
RUN npx -y playwright@${PLAYWRIGHT_VERSION} install --with-deps chromium \
  && chown -R node:node /ms-playwright

RUN mkdir -p /home/node/.claude /home/node/.config/gh /home/node/.config/git /home/node/.browser-profile \
  && chown -R node:node /home/node

USER node
# The native installer keeps Claude Code updatable by node; an npm global install is root-owned.
RUN curl -fsSL https://claude.ai/install.sh | bash
ENV PATH=/home/node/.local/bin:$PATH

# Warms the npx cache so .mcp.json's pinned version starts without a download.
RUN npx -y @playwright/mcp@${PLAYWRIGHT_MCP_VERSION} --version
