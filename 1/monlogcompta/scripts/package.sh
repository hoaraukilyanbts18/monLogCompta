#!/usr/bin/env bash
# Construit le JAR exécutable puis un installateur natif pour l'OS courant (jpackage, JDK 21).
# À lancer sur chaque OS cible : un JAR JavaFX embarque les natifs de la machine qui le construit.
set -euo pipefail
cd "$(dirname "$0")/.."

mvn -B clean package

rm -rf target/installer
mkdir -p target/installer target/jpackage-input
cp target/monlogcompta-1.0.0-shaded.jar target/jpackage-input/

jpackage \
  --type "${1:-app-image}" \
  --name MonLogCompta \
  --app-version 1.0.0 \
  --input target/jpackage-input \
  --main-jar monlogcompta-1.0.0-shaded.jar \
  --main-class fr.monlogcompta.Launcher \
  --dest target/installer

echo "Installateur généré dans target/installer"
# Types : app-image (tous OS), exe/msi (Windows, nécessite WiX 3), deb/rpm (Linux), dmg/pkg (macOS)
