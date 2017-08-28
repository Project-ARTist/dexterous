# dexterous

Dexterous is a commandline interface to ARTist'S partial dex merging and apk-packaging and -signing.

But that's not it's only functionality. You can also use it as an in depth dex anylsis framework.

Take a look at the class: [`saarland.cispa.dexterous.cli.Dexterously`](src/main/java/saarland/cispa/dexterous/cli/Dexterously.java)  as a starting point to implement your own analysis.

You can build  a fat jar with all depedencies with the following command:

``` bash
gradle DexterousJar
```

It implicitely uses the bundle debug keystore: `res/artist-debug.keystore`.


## dexterous usage

```
user@host ~/dexterous $ java -jar build/libs/dexterous.jar --help

usage: Dexterously <options>
 -b,--build-apk        Build merged APK
 -c,--codelib <file>   Path to codelib.apk (name doesn't matter).
 -h,--help             Prints this message.
 -s,--sign-apk         Build and sign merged APK
```

### Merge codelib partially and resign apk

e.g.: `java -jar dexterous.jar my_application.apk --codelib codelib.apk --build-apk --sign-apk`

### Analyse apk only

> Execute dexterous without the build / sign flags

`java -jar dexterous.jar my_application.apk`

## Third-party code usage:

> dexterous uses a lot of third-party code:

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

