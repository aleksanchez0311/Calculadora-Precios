#!/usr/bin/env bash
# Uso: ./encode_keystore.sh path/to/release.keystore > keystore.base64
set -e
if [ -z "$1" ]; then
  echo "Uso: $0 path/to/release.keystore"
  exit 2
fi
if [ ! -f "$1" ]; then
  echo "Archivo no encontrado: $1"
  exit 2
fi
base64 "$1" | tr -d '\n'
