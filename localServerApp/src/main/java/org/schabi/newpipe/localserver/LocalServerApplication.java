package org.schabi.newpipe.localserver;

import android.app.Application;
import com.google.android.material.color.DynamicColors;

public class LocalServerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Enable wallpaper-based dynamic color theming across all activities (Android 12+)
        DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
