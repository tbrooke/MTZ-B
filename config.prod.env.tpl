# PRODUCTION config for trust. Rendered from 1Password ON YOUR MAC, then copied
# to the server — see "Secrets" in CLAUDE.md.
#
#   op inject -i config.prod.env.tpl -o config.prod.env -f
#   scp config.prod.env tmb@trust:/home/tmb/mtz-b/config.env
#
# Safe to commit: references only, never values.
#
# This must stay key-for-key in step with config.env.tpl. It once drifted to
# half the keys, which would have shipped a live site with no images, no file
# uploads and a contact form that silently logged instead of sending.

# --- differs from dev -------------------------------------------------------
COOKIE_SECRET={{ op://MtZion/App/cookie_secret_prod }}
BASE_URL=https://mtzcg.com
# nginx terminates TLS and proxies over loopback, so the app itself serves
# plain HTTP. Setting this true behind the proxy causes a redirect loop.
SECURE=false

# --- same as dev ------------------------------------------------------------
CLOUDFLARE_ACCOUNT_ID={{ op://MtZion/Cloudflare/account_id }}
CLOUDFLARE_IMAGES_TOKEN={{ op://MtZion/Cloudflare/images_token }}
CLOUDFLARE_IMAGES_HASH={{ op://MtZion/Cloudflare/images_hash }}

R2_BUCKET={{ op://MtZion/R2/bucket }}
R2_ACCESS_KEY_ID={{ op://MtZion/R2/access_key_id }}
R2_SECRET_KEY={{ op://MtZion/R2/secret_key }}
R2_PUBLIC_URL={{ op://MtZion/R2/public_url }}

MAILERSEND_API_KEY={{ op://MtZion/MailerSend/api_key }}
MAILERSEND_FROM={{ op://MtZion/MailerSend/from }}
MAILERSEND_REPLY_TO={{ op://MtZion/MailerSend/reply_to }}
CONTACT_TO={{ op://MtZion/MailerSend/contact_to }}

# Fill these in before going live — the contact form emails whatever address it
# is given, and the free MailerSend tier is 100 messages/day.
TURNSTILE_SITE_KEY=
TURNSTILE_SECRET_KEY=

# Cloudflare Web Analytics site token (public — appears in page source).
CF_ANALYTICS_TOKEN=

# Admin alerting (optional)
BIFF_ADMIN_USER_ID=
BIFF_ADMIN_ALERT_EMAIL=
