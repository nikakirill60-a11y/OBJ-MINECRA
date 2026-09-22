#!/bin/sh
# Minimal Gradle wrapper launcher (posix sh).
# Requires: JDK 17+ and gradle/wrapper/gradle-wrapper.jar
# (run `gradle wrapper --gradle-version 8.5` once with a system Gradle to generate the jar,
#  or just build with system Gradle: `gradle build`).

APP_HOME=$(cd "$(dirname "$0")" && pwd -P) || exit 1

if [ -n "$JAVA_HOME" ] ; then
  JAVACMD="$JAVA_HOME/bin/java"
else
  JAVACMD="java"
fi

WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ] ; then
  echo "ERROR: $WRAPPER_JAR not found." >&2
  echo "Run 'gradle wrapper --gradle-version 8.5' once with a system Gradle 8.x," >&2
  echo "or build directly with system Gradle: 'gradle build'." >&2
  exit 1
fi

exec "$JAVACMD" -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
