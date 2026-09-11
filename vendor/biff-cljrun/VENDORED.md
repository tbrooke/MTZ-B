# biff-cljrun — vendored

Upstream: https://github.com/jacobobryant/biff-cljrun
Pinned SHA: `d3f71eb8f21bb3ad28cc268dce2a5973529a35b4`

## Why this is here

Gone from GitHub like every other `jacobobryant/biff*` repo. This one is a
*transitive* dependency — biff-tasks requires it — so it never appeared in our
deps.edn and was easy to miss when vendoring the direct ones.

See vendor/biff-fx/VENDORED.md for the whole story.
