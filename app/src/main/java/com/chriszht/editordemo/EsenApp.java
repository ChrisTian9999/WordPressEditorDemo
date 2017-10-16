package com.chriszht.editordemo;

import android.app.Application;
import android.content.Context;

/**
 * @author chris
 * @desc :
 * @date : 2017/10/16
 * @email : zhtian95@gmail.com
 */
public class EsenApp extends Application {

    public static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = this;

    }

    public static Context getContext() {
        return context;
    }
}
