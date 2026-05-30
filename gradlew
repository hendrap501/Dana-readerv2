#!/bin/sh
#
# Gradle start up script for UN*X
#
APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`
APP_HOME="`pwd -P`"
CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar
JAVA_OPTS=""
GRADLE_OPTS=""
MAX_FD="maximum"
warn () { echo "$*"; }
die () { echo; echo "$*"; echo; exit 1; }
OS_NAME="`uname`" 2>/dev/null
case "$OS_NAME" in
  CYGWIN* )         cygwin=true  ;;
  Darwin* )         darwin=true  ;;
  MINGW* )          msys=true    ;;
esac
if [ "$cygwin" = "true" -o "$msys" = "true" ] ; then
  APP_HOME=`cygpath --path --mixed "$APP_HOME"`
  CLASSPATH=`cygpath --path --mixed "$CLASSPATH"`
fi
exec "$JAVACMD" $JAVA_OPTS $GRADLE_OPTS \
  -classpath "$CLASSPATH" \
  org.gradle.wrapper.GradleWrapperMain \
  "$@"
