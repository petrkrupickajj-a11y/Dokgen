@echo off
setlocal

rem Sestavi appku do samostatneho Windows programu (dist\Dokgen\Dokgen.exe),
rem ktery jde spustit dvojklikem - zabaleny i s vlastni Javou, takze staci
rem jen tenhle .exe zkopirovat/predat dal, cilovy pocitac nepotrebuje mit
rem Javu nainstalovanou. Vyzaduje JDK 17+ (jpackage je jeho soucast).

echo Sestavuji appku (mvnw package)...
call mvnw.cmd clean package -DskipTests
if errorlevel 1 (
    echo.
    echo Sestaveni selhalo - zkontroluj chyby vyse.
    pause
    exit /b 1
)

if exist target\jpackage-input rmdir /s /q target\jpackage-input
mkdir target\jpackage-input
copy /y target\dokgen-1.0.0.jar target\jpackage-input\ >nul

if exist dist rmdir /s /q dist

echo.
echo Balim do .exe (jpackage)...
jpackage ^
    --type app-image ^
    --input target\jpackage-input ^
    --main-jar dokgen-1.0.0.jar ^
    --name Dokgen ^
    --app-version 1.0.0 ^
    --vendor "Petr Krupicka" ^
    --description "Generator dokumentu ze sablon" ^
    --dest dist

if errorlevel 1 (
    echo.
    echo Vytvoreni .exe selhalo - over, ze prikaz "jpackage --version" v konzoli funguje
    echo ^(je soucasti JDK 17+, ne samostatneho JRE^).
    pause
    exit /b 1
)

echo.
echo Hotovo. Spustitelny program: dist\Dokgen\Dokgen.exe
echo Muzes si na nej vytvorit zastupce na plochu.
pause
