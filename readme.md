# Jabble

## Key Technologies

### Stack & Development

- [devbox](https://www.jetpack.io/devbox) and nix flakes for a reproducible dev environment, <devbox.json>
- [direnv](https://direnv.net/) to automatically load dev environment when entering project directory, <.envrc>
- [just](https://github.com/casey/just) runner for common commands used in development, <justfile>
- [process-compose](https://github.com/F1bonacc1/process-compose) process orchestrator used for dev environment (<process-compose.yml>) and prod environment (<process-compose-prod.yml>)
- [entr](https://github.com/eradman/entr) Run arbitrary commands when files change, used for development reloading
- [Scala 3](https://www.scala-lang.org/) programming language, compiled to javascript using [ScalaJS](https://www.scala-js.org/), used for frontend and backend
- [Mill](https://mill-build.com) build tool, <build.sc>
- [Outwatch](https://github.com/outwatch/outwatch/) functional web-frontend library, <frontend/src/frontend/FrontendMain.scala>
- [Vite](https://vitejs.dev) hot reloading and bundling, <vite.config.mts>
- earthly locally runnable docker-like CI with very good caching, <Earthfile>
- [TailwindCSS](https://tailwindcss.com/)
- [Keratin Authn](https://keratin.github.io/) simple authentication service
- [fly.io](https://fly.io/) simple cloud hosting provider, <fly.toml>
- smithy with smithy4s for http api code generation
- mergify for automatic PR merging with a merge queue
- bun for fast npm installs
- wrk for http benchmarking

### Database

- [SQLite](https://www.sqlite.org/) lightweight, fast single-file relational database
- [magnum](https://github.com/AugustNagro/magnum), JDBC library for Scala 3
- [HikariCP](https://github.com/brettwooldridge/HikariCP) fast JDBC connection pool, see <backend/src/backend/AppConfig.scala>
- [scala-db-codegen](https://github.com/cornerman/scala-db-codegen) using [SchemaCrawler](https://www.schemacrawler.com/) to read <schema.sql> and generate schema types for magnum. Template <schema.scala.ssp>, config in <build.sc>
- [flyway](https://flywaydb.org/) for Database Migrations
- [Atlas](https://atlasgo.io/docs) generate SQL migrations by diffing <schema.sql> with migrations <backend/resources/migrations/> (`just new-migration <name>`) or the current dev-database (`just migrate-dev`).
- [sqlc](https://docs.sqlc.dev/en/latest/) with [sqlc-gen-from-template](https://github.com/fdietze/sqlc-gen-from-template) to generate type-safe functions in <backend/src/backend/queries/Queries.scala> using a template <queries_template.go.tmpl> for <queries.sql>. Config: [sqlc.yml], `just generate-query-code`

## Getting Started

1. Setup on your system:
   - [devbox](https://www.jetpack.io/devbox)
   - [direnv](https://direnv.net/)
  
   ```
   # to enter the devbox dev shell loaded by direnv
   direnv allow
   ```

1. Run the development stack

  ```
  just dev
  ```

1. Point your browser to <http://localhost:12345>
