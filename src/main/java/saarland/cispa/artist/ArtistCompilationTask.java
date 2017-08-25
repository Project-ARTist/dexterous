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
package saarland.cispa.artist;

import android.content.Context;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import saarland.cispa.artist.gui.artist.ArtistGuiProgress;
import saarland.cispa.artist.settings.ArtistRunConfig;
import saarland.cispa.artist.utils.ArtistInterruptedException;
import trikita.log.Log;

public class ArtistCompilationTask implements Callable, ArtistGuiProgress {

    private static final String TAG = "ArtistTask";

    private final ArtistRunConfig config;
    private final Context context;
    private List<ArtistGuiProgress> guiCallbacks = null;
    private CompilationResultReceiver resultCallback = null;


    private String lastStatusMessage = "";


    public ArtistCompilationTask(final ArtistRunConfig artistConfig,
                                 final Context applicationContext,
                                 final ArtistGuiProgress artistGuiProgress) {
        this.config = artistConfig;
        this.context = applicationContext;
        this.guiCallbacks = new ArrayList<>();
        this.guiCallbacks.add(artistGuiProgress);
    }

    public ArtistCompilationTask(final ArtistRunConfig artistConfig,
                                 final Context applicationContext) {
        this.config = artistConfig;
        this.context = applicationContext;
        this.guiCallbacks = new ArrayList<>();
    }

    public void addArtistGuiProgressCallback(final ArtistGuiProgress artistGuiProgress) {
        if (artistGuiProgress != null) {
            this.guiCallbacks.add(artistGuiProgress);
        }
    }

    public String getAppName() {
        return this.config.app_name;
    }

    @Override
    public Boolean call() throws Exception {
        Log.d(TAG, String.format("ArtistCompilationTask.call(%s)", config.app_name));
        final Artist artist = new ArtistImpl(config);

        artist.addGuiProgressListener(this);

        boolean success = false;
        try {
            artist.init(this.context);
            success = artist.Run(this.context);
        } catch (final Throwable t) {
            Log.e(TAG, String.format("Artist Failed: %s (%s)",
                    this.config.app_apk_name,
                    this.config.app_apk_file_path),
                    t
            );
        } finally {
            this.done();
        }
        Log.d(TAG, String.format("ArtistCompilationTask.call(%s) DONE success[%b]", config.app_name, success));
        if (resultCallback != null) {
            final Bundle data = new Bundle();
            data.putString(ArtistImpl.INTENT_EXTRA_APP_NAME, this.config.app_package_name);
            if (success) {
                resultCallback.send(ArtistImpl.COMPILATION_SUCCESS, data);
            } else {
                resultCallback.send(ArtistImpl.COMPILATION_ERROR, data);
            }
        }

        return success;
    }

    public void addResultCallback(final CompilationResultReceiver listener) {
        this.resultCallback = listener;
    }

    public static void checkThreadCancellation() throws ArtistInterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            Log.d(TAG, String.format("checkThreadCancellation() interrupted[%b]",
                    Thread.currentThread().isInterrupted()));
            throw new ArtistInterruptedException("Thread is interrupted.");
        }
    }

    @Override
    public void updateProgress(int progress, final String message) {
        this.lastStatusMessage = progress + ": " + message;
        for (ArtistGuiProgress callback : this.guiCallbacks) {
            try {
                callback.updateProgress(progress, message);
            } catch (final Throwable t) {
                Log.e(TAG, "callback failed: ", t);
            }
        }
    }

    @Override
    public void updateProgressVerbose(int progress, String message) {
        for (ArtistGuiProgress callback : this.guiCallbacks) {
            try {
                callback.updateProgressVerbose(progress, message);
            } catch (final Throwable t) {
                Log.e(TAG, "callback failed: ", t);
            }
        }
    }
    @Override
    public void kill(String message) {
        for (ArtistGuiProgress callback : this.guiCallbacks) {
            try {
                callback.kill(message);
            } catch (final Throwable t) {
                Log.e(TAG, "callback failed: ", t);
            }
        }
    }

    @Override
    public void doneSuccess(String message) {
        for (ArtistGuiProgress callback : this.guiCallbacks) {
            try {
                callback.doneSuccess(message);
            } catch (final Throwable t) {
                Log.e(TAG, "callback failed: ", t);
            }

        }
    }

    @Override
    public void doneFailed(String message) {
        for (ArtistGuiProgress callback : this.guiCallbacks) {
            try {
                callback.doneFailed(message);
            } catch (final Throwable t) {
                Log.e(TAG, "callback failed: ", t);
            }
        }
    }

    @Override
    public void done() {
        for (ArtistGuiProgress callback : this.guiCallbacks) {
            try {
                callback.done();
            } catch (final Throwable t) {
                Log.e(TAG, "callback failed: ", t);
            }
        }
    }

    public String getLastStatusMessage() {
        return lastStatusMessage;
    }

    public CompilationResultReceiver getResultCallback() {
        return resultCallback;
    }
}
