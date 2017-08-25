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
 * @author "Oliver Schranz <oliver.schranz@cispa.saarland>"
 * @author "Sebastian Weisgerber <weisgerber@cispa.saarland>"
 *
 */
package saarland.cispa.artist.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import trikita.log.Log;

public enum ProcessExecutor {
    INSTANCE;

    private final static String DEFAULT_PROCESS = "__DEFAULT__";

    private static String DEX2OAT_PROCESS_NAME
            = "/data/user/0/saarland.cispa.artist.artistgui/files/artist/dex2oat";

    public static void setDex2oatProcessName(final String dex2oatProcessName) {
        DEX2OAT_PROCESS_NAME = dex2oatProcessName;
    }

    private final static Map<String, Process> PROCESSES = new ConcurrentHashMap<>();

    private ProcessExecutor() { /* SINGLETON */ }

    private static final String TAG = "ArtistProcExecutor";

    public static String processName(final String appName, final String taskName) {
        return appName + ":" + taskName;
    }

    public static boolean execute(final String command, final boolean rootExecution) {
        return execute(command, rootExecution, null, DEFAULT_PROCESS);
    }

    public static boolean execute(final String command, final boolean rootExecution, final String processName) {
        return execute(command, rootExecution, null, processName);
    }

    public static boolean execute(final String command,
                                  boolean rootExecution,
                                  final StringBuffer outputBuffer) {
        return execute(command, rootExecution, outputBuffer, DEFAULT_PROCESS);
    }

    public static boolean execute(final String command,
                                  boolean rootExecution,
                                  final StringBuffer outputBuffer,
                                  final String processName) {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            if (rootExecution) {
                Log.d(TAG, String.format("execute() SU <%s>", command));
                pb.command("su", "-c", command);
            } else {
                Log.d(TAG, String.format("execute() <%s>", command));
                pb.command(command);
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // ["Process[pid=18546, hasExited=false]"]
            Log.d(TAG, String.format("> execute() ProcessInfos: [%s]", process.toString()));

            PROCESSES.put(processName, process);
        
            Log.d(TAG, String.format("> execute() Waiting: %s", processName));
            process.waitFor();
            Log.d(TAG, String.format("> execute() Waiting: %s DONE", processName));

            final BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream()));

            int read;
            char[] buffer = new char[AndroidUtils.FILE_BUFFER_DEFAULT_SIZE];

            final StringBuffer output = new StringBuffer();

            while ((read = reader.read(buffer)) > 0) {
                output.append(buffer, 0, read);
            }
            reader.close();

            Log.d(TAG, "> $ " + output.toString());

            if (output != null && outputBuffer != null) {
                outputBuffer.append(output);
            }
            final boolean SUCCESS = process.exitValue() == 0;
            PROCESSES.remove(processName);
            Log.d(TAG, String.format("> execute() <%s> SUCCESS", command));
            return SUCCESS;
        } catch (final IOException|InterruptedException e) {
            Log.d(TAG, String.format("> execute() <%s> FAILED", command), e);
            return false;
        }
    }

    public static void killDefaultExecutorProcess() {
        killExecutorProcess(DEFAULT_PROCESS);
    }

    public static void killAllExecutorProcesses() {
        Log.d(TAG, String.format("killAllExecutorProcesses() [count: %d]", PROCESSES.size()));
        for (final String processName : PROCESSES.keySet()) {
            Log.i(TAG, String.format("Killing Process %s", processName));
            final Process process = PROCESSES.remove(processName);
            AndroidUtils.suKill(process);
            killSystemProcessDex2oat();
            process.destroy();
        }
    }

    public static void killExecutorProcess(final String processName) {
        Log.d(TAG, String.format("killDefaultExecutorProcess() %s", processName));
        final Process process = PROCESSES.remove(processName);
        if (process != null) {
            Log.d(TAG, String.format("killDefaultExecutorProcess() %s FOUND -> EXTERMINATE!", processName));
            AndroidUtils.suKill(process);
            killSystemProcessDex2oat();
            process.destroy();
        }
        Log.d(TAG, String.format("killDefaultExecutorProcess() %s DONE", processName));
    }

    public static void killSystemProcessDex2oat() {
        killSystemProcess(DEX2OAT_PROCESS_NAME);
    }
    public static void killSystemProcess(final String processName) {
        Log.d(TAG, String.format("killSystemProcess()"));
        execute("pgrep -f \"" + processName + "\" | xargs kill -9", true, "pgrep_dex2oat");
        Log.d(TAG, String.format("killSystemProcess() DONE"));
    }

}
