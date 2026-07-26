#!/data/data/com.termux/files/usr/bin/bash

# Sankofa Mini PC — Colibri/Termux Phase 1 probe
#
# This script downloads source code and build tools only. It does not download
# any large model weights. Run it explicitly with:
#   bash experiments/colibri-termux/probe.sh

set -uo pipefail

COLIBRI_URL="${COLIBRI_URL:-https://github.com/JustVugg/colibri.git}"
SANKOFA_HOME="${SANKOFA_HOME:-$HOME/.sankofa}"
EXPERIMENT_HOME="$SANKOFA_HOME/experiments/colibri-termux"
SOURCE_DIR="$EXPERIMENT_HOME/source"
STAMP="$(date +%Y%m%d-%H%M%S)"
LOG_FILE="$EXPERIMENT_HOME/probe-$STAMP.log"
RESULT_FILE="$EXPERIMENT_HOME/latest-result.txt"

mkdir -p "$EXPERIMENT_HOME"
exec > >(tee -a "$LOG_FILE") 2>&1

RESULT="ENVIRONMENT_FAILED"
EXIT_CODE=1

finish() {
  local shell_code=$?
  {
    echo
    echo "============================================================"
    echo "Sankofa Colibri/Termux probe result: $RESULT"
    echo "Log: $LOG_FILE"
    echo "============================================================"
  } | tee "$RESULT_FILE"

  if [ "$EXIT_CODE" -ne 0 ]; then
    exit "$EXIT_CODE"
  fi
  exit "$shell_code"
}
trap finish EXIT

section() {
  echo
  echo "## $1"
}

command_exists() {
  command -v "$1" >/dev/null 2>&1
}

section "Safety and scope"
echo "This probe installs build tools and clones Colibri source."
echo "It will not download GLM-5.2, Kimi K3, or other model weights."
echo "Stop the run if Android reports overheating."

section "Termux environment"
if ! command_exists pkg || [ -z "${PREFIX:-}" ]; then
  echo "ERROR: This script must run inside Termux."
  RESULT="ENVIRONMENT_FAILED"
  exit 1
fi

printf 'Date: '; date -Iseconds 2>/dev/null || date
printf 'Kernel: '; uname -a
printf 'Architecture: '; uname -m
printf 'PREFIX: %s\n' "$PREFIX"
printf 'Home: %s\n' "$HOME"

if command_exists termux-info; then
  echo
  termux-info || true
fi

if command_exists getprop; then
  echo
  echo "Android properties:"
  printf '  manufacturer: %s\n' "$(getprop ro.product.manufacturer 2>/dev/null)"
  printf '  model: %s\n' "$(getprop ro.product.model 2>/dev/null)"
  printf '  device: %s\n' "$(getprop ro.product.device 2>/dev/null)"
  printf '  Android: %s\n' "$(getprop ro.build.version.release 2>/dev/null)"
  printf '  API level: %s\n' "$(getprop ro.build.version.sdk 2>/dev/null)"
  printf '  ABI: %s\n' "$(getprop ro.product.cpu.abi 2>/dev/null)"
fi

if [ -r /proc/meminfo ]; then
  echo
  echo "Memory:"
  grep -E '^(MemTotal|MemAvailable|SwapTotal|SwapFree):' /proc/meminfo || true
fi

echo
echo "Storage:"
df -h "$HOME" "$EXPERIMENT_HOME" 2>/dev/null || df -h "$HOME"

echo
echo "CPU summary:"
grep -E '^(Hardware|model name|Processor|Features|CPU architecture)' /proc/cpuinfo 2>/dev/null | head -20 || true

section "Install build tools"
if ! pkg install -y git clang make python pkg-config; then
  echo "ERROR: Termux could not install the required build tools."
  RESULT="ENVIRONMENT_FAILED"
  exit 1
fi

for tool in git clang make python; do
  if ! command_exists "$tool"; then
    echo "ERROR: Required tool is missing after installation: $tool"
    RESULT="ENVIRONMENT_FAILED"
    exit 1
  fi
done

git --version
clang --version | head -3
make --version | head -1
python --version

section "OpenMP compiler probe"
OMP_SOURCE="$EXPERIMENT_HOME/openmp-probe.c"
OMP_BINARY="$EXPERIMENT_HOME/openmp-probe"
cat > "$OMP_SOURCE" <<'EOF'
#include <stdio.h>
#ifdef _OPENMP
#include <omp.h>
#endif
int main(void) {
#ifdef _OPENMP
    printf("openmp=yes threads=%d version=%d\n", omp_get_max_threads(), _OPENMP);
    return 0;
#else
    printf("openmp=no\n");
    return 2;
#endif
}
EOF

OPENMP_AVAILABLE=0
if clang -O2 -fopenmp "$OMP_SOURCE" -o "$OMP_BINARY"; then
  if "$OMP_BINARY"; then
    OPENMP_AVAILABLE=1
  fi
else
  echo "OpenMP build failed. The probe will also try a single-thread fallback."
fi
printf 'OpenMP available: %s\n' "$OPENMP_AVAILABLE"

section "Fetch Colibri source"
if [ -d "$SOURCE_DIR/.git" ]; then
  git -C "$SOURCE_DIR" fetch --depth 1 origin main
  git -C "$SOURCE_DIR" reset --hard FETCH_HEAD
else
  rm -rf "$SOURCE_DIR"
  git clone --depth 1 --branch main "$COLIBRI_URL" "$SOURCE_DIR"
fi

COLIBRI_COMMIT="$(git -C "$SOURCE_DIR" rev-parse HEAD)"
printf 'Colibri commit: %s\n' "$COLIBRI_COMMIT"

if [ ! -d "$SOURCE_DIR/c" ]; then
  echo "ERROR: Colibri C source directory was not found."
  RESULT="BUILD_FAILED"
  exit 1
fi

cd "$SOURCE_DIR/c"

section "Build Colibri"
make clean >/dev/null 2>&1 || true
BUILD_OK=0
BUILD_MODE="none"

if [ "$OPENMP_AVAILABLE" -eq 1 ]; then
  echo "Attempt 1: upstream Makefile with Termux clang and OpenMP"
  if make -s colibri CC=clang ARCH=native; then
    BUILD_OK=1
    BUILD_MODE="upstream-openmp"
  else
    echo "Attempt 1 failed."
  fi
fi

if [ "$BUILD_OK" -eq 0 ]; then
  echo "Attempt 2: Android ARM64 single-thread fallback"
  make clean >/dev/null 2>&1 || true
  ANDROID_CFLAGS="-O3 -mcpu=native -pthread -Wall -Wextra -Wno-unused-parameter -Wno-misleading-indentation -Wno-unused-function"
  if make -s colibri CC=clang ARCH=native CFLAGS="$ANDROID_CFLAGS" LDFLAGS="-lm -pthread"; then
    BUILD_OK=1
    BUILD_MODE="android-single-thread-native"
  else
    echo "Attempt 2 failed."
  fi
fi

if [ "$BUILD_OK" -eq 0 ]; then
  echo "Attempt 3: generic ARMv8-A single-thread fallback"
  make clean >/dev/null 2>&1 || true
  GENERIC_CFLAGS="-O3 -march=armv8-a -pthread -Wall -Wextra -Wno-unused-parameter -Wno-misleading-indentation -Wno-unused-function"
  if make -s colibri CC=clang ARCH=native CFLAGS="$GENERIC_CFLAGS" LDFLAGS="-lm -pthread"; then
    BUILD_OK=1
    BUILD_MODE="android-single-thread-generic"
  else
    echo "Attempt 3 failed."
  fi
fi

if [ "$BUILD_OK" -eq 0 ] || [ ! -x ./colibri ]; then
  echo "ERROR: Colibri did not compile on this Termux environment."
  RESULT="BUILD_FAILED"
  exit 1
fi

printf 'Build mode: %s\n' "$BUILD_MODE"
file ./colibri 2>/dev/null || true
ls -lh ./colibri

section "Tiny correctness self-test"
SELFTEST_OUTPUT="$EXPERIMENT_HOME/selftest-$STAMP.txt"
if [ -d ./glm_tiny ] && [ -f ./ref_glm.json ]; then
  set +e
  SNAP=./glm_tiny TF=1 ./colibri 64 16 16 2>&1 | tee "$SELFTEST_OUTPUT"
  SELFTEST_CODE=${PIPESTATUS[0]}
  set -e

  if grep -q '32/32 positions' "$SELFTEST_OUTPUT"; then
    echo "Expected marker found: 32/32 positions"
    RESULT="PASS"
    EXIT_CODE=0
    exit 0
  fi

  echo "The engine built, but the expected tiny-test marker was not found."
  echo "Self-test exit code: $SELFTEST_CODE"
  RESULT="BUILD_ONLY"
  EXIT_CODE=0
  exit 0
else
  echo "The engine built, but the tiny fixture was not present in this checkout."
  RESULT="BUILD_ONLY"
  EXIT_CODE=0
  exit 0
fi
