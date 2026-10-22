#!/bin/sh
set -eu
# Nacos 2.3.2 ships an embedded nacos account; change its password before starting clients.
login() { curl --fail --silent --show-error -X POST http://nacos:8848/nacos/v1/auth/login --data-urlencode username=nacos --data-urlencode "password=$1"; }
if login "$NACOS_PASSWORD" >/dev/null 2>&1; then exit 0; fi
response=$(login nacos)
token=$(printf '%s' "$response" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
[ -n "$token" ] || { echo 'Nacos initial login failed'; exit 1; }
curl --fail --silent --show-error -X PUT http://nacos:8848/nacos/v1/auth/users --data-urlencode username=nacos --data-urlencode "newPassword=$NACOS_PASSWORD" --data-urlencode "accessToken=$token" >/dev/null
echo 'Nacos password initialized'
