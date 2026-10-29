#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
panna_check_dir="$(mktemp -d)"
trap 'rm -rf "$panna_check_dir"' EXIT
java com.sun.tools.javac.Main -encoding UTF-8 -d "$panna_check_dir" src/main/java/com/harpreet/panna/PannaText.java src/main/java/com/harpreet/panna/PannaSearch.java src/main/java/com/harpreet/panna/PannaResearch.java ResearchCheck.java src/main/java/com/harpreet/panna/PannaStudy.java StudyCheck.java src/main/java/com/harpreet/panna/PannaCatalog.java CatalogCheck.java LogicCheck.java SyntaxCheck.java
java -cp "$panna_check_dir" ResearchCheck
java -cp "$panna_check_dir" StudyCheck
java -cp "$panna_check_dir" CatalogCheck
java -cp "$panna_check_dir" LogicCheck
java -cp "$panna_check_dir" SyntaxCheck
