#!/bin/sh

echo "[ Configuring Traefik... ]"

mkdir -p /certs

export CONSUL_HTTP_ADDR=$CONSUL_URL
export CONSUL_HTTP_TOKEN=$CONSUL_TOKEN

consul kv get certs/traefik/crt > /certs/certificate.crt
consul kv get certs/traefik/key > /certs/certificate.key

echo "[ Starting Traefik... ]"

# Forwarding to the base entrypoint
exec traefik "$@"