package saarland.cispa.dexterous.cli;

import org.apache.commons.cli.*;
import saarland.cispa.dexterous.Config;
import trikita.log.Log;

import java.io.File;

public class Main {

    private static final String TOOLNAME = "Dexterously";

    public static void main(final String[] args) {
        setupLogging();

        final Config runConfig = parseCommandLineArguments(args);

        Dexterously dexterously = new Dexterously(runConfig);

        // Analyzer Test
        dexterously.analyze();

        dexterously.summary();

        if (runConfig.build_apk) {
            dexterously.mergeCodeLib();
            dexterously.buildApk();
            if (runConfig.sign_apk) {
                dexterously.signApk();
            }
        }
    }

    private static void setupLogging() {
//        /// See: @url http://www.tinylog.org/configuration
//        Configurator.currentConfig()
//                /*.formatPattern("{level}:\t{message}")*/
//                .formatPattern("{message}")
//                .level(Level.INFO)
//                .activate();
    }

    private static Options setupOptions() {
        final Options options = new Options();

        options.addOption(
                Option.builder("h")
                        .argName("file")
                        .optionalArg(true)
                        .longOpt("help")
                        .desc("Prints this message.")
                        .build()
        );

        options.addOption(
                Option.builder("c")
                        .argName("file")
                        .optionalArg(true)
                        .longOpt("codelib")
                        .desc("Path to codelib.apk (name doesn't matter).")
                        .hasArg()
                        .build()
        );

        options.addOption(
                Option.builder("b")
                        .argName("flag")
                        .longOpt("build-apk")
                        .desc("Build merged APK")
                        .hasArg(false)
                        .build()
        );
        options.addOption(
                Option.builder("s")
                        .argName("flag")
                        .longOpt("sign-apk")
                        .desc("Build and sign merged APK")
                        .hasArg(false)
                        .build()
        );

        return options;
    }

    private static Config parseCommandLineArguments(final String[] args) {
        final CommandLineParser parser = new DefaultParser();
        final CommandLine arguments;
        Config runConfig = new Config();
        try {
            arguments = parser.parse(setupOptions(), args);
            if (arguments.hasOption("help")) {
                usageExit();
            }
            // parse mode of operation
            if (arguments.hasOption("build-apk")) {
                runConfig.build_apk = true;
            }
            if (arguments.hasOption("sign-apk")) {
                runConfig.build_apk = true;
                runConfig.sign_apk = true;
            }
            runConfig.codelib = new File(arguments.getOptionValue("codelib"));
            if (!isValidDexfile(runConfig.codelib)) {
                throw new ParseException(String.format("CodeLib is invalid: %s",
                        runConfig.codelib.getAbsolutePath()));
            }
            for (final String argument : arguments.getArgList()) {
                final File dexFile = new File(argument);
                if (isValidDexfile(dexFile)) {
                    runConfig.dexFiles.add(dexFile);
                } else {
                    throw new ParseException(String.format("DexFile is invalid: %s", dexFile.getAbsolutePath()));
                }
            }
        } catch (final ParseException e) {
            Log.e(e.getMessage());
            usageExit();
        }
        return runConfig;
    }

    private static boolean isValidDexfile(File dexFile) {
        return dexFile.exists() && ! dexFile.isDirectory();
    }

    private static void usageExit() {
        final String USAGE = TOOLNAME + " <options> ";
        // automatically generate the help statement
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(USAGE, setupOptions());
        System.exit(1);
    }

}
