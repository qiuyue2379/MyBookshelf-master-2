//Copyright (c) 2017. 章钦豪. All rights reserved.
package com.kunfei.bookshelf;

import android.Manifest;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;

import com.kunfei.bookshelf.help.AppFrontBackHelper;
import com.kunfei.bookshelf.help.Constant;
import com.kunfei.bookshelf.help.CrashHandler;
import com.kunfei.bookshelf.help.FileHelp;
import com.kunfei.bookshelf.model.UpLastChapterModel;
import com.kunfei.bookshelf.utils.Theme.ThemeStore;

import java.io.File;
import java.util.concurrent.TimeUnit;

import androidx.annotation.RequiresApi;
import androidx.multidex.MultiDex;

import com.tencent.smtt.sdk.QbSdk;

public class MApplication extends Application {
    public final static boolean DEBUG = BuildConfig.DEBUG;
    public final static String channelIdDownload = "channel_download";
    public final static String channelIdReadAloud = "channel_read_aloud";
    public final static String[] PerList = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    public final static int RESULT__PERMS = 263;
    public static String downloadPath;
    private static MApplication instance;
    private static String versionName;
    private static int versionCode;
    private static String packageName;

    private SharedPreferences configPreferences;
    private boolean donateHb;

    public static MApplication getInstance() {
        return instance;
    }

    public static int getVersionCode() {
        return versionCode;
    }

    public static String getVersionName() {
        return versionName;
    }

    public static String packageName() {
        return packageName;
    }

    public static Resources getAppResources() {
        return getInstance().getResources();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        CrashHandler.getInstance().init(this);
        // default theme
        try {
            versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            packageName = getPackageManager().getPackageInfo(getPackageName(), 0).packageName;
        } catch (PackageManager.NameNotFoundException e) {
            versionCode = 0;
            versionName = "0.0.0";
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannelIdDownload();
            createChannelIdReadAloud();
        }
        configPreferences = getSharedPreferences("CONFIG", 0);
        downloadPath = configPreferences.getString(getString(R.string.pk_download_path), "");
        if (TextUtils.isEmpty(downloadPath)) {
            setDownloadPath(FileHelp.getCachePath());
        }
        if (!ThemeStore.isConfigured(this, versionCode)) {
            upThemeStore();
        }
        AppFrontBackHelper.getInstance().register(this, new AppFrontBackHelper.OnAppStatusListener() {
            @Override
            public void onFront() {
                donateHb = System.currentTimeMillis() - configPreferences.getLong("DonateHb", 0) <= TimeUnit.DAYS.toMillis(3);
            }

            @Override
            public void onBack() {
                if (UpLastChapterModel.model != null) {
                    UpLastChapterModel.model.onDestroy();
                }
            }
        });

        QbSdk.PreInitCallback cb = new QbSdk.PreInitCallback() {
            @Override
            public void onViewInitFinished(boolean arg0) {
                // TODO Auto-generated method stub
                //x5內核初始化完成的回调，为true表示x5内核加载成功，否则表示x5内核加载失败，会自动切换到系统内核。
               // Log.d("app", " onViewInitFinished is " + arg0);
            }
            @Override
            public void onCoreInitFinished() {
                // TODO Auto-generated method stub
            }
        };
        //x5内核初始化接口
        QbSdk.initX5Environment(getApplicationContext(),  cb);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    public void upThemeStore() {
        if (configPreferences.getBoolean("nightTheme", false)) {
            ThemeStore.editTheme(this)
                    .primaryColor(configPreferences.getInt("colorPrimaryNight", getResources().getColor(R.color.md_grey_800)))
                    .accentColor(configPreferences.getInt("colorAccentNight", getResources().getColor(R.color.md_pink_800)))
                    .backgroundColor(configPreferences.getInt("colorBackgroundNight", getResources().getColor(R.color.md_grey_800)))
                    .apply();
        } else {
            ThemeStore.editTheme(this)
                    .primaryColor(configPreferences.getInt("colorPrimary", getResources().getColor(R.color.md_grey_100)))
                    .accentColor(configPreferences.getInt("colorAccent", getResources().getColor(R.color.md_pink_600)))
                    .backgroundColor(configPreferences.getInt("colorBackground", getResources().getColor(R.color.md_grey_100)))
                    .apply();
        }
    }

    public void setDownloadPath(String downloadPath) {
        MApplication.downloadPath = downloadPath;
        Constant.BOOK_CACHE_PATH = MApplication.downloadPath + File.separator + "book_cache" + File.separator;
        SharedPreferences.Editor editor = configPreferences.edit();
        editor.putString(getString(R.string.pk_download_path), downloadPath);
        editor.apply();
    }

    public SharedPreferences getConfigPreferences() {
        return configPreferences;
    }

    public boolean getDonateHb() {
        return donateHb;
    }

    public void upDonateHb() {
        SharedPreferences.Editor editor = configPreferences.edit();
        editor.putLong("DonateHb", System.currentTimeMillis());
        editor.apply();
        donateHb = true;
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannelIdDownload() {
        //用唯一的ID创建渠道对象
        NotificationChannel firstChannel = new NotificationChannel(channelIdDownload,
                getString(R.string.download_offline),
                NotificationManager.IMPORTANCE_LOW);
        //初始化channel
        firstChannel.enableLights(false);
        firstChannel.enableVibration(false);
        firstChannel.setSound(null, null);
        //向notification manager 提交channel
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(firstChannel);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannelIdReadAloud() {
        //用唯一的ID创建渠道对象
        NotificationChannel firstChannel = new NotificationChannel(channelIdReadAloud,
                getString(R.string.read_aloud),
                NotificationManager.IMPORTANCE_LOW);
        //初始化channel
        firstChannel.enableLights(false);
        firstChannel.enableVibration(false);
        firstChannel.setSound(null, null);
        //向notification manager 提交channel
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(firstChannel);
        }
    }

}