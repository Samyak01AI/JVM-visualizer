@echo off
setlocal

echo ======================================================
echo   JVM Memory Management Simulator - Build and Run
echo ======================================================

set SRC_DIR=src
set OUT_DIR=out
set MAIN_CLASS=com.jvm.visualizer.Main

:: Create output directory
if not exist %OUT_DIR% mkdir %OUT_DIR%

:: Collect all .java source files
echo [1/2] Compiling all Java sources...
dir /s /b %SRC_DIR%\*.java > sources.txt

javac -encoding UTF-8 -d %OUT_DIR% @sources.txt

if %ERRORLEVEL% neq 0 (
    echo.
    echo [FAILED] Compilation failed. Make sure Java JDK is installed and on PATH.
    del sources.txt
    pause
    exit /b 1
)

del sources.txt
echo       Compilation successful!
echo.
echo [2/2] Launching JVM Memory Simulator...
java -cp %OUT_DIR% %MAIN_CLASS%

endlocal
