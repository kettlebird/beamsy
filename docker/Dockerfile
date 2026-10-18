# Linux build environment for the native (OpenSSL/FFmpeg) part, which cannot be built on a Windows host.
# Mirrors .github/workflows/apk.yml (JDK 21 on Ubuntu).
FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive \
    ANDROID_HOME=/opt/android-sdk \
    JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

RUN apt-get update && apt-get install -y --no-install-recommends \
        openjdk-21-jdk-headless build-essential perl git curl unzip rsync ca-certificates python3 \
    && rm -rf /var/lib/apt/lists/*

# Google's official command-line tools, then exactly what app/build.gradle.kts asks for.
RUN mkdir -p $ANDROID_HOME/cmdline-tools \
    && curl -fsSL -o /tmp/clt.zip https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip \
    && unzip -q /tmp/clt.zip -d /tmp/clt && mv /tmp/clt/cmdline-tools $ANDROID_HOME/cmdline-tools/latest && rm -rf /tmp/clt /tmp/clt.zip \
    && yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses >/dev/null \
    && $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platform-tools" "platforms;android-37.0" \
        "build-tools;36.0.0" "ndk;27.0.12077973" "cmake;3.22.1"

# root-run git on the bind-mounted repo would otherwise refuse with "dubious ownership"
RUN git config --global --add safe.directory '*'

COPY build.sh /usr/local/bin/airplay-build
RUN chmod +x /usr/local/bin/airplay-build
WORKDIR /work
ENTRYPOINT ["airplay-build"]
