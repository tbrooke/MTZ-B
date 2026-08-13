# biff-config — vendored

Upstream: https://github.com/jacobobryant/biff-config
Pinned SHA: `f7289e16e0a0c257d4bcfaabc18d45251f1bc422`
License: MIT (see LICENSE)

## Why this is here

The upstream repository **no longer exists** — `jacobobryant/biff-config` returns 404.
The project built only because this SHA happened to be cached in `~/.gitlibs`
on one laptop; a fresh clone, a CI runner, or a Docker build could not resolve
it, which is how this was found.

The code in the `jacobobryant/biff` monorepo (`libs/config`) has since
diverged and exposes a different API, so switching to it is a migration rather
than a swap. This copy is byte-identical to the SHA the app is tested against.

Do not edit. To update, migrate to the monorepo version deliberately.
