package ru.obvilion.launcher.utils;

import android.os.Build;

import java.io.File;

import ru.obvilion.launcher.Vars;

public class SystemUtils {
    public static String getArchitecture() {
        File nativeLibDir = new File(Vars.THIS_APP.getApplicationInfo().nativeLibraryDir);
        String arch = nativeLibDir.getName();
        switch (arch) {
            case "arm": arch = "arm/aarch32"; break;
            case "arm64": arch = "arm64/aarch64"; break;
            case "x86": arch = "x86/i*86"; break;
            case "x86_64": arch = "x86_64/amd64"; break;
        }

        // Special case for Asus x86 devixes
        if (Build.SUPPORTED_ABIS[0].equals("x86")) {
            Vars.THIS_APP.getApplicationInfo().nativeLibraryDir = nativeLibDir.getParent() + "/x86";
            arch = "x86/i*86";
        }

        return arch;
    }
}
