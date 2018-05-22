/**
 * The ARTist Project (https://artist.cispa.saarland)
 *
 * Copyright (C) 2017 CISPA (https://cispa.saarland), Saarland University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author "Sebastian Weisgerber <weisgerber@cispa.saarland>"
 *
 */
package saarland.cispa.dexterous.cli;

import org.apache.commons.cli.*;

import comm.android.dx.merge.DexMerger;
import saarland.cispa.dexterous.Config;
import trikita.log.Log;

import java.io.File;

public class Main {

    private static final String TOOLNAME = "Dexterously";

    public static void main(final String[] args) {
        final Config runConfig = parseCommandLineArguments(args);

        Dexterously dexterously = new Dexterously(runConfig);

        // Analyzer Test
        if (runConfig.analyze_apk) {
            dexterously.analyze();
            dexterously.summary();
        } else {
            dexterously.info();
        }
        try {
            if (runConfig.build_apk) {
                    dexterously.mergeCodeLib();
                dexterously.buildApk();
                if (runConfig.sign_apk) {
                    dexterously.signApk();
                }
            }
            if (runConfig.merge_dex) {
                dexterously.mergeDexfiles();
            }
        } catch (DexMerger.MergeException e) {
            e.printStackTrace();
        }
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
                Option.builder("m")
                        .argName("flag")
                        .longOpt("merge")
                        .desc("Build merged APK")
                        .hasArg(false)
                        .build()
        );

        options.addOption(
                Option.builder("b")
                        .argName("flag")
                        .longOpt("build-apk")
                        .desc("Build partially merged APK")
                        .hasArg(false)
                        .build()
        );

        options.addOption(
                Option.builder("s")
                        .argName("flag")
                        .longOpt("sign-apk")
                        .desc("Build and sign partialy merged APK")
                        .hasArg(false)
                        .build()
        );

        options.addOption(
                Option.builder("a")
                        .argName("flag")
                        .longOpt("analyze")
                        .desc("Analyze APK")
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
            if (arguments.hasOption("help") || arguments.getOptions().length == 0) {
                usageExit();
            }
            // parse mode of operation
            if (arguments.hasOption("merge")) {
                runConfig.merge_dex = true;
            }
            // parse mode of operation
            if (arguments.hasOption("build-apk")) {
                runConfig.build_apk = true;
            }
            if (arguments.hasOption("sign-apk")) {
                runConfig.build_apk = true;
                runConfig.sign_apk = true;
            }
            if (arguments.hasOption("analyze")) {
                runConfig.analyze_apk = true;
            }
            if (arguments.hasOption("codelib")) {
                runConfig.codelib = new File(arguments.getOptionValue("codelib"));
                if (!isValidDexfile(runConfig.codelib)) {
                    throw new ParseException(String.format("CodeLib is invalid: %s",
                            runConfig.codelib.getAbsolutePath()));
                }
            }
            if (runConfig.merge_dex && (runConfig.build_apk || runConfig.sign_apk)) {
                throw new ParseException(String.format("Either user --merge OR --build-apk/--sign-apk"));
            }
            for (final String argument : arguments.getArgList()) {
                final File dexFile = new File(argument);
                if (isValidDexfile(dexFile)) {
                    runConfig.dexFiles.add(dexFile);
                } else {
                    throw new ParseException(String.format("DexFile is invalid: %s", dexFile.getAbsolutePath()));
                }
            }
        } catch (final ParseException|NullPointerException e) {
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
