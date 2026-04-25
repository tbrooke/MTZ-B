# biff-starter-sqlite

A minimal Biff starter app with SQLite-backed authentication, a tiny `/app` page, and the same component/module shape used in larger Biff apps.

## Getting started

1. Install Java, the Clojure CLI, and Node.js.
2. Generate local secrets:

   ```bash
   clj -M:run generate-config
   ```

3. Start the dev workflow:

   ```bash
   clj -M:run dev
   ```

4. Visit `http://localhost:8080`.

The dev task starts the app, watches Clojure files, recompiles Tailwind CSS, and exposes nREPL on port `7888` by default.

## Project structure

- `src/com/example.clj` wires the system, components, and route handler.
- `src/com/example/modules.clj` lists the application modules.
- `src/com/example/app/` contains the landing page, auth module, and `/app` page.
- `src/com/example/lib/` contains middleware, HTML helpers, and email delivery.
- `src/com/example/model/` contains the SQLite schema and graph resolvers.

## Frontend notes

Use **htmx or Datastar** for frontend interactivity as the app grows. This starter keeps the initial UI simple and server-rendered.

Tailwind source lives in `resources/tailwind.css`, and the generated stylesheet is written to `target/resources/public/css/main.css`.

## Useful commands

```bash
clj -M:test
clj -M:prod
clj -M:run css
clj -M:run logs
clj -M:run restart
clj -M:run soft-deploy
```

## Deploying to a fresh Ubuntu server

1. Run the setup script as root:

   ```bash
   sudo ./server-setup.sh your-app-name
   ```

2. SSH to the new box and copy in `config.env`.
3. Set `DEPLOY_TO` in `config.env` (or your shell) to the git remote created by `server-setup.sh`.
4. Deploy from your workstation with:

   ```bash
   clj -M:run soft-deploy
   ```

5. If you need a full push/restart cycle instead, use:

   ```bash
   clj -M:run deploy
   clj -M:run restart
   clj -M:run logs
   ```

## Biff-specific pointers

- Modules contribute `:routes`, `:biff/init`, schema, and graph resolvers.
- `com.example/ring-module` builds the Ring handler from the active module list.
- SQLite writes that should enforce ownership rules can go through `:biff.fx.sqlite/authorized-write`.
