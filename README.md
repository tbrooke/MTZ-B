# biff-starter-sqlite

A minimal Biff starter app with SQLite-backed authentication, a tiny `/app` page, and the same component/module shape used in larger Biff apps.

## Getting started

1. Create a new repo from this template repo on GitHub.
2. Install Java and the Clojure CLI.
3. Generate local config files:

   ```bash
   clj -M:run generate-config
   ```

4. Start the dev workflow:

   ```bash
   clj -M:run dev
   ```

5. Visit `http://localhost:8080`.

The dev task starts the app, recompiles Tailwind CSS with the standalone binary, and exposes nREPL on port `7888` by default.

The config templates live in `resources/TEMPLATE.config.env` and `resources/TEMPLATE.config.prod.env`.

## Project structure

- `src/com/example.clj` wires the system, components, and route handler.
- `src/com/example/modules.clj` lists the application modules.
- `src/com/example/app/` contains the landing page, auth module, and `/app` page.
- `src/com/example/lib/` contains app-specific middleware, HTML helpers, and email delivery.
- `libs/biff-ring/` contains the local `com.biffweb.ring` library extracted from the starter app.
- `src/com/example/model/` contains the SQLite schema and graph resolvers.

## Frontend notes

Use **htmx or Datastar** for frontend interactivity as the app grows. This starter keeps the initial UI simple and server-rendered.

Tailwind source lives in `resources/tailwind.css`, and the generated stylesheet is written to `target/resources/public/css/main.css`.

## Useful commands

```bash
clj -M:run test
clj -M:prod
clj -M:run css
clj -M:run logs
clj -M:run restart
clj -M:run soft-deploy
```

## Deploying to a fresh Ubuntu server

1. Copy the setup script to the server and run it there as root:

   ```bash
   scp server-setup.sh root@your-server:
   ssh root@your-server
   sudo ./server-setup.sh your-app-name
   ```

2. Fill in `config.prod.env` with your production values, especially `BASE_URL`, `SERVER`, and any email/admin settings you need.
3. Deploy from your workstation with:

   ```bash
   clj -M:run soft-deploy
   ```

4. If you need a full push/restart cycle instead, use:

   ```bash
   clj -M:run deploy
   clj -M:run restart
   clj -M:run logs
   ```

## Biff-specific pointers

- Modules contribute `:biff.ring/routes`, `:biff.ring/api-routes`, `:biff/init`, schema, and graph resolvers.
- `com.biffweb.ring/module` builds the Ring handler from the active module list.
- SQLite writes that should enforce ownership rules can go through `:biff.fx.sqlite/authorized-write`.
