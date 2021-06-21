package ru.obvilion.launcher;

import android.app.Application;
import android.os.Environment;

import net.kdt.pojavlaunch.LauncherActivity;
import net.kdt.pojavlaunch.LoginActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

public class Vars {
    public static Application THIS_APP;

    public static LoginActivity LOGIN_ACTIVITY;
    public static LauncherActivity LAUNCHER_ACTIVITY;

    public static int LAST_SERVER_TAB;
    public static JSONArray SERVERS;
    public static JSONArray SERVERS_FILES;

    public static File EXT_STORAGE = Environment.getExternalStorageDirectory();
    public static File APP_DATA;

    public static File LAUNCHER_HOME = new File(EXT_STORAGE, "ObvilionNetwork");
    public static File GAME_DIR = new File(LAUNCHER_HOME, "minecraft");
    public static File JAVA_DIR = new File(LAUNCHER_HOME, "java");
    public static File LOG_FILE = new File(APP_DATA, "latest.log");

    public static String ARCHITECTURE;
}
