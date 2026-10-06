#!/bin/sh
# Compile and run the Sentiment Analysis web app (needs a JDK 17+)
cd "$(dirname "$0")"
mkdir -p out
javac -d out $(find src -name "*.java") && cp -r data web out/ && java -cp out sentiment.Main
