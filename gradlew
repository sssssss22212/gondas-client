#!/bin/sh
APP_HOME=$( cd "${0%/*}" && pwd )
exec java -cp "$APP_HOME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
