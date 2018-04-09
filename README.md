# dexterous 

[![Build Status](https://travis-ci.org/Project-ARTist/dexterous.svg?branch=master)](https://travis-ci.org/Project-ARTist/dexterous) [![Gitter](https://badges.gitter.im/Project-ARTist/meta.svg)](https://gitter.im/project-artist/Lobby?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=body_badge)

Dexterous is a commandline interface to ARTist's partial dex merging, apk-packaging and apk-signing. It is used as a preprocessing step before handing apk and zip files to ARTist, where its main task is to *partially merge* a [CodeLib](https://github.com/Project-ARTist/CodeLib) so that ARTist modules can, e.g., inject calls to CodeLib methods into the target code. Partial merge means that the CodeLib dex files are added to the target and their methods are registered with the existing target dex files. Essentially, the CodeLib's *symbols* (dex method identifiers) are added to the target dex files to make it possible for the target code to call into the CodeLib. For more information about the ARTist ecosystem, see the dedicated section below. 

In addition to partially merging dex files, dexterous can also be used for analyzing dex files: 
Take a look at the class [`saarland.cispa.dexterous.cli.Dexterously`](src/main/java/saarland/cispa/dexterous/cli/Dexterously.java)  to get started with implementing your own analysis.

## Build

You can build  a fat jar with all depedencies with the following command:

``` bash
./gradlew DexterousJar
```
### Build files

`desktop.gradle` is the build file for the desktop build.

`build.gradle` is the build file for the android-library build.

It implicitly uses the bundled debug keystore: `res/artist-debug.keystore`.


## dexterous usage

```
user@host ~/dexterous $ java -jar build/libs/dexterous.jar --help

usage: Dexterously <options>
 -a,--analyze          Analyze APK
 -b,--build-apk        Build partially merged APK
 -c,--codelib <file>   Path to codelib.apk (name doesn't matter).
 -h,--help             Prints this message.
 -m,--merge            Build merged APK
 -s,--sign-apk         Build and sign partialy merged APK
```

### Merge codelib partially w/o signing the apk

`java -jar dexterous.jar my_application.apk --codelib codelib.apk --build-apk`

### Merge codelib partially and resign apk

`java -jar dexterous.jar my_application.apk --codelib codelib.apk --build-apk --sign-apk`

### Merge two dex files completely

`java -jar dexterous.jar --merge my_application.dex library.dex`

### Analyze apk only

> Execute dexterous without the build / sign flags

`java -jar dexterous.jar --analyze my_application.apk`

## Third-party code usage:

> dexterous uses a lot of third-party code

### Dependency libraries:

> The following code is used as depedency libraries:

- [com.android.support.design](https://developer.android.com/training/material/design-library.html)
- [co.trikita.log ](https://github.com/zserge/log)
- [Apache commons-io](https://commons.apache.org/proper/commons-io/)
- [Apache commons-cli](https://commons.apache.org/proper/commons-cli/)
- [Spongycastle](https://rtyley.github.io/spongycastle/):
    - com.madgag.spongycastle.core
    - com.madgag.spongycastle.prov
    - com.madgag.spongycastle.pkix


### Included code

> The following code is included, but moved to different packages, modified and fixed:

- dex, dx and multidex folders from android's [dalvik repo](https://android.googlesource.com/platform/dalvik/), specifically from [this  dalvik subfolder](https://android.googlesource.com/platform/dalvik/+/master/dx/src/com/android).
- [apksig](https://android.googlesource.com/platform/tools/apksig/) which is used on the dexterous standalone tool
- [zipsigner](https://sites.google.com/site/zipsigner/) which is in the android library variant.


# ARTist - The Android Runtime Instrumentation and Security Toolkit

ARTist is a flexible open source instrumentation framework for Android's apps and Java middleware. It is based on the Android Runtime’s (ART) compiler and modifies code during on-device compilation. In contrast to existing instrumentation frameworks, it preserves the application's original signature and operates on the instruction level. 

ARTist can be deployed in two different ways: First, as a regular application using our [ArtistGui](https://github.com/Project-ARTist/ArtistGui) project (this repository) that allows for non-invasive app instrumentation on rooted devices, or second, as a system compiler for custom ROMs where it can additionally instrument the system server (Package Manager Service, Activity Manager Service, ...) and the Android framework classes (```boot.oat```). It supports Android versions after (and including) Marshmallow 6.0. 

For detailed tutorials and more in-depth information on the ARTist ecosystem, have a look at our [official documentation](https://artist.cispa.saarland) and join our [Gitter chat](https://gitter.im/project-artist/Lobby).

## Upcoming Beta Release

We are about to enter the beta phase soon, which will bring a lot of changes to the whole ARTist ecosystem, including a dedicated ARTist SDK for simplified Module development, a semantic versioning-inspired release and versioning scheme, an improved and updated version of our online documentation, great new Modules, and a lot more improvements. However, in particular during the transition phase, some information like the one in the repositories' README.md files and the documentation at [https://artist.cispa.saarland](https://artist.cispa.saarland) might be slightly out of sync. We apologize for the inconvenience and happily take feedback at [Gitter](https://gitter.im/project-artist/Lobby). To keep up with the current progress, keep an eye on the beta milestones of the Project: ARTist repositories and check for new blog posts at [https://artist.cispa.saarland](https://artist.cispa.saarland) . 

## Contribution

We hope to create an active community of developers, researchers and users around Project ARTist and hence are happy about contributions and feedback of any kind. There are plenty of ways to get involved and help the project, such as testing and writing Modules, providing feedback on which functionality is key or missing, reporting bugs and other issues, or in general talk about your experiences. The team is actively monitoring [Gitter](https://gitter.im/project-artist/) and of course the repositories, and we are happy to get in touch and discuss. We do not have a full-fledged contribution guide, yet, but it will follow soon (see beta announcement above). 

## Academia

ARTist is based on a paper called **ARTist - The Android Runtime Instrumentation and Security Toolkit**, published at the 2nd IEEE European Symposium on Security and Privacy (EuroS&P'17). The full paper is available [here](https://artist.cispa.saarland/res/papers/ARTist.pdf). If you are citing ARTist in your research, please use the following bibliography entry:

```
@inproceedings{artist,
  title={ARTist: The Android runtime instrumentation and security toolkit},
  author={Backes, Michael and Bugiel, Sven and Schranz, Oliver and von Styp-Rekowsky, Philipp and Weisgerber, Sebastian},
  booktitle={2017 IEEE European Symposium on Security and Privacy (EuroS\&P)},
  pages={481--495},
  year={2017},
  organization={IEEE}
}
```

There is a follow-up paper where we utilized ARTist to cut out advertisement libraries from third-party applications, move the library to a dedicated app (own security principal) and reconnect both using a custom Binder IPC protocol, all while preserving visual fidelity by displaying the remote advertisements as floating views on top of the now ad-cleaned application. The full paper **The ART of App Compartmentalization: Compiler-based Library Privilege Separation on Stock Android**, as it was published at the 2017 ACM SIGSAC Conference on Computer and Communications Security (CCS'17), is available [here](https://artist.cispa.saarland/res/papers/CompARTist.pdf).
