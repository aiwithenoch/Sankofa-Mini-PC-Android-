#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

REPO_URL="https://github.com/aiwithenoch/Sankofa-Mini-PC-Android-.git"
INSTALL_DIR="${SANKOFA_INSTALL_DIR:-$HOME/.sankofa-mini-pc}"
VENV_DIR="$INSTALL_DIR/.venv"

info() { printf '\033[1;32m[Sankofa]\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m[Error]\033[0m %s\n' "$*" >&2; exit 1; }

if [ -z "${PREFIX:-}" ] || [[ "$PREFIX" != *"com.termux"* ]]; then
  fail "Run this installer inside Termux on Android."
fi

info "Updating Termux packages…"
pkg update -y
pkg install -y python git curl

if [ -d "$INSTALL_DIR/.git" ]; then
  info "Updating existing installation…"
  git -C "$INSTALL_DIR" pull --ff-only
else
  info "Downloading Sankofa Mini PC…"
  rm -rf "$INSTALL_DIR"
  git clone --depth 1 "$REPO_URL" "$INSTALL_DIR"
fi

info "Creating isolated Python environment…"
python -m venv "$VENV_DIR"
"$VENV_DIR/bin/python" -m pip install --upgrade pip
"$VENV_DIR/bin/pip" install "$INSTALL_DIR"

ln -sf "$VENV_DIR/bin/sankofa" "$PREFIX/bin/sankofa"

info "Checking this phone…"
set +e
sankofa check
CHECK_CODE=$?
set -e
if [ "$CHECK_CODE" -ne 0 ]; then
  info "The foundation installed, but the device check reported limitations."
fi

info "Starting the local server…"
sankofa start

printf '\n'
printf 'Sankofa Mini PC is ready.\n'
printf 'Dashboard: http://127.0.0.1:8787\n'
printf 'Commands:  sankofa check | start | stop | status | logs\n'
printf '\n'
printf 'To keep it running, disable battery optimization for Termux.\n'
