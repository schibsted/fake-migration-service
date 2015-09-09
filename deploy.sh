#!/usr/bin/env bash

VERSION=${TRAVIS_TAG:-latest}

rm -rf deploy dist

mkdir deploy dist && \
cp build/libs/fake-migration-service*-all.jar deploy/fake-migration-service.jar && \
cp run.sh deploy && \
echo "Build: $TRAVIS_BUILD_NUMBER, Commit: $TRAVIS_COMMIT" > deploy/build.txt && \
tar -C deploy -zcvf dist/${VERSION}.tar.gz .
