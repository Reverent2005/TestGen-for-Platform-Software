@echo off
echo Compiling all Java files...
javac *.java

if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo.
echo Running sample example (default)...
java SampleExample

echo.
echo Running complex example...
java SampleExample createComplexExample

echo.
echo Running triple example (increment, process, reverse)...
java SampleExample tripleExample

echo.
echo All examples completed!
pause
