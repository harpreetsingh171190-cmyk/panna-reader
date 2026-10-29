#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
panna_check_dir="$(mktemp -d)"
trap 'rm -rf "$panna_check_dir"' EXIT
java com.sun.tools.javac.Main -encoding UTF-8 -d "$panna_check_dir" reader/src/main/java/com/amrik/panna/PannaText.java reader/src/main/java/com/amrik/panna/PannaSearch.java reader/src/main/java/com/amrik/panna/PannaResearch.java checks/PannaResearchCheck.java reader/src/main/java/com/amrik/panna/PannaStudy.java checks/PannaStudyCheck.java reader/src/main/java/com/amrik/panna/PannaCatalog.java checks/PannaCatalogCheck.java checks/PannaLogicCheck.java checks/PannaSyntaxCheck.java
java -cp "$panna_check_dir" PannaResearchCheck
java -cp "$panna_check_dir" PannaStudyCheck
java -cp "$panna_check_dir" PannaCatalogCheck
java -cp "$panna_check_dir" PannaLogicCheck
java -cp "$panna_check_dir" PannaSyntaxCheck
