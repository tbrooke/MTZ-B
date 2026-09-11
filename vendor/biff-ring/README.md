# biff-ring

Ring middleware and module helpers for Biff apps.

## Installation

Add to your `deps.edn`:

```clojure
{:deps {io.github.jacobobryant/biff-ring
        {:git/url "https://github.com/jacobobryant/biff-ring"
         :git/sha "..."}}}
```

## API

- `com.biffweb.ring/module`
- `com.biffweb.ring/use-jetty`
- `com.biffweb.ring/wrap-base-defaults`
- `com.biffweb.ring/wrap-site-defaults`
- `com.biffweb.ring/wrap-api-defaults`
- `com.biffweb.ring/wrap-ssl`
- `com.biffweb.ring/wrap-session`
- `com.biffweb.ring/wrap-https-scheme`
- `com.biffweb.ring/wrap-log-requests`
- `com.biffweb.ring/wrap-internal-error`
- `com.biffweb.ring/wrap-resource`
- `com.biffweb.ring/wrap-anti-forgery-websockets`

Modules can contribute:

- `:biff.ring/routes`
- `:biff.ring/api-routes`
- `:biff.ring/base-middleware`
- `:biff.ring/site-middleware`
- `:biff.ring/api-middleware`

## Running tests

```bash
clojure -X:test
```
