#!/usr/bin/env bash

APPLICATION_PORT=8080

docker-compose -f ./src/it/docker-compose.yml up --force-recreate -d

sbt run &
while ! nc -z 127.0.0.1 $APPLICATION_PORT; do sleep 1; done;

sbt it:test

docker-compose -f ./src/it/docker-compose.yml down

pkill java