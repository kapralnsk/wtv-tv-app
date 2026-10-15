#!/bin/sh
set -eu
apt-get purge -y gpgconf
apt-get clean && rm -rf /var/lib/apt/lists/*
! command -v gpgconf
