package org.telegram.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;

public class LauncherIconController {
    public static void tryFixLauncherIconIfNeeded() {
        for (LauncherIcon icon : LauncherIcon.values()) {
            if (isEnabled(icon) && icon.isNyx()) {
                return;
            }
        }

        setIcon(LauncherIcon.DEFAULT);
    }

    public static boolean isEnabled(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        int i = ctx.getPackageManager().getComponentEnabledSetting(icon.getComponentName(ctx));
        return i == PackageManager.COMPONENT_ENABLED_STATE_ENABLED || i == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && icon == LauncherIcon.DEFAULT;
    }

    public static void setIcon(LauncherIcon icon) {
        Context ctx = ApplicationLoader.applicationContext;
        PackageManager pm = ctx.getPackageManager();
        if (!icon.isNyx()) icon = LauncherIcon.DEFAULT;
        pm.setComponentEnabledSetting(icon.getComponentName(ctx),PackageManager.COMPONENT_ENABLED_STATE_ENABLED,PackageManager.DONT_KILL_APP);
        for (LauncherIcon i : LauncherIcon.values()) {
            if(i!=icon)pm.setComponentEnabledSetting(i.getComponentName(ctx),PackageManager.COMPONENT_ENABLED_STATE_DISABLED,PackageManager.DONT_KILL_APP);
        }
    }

    public enum LauncherIcon {
        DEFAULT("DefaultIcon", R.drawable.nyx_preview_original, R.drawable.nyx_empty, R.string.NyxIconOriginal),
        NYX_VOID("NyxVoidIcon", R.drawable.nyx_preview_void, R.drawable.nyx_empty, R.string.NyxIconVoid),
        NYX_AURORA("NyxAuroraIcon", R.drawable.nyx_preview_aurora, R.drawable.nyx_empty, R.string.NyxIconAurora),
        NYX_NEBULA("NyxNebulaIcon", R.drawable.nyx_preview_nebula, R.drawable.nyx_empty, R.string.NyxIconNebula),
        NYX_ECLIPSE("NyxEclipseIcon", R.drawable.nyx_preview_eclipse, R.drawable.nyx_empty, R.string.NyxIconEclipse),
        NYX_LUNAR("NyxLunarIcon", R.drawable.nyx_preview_lunar, R.drawable.nyx_empty, R.string.NyxIconLunar),
        NYX_MIDNIGHT("NyxMidnightIcon", R.drawable.nyx_preview_midnight, R.drawable.nyx_empty, R.string.NyxIconMidnight),
        NYX_EMBER("NyxEmberIcon", R.drawable.nyx_preview_ember, R.drawable.nyx_empty, R.string.NyxIconEmber),
        NYX_FROST("NyxFrostIcon", R.drawable.nyx_preview_frost, R.drawable.nyx_empty, R.string.NyxIconFrost),
        NYX_ROSE("NyxRoseIcon", R.drawable.nyx_preview_rose, R.drawable.nyx_empty, R.string.NyxIconRose),
        NYX_JADE("NyxJadeIcon", R.drawable.nyx_preview_jade, R.drawable.nyx_empty, R.string.NyxIconJade),
        NYX_SOLAR("NyxSolarIcon", R.drawable.nyx_preview_solar, R.drawable.nyx_empty, R.string.NyxIconSolar),
        NYX_ORBIT("NyxOrbitIcon", R.drawable.nyx_preview_orbit, R.drawable.nyx_empty, R.string.NyxIconOrbit),
        NYX_PRISM("NyxPrismIcon", R.drawable.nyx_preview_prism, R.drawable.nyx_empty, R.string.NyxIconPrism),
        NYX_OBSIDIAN("NyxObsidianIcon", R.drawable.nyx_preview_obsidian, R.drawable.nyx_empty, R.string.NyxIconObsidian),
        NYX_PAPER("NyxPaperIcon", R.drawable.nyx_preview_paper, R.drawable.nyx_empty, R.string.NyxIconPaper),
        VINTAGE("VintageIcon", R.drawable.icon_6_background_sa, R.mipmap.icon_6_foreground_sa, R.string.AppIconVintage),
        AQUA("AquaIcon", R.drawable.icon_4_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconAqua),
        PREMIUM("PremiumIcon", R.drawable.icon_3_background_sa, R.mipmap.icon_3_foreground_sa, R.string.AppIconPremium, true),
        TURBO("TurboIcon", R.drawable.icon_5_background_sa, R.mipmap.icon_5_foreground_sa, R.string.AppIconTurbo, true),
        NOX("NoxIcon", R.mipmap.icon_2_background_sa, R.mipmap.icon_foreground_sa, R.string.AppIconNox, true);

        public final String key;
        public final int background;
        public final int foreground;
        public final int title;
        public final boolean premium;

        private ComponentName componentName;

        public boolean isNyx() { return this==DEFAULT || key.startsWith("Nyx"); }

        public ComponentName getComponentName(Context ctx) {
            if (componentName == null) {
                componentName = new ComponentName(ctx.getPackageName(), "org.telegram.messenger." + key);
            }
            return componentName;
        }

        LauncherIcon(String key, int background, int foreground, int title) {
            this(key, background, foreground, title, false);
        }

        LauncherIcon(String key, int background, int foreground, int title, boolean premium) {
            this.key = key;
            this.background = background;
            this.foreground = foreground;
            this.title = title;
            this.premium = premium;
        }
    }
}
