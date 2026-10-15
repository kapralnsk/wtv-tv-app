#!/bin/sh
set -eu
# VS Code forwards the host GPG agent into any container that has gpgconf. Purging it also purges
# everything that depends on it; these are named so that nothing else goes unnoticed. Nothing in
# the image needs them: apt verifies signatures with gpgv, a separate package that stays.
named="gpgconf gpg gpg-agent gpgsm gnupg dirmngr gpg-wks-client gpg-wks-server libgpgme11 seahorse"
# Only installed ones: desktop-lite clears the apt lists, so apt cannot resolve any other name.
installed=$(dpkg-query -W -f '${Package} ${db:Status-Status}\n' $named 2>/dev/null | awk '$2 == "installed" { print $1 }')
if [ -n "$installed" ]; then
  for pkg in $(apt-get -s purge $installed | awk '/^Purg/ { print $2 }'); do
    case " $named " in
      *" $pkg "*) ;;
      *) echo "no-gpg: purging gpgconf would also remove $pkg" >&2; exit 1 ;;
    esac
  done
  apt-get purge -y $installed
fi
apt-get clean && rm -rf /var/lib/apt/lists/*
! command -v gpgconf
