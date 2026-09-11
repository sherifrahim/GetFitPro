#!/usr/bin/env bash
# One-shot, idempotent deploy of forge-sync on the Oracle VM. Run as ubuntu with passwordless sudo.
# Safe to re-run: every step checks before it changes anything. Never touches threatfeed.
set -euo pipefail

APP=/home/ubuntu/forge-sync
HOST=${FORGE_HOST:-getfit.mooo.com}

echo "== python venv =="
cd "$APP"
[ -d venv ] || python3 -m venv venv
venv/bin/pip install -q --upgrade pip
venv/bin/pip install -q -r requirements.txt

echo "== postgres role + database =="
if ! sudo -u postgres psql -Atc "select 1 from pg_roles where rolname='forge'" | grep -q 1; then
  PW=$(python3 -c 'import secrets; print(secrets.token_urlsafe(24))')
  sudo -u postgres psql -qc "create role forge login password '$PW'"
  echo "DATABASE_URL=postgresql://forge:$PW@127.0.0.1:5432/forge" >> .env.new
fi
if ! sudo -u postgres psql -Atc "select 1 from pg_database where datname='forge'" | grep -q 1; then
  sudo -u postgres psql -qc "create database forge owner forge"
fi
sudo -u postgres psql -q -d forge -f schema.sql
sudo -u postgres psql -q -d forge -c "alter table snapshots owner to forge" 2>/dev/null || true

echo "== .env =="
if [ ! -f .env ]; then
  TOKEN=$(python3 -c 'import secrets; print(secrets.token_urlsafe(32))')
  { cat .env.new 2>/dev/null || true; echo "FORGE_SYNC_TOKEN=$TOKEN"; echo "KEEP_VERSIONS=20"; } > .env
  chmod 600 .env
fi
rm -f .env.new

echo "== systemd =="
sudo cp forge-sync.service /etc/systemd/system/forge-sync.service
sudo systemctl daemon-reload
sudo systemctl enable --now forge-sync
sleep 2
sudo systemctl --no-pager --lines=5 status forge-sync | sed -n '1,8p'

echo "== nginx =="
sed "s/forge\.mooo\.com/$HOST/" nginx-forge.conf | sudo tee /etc/nginx/sites-available/forge > /dev/null
sudo ln -sf /etc/nginx/sites-available/forge /etc/nginx/sites-enabled/forge
sudo nginx -t
sudo systemctl reload nginx

echo "== smoke =="
curl -sf http://127.0.0.1:8001/v1/health && echo
curl -sf -H "Host: $HOST" http://127.0.0.1/v1/health && echo
echo "deployed. token is in $APP/.env (FORGE_SYNC_TOKEN)."
