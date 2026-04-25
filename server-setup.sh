#!/usr/bin/env bash
set -x
set -e

APP=$1
if [ "$APP" = "" ]; then
  echo 'Usage: `./server-setup.sh <app name>`'
  exit 1
fi

if [ "$(whoami)" != root ]; then
  echo This script must be ran as root.
  exit 2
fi

echo Running \`apt-get update\`. If this fails, you may need to wait a few seconds for a background \
     \`apt\` command to finish.
apt-get update
apt-get upgrade
apt-get -y install default-jre rlwrap ufw git snapd
if ! command -v clj >/dev/null 2>&1; then
  curl -L -O https://github.com/clojure/brew-install/releases/latest/download/linux-install.sh
  chmod +x linux-install.sh
  ./linux-install.sh
fi
if ! command -v trench >/dev/null 2>&1; then
  TRENCH_VERSION=0.4.0
  if [ "$(uname -m)" = "aarch64" ]; then
    ARCH=arm64
  else
    ARCH=amd64
  fi
  TRENCH_FILE=trenchman_${TRENCH_VERSION}_linux_${ARCH}.tar.gz
  curl -L -O https://github.com/athos/trenchman/releases/download/v$TRENCH_VERSION/$TRENCH_FILE
  tar zxvfC $TRENCH_FILE /usr/local/bin trench
fi

if ! [ -d /home/$APP ]; then
  useradd -m $APP
  mkdir -m 700 -p /home/$APP/.ssh
  cp -r /root/.ssh/* /home/$APP/.ssh
  chown -R $APP:$APP /home/$APP/.ssh
fi

set_up_app () {
  cd
  mkdir -p repo.git
  cd repo.git
  git init --bare
  cat > hooks/post-receive << EOD
#!/usr/bin/env bash
git --work-tree=/home/$APP/repo --git-dir=/home/$APP/repo.git checkout -f
EOD
  chmod +x hooks/post-receive
}
sudo -u $APP bash -c "$(declare -f set_up_app); set_up_app"

PORT=8080
if [ -f /etc/caddy/Caddyfile ]; then
  while grep -q $PORT /etc/caddy/Caddyfile; do
    PORT=$((PORT + 1))
  done
fi

make_service() {
  name="$1"
  file="/etc/systemd/system/$name.service"
  cat > "$file" << EOD
[Unit]
Description=$name
StartLimitIntervalSec=500
StartLimitBurst=5

[Service]
Restart=on-failure
RestartSec=5s
EOD
  cat >> "$file"
  cat >> "$file" << EOD

[Install]
WantedBy=multi-user.target
EOD
  systemctl enable "$name"
}

make_service $APP << EOD
User=$APP
Environment="PORT=$PORT"
WorkingDirectory=/home/$APP/repo
ExecStart=/bin/sh -c "mkdir -p target/resources; clj -M:prod"
EOD

cat > /etc/systemd/journald.conf << EOD
[Journal]
Storage=persistent
EOD
systemctl restart systemd-journald

cat > /etc/sudoers.d/restart-$APP << EOD
$APP ALL= NOPASSWD: /bin/systemctl reset-failed $APP.service
$APP ALL= NOPASSWD: /bin/systemctl restart $APP
$APP ALL= NOPASSWD: /usr/bin/systemctl reset-failed $APP.service
$APP ALL= NOPASSWD: /usr/bin/systemctl restart $APP
EOD
chmod 440 /etc/sudoers.d/restart-$APP

ufw allow OpenSSH
ufw allow http
ufw allow https
ufw --force enable

read -p "Enter your app's domain name (e.g. example.com): " DOMAIN
if [ -f /etc/caddy/Caddyfile ] && grep -q $DOMAIN /etc/caddy/Caddyfile ; then
  echo $DOMAIN is already configured in /etc/caddy/Caddyfile
else
  if ! command -v caddy >/dev/null 2>&1; then
    apt install -y debian-keyring debian-archive-keyring apt-transport-https
    curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' | gpg --dearmor -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
    curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' | tee /etc/apt/sources.list.d/caddy-stable.list
    chmod o+r /usr/share/keyrings/caddy-stable-archive-keyring.gpg
    chmod o+r /etc/apt/sources.list.d/caddy-stable.list
    apt-get update
    apt-get install caddy
  fi
  cat >> /etc/caddy/Caddyfile << EOD
$DOMAIN {
    encode gzip
    reverse_proxy localhost:$PORT
}
EOD
  systemctl reload caddy
  ufw allow http
  ufw allow https
fi
