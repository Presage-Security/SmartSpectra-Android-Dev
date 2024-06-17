# Physiology SDK for Android
This repository hosts a private SDK for measuring heart and respiration rates.

## Using Git LFS
This repository utilizes Git Large File Storage (LFS) for managing binary files such as .so, .jar, .tflite, etc.

To use Git LFS:

1. Ensure Git LFS is installed: `apt install git-lfs` _OR_ `brew install git-lfs`.
2. Run `git lfs install`. This step is only required once after installation.
3. If you've installed Git LFS after already cloning this repository, execute `git lfs pull`.
Git LFS functions similarly to `.gitignore`. The configuration for files managed as LFS objects is in the `.gitattributes` file.

## Modules
There are two main modules in this repository:

* **internal-demo**: This is an app used for development and auto-tests. It should **not be confused** with the [public demo](https://github.com/Presage-Security/SmartSpectra-Android-App) repo intended for end users.
* **sdk**: This module is published to Maven as a public library for end users.

## Mediapipe Integration
The core measurement logic is implemented using Mediapipe.

Graph definitions and TensorFlow Lite files are stored in `sdk/src/main/assets`. This allows for updates to the graph without needing to update the Mediapipe binaries.

To update Mediapipe binaries, build mediapipe.aar and use the script `update_mediapipe_aar.sh path_to_new/mediapipe.aar`.

