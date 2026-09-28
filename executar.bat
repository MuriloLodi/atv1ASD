@echo off
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > fontes.txt
javac -encoding UTF-8 -d out @fontes.txt
if errorlevel 1 (
    del fontes.txt
    exit /b 1
)
del fontes.txt
java -cp out ecommerce.Aplicacao
