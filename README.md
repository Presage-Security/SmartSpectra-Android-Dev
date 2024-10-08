# Physiology SDK for Android
This repository hosts a private SDK for measuring heart and respiration rates.

## Using Git LFS
This repository utilizes Git Large File Storage (LFS) for managing binary files such as .so, .jar, .tflite, etc.

To use Git LFS:

1. Ensure Git LFS is installed [docs.github.com](https://docs.github.com/en/repositories/working-with-files/managing-large-files/installing-git-large-file-storage).
2. Run `git lfs install`. This step is only required once after installation.
3. If you've installed Git LFS after already cloning this repository, execute `git lfs pull`.
Git LFS functions similarly to `.gitignore`. The configuration for files managed as LFS objects is in the `.gitattributes` file.

## Modules
There are two main modules in this repository:

* **[internal-demo](/internal-demo)**: This is an app used for development and auto-tests. It should **not be confused** with the [public demo](https://github.com/Presage-Security/SmartSpectra-Android-App) repo intended for end users.
* **[sdk](/sdk)**: This module is published to Maven as a public library for end users.
* **android app** [public demo](https://github.com/Presage-Security/SmartSpectra-Android-App).

## Mediapipe Integration
The core measurement logic is implemented using Mediapipe.

Graph definitions and TensorFlow Lite files are stored in `sdk/src/main/assets`. This allows for updates to the graph without needing to update the Mediapipe binaries.

To update Mediapipe binaries, build mediapipe.aar and use the script `update_mediapipe_aar.sh path_to_new/mediapipe.aar`.

### Troubleshooting

If, after updating the framework, you encounter something like this:
![GradleSyncError.png](media/GradleSyncError.png)

OR the `R` value in `internal-demo/src/main/java/com/presagetech/smartspectra_example/MainActivity.kt` stops getting resolved by the AndroidStudio linter,

Run `Sync Project With Gradle Files` (the little icon in the top right corner with left-down-pointing arrow and some animal, possibly an elephant).