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

