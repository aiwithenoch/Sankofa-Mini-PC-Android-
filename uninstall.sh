#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
INSTALL_DIR="${SANKOFA_INSTALL_DIR:-$HOME/.sankofa-mini-pc}"
DATA_DIR="${SANKOFA_HOME:-$HOME/.sankofa}"

command -v sankofa >/dev/null 2>&1 && sankofa stop || true
rm -f "$PREFIX/bin/sankofa"
rm -rf "$INSTALL_DIR"
printf 'Sankofa program files removed.\n'
printf 'Local data remains at %s\n' "$DATA_DIR"
printf 'Delete it manually only if you no longer need your conversations or models.\n'
