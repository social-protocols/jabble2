# List available recipes in the order in which they appear in this file
_default:
    @just --list --unsorted

dev:
    process-compose up -t=false

db:
  sqlite3 data.db

gen-bsp:
    mill mill.bsp.BSP/install

# creates a new migration by diffing existing migrations against schema.sql
new-migration name:
  scripts/new-db-migration-atlas "{{name}}"

docker:
  earthly +build-docker

docker-run:
  # to use jvm debugging, in Earthfile, add the JAVA_OPTS_DEBUG options the java command
  docker run -p 8081:8081 -p 9010:9010 --cpus 1 -m 256M app

ci:
  (git ls-files && git ls-files --others --exclude-standard) | entr -cnr earthly +ci-test

rpc-clean:
  # workaround after changing rpc api trait
  mill clean "{rpc,frontend,backend}"
