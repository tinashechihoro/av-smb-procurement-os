@REM Maven Wrapper for Windows
@REM Downloads Maven if not cached, then runs it

@echo off
setlocal

set MAVEN_PROJECTBASEDIR=%~dp0
set WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar
set WRAPPER_URL=https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar

if not exist "%WRAPPER_JAR%" (
    echo Downloading Maven Wrapper...
    powershell -Command "(New-Object Net.WebClient).DownloadFile('%WRAPPER_URL%', '%WRAPPER_JAR%')"
)

set MAVEN_OPTS=-Dmaven.multiModuleProjectDirectory="%MAVEN_PROJECTBASEDIR%"

java %MAVEN_OPTS% -jar "%WRAPPER_JAR%" %*
