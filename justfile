# List available recipes in the order in which they appear in this file
_default:
  @just --list --unsorted

# start local development environment (interleaved logs)
dev:
  find process-compose.yml | entr -nr process-compose up -t=false

# start local development environment (tui)
dev-tui:
  process-compose up

# open app database in sqlite repl
db:
  sqlite3 data/app.db

# clear and seed databases
reset-db:
  rm -f data/app.db
  rm -f data/authn.db
  rm -f data/globalbrain.db
  sqlite3 -init /dev/null data/app.db < schema.sql

# workaround for rpc linking errors
clean-rpc:
  mill clean "{rpc,frontend,backend}";

# generate BSP (build server protocol) project
gen-bsp:
  mill mill.bsp.BSP/install

# generates a type-safe function for every query in queries.sql
generate-query-code:
  scripts/generate-query-code

# creates a new migration by diffing existing migrations against schema.sql
new-migration name:
  scripts/new-db-migration-atlas "{{name}}"

# migrate the dev database to reflect changes in schema.sql without losing data
migrate-dev:
  scripts/dev-db-migration-atlas

# checks if migrations produce the same schema as in schema.sql
check-migrations:
  find backend/resources/migrations schema.sql scripts/diff_schemas | entr -cnr scripts/diff_schemas

# build production docker image
docker-build:
  earthly +docker-build

# run production docker image
docker-run:
  # TODO: why is the image still running after Ctrl+C? Might have to do with --init?
  # to use jvm debugging on port 9010, 
  # in process-compose-prod.yml, add the JAVA_OPTS_DEBUG options to the java command
  
  docker run --name app --rm \
    --init \
    -p 8081:8081 -p 3000:3000 -p 9010:9010 \
    --cpus 1 -m 1024M \
    -e APP_JDBC_URL="jdbc:sqlite:/data/app.db" \
    -e GLOBALBRAIN_DATABASE_PATH="/data/globalbrain.db" \
    -e AUTHN_DATABASE_URL="sqlite3://localhost//data/authn.db" \
    -e AUTHN_APP_DOMAINS="localhost" \
    -e AUTHN_URL="http://localhost:3000" \
    -e AUTHN_SECRET_KEY_BASE="test" \
    -e AUTHN_HTTP_AUTH_USERNAME="admin" \
    -e AUTHN_HTTP_AUTH_PASSWORD="adminpw" \
    app

# run ci checks locally
ci:
  (git ls-files && git ls-files --others --exclude-standard) | entr -cnr earthly +ci-test

# format source code
format:
  scalafmt backend frontend rpc

# count lines of code in repo
cloc:
  cloc --vcs=git

# deploy local state to production
prod-deploy:
  read -p 'Are you sure? (y/n): ' confirm && [[ $confirm == [yY] ]] && \
  FLY_API_TOKEN=$(flyctl tokens create deploy) earthly --allow-privileged --secret FLY_API_TOKEN +ci-deploy --COMMIT_SHA=$(git rev-parse HEAD) --FLY_APP_NAME=jabble

# show live logs from production
prod-logs:
  flyctl logs

# ssh connection to production server
prod-ssh:
  flyctl ssh console
