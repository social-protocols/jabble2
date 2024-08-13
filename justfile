# List available recipes in the order in which they appear in this file
_default:
    @just --list --unsorted

dev:
    process-compose up -t=false

dev-tui:
    process-compose up

db:
  sqlite3 data.db

gen-bsp:
    mill mill.bsp.BSP/install

# creates a new migration by diffing existing migrations against schema.sql
new-migration name:
  scripts/new-db-migration-atlas "{{name}}"

# checks if migrations produce the same schema as in schema.sql
check-migrations:
  find backend/resources/migrations schema.sql scripts/diff_schemas | entr -cnr scripts/diff_schemas

# generates a type-safe function for every query in queries.sql
generate-query-code:
  scripts/generate-query-code

docker-build:
  earthly +build-docker

docker-run:
  # to use jvm debugging, in Earthfile, add the JAVA_OPTS_DEBUG options the java command
  docker run -p 8081:8081 -p 9010:9010 --cpus 1 -m 256M app

ci:
  (git ls-files && git ls-files --others --exclude-standard) | entr -cnr earthly +ci-test

format:
  scalafmt backend frontend rpc

cloc:
  cloc backend frontend rpc index.html main.js queries.sql query_template.go.tmpl schema.schema.sql schema.sql style.css
