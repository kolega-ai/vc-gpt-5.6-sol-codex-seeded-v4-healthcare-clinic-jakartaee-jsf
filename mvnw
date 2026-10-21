#!/bin/sh
# Licensed Maven Wrapper script generated for this repository.
set -eu
BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROP="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"
URL=$(sed -n 's/^distributionUrl=//p' "$PROP")
MVN_DIR="$BASE_DIR/.mvn/apache-maven-3.9.11"
if [ ! -x "$MVN_DIR/bin/mvn" ]; then
  ZIP="$BASE_DIR/.mvn/maven.zip"
  curl -fsSL "$URL" -o "$ZIP"
  unzip -q "$ZIP" -d "$BASE_DIR/.mvn"
  rm -f "$ZIP"
fi
exec "$MVN_DIR/bin/mvn" "$@"
