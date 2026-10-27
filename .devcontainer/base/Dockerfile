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

RUN mkdir -p /home/node/.claude /home/node/.config/gh /home/node/.config/git /home/node/.config/wtv-app /home/node/.browser-profile \
  && chown -R node:node /home/node

USER node
