#!/bin/sh
# Mints installation tokens for the implementer GitHub App from its private key, so no GitHub
# login is stored in the container. Tokens cover the repository named by WTV_REPO, last an hour
# and are cached until 10 minutes before they expire.
#
#   wtv-app-token                     print a token
#   wtv-app-token git-credential get  git credential helper protocol
#   wtv-app-token identity            print the bot's name and noreply email, tab-separated
#   wtv-app-token permissions         print the cached token's permissions and repositories
set -eu

dir="$HOME/.config/wtv-app"
key="$dir/private-key.pem"
cache="$HOME/.cache/wtv-app/token.json"
api=https://api.github.com

die() { echo "wtv-app-token: $1" >&2; exit 1; }

[ -r "$key" ] || die "no private key at $key (see .devcontainer/README.md)"
[ -r "$dir/app-id" ] || die "no app ID at $dir/app-id (see .devcontainer/README.md)"
app_id=$(tr -d '[:space:]' < "$dir/app-id")

[ -n "${WTV_REPO:-}" ] || die "WTV_REPO is not set (devcontainer.json's containerEnv sets it)"
repo=$WTV_REPO
repo_name=${repo#*/}

b64url() { openssl base64 -A | tr '+/' '-_' | tr -d '='; }

jwt() {
  now=$(date +%s)
  header=$(printf '{"alg":"RS256","typ":"JWT"}' | b64url)
  # GitHub rejects an iat in its future and an exp more than 10 minutes past its clock. Centering
  # the window on now tolerates a container clock up to 5 minutes off either way.
  payload=$(printf '{"iat":%s,"exp":%s,"iss":"%s"}' $((now - 300)) $((now + 300)) "$app_id" | b64url)
  signature=$(printf '%s.%s' "$header" "$payload" | openssl dgst -sha256 -sign "$key" | b64url)
  printf '%s.%s.%s' "$header" "$payload" "$signature"
}

# On an HTTP error, GitHub's response goes to stderr, so the cause shows next to die's message.
call() {
  method=$1 path=$2 auth=$3
  shift 3
  out=$(curl -sS --fail-with-body -X "$method" "$api$path" \
    -H "Authorization: Bearer $auth" \
    -H 'Accept: application/vnd.github+json' \
    -H 'X-GitHub-Api-Version: 2022-11-28' "$@") || { printf '%s\n' "$out" >&2; return 1; }
  printf '%s' "$out"
}

fresh() {
  [ -s "$cache" ] || return 1
  expires=$(date -d "$(jq -r .expires_at "$cache")" +%s) || return 1
  [ $((expires - $(date +%s))) -gt 600 ]
}

mint() {
  token_jwt=$(jwt)
  response=$(call GET "/repos/$repo/installation" "$token_jwt") \
    || die "looking up the app's installation on $repo failed"
  installation=$(printf '%s' "$response" | jq -r .id)
  # Narrowed at mint time, so a broader app registration still yields only these permissions.
  body=$(jq -nc --arg r "$repo_name" \
    '{repositories: [$r],
      permissions: {contents: "write", issues: "write", pull_requests: "write", metadata: "read"}}')
  mkdir -p "$(dirname "$cache")"
  umask 077
  # Concurrent calls each mint into their own file; mv within the directory replaces the cache
  # atomically, so a reader never sees a partial one.
  tmp=$(mktemp "$cache.XXXXXX")
  trap 'rm -f "$tmp"' EXIT
  call POST "/app/installations/$installation/access_tokens" "$token_jwt" -d "$body" > "$tmp" \
    || die "minting a token failed"
  jq -e .token "$tmp" > /dev/null || die "the minted token response has no token"
  mv "$tmp" "$cache"
}

token() {
  fresh || mint
  jq -r .token "$cache"
}

case "${1:-}" in
  "")
    token
    ;;
  git-credential)
    [ "${2:-}" = get ] || exit 0
    host=$(sed -n 's/^host=//p')
    [ "$host" = github.com ] || exit 0
    tok=$(token) || exit 1
    printf 'username=x-access-token\npassword=%s\n' "$tok"
    ;;
  identity)
    # An app's bot never changes its name or ID, so one lookup per app ID is enough. The cache
    # sits in the key volume, so it survives rebuilds.
    cached="$dir/identity-$app_id"
    if [ ! -s "$cached" ]; then
      token_jwt=$(jwt)
      app=$(call GET /app "$token_jwt") || die "reading the app failed"
      slug=$(printf '%s' "$app" | jq -r .slug)
      tok=$(token) || exit 1
      bot=$(call GET "/users/$slug%5Bbot%5D" "$tok") || die "reading the bot user failed"
      id=$(printf '%s' "$bot" | jq -r .id)
      case "$slug:$id" in
        :* | *: | null:* | *:*[!0-9]*) die "GitHub returned no usable bot name or ID" ;;
      esac
      tmp=$(mktemp "$cached.XXXXXX")
      trap 'rm -f "$tmp"' EXIT
      printf '%s[bot]\t%s+%s[bot]@users.noreply.github.com\n' "$slug" "$id" "$slug" > "$tmp"
      mv "$tmp" "$cached"
    fi
    cat "$cached"
    ;;
  permissions)
    token > /dev/null
    jq -c '{permissions, repositories: [.repositories[].full_name]}' "$cache"
    ;;
  *)
    die "unknown command: $1"
    ;;
esac
