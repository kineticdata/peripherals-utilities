#!/bin/bash

# Stop running the script on an error
set -e
# Build the jar
docker build -f Dockerfile.builder -t kineticdata/filehub-adapter-local-builder:latest .
docker run -v $PWD:/project -w=/project kineticdata/filehub-adapter-local-builder:latest