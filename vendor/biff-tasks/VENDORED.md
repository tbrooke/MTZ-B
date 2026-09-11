# biff-tasks — vendored

Upstream: https://github.com/jacobobryant/biff-tasks
Pinned SHA: `02d63d6d30e64714262fce15b2d725d24213dd24`

## Why this is here

The upstream repository **no longer exists** — it returns 404, as does every
other `jacobobryant/biff*` repo this project depended on. Seven in all, found
together on 11 September 2026 when a Docker build failed with

    fatal: could not read Username for 'https://github.com': terminal prompts disabled

which is what git says when a repo is private or gone.

The project had kept building only because these SHAs were cached in
`~/.gitlibs` on one laptop. Nothing else could resolve them: not a fresh clone,
not CI, not the container — which is why production sat on the 14 August image
for a month without an obvious reason.

The cause, confirmed afterwards: Jacob Bryant released **Biff v2.0.0** and
folded the split prerelease repositories back into the `jacobobryant/biff`
monorepo under `libs/`; the standalone repos went with them. So this is a
deleted *prerelease*, not abandoned code — the released equivalent exists,
one migration away.

This copy is byte-identical to the SHA the app is tested against.

Do not edit. To update, migrate to the `jacobobryant/biff` monorepo
deliberately — its API has diverged, so that is a migration, not a swap.
