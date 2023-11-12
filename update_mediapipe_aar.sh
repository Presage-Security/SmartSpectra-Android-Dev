#!/bin/bash

ZIP_FILE=${1:-presage_android_aar.aar}

unzip -o "$ZIP_FILE" "jni/*" -d "./sdk/"
unzip -o "$ZIP_FILE" "classes.jar" -d "sdl/libs/"
