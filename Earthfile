# https://docs.earthly.dev/basics

VERSION 0.8

devbox:
  FROM jetpackio/devbox:latest

  # code generated using `devbox generate dockerfile`:
  # Installing your devbox project
  WORKDIR /code
  USER root:root
  RUN mkdir -p /code && chown ${DEVBOX_USER}:${DEVBOX_USER} /code
  USER ${DEVBOX_USER}:${DEVBOX_USER}
  COPY --chown=${DEVBOX_USER}:${DEVBOX_USER} devbox.json devbox.json
  COPY --chown=${DEVBOX_USER}:${DEVBOX_USER} devbox.lock devbox.lock
  RUN devbox run -- echo "Installed Packages."

  USER root:root
  RUN apt-get update && apt-get -y install curl
  USER ${DEVBOX_USER}:${DEVBOX_USER}

test-migrations:
  FROM +devbox
  WORKDIR /code
  COPY schema.sql ./
  COPY backend/resources/migrations backend/resources/migrations
  COPY scripts/diff_schemas scripts/diff_schemas
  RUN devbox run -- scripts/diff_schemas

test-generate-query-code:
  FROM +devbox
  WORKDIR /code
  # since generated code is formatted using scalafmt, cache coursier
  CACHE --chmod 0777 --id coursier /home/devbox/.cache/coursier
  COPY scripts/generate-query-code scripts/generate-query-code
  COPY schema.sql queries.sql queries_template.go.tmpl sqlc.yml .scalafmt.conf ./
  # workaround for https://github.com/scalameta/scalafmt/issues/4156
  # since sqlc is piping generated scala code through scalafmt (sqlc.yml)
  RUN echo "object Main" | scalafmt --stdin --stdout > /dev/null
  COPY backend/src/backend/queries/Queries.scala backend/src/backend/queries/
  RUN devbox run -- "sqlc vet && sqlc diff"

mill-compile:
  FROM +devbox
  WORKDIR /code
  ENV CI=true
  CACHE --chmod 0777 --id mill-cache /home/devbox/.mill
  CACHE --chmod 0777 --id mill-cache /home/devbox/.cache/coursier
  CACHE --chmod 0777 --id mill-out ./out # mill build folder
  COPY +node-modules/node_modules/@shoelace-style/shoelace/dist ./node_modules/@shoelace-style/shoelace/dist
  COPY build.sc schema.sql schema.scala.ssp ./
  COPY --dir rpc backend frontend ./
  RUN devbox run -- mill --jobs 0 --color true __.compile


mill-build-prod:
  FROM +devbox
  WORKDIR /code
  COPY +node-modules/node_modules/@shoelace-style/shoelace/dist ./node_modules/@shoelace-style/shoelace/dist
  ENV CI=true
  COPY build.sc schema.sql schema.scala.ssp ./
  RUN devbox run -- mill __.compile # compile build setup
  COPY --dir rpc ./
  RUN devbox run -- mill 'rpc.{js,jvm}.compile'
  COPY --dir backend ./
  RUN devbox run -- mill backend.assembly
  COPY --dir frontend ./
  RUN devbox run -- mill frontend.fullLinkJS

  # copy artifacts out of cached (not persisted) `out` folder
  RUN cp out/backend/assembly.dest/out.jar dist-backend.jar \
   && cp -a out/frontend/fullLinkJS.dest dist-frontend
  SAVE ARTIFACT dist-backend.jar backend.jar
  SAVE ARTIFACT dist-frontend frontend

node-modules:
  FROM +devbox
  WORKDIR /code
  COPY package.json bun.lockb ./
  RUN devbox run -- bun install
  SAVE ARTIFACT node_modules

vite-build:
  FROM +devbox
  ARG --required AUTHN_URL
  WORKDIR /code
  COPY --dir +node-modules/node_modules ./
  COPY --dir +mill-build-prod/frontend ./out/frontend/fullLinkJS.dest
  COPY --dir main.js index.html vite.config.mts tailwind.config.js postcss.config.js style.css public ./
  RUN devbox run -- bunx vite build
  SAVE ARTIFACT --keep-ts dist static # timestamps must be kept for browser caching


docker-build:
  FROM github.com/social-protocols/GlobalBrain.jl:$GLOBALBRAIN_REF+docker-build
  ARG PROCESS_COMPOSE_VERSION=v1.18.0
  ARG GLOBALBRAIN_REF=fd87c7c0417e74701ed08df5f315c6c9eab7b0e1
  ARG AUTHN_VERSION=v1.20.1
  # https://adoptium.net/blog/2021/12/eclipse-temurin-linux-installers-available/
  RUN apt-get update \
   && apt-get install -y --no-install-recommends \
      wget \
      sqlite3 \
      htop atop net-tools \
      apt-transport-https \
      gnupg \
   && wget --quiet -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | apt-key add - \
   && echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list \
   && apt-get update \
   && apt-get install -y --no-install-recommends \
      temurin-22-jre \
   && wget --quiet -O - https://github.com/F1bonacc1/process-compose/releases/download/$PROCESS_COMPOSE_VERSION/process-compose_linux_amd64.tar.gz \
    | tar -xz -C /usr/local/bin \
   && rm -f process-compose_linux_amd64.tar.gz \
   && wget --quiet https://github.com/keratin/authn-server/releases/download/$AUTHN_VERSION/authn-linux64 -O /usr/local/bin/authn-server \
   && chmod +x /usr/local/bin/authn-server

  WORKDIR /app
  COPY +mill-build-prod/backend.jar ./
  COPY --dir --keep-ts +vite-build/static ./
  COPY ./process-compose-prod.yml process-compose.yml
  RUN mkdir /data
  RUN find . -maxdepth 2
  CMD process-compose --no-server --tui=false
  SAVE IMAGE app:latest


app-deploy:
  # run locally:
  # FLY_API_TOKEN=$(flyctl tokens create deploy) earthly --allow-privileged --secret FLY_API_TOKEN +ci-deploy --COMMIT_SHA=$(git rev-parse HEAD) --FLY_APP_NAME=<fly-app-name>
  ARG --required COMMIT_SHA
  ARG --required FLY_APP_NAME
  ARG IMAGE="registry.fly.io/$FLY_APP_NAME:deployment-$COMMIT_SHA"
  FROM earthly/dind:alpine-3.19-docker-25.0.5-r0
  RUN apk add curl \
   && set -eo pipefail; curl -L https://fly.io/install.sh | sh
  COPY fly.toml ./
  WITH DOCKER --load $IMAGE=+docker-build
    RUN --secret FLY_API_TOKEN \
        docker image ls \
     && /root/.fly/bin/flyctl auth docker \
     && docker push $IMAGE \
     && /root/.fly/bin/flyctl deploy --image $IMAGE --build-arg COMMIT_SHA=$COMMIT_SHA
  END

scalafmt:
  FROM +devbox
  WORKDIR /code
  COPY .scalafmt.conf ./

  # https://scalameta.org/scalafmt/docs/installation.html#native-image
  USER root:root
  ENV VERSION=$(awk -F'"' '/version/ {print $2; exit}' .scalafmt.conf)
  ENV INSTALL_LOCATION=/usr/local/bin/scalafmt-native
  RUN curl https://raw.githubusercontent.com/scalameta/scalafmt/master/bin/install-scalafmt-native.sh \
      | bash -s -- $VERSION $INSTALL_LOCATION \
   && $INSTALL_LOCATION --version
  USER ${DEVBOX_USER}:${DEVBOX_USER}

  COPY --dir backend frontend rpc ./
  RUN scalafmt-native --check

lint:
  BUILD +scalafmt

ci-test:
  BUILD +test-migrations
  BUILD +test-generate-query-code
  BUILD +lint
  BUILD +mill-compile

ci-deploy:
  # To run manually:
  # FLY_API_TOKEN=$(flyctl tokens create deploy) FLY_APP_NAME=<my-app-name> earthly --allow-privileged --secret FLY_API_TOKEN +ci-deploy --COMMIT_SHA=$(git rev-parse HEAD)
  BUILD +ci-test
  ARG --required COMMIT_SHA
  ARG --required FLY_APP_NAME
  BUILD +app-deploy --COMMIT_SHA=$COMMIT_SHA --FLY_APP_NAME=$FLY_APP_NAME
