# List available recipes in the order in which they appear in this file
_default:
    @just --list --unsorted

dev:
    process-compose up -t=false

dev-tui:
    process-compose up

db:
  sqlite3 data/app.db

reset-db:
  rm -f data/app.db
  rm -f data/authn.db
  rm -f data/globalbrain.db
  sqlite3 -init /dev/null data/app.db < schema.sql

gen-bsp:
    mill mill.bsp.BSP/install

# creates a new migration by diffing existing migrations against schema.sql
new-migration name:
  scripts/new-db-migration-atlas "{{name}}"

# migrate the dev database to reflect changes in schema.sql without losing data
migrate-dev:
  scripts/dev-db-migration-atlas

# checks if migrations produce the same schema as in schema.sql
check-migrations:
  find backend/resources/migrations schema.sql scripts/diff_schemas | entr -cnr scripts/diff_schemas

# generates a type-safe function for every query in queries.sql
generate-query-code:
  scripts/generate-query-code

docker-build:
  earthly +docker-build

docker-run:
  # to use jvm debugging on port 9010, 
  # in process-compose-prod.yml, add the JAVA_OPTS_DEBUG options to the java command
  
  docker run \
    --init \
    -p 8081:8081 -p 3000:3000 -p 9010:9010 \
    --cpus 1 -m 1024M \
    -e APP_JDBC_URL="jdbc:sqlite:/data/app.db" \
    -e GLOBALBRAIN_DATABASE_PATH="/data/globalbrain.db" \
    -e AUTHN_DATABASE_URL="sqlite3://localhost//data/authn.db" \
    -e AUTHN_APP_DOMAINS="localhost" \
    -e AUTHN_AUTHN_URL="http://localhost:3000" \
    -e AUTHN_SECRET_KEY_BASE="test" \
    -e AUTHN_HTTP_AUTH_USERNAME="admin" \
    -e AUTHN_HTTP_AUTH_PASSWORD="adminpw" \
    app

ci:
  (git ls-files && git ls-files --others --exclude-standard) | entr -cnr earthly +ci-test

format:
  scalafmt backend frontend rpc

cloc:
  cloc backend frontend rpc index.html main.js queries.sql query_template.go.tmpl schema.schema.sql schema.sql style.css
