#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
panna_check_dir="$(mktemp -d)"
trap 'rm -rf "$panna_check_dir"' EXIT
java com.sun.tools.javac.Main -encoding UTF-8 -d "$panna_check_dir" PannaText.java PannaSearch.java PannaResearch.java ResearchCheck.java PannaStudy.java StudyCheck.java PannaCatalog.java CatalogCheck.java LogicCheck.java SyntaxCheck.java
java -cp "$panna_check_dir" ResearchCheck
java -cp "$panna_check_dir" StudyCheck
java -cp "$panna_check_dir" CatalogCheck
java -cp "$panna_check_dir" LogicCheck
java -cp "$panna_check_dir" SyntaxCheck
