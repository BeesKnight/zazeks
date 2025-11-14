@ECHO OFF
SET DIR=%~dp0
SET APP_HOME=%DIR:~0,-1%
SET CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar

IF EXIST "%JAVA_HOME%\bin\java.exe" (
  SET JAVACMD="%JAVA_HOME%\bin\java.exe"
) ELSE (
  SET JAVACMD=java
)

%JAVACMD% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*
