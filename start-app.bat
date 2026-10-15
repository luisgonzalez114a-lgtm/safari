@echo off
setlocal
cd /d "%~dp0"

set "MVN=%~dp0apache-maven-3.9.9\bin\mvn.cmd"

if not exist "%MVN%" (
    echo No se encontro Maven en la carpeta del proyecto.
    echo Revisa que la carpeta apache-maven-3.9.9 exista.
    pause
    exit /b 1
)

where java >nul 2>nul
if errorlevel 1 (
    echo No se encontro Java. Instala Java 17 o superior y vuelve a intentarlo.
    pause
    exit /b 1
)

echo Iniciando SAFARI...
start "SAFARI - Servidor" cmd /k ""%MVN%" spring-boot:run"

echo Esperando a que SAFARI este listo...
for /L %%i in (1,1,60) do (
    curl.exe --silent --fail --output NUL --max-time 2 http://localhost:8096/actuator/health
    if not errorlevel 1 goto :ready
    timeout /t 1 /nobreak >nul
)

echo.
echo SAFARI no respondio en 60 segundos.
echo Revisa la ventana "SAFARI - Servidor" y confirma que MySQL esta activo
echo y que ya importaste safari.sql en phpMyAdmin.
pause
exit /b 1

:ready
echo SAFARI esta listo. Abriendo el navegador...
start "" http://localhost:8096/login
exit /b 0
