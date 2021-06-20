package ru.obvilion.launcher;

import android.app.Application;
import android.os.Environment;

import net.kdt.pojavlaunch.LoginActivity;

import java.io.File;

public class Vars {
    public static Application THIS_APP;

    public static LoginActivity LOGIN_ACTIVITY;
    public static int LAST_SERVER_TAB;

    public static File EXT_STORAGE = Environment.getExternalStorageDirectory();
    public static File APP_DATA;

    public static File LAUNCHER_HOME = new File(EXT_STORAGE, "ObvilionNetwork");
    public static File GAME_DIR = new File(LAUNCHER_HOME, "minecraft");
    public static File JAVA_DIR = new File(LAUNCHER_HOME, "java");
    public static File LOG_FILE = new File(LAUNCHER_HOME, "latest.log");

    public static String ARCHITECTURE;
}
