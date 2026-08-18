package org.schabi.newpipe.localserver;

import android.content.Context;
import android.content.res.Configuration;
import android.util.TypedValue;
import androidx.annotation.AttrRes;
import com.google.android.material.color.DynamicColors;

import java.util.HashMap;
import java.util.Map;

public class DynamicColorHelper {

    public static Map<String, String> getThemeColors(Context context, boolean dark) {
        Map<String, String> colors = new HashMap<>();
        try {
            Configuration config = new Configuration(context.getResources().getConfiguration());
            config.uiMode = (config.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | 
                    (dark ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
            Context themedContext = context.createConfigurationContext(config);
            themedContext.setTheme(R.style.Theme_LocalMediaServer);
            
            // Wrap context with dynamic colors if supported
            Context wrappedContext = DynamicColors.wrapContextIfAvailable(themedContext);

            colors.put("primary", getHexColor(wrappedContext, com.google.android.material.R.attr.colorPrimary, dark ? "#d0bcff" : "#6750A4"));
            colors.put("primaryContainer", getHexColor(wrappedContext, com.google.android.material.R.attr.colorPrimaryContainer, dark ? "#4f378b" : "#e9ddff"));
            colors.put("secondary", getHexColor(wrappedContext, com.google.android.material.R.attr.colorSecondary, dark ? "#ccc2dc" : "#625b71"));
            colors.put("secondaryContainer", getHexColor(wrappedContext, com.google.android.material.R.attr.colorSecondaryContainer, dark ? "#4a4458" : "#e8def8"));
            colors.put("tertiary", getHexColor(wrappedContext, com.google.android.material.R.attr.colorTertiary, dark ? "#efb8c8" : "#7d5260"));
            colors.put("tertiaryContainer", getHexColor(wrappedContext, com.google.android.material.R.attr.colorTertiaryContainer, dark ? "#633b48" : "#ffd8e4"));
            colors.put("surface", getHexColor(wrappedContext, com.google.android.material.R.attr.colorSurface, dark ? "#141218" : "#fbfafe"));
            colors.put("onSurface", getHexColor(wrappedContext, com.google.android.material.R.attr.colorOnSurface, dark ? "#e6e1e5" : "#1d1b20"));
            colors.put("surfaceContainer", getHexColor(wrappedContext, com.google.android.material.R.attr.colorSurfaceContainer, dark ? "#211f26" : "#f3f4f9"));
            colors.put("surfaceContainerLow", getHexColor(wrappedContext, com.google.android.material.R.attr.colorSurfaceContainerLow, dark ? "#1d1b20" : "#f7f2fa"));
            colors.put("surfaceContainerHigh", getHexColor(wrappedContext, com.google.android.material.R.attr.colorSurfaceContainerHigh, dark ? "#2b2930" : "#ece6f0"));
            colors.put("outline", getHexColor(wrappedContext, com.google.android.material.R.attr.colorOutline, dark ? "#938f99" : "#79747e"));

            String surface = colors.get("surface");
            if (surface != null && dark != isColorDark(surface)) {
                colors.put("primary", dark ? "#d0bcff" : "#6750A4");
                colors.put("primaryContainer", dark ? "#4f378b" : "#e9ddff");
                colors.put("secondary", dark ? "#ccc2dc" : "#625b71");
                colors.put("secondaryContainer", dark ? "#4a4458" : "#e8def8");
                colors.put("tertiary", dark ? "#efb8c8" : "#7d5260");
                colors.put("tertiaryContainer", dark ? "#633b48" : "#ffd8e4");
                colors.put("surface", dark ? "#141218" : "#fbfafe");
                colors.put("onSurface", dark ? "#e6e1e5" : "#1d1b20");
                colors.put("surfaceContainer", dark ? "#211f26" : "#f3f4f9");
                colors.put("surfaceContainerLow", dark ? "#1d1b20" : "#f7f2fa");
                colors.put("surfaceContainerHigh", dark ? "#2b2930" : "#ece6f0");
                colors.put("outline", dark ? "#938f99" : "#79747e");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return colors;
    }

    private static boolean isColorDark(String hexColor) {
        try {
            if (hexColor.startsWith("#")) {
                hexColor = hexColor.substring(1);
            }
            int rgb = Integer.parseInt(hexColor, 16);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int b = rgb & 0xFF;
            double brightness = (r * 299 + g * 587 + b * 114) / 1000.0;
            return brightness < 128;
        } catch (Exception e) {
            return false;
        }
    }

    private static String getHexColor(Context context, @AttrRes int attr, String fallback) {
        try {
            TypedValue typedValue = new TypedValue();
            if (context.getTheme().resolveAttribute(attr, typedValue, true)) {
                int color = typedValue.data;
                return String.format("#%06X", (0xFFFFFF & color));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fallback;
    }
}
