@ECHO OFF
SETLOCAL
set GRADLE_VERSION=8.7
set HERE=%~dp0
set GRADLE_BIN=%HERE%\.gradle\gradle-%GRADLE_VERSION%\bin\gradle.bat
IF NOT EXIST "%GRADLE_BIN%" (
  powershell -Command "Invoke-WebRequest https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip -OutFile gradle-temp.zip"
  powershell -Command "Expand-Archive gradle-temp.zip -DestinationPath .gradle"
  del gradle-temp.zip
)
"%GRADLE_BIN%" %*
