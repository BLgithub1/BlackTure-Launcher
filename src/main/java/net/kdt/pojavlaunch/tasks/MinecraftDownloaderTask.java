package net.kdt.pojavlaunch.tasks;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.util.*;
import com.google.gson.*;
import java.io.*;
import java.util.*;
import net.kdt.pojavlaunch.*;
import net.kdt.pojavlaunch.prefs.*;
import net.kdt.pojavlaunch.utils.*;
import net.kdt.pojavlaunch.value.*;
import org.apache.commons.io.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ru.obvilion.launcher.Vars;
import ru.obvilion.launcher.utils.JsonUtils;

public class MinecraftDownloaderTask extends AsyncTask<String, String, Throwable> {
    private BaseLauncherActivity mActivity;

    private int serverID;
    private String serverName;
    private JSONObject serverData;
    private JSONObject serverFilesData;
    private long clientSize;
    private File serverDir;

    private boolean launchWithError = false;

    public MinecraftDownloaderTask(BaseLauncherActivity activity) {
        mActivity = activity;
    }
    
    @Override
    protected void onPreExecute() {
        mActivity.mLaunchProgress.setMax(1);
        mActivity.statusIsLaunching(true);
    }

    //FIXME
    private JMinecraftVersionList.Version verInfo;

    @Override
    protected Throwable doInBackground(final String[] p1) {
        try {
            serverID = Integer.parseInt(p1[0]);
            serverData = Vars.SERVERS.getJSONObject(serverID);
            serverName = serverData.getString("name");

            Log.i("loader", "Starting loading version "
                    + serverName
                    /* + " " + serverData.getString("version") */); // TODO: Add version to API

            JSONObject obj = JsonUtils.readJsonFromUrl("https://obvilionnetwork.ru/api/servers/info");
            Vars.SERVERS_FILES = obj.getJSONArray("servers");

            /* Get files for client */
            serverFilesData = Vars.SERVERS_FILES.getJSONObject(serverID);
            if (!serverFilesData.getString("name").equals(serverName)) {
                for (int i = 0; i < Vars.SERVERS_FILES.length(); i++) {
                    JSONObject server = Vars.SERVERS_FILES.getJSONObject(i);

                    if (server.getString("name").equals(serverName)) {
                        serverFilesData = server;
                    }
                }
            }

            serverDir = new File(Vars.GAME_DIR, serverName);
            serverDir.mkdir();
            Vars.SELECTED_SERVER_DIR = serverDir;

            // TODO: All client size
            clientSize = serverFilesData.getJSONObject("core").getLong("size");
            clientSize += getModulesSize(serverFilesData.getJSONArray("libraries"));
            clientSize += getModulesSize(serverFilesData.getJSONArray("natives"));
            clientSize += getModulesSize(serverFilesData.getJSONArray("mods"));
            clientSize += getModulesSize(serverFilesData.getJSONArray("other"));

            downloadModule(serverFilesData.getJSONObject("core"));
            downloadAllModules(serverFilesData.getJSONArray("libraries"));
            //downloadAllModules(serverFilesData.getJSONArray("natives"));
            downloadAllModules(serverFilesData.getJSONArray("mods"));
            downloadAllModules(serverFilesData.getJSONArray("other"));
            downloadMobileFix();

            mActivity.mIsAssetsProcessing = true;
            mActivity.mPlayButton.post(() -> {
                mActivity.mPlayButton.setText("Skip");
                mActivity.mPlayButton.setEnabled(true);
            });

            downloadAssets(serverFilesData.getJSONArray("assets"));

            return null;
        } catch (Exception e) {
            Log.e("loader", e.getLocalizedMessage());
            return e;
        }
    }

    private void downloadMobileFix() throws IOException {
        File target = new File(serverDir, "config/splash.properties");
        if (target.length() != 374)
        Tools.downloadFileMonitored(
                "https://obvilionnetwork.ru/api/files/temp/splash.properties",
                target.getPath(),
                new Tools.DownloaderFeedback() {
                    @Override
                    public void updateProgress(int curr, int max) {
                        publishDownloadProgress(target.getName(), curr, max);
                    }
                }
        );
    }

    private void downloadModule(JSONObject module) throws JSONException, IOException {
        String path = module.getString("path");
        File target = new File(serverDir, path);

        target.getParentFile().mkdirs();

        if (target.length() != module.getLong("size")) {
            Tools.downloadFileMonitored(
                    "https://obvilionnetwork.ru/api/files/" + module.getString("link"),
                    target.getPath(),
                    new Tools.DownloaderFeedback() {
                        @Override
                        public void updateProgress(int curr, int max) {
                            publishDownloadProgress(target.getName(), curr, max);
                        }
                    }
            );
        }
    }

    private void downloadAllModules(JSONArray modules) throws JSONException, IOException {
        for (int i = 0; i < modules.length(); i++) {
            downloadModule(modules.getJSONObject(i));
        }
    }

    private void downloadAssets(JSONArray modules) throws JSONException, IOException {
        for (int i = 0; i < modules.length(); i++) {
            if (!mActivity.mIsAssetsProcessing) {
                return;
            }

            downloadModule(modules.getJSONObject(i));
        }
    }

    private long getModulesSize(JSONArray modules) throws JSONException {
        long all = 0;
        for (int i = 0; i < modules.length(); i++) {
            all += modules.getJSONObject(i).getLong("size");
        }

        return all;
    }

    private void publishDownloadProgress(String target, int curr, int max) {
        mActivity.mLaunchProgress.setMax(max);
        mActivity.mLaunchProgress.setProgress(curr);

        publishProgress("0", mActivity.getString(R.string.mcl_launch_downloading_progress, target,
            curr / 1024d / 1024d, max / 1024d / 1024d), "");
    }


    private int addProgress = 0;

    @Override
    protected void onProgressUpdate(String... p1) {
        int addedProg = Integer.parseInt(p1[0]);
        if (addedProg != -1) {
            addProgress = addProgress + addedProg;
            mActivity.mLaunchTextStatus.setText(p1[1]);
        }

        if (p1.length < 3) {
            mActivity.mConsoleView.putLog(p1[1] + "\n");
        }
    }

    @Override
    protected void onPostExecute(Throwable p1) {
        mActivity.mPlayButton.setText("Play");
        mActivity.mPlayButton.setEnabled(true);
        mActivity.mLaunchProgress.setMax(100);
        mActivity.mLaunchProgress.setProgress(0);
        mActivity.statusIsLaunching(false);

        if (p1 != null) {
            p1.printStackTrace();
            Tools.showError(mActivity, p1);
        }

        if (!launchWithError) {
            try {
                Intent mainIntent = new Intent(mActivity, MainActivity.class);

                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
                mainIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                if (LauncherPreferences.PREF_FREEFORM) {
                    DisplayMetrics dm = new DisplayMetrics();
                    mActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);

                    ActivityOptions options = (ActivityOptions) ActivityOptions.class.getMethod("makeBasic").invoke(null);
                    Rect freeformRect = new Rect(0, 0, dm.widthPixels / 2, dm.heightPixels / 2);
                    options.getClass().getDeclaredMethod("setLaunchBounds", Rect.class).invoke(options, freeformRect);
                    mActivity.startActivity(mainIntent, options.toBundle());
                } else {
                    mActivity.startActivity(mainIntent);
                }
            } catch (Throwable e) {
                Tools.showError(mActivity, e);
            }
        }

        mActivity.mTask = null;
    }
}
