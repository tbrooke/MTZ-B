# ─────────────────────────────────────────────────────────────────────────────
# Build: uberjar + Tailwind CSS + the Tiptap admin bundle.
#
# Everything is built here rather than shipping a jar from a laptop, so the
# server can rebuild from a git checkout and the result is reproducible.
# ─────────────────────────────────────────────────────────────────────────────
FROM clojure:temurin-21-tools-deps-bookworm AS build

WORKDIR /build

# Node is only needed at build time, for esbuild (admin.js) and Tailwind.
RUN apt-get update \
 && apt-get install -y --no-install-recommends nodejs npm curl ca-certificates \
 && rm -rf /var/lib/apt/lists/*

# Dependency manifests first, so the slow layers cache across source edits.
# vendor/ comes too: two Biff libraries are :local/root because their upstream
# repos were deleted, and the classpath cannot resolve without them.
COPY deps.edn ./
COPY vendor/ ./vendor/
RUN clojure -P && clojure -P -T:build uber

COPY package.json ./
RUN npm install --no-audit --no-fund

# Tailwind's standalone binary, not the npm package: `npx @tailwindcss/cli`
# fails to resolve its native binding inside a container (npm's long-standing
# optional-dependency bug). This is also what the project uses locally.
ARG TAILWIND_VERSION=4.2.4
ARG TARGETARCH
RUN set -eux; \
    case "${TARGETARCH:-amd64}" in \
      amd64) tw_arch=x64 ;; \
      arm64) tw_arch=arm64 ;; \
      *) echo "unsupported arch ${TARGETARCH}" >&2; exit 1 ;; \
    esac; \
    curl -fsSL -o /usr/local/bin/tailwindcss \
      "https://github.com/tailwindlabs/tailwindcss/releases/download/v${TAILWIND_VERSION}/tailwindcss-linux-${tw_arch}"; \
    chmod +x /usr/local/bin/tailwindcss; \
    tailwindcss --help > /dev/null

COPY . .

# Order matters: the CSS lands in target/resources, which deps.edn puts on the
# classpath, so it must exist BEFORE the uberjar is assembled.
RUN npm run build \
 && tailwindcss -i resources/tailwind.css \
                -o target/resources/public/css/main.css --minify \
 && clojure -T:build uber

# ─────────────────────────────────────────────────────────────────────────────
# Runtime
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy

# sqlite3def performs the schema migration on every boot. biff.sqlite looks for
# it on PATH first and only downloads it when absent — installing it here keeps
# startup off the network.
ARG SQLDEF_VERSION=3.10.1
# TARGETARCH is supplied by BuildKit, so the image builds on both the amd64
# server and an arm64 laptop.
ARG TARGETARCH
RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends curl ca-certificates sqlite3 tini; \
    arch="${TARGETARCH:-amd64}"; \
    curl -fsSL -o /tmp/sqlite3def.tar.gz \
      "https://github.com/sqldef/sqldef/releases/download/v${SQLDEF_VERSION}/sqlite3def_linux_${arch}.tar.gz"; \
    tar -xzf /tmp/sqlite3def.tar.gz -C /usr/local/bin sqlite3def; \
    chmod +x /usr/local/bin/sqlite3def; \
    rm -f /tmp/sqlite3def.tar.gz; \
    rm -rf /var/lib/apt/lists/*; \
    # Fail the build now rather than at first boot if the binary is unusable.
    sqlite3def --help > /dev/null

# Run unprivileged. The uid is fixed so the bind-mounted storage directory can
# be chowned to match on the host.
RUN useradd --system --uid 10001 --create-home --shell /usr/sbin/nologin mtz
WORKDIR /app

COPY --from=build /build/target/mtzion.jar /app/mtzion.jar

# storage/ is a bind mount in production; creating it keeps a bare `docker run`
# working too.
RUN mkdir -p /app/storage/sqlite /app/storage/uploads && chown -R mtz:mtz /app
USER mtz

ENV HOST=0.0.0.0 \
    PORT=8080 \
    SQLITE_DB_PATH=storage/sqlite/main.db \
    UPLOAD_DIR=storage/uploads \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:-OmitStackTraceInFastThrow"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/_biff/admin/health || exit 1

# tini reaps zombies and forwards signals, so `docker stop` shuts the JVM down
# cleanly instead of killing it after the timeout.
ENTRYPOINT ["/usr/bin/tini", "--"]
CMD ["sh", "-c", "exec java $JAVA_OPTS -jar /app/mtzion.jar"]
