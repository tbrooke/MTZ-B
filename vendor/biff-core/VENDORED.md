# biff-core — vendored

Upstream: https://github.com/jacobobryant/biff-core
Pinned SHA: `d9630f7024ac0009206fa89dde0f03651fdda5ab`
License: MIT (see LICENSE)

## Why this is here

The upstream repository **no longer exists** — `jacobobryant/biff-core` returns 404.
The project built only because this SHA happened to be cached in `~/.gitlibs`
on one laptop; a fresh clone, a CI runner, or a Docker build could not resolve
it, which is how this was found.

Jacob Bryant released **Biff v2.0.0** and folded the split prerelease
repositories back into the `jacobobryant/biff` monorepo under `libs/`;
the standalone repos went with them. So this is a deleted *prerelease*,
not abandoned code — the released equivalent exists, one migration away.

The code in the `jacobobryant/biff` monorepo (`libs/core`) has since
diverged and exposes a different API, so switching to it is a migration rather
than a swap. This copy is byte-identical to the SHA the app is tested against.

Do not edit. To update, migrate to the monorepo version deliberately.
