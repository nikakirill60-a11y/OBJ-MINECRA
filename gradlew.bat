@echo off
rem Minimal Gradle wrapper launcher (Windows).
rem Requires: JDK 17+ and gradle\wrapper\gradle-wrapper.jar
rem (run `gradle wrapper --gradle-version 8.5` once with a system Gradle to generate the jar,
rem  or just build with system Gradle: `gradle build`).

set APP_HOME=%~dp0

if defined JAVA_HOME (
  set JAVACMD=%JAVA_HOME%\bin\java.exe
) else (
  set JAVACMD=java.exe
)

if not exist "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" (
  echo ERROR: gradle\wrapper\gradle-wrapper.jar not found.
  echo Run 'gradle wrapper --gradle-version 8.5' once with a system Gradle 8.x,
  echo or build directly with system Gradle: 'gradle build'.
  exit /b 1
)

"%JAVACMD%" -classpath "%APP_HOME%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
