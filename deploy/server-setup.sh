#!/bin/bash
# One-time setup for a fresh Ubuntu 22.04 DigitalOcean droplet.
# Run as root: bash server-setup.sh
set -e

# ── Java 21 ──────────────────────────────────────────────────────────────────
apt-get update
apt-get install -y wget apt-transport-https gnupg
wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor -o /etc/apt/trusted.gpg.d/adoptium.gpg
echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" \
  > /etc/apt/sources.list.d/adoptium.list
apt-get update
apt-get install -y temurin-21-jdk

# ── nginx + certbot ──────────────────────────────────────────────────────────
apt-get install -y nginx certbot python3-certbot-nginx

# ── App user ─────────────────────────────────────────────────────────────────
useradd -m -s /bin/bash app

# ── Storage directories ───────────────────────────────────────────────────────
mkdir -p /home/app/storage/sqlite
chown -R app:app /home/app/storage

# ── SSH key for GitHub Actions deploys ───────────────────────────────────────
# Paste the PUBLIC key for your deploy keypair here, then uncomment:
# mkdir -p /home/app/.ssh
# echo "ssh-ed25519 AAAA... github-actions" >> /home/app/.ssh/authorized_keys
# chown -R app:app /home/app/.ssh
# chmod 700 /home/app/.ssh && chmod 600 /home/app/.ssh/authorized_keys

# ── systemd service ──────────────────────────────────────────────────────────
# Copy config.prod.env to /home/app/config.env (contains secrets — do not commit)
# cp config.prod.env /home/app/config.env
# chown app:app /home/app/config.env && chmod 600 /home/app/config.env

cp mtzion.service /etc/systemd/system/mtzion.service
systemctl daemon-reload
systemctl enable mtzion

# ── nginx ────────────────────────────────────────────────────────────────────
cp nginx.conf /etc/nginx/sites-available/mtzion
ln -sf /etc/nginx/sites-available/mtzion /etc/nginx/sites-enabled/mtzion
rm -f /etc/nginx/sites-enabled/default
nginx -t && systemctl reload nginx

echo ""
echo "Next steps:"
echo "  1. Copy config.prod.env to /home/app/config.env"
echo "  2. Add the GitHub Actions deploy public key to /home/app/.ssh/authorized_keys"
echo "  3. Run: sudo certbot --nginx -d mtzcg.com -d www.mtzcg.com"
echo "  4. Add DEPLOY_HOST and DEPLOY_SSH_KEY to GitHub repo secrets"
echo "  5. Push to master to trigger the first deploy"
