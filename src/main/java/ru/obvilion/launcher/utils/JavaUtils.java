package ru.obvilion.launcher.utils;

import android.util.Log;
import android.view.View;

import net.kdt.pojavlaunch.LoginActivity;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.utils.DownloadUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import ru.obvilion.launcher.Vars;

public class JavaUtils {
    public static boolean isJavaRuntimeInstalled() {
        boolean prefValue = LoginActivity.firstLaunchPrefs.getBoolean("isJavaRuntimeInstalled", false);

        try {
            return prefValue && (Tools.read(
                    new FileInputStream(Tools.DIR_HOME_JRE + "/version")
            ).equals(
                    DownloadUtils.downloadString("https://obvilion.ru/api/files/java/version")
            ));
        } catch(IOException e) {
            Log.e("JVMCtl","failed to read file", e);
            return false;
        }
    }

    public static boolean installRuntimeAutomatically() {
        Vars.LOGIN_ACTIVITY.runOnUiThread(() -> {
            Vars.LOGIN_ACTIVITY.bar.setVisibility(View.VISIBLE);
        });

        AtomicReference<String> tecFile = new AtomicReference<>();
        tecFile.set("universal.tar.xz");

        Tools.DownloaderFeedback fb = new Tools.DownloaderFeedback() {
            @Override
            public void updateProgress(int curr, int max) {
                Vars.LOGIN_ACTIVITY.runOnUiThread(() -> {
                    Vars.LOGIN_ACTIVITY.bar.setMax(max);
                    Vars.LOGIN_ACTIVITY.bar.setProgress(curr);
                    Vars.LOGIN_ACTIVITY.startupTextView.setText(
                            Vars.LOGIN_ACTIVITY.getString(R.string.mcl_launch_downloading_progress, tecFile.get(), curr / 1024f / 1024, max / 1024f / 1024)
                    );
                });
            }
        };

        File rtUniversal = new File(Tools.DIR_HOME_JRE + "/universal.tar.xz");
        File rtPlatformDependent = new File(Tools.DIR_HOME_JRE + "/cust-bin.tar.xz");
        File versionFile = new File(Tools.DIR_HOME_JRE + "/version");

        if (!new File(Tools.DIR_HOME_JRE).exists()) {
            new File(Tools.DIR_HOME_JRE).mkdirs();
        } else {
            //SANITY: remove the existing files
            for (File f : new File(Tools.DIR_HOME_JRE).listFiles()) {
                if (f.isDirectory()) {
                    try {
                        FileUtils.deleteDirectory(f);
                    } catch (IOException e1) {
                        Log.e("JREAuto","da fuq is wrong wit ur device? n2",e1);
                    }
                } else {
                    f.delete();
                }
            }
        }

        try {
            Tools.downloadFileMonitored(
                    "https://obvilion.ru/api/files/java/universal.tar.xz",
                    rtUniversal.getPath(),
                    fb
            );
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        try {
            tecFile.set("version");
            Tools.downloadFileMonitored(
                    "https://obvilion.ru/api/files/java/version",
                    versionFile.getPath(),
                    fb
            );
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        try {
            Vars.LOGIN_ACTIVITY.uncompressTarXZ(rtUniversal, new File(Tools.DIR_HOME_JRE));
        } catch (IOException e){
            Log.e("JREAuto","Failed to unpack universal. Custom embedded-less build?", e);
            return false;
        }

        try {
            tecFile.set("bin-" + Tools.CURRENT_ARCHITECTURE.split("/")[0] + ".tar.xz");
            Tools.downloadFileMonitored(
                    "https://obvilion.ru/api/files/java/" + tecFile.get(),
                    rtPlatformDependent.getPath(),
                    fb
            );
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        try {
            Vars.LOGIN_ACTIVITY.runOnUiThread(() -> {
                Vars.LOGIN_ACTIVITY.bar.setVisibility(View.INVISIBLE);
            });
            Vars.LOGIN_ACTIVITY.uncompressTarXZ(rtPlatformDependent, new File(Tools.DIR_HOME_JRE));
        } catch (IOException e) {
            for (File f : new File(Tools.DIR_HOME_JRE).listFiles()) {
                if (f.isDirectory()){
                    try {
                        FileUtils.deleteDirectory(f);
                    } catch(IOException e1) {
                        Log.e("JREAuto","da fuq is wrong wit ur device?",e1);
                    }
                } else {
                    f.delete();
                }
            }

            return false;
        }

        return true;
    }
}
