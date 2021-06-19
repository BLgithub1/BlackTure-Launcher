package net.kdt.pojavlaunch;

import android.app.*;
import android.content.*;
import android.text.*;
import android.text.method.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.*;
import com.kdt.pickafile.*;
import java.io.*;
import net.kdt.pojavlaunch.fragments.*;
import net.kdt.pojavlaunch.prefs.*;
import net.kdt.pojavlaunch.tasks.*;

import androidx.appcompat.app.AlertDialog;
import net.kdt.pojavlaunch.value.*;

public abstract class BaseLauncherActivity extends BaseActivity {
	public Button mPlayButton;
	public ConsoleFragment mConsoleView;
    public ProgressBar mLaunchProgress;
	public Spinner mVersionSelector;
	public TextView mLaunchTextStatus, mTextVersion;
    
    public JMinecraftVersionList mVersionList;
	public MinecraftDownloaderTask mTask;
	public MinecraftAccount mProfile;
	public String[] mAvailableVersions;
    
	public boolean mIsAssetsProcessing = false;
    protected boolean canBack = false;
    
    public abstract void statusIsLaunching(boolean isLaunching);

    public void mcaccLogout(View view) {
        finish();
    }

    private void installMod(boolean customJavaArgs) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.alerttitle_installmod);
        builder.setNegativeButton(android.R.string.cancel, null);

        final AlertDialog dialog;
        if (customJavaArgs) {
            final EditText edit = new EditText(this);
            edit.setSingleLine();
            edit.setHint("-jar/-cp /path/to/file.jar ...");
            builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface di, int i) {
                        Intent intent = new Intent(BaseLauncherActivity.this, JavaGUILauncherActivity.class);
                        intent.putExtra("skipDetectMod", true);
                        intent.putExtra("javaArgs", edit.getText().toString());
                        startActivity(intent);
                    }
                });
            dialog = builder.create();
            dialog.setView(edit);
        } else {
            dialog = builder.create();
            FileListView flv = new FileListView(dialog,"jar");
            flv.setFileSelectedListener(new FileSelectedListener(){
                    @Override
                    public void onFileSelected(File file, String path) {
                        Intent intent = new Intent(BaseLauncherActivity.this, JavaGUILauncherActivity.class);
                        intent.putExtra("modFile", file);
                        startActivity(intent);
                        dialog.dismiss();

                    }
                });
            dialog.setView(flv);
        }
        dialog.show();
    }

    public void launchGame(View v) {
        if (!canBack && mIsAssetsProcessing) {
            mIsAssetsProcessing = false;
            statusIsLaunching(false);
        } else if (canBack) {
            v.setEnabled(false);
            mTask = new MinecraftDownloaderTask(this);
            mTask.execute(mProfile.selectedVersion);
        }
    }
    
    @Override
    public void onBackPressed() {
        if (canBack) {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        System.out.println("call to onPostResume");
        Tools.updateWindowSize(this);
        System.out.println("call to onPostResume; E");
    }
    
    @Override
    protected void onResume(){
        super.onResume();
        System.out.println("call to onResume");
        final int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        final View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(uiOptions);
        System.out.println("call to onResume; E");
    }
    SharedPreferences.OnSharedPreferenceChangeListener listRefreshListener = null;
    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if(listRefreshListener == null) {
            final BaseLauncherActivity thiz = this;
            listRefreshListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    if(key.startsWith("vertype_")) {
                        System.out.println("Verlist update needed!");
                        new RefreshVersionListTask(thiz).execute();
                    }
                }
            };
            LauncherPreferences.DEFAULT_PREF.registerOnSharedPreferenceChangeListener(listRefreshListener);
        }
        new RefreshVersionListTask(this).execute();
        System.out.println("call to onResumeFragments");
        try{
            final ProgressDialog barrier = new ProgressDialog(this);
            barrier.setMessage(getString(R.string.global_waiting));
            barrier.setProgressStyle(barrier.STYLE_SPINNER);
            barrier.setCancelable(false);
            barrier.show();

            new Thread(new Runnable(){

                    @Override
                    public void run()
                    {
                        while (mConsoleView == null) {
                            try {
                                Thread.sleep(20);
                            } catch (Throwable th) {}
                        }

                        try {
                            Thread.sleep(100);
                        } catch (Throwable th) {}

                        runOnUiThread(new Runnable() {
                                @Override
                                public void run()
                                {
                                    try {
                                        mConsoleView.putLog("");
                                        barrier.dismiss();
                                    } catch (Throwable th) {
                                        startActivity(getIntent());
                                        finish();
                                    }
                                }
                            });
                    }
                }).start();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.out.println("call to onResumeFragments; E");
    }
    
    // Catching touch exception
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    protected abstract void initTabs(int pageIndex);
}
