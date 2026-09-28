#!/bin/sh
rm -rf out
mkdir out
javac -encoding UTF-8 -d out $(find src -name "*.java") || exit 1
java -cp out ecommerce.Aplicacao
