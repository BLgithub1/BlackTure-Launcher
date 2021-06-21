package ru.obvilion.launcher;

import android.content.*;
import android.content.pm.*;
import android.content.res.*;
import android.os.*;
import androidx.core.app.*;

import android.util.*;
import java.io.*;
import java.text.*;
import java.util.*;

import net.kdt.pojavlaunch.BaseMainActivity;
import net.kdt.pojavlaunch.FatalErrorActivity;
import net.kdt.pojavlaunch.FontChanger;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.fragments.ServerFragment;
import net.kdt.pojavlaunch.prefs.LauncherPreferenceFragment;
import net.kdt.pojavlaunch.utils.*;

import org.json.JSONArray;
import org.json.JSONObject;

import ru.obvilion.launcher.io.DualStream;
import ru.obvilion.launcher.utils.JsonUtils;
import ru.obvilion.launcher.utils.SystemUtils;

public class Application extends android.app.Application
{
	public static String CRASH_REPORT_TAG = "ObvilionNetworkCrashReport";
	
	@Override
	public void onCreate() {
		Vars.THIS_APP = this;

		setExceptionHandler();
		
		try {
			super.onCreate();

			Tools.APP_NAME = getResources().getString(R.string.app_short_name);
			Tools.DIR_DATA = getDir("files", MODE_PRIVATE).getParent();
            Tools.DIR_HOME_JRE = Tools.DIR_DATA + "/jre_runtime";
            Tools.DIR_ACCOUNT_OLD = Tools.DIR_DATA + "/Users";
            Tools.DIR_ACCOUNT_NEW = Tools.DIR_DATA + "/accounts";
            Tools.CURRENT_ARCHITECTURE = SystemUtils.getArchitecture();

            Vars.APP_DATA = getDir("files", MODE_PRIVATE).getParentFile();
            Vars.ARCHITECTURE = Tools.CURRENT_ARCHITECTURE.split("/")[0];

			try {
				PrintStream out = new PrintStream(new FileOutputStream(Vars.LOG_FILE));
				System.setOut(new DualStream(System.out, out));
				System.setErr(new DualStream(System.err, out));
			} catch (Exception e) {
				Log.e("Logger", "Error on set log file");
			}

			new Thread(() -> {
				try {
					JSONObject obj = JsonUtils.readJsonFromUrl("https://obvilionnetwork.ru/api/servers");
					Vars.SERVERS = obj.getJSONArray("servers");

					JSONArray sorted = new JSONArray();
					for (int i = 0; i < Vars.SERVERS.length(); i++) {
						JSONObject server = Vars.SERVERS.getJSONObject(i);

						if (server.getString("type").equals("minecraft")) {
							sorted.put(Vars.SERVERS.get(i));
						}
					}

					Vars.SERVERS = sorted;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start();

			FontChanger.initFonts(this);
		} catch (Throwable th) {
			Intent errorIntent = new Intent(this, FatalErrorActivity.class);
			errorIntent.putExtra("throwable", th);
			startActivity(errorIntent);
		}
	}
    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleUtils.setLocale(base));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LocaleUtils.setLocale(this);
    }

    public void setExceptionHandler() {
		Thread.setDefaultUncaughtExceptionHandler((thread, th) -> {
			boolean storagePermAllowed = Build.VERSION.SDK_INT < 23 || ActivityCompat.checkSelfPermission(Application.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
			File crashFile = new File(storagePermAllowed ? Tools.DIR_GAME_HOME : Tools.DIR_DATA, "latestcrash.txt");
			try {
				// Write to file, since some devices may not able to show error
				crashFile.getParentFile().mkdirs();
				crashFile.createNewFile();
				PrintStream crashStream = new PrintStream(crashFile);
				crashStream.append("Obvilion Network Launcher crash report\n");
				crashStream.append(" - Time: " + DateFormat.getDateTimeInstance().format(new Date()) + "\n");
				crashStream.append(" - Device: " + Build.PRODUCT + " " + Build.MODEL + "\n");
				crashStream.append(" - Android version: " + Build.VERSION.RELEASE + "\n");
				crashStream.append(" - Crash stack trace:\n");
				crashStream.append(Log.getStackTraceString(th));
				crashStream.close();
			} catch (Throwable th2) {
				Log.e(CRASH_REPORT_TAG, " - Exception attempt saving crash stack trace:", th2);
				Log.e(CRASH_REPORT_TAG, " - The crash stack trace was:", th);
			}

			FatalErrorActivity.showError(Application.this, crashFile.getAbsolutePath(), storagePermAllowed, th);
			// android.os.Process.killProcess(android.os.Process.myPid());

			BaseMainActivity.fullyExit();
		});
	}
}
