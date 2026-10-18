#!/bin/bash
# Build in a Linux-native copy of the repo mounted at /src (a Windows bind mount is slow and its local.properties points at the host SDK),
# then copy the APKs back. Usage: airplay-build [gradle args, default: :app:assembleDebug]
set -e
rsync -a --delete --exclude=/.git --exclude=local.properties --exclude=/.gradle --exclude=/build --exclude=/app/build --exclude=/unlock/build --exclude=/app/.cxx --exclude=/.kotlin /src/ /work/
echo "sdk.dir=$ANDROID_HOME" > /work/local.properties
cd /work
bash ./gradlew --no-daemon --console=plain "${@:-:app:assembleDebug}"
mkdir -p /src/app/build/outputs
rsync -a /work/app/build/outputs/ /src/app/build/outputs/
mkdir -p /src/unlock/build/outputs
rsync -a /work/unlock/build/outputs/ /src/unlock/build/outputs/ || true
