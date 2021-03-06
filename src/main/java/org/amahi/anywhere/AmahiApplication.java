/*
 * Copyright (c) 2014 Amahi
 *
 * This file is part of Amahi.
 *
 * Amahi is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Amahi is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Amahi. If not, see <http ://www.gnu.org/licenses/>.
 */

package org.amahi.anywhere;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.StrictMode;
import android.support.annotation.RequiresApi;

import com.crashlytics.android.Crashlytics;

import org.amahi.anywhere.job.NetConnectivityJob;
import org.amahi.anywhere.job.PhotosContentJob;

import dagger.ObjectGraph;
import io.fabric.sdk.android.Fabric;
import timber.log.Timber;

/**
 * Application declaration. Basically sets things up at the startup time,
 * such as dependency injection, logging, crash reporting and possible ANR detecting.
 */
public class AmahiApplication extends Application {
    private ObjectGraph injector;

    private static final String UPLOAD_CHANNEL_ID = "file_upload";
    private static final String DOWNLOAD_CHANNEL_ID = "file_download";

    public static AmahiApplication from(Context context) {
        return (AmahiApplication) context.getApplicationContext();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        setUpLogging();
        setUpReporting();
        setUpDetecting();

        setUpInjections();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            setUpJobs();
        }
    }

    private void setUpLogging() {
        if (isDebugging()) {
            Timber.plant(new Timber.DebugTree());
        }
    }

    private boolean isDebugging() {
        return BuildConfig.DEBUG;
    }

    private void setUpReporting() {
        if (!isDebugging()) {
            Fabric.with(this, new Crashlytics());
        }
    }

    private void setUpDetecting() {
        if (isDebugging()) {
            StrictMode.enableDefaults();
        }
    }

    private void setUpInjections() {
        injector = ObjectGraph.create(new AmahiModule(this));
    }

    public void inject(Object injectionsConsumer) {
        injector.inject(injectionsConsumer);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void setUpJobs() {
        if (!PhotosContentJob.isScheduled(this)) {
            PhotosContentJob.scheduleJob(this);
        }
        if (!NetConnectivityJob.isScheduled(this)) {
            NetConnectivityJob.scheduleJob(this);
        }
    }

    public static class JobIds {
        public static final int PHOTOS_CONTENT_JOB = 125;
        public static final int NET_CONNECTIVITY_JOB = 126;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {

        // Creating NotificationChannel only for API 26+
        int importance = NotificationManager.IMPORTANCE_DEFAULT;

        NotificationChannel uploadChannel = new NotificationChannel(UPLOAD_CHANNEL_ID, getString(R.string.upload_channel), importance);
        uploadChannel.setDescription(getString(R.string.upload_channel_desc));

        NotificationChannel downloadChannel = new NotificationChannel(DOWNLOAD_CHANNEL_ID, getString(R.string.download_channel), importance);
        downloadChannel.setDescription(getString(R.string.download_channel_desc));

        // Once the channel is registered, it's importance and behaviour can't be changed
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(uploadChannel);
        notificationManager.createNotificationChannel(downloadChannel);

    }

}
