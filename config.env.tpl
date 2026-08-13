# Rendered from 1Password — DO NOT edit the generated file, edit this template.
#   op inject -i config.env.tpl -o config.env -f
# Values live in the MtZion vault. This template is safe to commit: it contains
# only references, never secrets.
COOKIE_SECRET={{ op://MtZion/App/cookie_secret_dev }}
SECURE=false

# Optional MailerSend settings
MAILERSEND_API_KEY={{ op://MtZion/MailerSend/api_key }}
MAILERSEND_FROM={{ op://MtZion/MailerSend/from }}
MAILERSEND_REPLY_TO={{ op://MtZion/MailerSend/reply_to }}

# Optional Turnstile settings
TURNSTILE_SITE_KEY=
TURNSTILE_SECRET_KEY=

# Optional deploy settings
SERVER=

# Optional admin settings
BIFF_ADMIN_USER_ID=
BIFF_ADMIN_ALERT_EMAIL=

CLOUDFLARE_ACCOUNT_ID={{ op://MtZion/Cloudflare/account_id }}
CLOUDFLARE_IMAGES_TOKEN={{ op://MtZion/Cloudflare/images_token }}
CLOUDFLARE_IMAGES_HASH={{ op://MtZion/Cloudflare/images_hash }}

R2_BUCKET={{ op://MtZion/R2/bucket }}
R2_ACCESS_KEY_ID={{ op://MtZion/R2/access_key_id }}
R2_SECRET_KEY={{ op://MtZion/R2/secret_key }}
R2_PUBLIC_URL={{ op://MtZion/R2/public_url }}

# Where contact-form submissions are delivered (the church office).
CONTACT_TO={{ op://MtZion/MailerSend/contact_to }}

# Cloudflare Web Analytics site token (public — appears in page source).
# Empty means no beacon is emitted at all.
CF_ANALYTICS_TOKEN=
