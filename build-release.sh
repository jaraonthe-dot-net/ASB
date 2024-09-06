#!/usr/bin/env bash

# This is used to build an ASB release, which includes a .jar, examples, docs,
# and executable files, all packaged into a zip file.
#
# Requirements:
# - Run this on Linux
# - javac 21 (or later)
# - rsync
# - zip

MAIN_SRC_FILE="src/net/jaraonthe/java/asb/ASB.java"

cd "$(dirname "$0")"

# Check Prerequisites
RESULT=$(javac --version | grep -Pc "javac [2-9][1-9]")
if [[ $RESULT == "0" ]]; then
    echo "Unsupported javac version, should be 21 or higher, is:"
    javac --version
    exit 1
fi
echo "Using $(javac --version)"

# Version string configured in ASB.java - should be updated for every new release
VERSION=$(grep "String VERSION = " $MAIN_SRC_FILE | cut -d "\"" -f 2)
OUT_DIR="build/ASB$VERSION"

read -p "Is this the ASB version you want to build? \"$VERSION\" (y/n)" -n 1 -r
echo # newline
if [[ $REPLY =~ [^Yy] ]]; then
    echo "ABORT"
    exit
fi

read -p "Is CHANGELOG up to date? (y/n)" -n 1 -r
echo # newline
if [[ $REPLY =~ [^Yy] ]]; then
    echo "ABORT"
    exit
fi

rm -r "build"
mkdir -pv "build" || exit 1

echo "Compiling..."
javac $MAIN_SRC_FILE --source-path "src" -d "build/classfiles" --release 21 -Werror || { echo "javac failed"; exit 1; }

echo "Creating .jar"
jar -c -f "$OUT_DIR/ASB.jar" -e "net.jaraonthe.java.asb.ASB" -C "build/classfiles" . || exit 1

echo "Adding executable files"
# Linux
cp "release-files/asb" "$OUT_DIR/asb" || exit 1
chmod 775 "$OUT_DIR/asb" || exit 1
# Windows
cp "release-files/asb.bat" "$OUT_DIR/asb.bat" || exit 1

# TODO compile doc markdown into html?

echo "Adding supporting files"
$OUT_DIR/asb --version > $OUT_DIR/VERSION || exit 1
cp "CHANGELOG" "$OUT_DIR/CHANGELOG" || exit 1
cp "README.md" "$OUT_DIR/README.md" || exit 1
cp "LICENSE.txt" "$OUT_DIR/LICENSE.txt" || exit 1
rsync -a --exclude=".*" "doc" "$OUT_DIR" || exit 1
rsync -a --exclude=".*" "asb/example" "$OUT_DIR" || exit 1
rsync -a --exclude=".*" "asb/lib" "$OUT_DIR" || exit 1

echo "Creating zip file"
cd "build"
zip -r "ASB$VERSION.zip" "ASB$VERSION"
cd ..

echo "Done."
