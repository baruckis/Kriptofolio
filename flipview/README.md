# FlipView (vendored)

Local copy of [FlipView](https://github.com/davideas/FlipView) by Davide Steduto, licensed under
the Apache License 2.0 (`LICENSE` in this folder). The app uses it for the flipping coin icons.

Source: the `eu.davidea:flipview:1.2.0` sources jar and AAR resources from Maven Central. The Java
files and resources are copied unchanged, except that Windows line endings (CRLF) are converted to
LF and the resource folder qualifier `-v4`, which the build added to the published AAR, is
dropped. The package name `eu.davidea.flipview` stays the
same, so layouts and code need no change.

Why it is vendored: the republished 1.2.0 artifact contains Java 21 bytecode and drags in
`kotlin-stdlib 2.2.10`, which forced JDK 21 and AGP 8.6.0 on the whole build
(`UPGRADE-NOTES.md`, section 6). Built from source here, it compiles to the app's Java 8 target.

Do not change these files except to fix a defect; record any change below.

## Changes from upstream

None.
