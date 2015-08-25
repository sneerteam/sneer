FROM ubuntu:14.04

# Development user
RUN echo "%sudo ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers \
    && useradd -u 1000 -G sudo -d /home/developer --shell /bin/bash -m developer \
    && echo "secret\nsecret" | passwd developer

# Basic packages and Java 8
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
                      blackbox \
                      build-essential \
                      curl \
                      bison \
                      git \
                      gperf \
                      lib32gcc1 \
                      lib32bz2-1.0 \
                      lib32ncurses5 \
                      lib32stdc++6 \
                      lib32z1 \
                      libc6-i386 \
                      libxml2-utils \
                      make \
                      software-properties-common \
                      unzip \
                      libX11-dev libxext-dev libxrender-dev libxtst-dev
    RUN add-apt-repository ppa:webupd8team/java \
    && apt-get update \
    && echo oracle-java8-installer shared/accepted-oracle-license-v1-1 select true | sudo /usr/bin/debconf-set-selections \
    && apt-get install -y oracle-java8-installer \
                      ca-certificates-java \
    && sudo apt-get install oracle-java8-set-default \
    && apt-get clean \
    && apt-get autoremove \
    && rm -rf /var/lib/apt/lists/*

# Set things up using the dev user and reduce the need for `chown`s
USER developer

# Android SDK
RUN curl -sL http://dl.google.com/android/android-sdk_r24.3.4-linux.tgz | tar -zxv -C /home/developer/

# Android Studio
RUN cd /opt \
    && sudo mkdir android-studio \
    && sudo chown developer:developer android-studio \
    && curl -L https://dl.google.com/dl/android/studio/ide-zips/1.3.1.0/android-studio-ide-141.2135290-linux.zip > /tmp/android-studio.zip \
    && unzip /tmp/android-studio.zip \
    && rm /tmp/android-studio.zip

# Configure the SDK
ENV ANDROID_HOME="/home/developer/android-sdk-linux" \
    PATH="${PATH}:/home/developer/android-sdk-linux/tools:/home/developer/android-sdk-linux/platform-tools:/home/developer/bin" \
    JAVA_HOME="/usr/lib/jvm/java-8-oracle"

RUN echo y | android update sdk --all --no-ui --force --filter android-22
RUN echo y | android update sdk --all --no-ui --force --filter platform-tools
RUN echo y | android update sdk --all --no-ui --force --filter extra-android-m2repository
RUN echo y | android update sdk --all --no-ui --force --filter extra-google-m2repository
RUN echo y | android update sdk --all --no-ui --force --filter source-22
RUN echo y | android update sdk --all --no-ui --force --filter build-tools-22.0.1
RUN echo y | android update sdk --all --no-ui --force --filter android-21
RUN echo y | android update sdk --all --no-ui --force --filter build-tools-21.1.2

# TODO: Merge this into the studio installation step
RUN sudo ln -s /opt/android-studio/bin/studio.sh /bin/studio

# Install Leiningen
RUN cd ~ \
 && mkdir bin \
 && cd bin \
 && wget https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein \
 && chmod a+x ~/bin/lein \
 && lein \
 && echo y | lein downgrade 2.4.3

# Install Cursive
RUN mkdir -p /home/developer/.AndroidStudio1.3/config/plugins/ \
 && cd /home/developer/.AndroidStudio1.3/config/plugins/ \
 && curl -L https://cursiveclojure.com/cursive-14.1-0.1.59.zip > cursive.zip \
 && unzip cursive.zip \
 && ls
