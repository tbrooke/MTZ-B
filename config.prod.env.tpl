# Rendered from 1Password — DO NOT edit the generated file, edit this template.
#   op inject -i config.prod.env.tpl -o config.prod.env -f
# Values live in the MtZion vault. This template is safe to commit: it contains
# only references, never secrets.
COOKIE_SECRET={{ op://MtZion/App/cookie_secret_prod }}
BASE_URL=https://mtzcg.com

# Optional MailerSend settings
MAILERSEND_API_KEY=
MAILERSEND_FROM=
MAILERSEND_REPLY_TO=

# Optional Turnstile settings
TURNSTILE_SITE_KEY=
TURNSTILE_SECRET_KEY=

# Optional admin settings
BIFF_ADMIN_USER_ID=
BIFF_ADMIN_ALERT_EMAIL=
