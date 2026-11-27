#!/bin/sh
# Creates a Python virtual environment in ./venv and installs the script's dependencies.
# Works on Linux (RHEL/CentOS/Rocky, Ubuntu/Debian, ...) and macOS. Nothing is installed system-wide.
# Without internet access, set PIP_INDEX_URL to an internal PyPI mirror before running.
set -e

cd "$(dirname "$0")"

install_hint() {
  if command -v apt-get >/dev/null 2>&1; then
    echo "  sudo apt-get install -y python3 python3-venv"
  elif command -v dnf >/dev/null 2>&1; then
    echo "  sudo dnf install -y python3"
  elif command -v yum >/dev/null 2>&1; then
    echo "  sudo yum install -y python3"
  elif command -v brew >/dev/null 2>&1; then
    echo "  brew install python3"
  else
    echo "  Install Python 3 (3.6 or later) with the venv module using your system's package manager."
  fi
}

if ! command -v python3 >/dev/null 2>&1; then
  echo "ERROR: python3 not found. Install it with:"
  install_hint
  exit 1
fi

if ! python3 -c 'import sys; sys.exit(0 if sys.version_info >= (3, 6) else 1)'; then
  echo "ERROR: Python 3.6 or later is required. Found: $(python3 --version 2>&1)"
  exit 1
fi

if ! python3 -c 'import venv, ensurepip' >/dev/null 2>&1; then
  echo "ERROR: the Python venv module is missing. Install it with:"
  install_hint
  exit 1
fi

if [ ! -x venv/bin/python ]; then
  echo "Creating virtual environment in $(pwd)/venv"
  python3 -m venv venv
fi

# Recent pip is needed to find prebuilt psycopg2-binary packages.
# pip only installs versions that support the installed Python version.
venv/bin/python -m pip install --upgrade pip
venv/bin/python -m pip install -r requirements.txt
venv/bin/python -c 'import requests, psycopg2' >/dev/null

echo
echo "Done. Before running the script, activate the environment:"
echo "  source venv/bin/activate"
