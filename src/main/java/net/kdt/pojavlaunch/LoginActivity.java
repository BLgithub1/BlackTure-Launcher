package net.kdt.pojavlaunch;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.system.Os;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.kdt.mcgui.MineButton;
import com.kdt.pickafile.FileListView;
import com.kdt.pickafile.FileSelectedListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import net.kdt.pojavlaunch.customcontrols.CustomControls;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.utils.JREUtils;
import net.kdt.pojavlaunch.utils.LocaleUtils;
import net.kdt.pojavlaunch.value.MinecraftAccount;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.json.JSONTokener;

import ru.obvilion.launcher.Vars;
import ru.obvilion.launcher.utils.Base64;
import ru.obvilion.launcher.utils.JavaUtils;
import ru.obvilion.launcher.utils.JsonUtils;

public class LoginActivity extends BaseActivity {
    private final Object mLockStoragePerm = new Object();
    private final Object mLockSelectJRE = new Object();
    
    private EditText edit2, edit3;
    public ProgressBar bar;
    public TextView startupTextView;
    private final int REQUEST_STORAGE_REQUEST_CODE = 1;
    private CheckBox sRemember;
    public static SharedPreferences firstLaunchPrefs;
    private MinecraftAccount mProfile = null;
    public MineButton auth_button;
    
    private static boolean isSkipInit = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState); // false;

        Vars.LOGIN_ACTIVITY = this;
        Tools.updateWindowSize(this);
        
        firstLaunchPrefs = getSharedPreferences("pojav_extract", MODE_PRIVATE);
        new InitTask().execute(isSkipInit);
    }

    @Override
    public void onResume() {
        super.onResume();
        Tools.updateWindowSize(this);

        // Clear current profile
        Profile.setCurrentProfile(this, null);
    }

    private class InitTask extends AsyncTask<Boolean, String, Integer> {
        private AlertDialog startAle;
        private ProgressBar progress;

        @Override
        protected void onPreExecute() {
            LinearLayout startScr = new LinearLayout(LoginActivity.this);
            LayoutInflater.from(LoginActivity.this).inflate(R.layout.launcher_start_screen, startScr);

            FontChanger.changeFonts(startScr);

            bar = (ProgressBar) startScr.findViewById(R.id.startscreenProgress2);
            progress = (ProgressBar) startScr.findViewById(R.id.startscreenProgress);
            startupTextView = (TextView) startScr.findViewById(R.id.startscreen_text);

            AlertDialog.Builder startDlg = new AlertDialog.Builder(LoginActivity.this, R.style.AppTheme);
            startDlg.setView(startScr);
            startDlg.setCancelable(false);

            startAle = startDlg.create();
            startAle.show();
            startAle.getWindow().setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            );

            setContentView(R.layout.launcher_start_screen);
        }
        
        private int revokeCount = -1;
        
        @Override
        protected Integer doInBackground(Boolean[] params) {
            // If trigger a quick restart
            if (params[0]) return 0;
            
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {}

            publishProgress("visible");

            while (Build.VERSION.SDK_INT >= 23 && !isStorageAllowed()){
                try {
                    revokeCount++;
                    if (revokeCount >= 3) {
                        Toast.makeText(LoginActivity.this, R.string.toast_permission_denied, Toast.LENGTH_LONG).show();
                        finish();
                        return 0;
                    }
                    
                    requestStoragePermission();
                    
                    synchronized (mLockStoragePerm) {
                        mLockStoragePerm.wait();
                    }
                } catch (InterruptedException e) {}
            }

            try {
                initMain();
            } catch (Throwable th) {
                Tools.showError(LoginActivity.this, th, true);
                return 1;
            }

            return 0;
        }

        @Override
        protected void onProgressUpdate(String... obj)
        {
            if (obj[0].equals("visible")) {
                progress.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected void onPostExecute(Integer obj) {
            startAle.dismiss();
            if (obj == 0) uiInit();
        }
    }
    
    private void uiInit() {
        setContentView(R.layout.launcher_login);

        Spinner spinnerChgLang = findViewById(R.id.login_spinner_language);

        String defaultLang = LocaleUtils.DEFAULT_LOCALE.getDisplayName();
        SpannableString defaultLangChar = new SpannableString(defaultLang);
        defaultLangChar.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, defaultLang.length(), 0);
        
        final ArrayAdapter<DisplayableLocale> langAdapter = new ArrayAdapter<DisplayableLocale>(this, android.R.layout.simple_spinner_item);
        
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(getAssets().open("language_list.txt")));
            String line;
            while ((line = reader.readLine()) != null) {
                File currFile = new File("/" + line);
                // System.out.println(currFile.getAbsolutePath());
                if (currFile.getAbsolutePath().contains("/values-") || currFile.getName().startsWith("values-")) {
                    // TODO use regex(?)
                    langAdapter.add(new DisplayableLocale(currFile.getName().replace("values-", "").replace("-r", "-")));
                }
            }
        } catch (IOException e) {
            Tools.showError(this, e);
        }
        
        langAdapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        
        int selectedLang = 0;
        for (int i = 0; i < langAdapter.getCount(); i++) {
            if (Locale.getDefault().getDisplayLanguage().equals(langAdapter.getItem(i).mLocale.getDisplayLanguage())) {
                selectedLang = i;
                break;
            }
        }
        
        spinnerChgLang.setAdapter(langAdapter);
        spinnerChgLang.setSelection(selectedLang);
        spinnerChgLang.setOnItemSelectedListener(new Spinner.OnItemSelectedListener(){
            private boolean isInitCalled;
            @Override
            public void onItemSelected(AdapterView<?> adapter, View view, int position, long id) {
                if (!isInitCalled) {
                    isInitCalled = true;
                    return;
                }
                
                Locale locale = langAdapter.getItem(position).mLocale;
                
                LauncherPreferences.PREF_LANGUAGE = locale.getLanguage();
                LauncherPreferences.DEFAULT_PREF.edit().putString("language", LauncherPreferences.PREF_LANGUAGE).apply();
                
                // Restart to apply language change
                finish();
                startActivity(getIntent());
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> adapter) {

            }
        });
            
        edit2 = (EditText) findViewById(R.id.login_edit_email);
        edit3 = (EditText) findViewById(R.id.login_edit_password);

        boolean tr = false;

        try {
            Vars.CONFIG = JsonUtils.readJsonFromFile(Vars.CONFIG_FILE);

            if (Vars.CONFIG != null) {
                tr = true;
                edit2.setText(Base64.decrypt(Vars.CONFIG.getString("login")));
                edit3.setText(Base64.decrypt(Vars.CONFIG.getString("pass")));
            }
        } catch (Exception e) {

        }
        
        sRemember = findViewById(R.id.login_switch_remember);
        auth_button = (MineButton) findViewById(R.id.mineButton);
        isSkipInit = true;

        if (tr) {
            auth_button.setEnabled(false);
            auth_button.setText(getString(R.string.login_auto_auth));
            loginMC(null);
        }
    }
   
    private void unpackComponent(AssetManager am, String component) throws IOException {
        File versionFile = new File(Tools.DIR_GAME_HOME + "/" + component + "/version");
        InputStream is = am.open("components/" + component + "/version");
        if(!versionFile.exists()) {
            if (versionFile.getParentFile().exists() && versionFile.getParentFile().isDirectory()) {
                FileUtils.deleteDirectory(versionFile.getParentFile());
            }
            versionFile.getParentFile().mkdir();
            
            Log.i("UnpackPrep", component + ": Pack was installed manually, or does not exist, unpacking new...");
            String[] fileList = am.list("components/" + component);
            for(String s : fileList) {
                Tools.copyAssetFile(this, "components/" + component + "/" + s, Tools.DIR_GAME_HOME + "/" + component, true);
            }
        } else {
            FileInputStream fis = new FileInputStream(versionFile);
            String release1 = Tools.read(is);
            String release2 = Tools.read(fis);
            if (!release1.equals(release2)) {
                if (versionFile.getParentFile().exists() && versionFile.getParentFile().isDirectory()) {
                    FileUtils.deleteDirectory(versionFile.getParentFile());
                }
                versionFile.getParentFile().mkdir();
                
                String[] fileList = am.list("components/" + component);
                for (String s : fileList) {
                    Tools.copyAssetFile(this, "components/" + component + "/" + s, Tools.DIR_GAME_HOME + "/" + component, true);
                }
            } else {
                Log.i("UnpackPrep", component + ": Pack is up-to-date with the launcher, continuing...");
            }
        }
    }

    public static void disableSplash(String dir) {
        mkdirs(dir + "/config");
        File forgeSplashFile = new File(dir, "config/splash.properties");
        String forgeSplashContent = "enabled=true";
        try {
            if (forgeSplashFile.exists()) {
                forgeSplashContent = Tools.read(forgeSplashFile.getAbsolutePath());
            }
            if (forgeSplashContent.contains("enabled=true")) {
                Tools.write(forgeSplashFile.getAbsolutePath(),
                        forgeSplashContent.replace("enabled=true", "enabled=false"));
            }
        } catch (IOException e) {
            Log.w(Tools.APP_NAME, "Could not disable Forge 1.12.2 and below splash screen!", e);
        }
    }

    private void initMain() throws Throwable {
        mkdirs(Tools.DIR_ACCOUNT_NEW);
        Migrator.migrateAccountData(this);
        
        mkdirs(Tools.DIR_GAME_HOME);
        mkdirs(Tools.DIR_GAME_HOME + "/lwjgl3");
        mkdirs(Tools.DIR_GAME_HOME + "/config");
        if (!Migrator.migrateGameDir()) {
            mkdirs(Tools.DIR_GAME_NEW);
            mkdirs(Tools.DIR_GAME_NEW + "/mods");
            mkdirs(Tools.DIR_HOME_VERSION);
            mkdirs(Tools.DIR_HOME_LIBRARY);
        }

        mkdirs(Tools.CTRLMAP_PATH);
        
        try {
            new CustomControls(this).save(Tools.CTRLDEF_FILE);

            Tools.copyAssetFile(this, "components/security/pro-grade.jar", Tools.DIR_DATA, true);
            Tools.copyAssetFile(this, "components/security/java_sandbox.policy", Tools.DIR_DATA, true);
            Tools.copyAssetFile(this, "options.txt", Tools.DIR_GAME_NEW, false);
            // TODO: Remove after implement.
            Tools.copyAssetFile(this, "launcher_profiles.json", Tools.DIR_GAME_NEW, false);

            AssetManager am = this.getAssets();
            
            unpackComponent(am, "caciocavallo");
            unpackComponent(am, "lwjgl3");
            if (!JavaUtils.isJavaRuntimeInstalled()) {
                if (!JavaUtils.installRuntimeAutomatically()) {
                    File jreTarFile = selectJreTarFile();
                    uncompressTarXZ(jreTarFile, new File(Tools.DIR_HOME_JRE));
                }

                firstLaunchPrefs.edit().putBoolean("isJavaRuntimeInstalled", true).apply();
            }
            
            JREUtils.relocateLibPath(this);

            File ftIn = new File(Tools.DIR_HOME_JRE, Tools.DIRNAME_HOME_JRE + "/libfreetype.so.6");
            File ftOut = new File(Tools.DIR_HOME_JRE, Tools.DIRNAME_HOME_JRE + "/libfreetype.so");
            if (ftIn.exists() && (!ftOut.exists() || ftIn.length() != ftOut.length())) {
                ftIn.renameTo(ftOut);
            }
            
            // Refresh libraries
            copyDummyNativeLib("libawt_xawt.so");
            // copyDummyNativeLib("libfontconfig.so");
        } catch (Throwable e) {
            Tools.showError(this, e);
        }
    }

    private void copyDummyNativeLib(String name) throws Throwable {
        File fileLib = new File(Tools.DIR_HOME_JRE, Tools.DIRNAME_HOME_JRE + "/" + name);
        fileLib.delete();
        FileInputStream is = new FileInputStream(new File(getApplicationInfo().nativeLibraryDir, name));
        FileOutputStream os = new FileOutputStream(fileLib);
        IOUtils.copy(is, os);
        is.close();
        os.close();
    }
    
    private File selectJreTarFile() throws InterruptedException {
        final StringBuilder selectedFile = new StringBuilder();
        
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
            builder.setTitle(getString(R.string.alerttitle_install_jre, Tools.CURRENT_ARCHITECTURE));
            builder.setCancelable(false);

            final AlertDialog dialog = builder.create();
            FileListView flv = new FileListView(dialog, "tar.xz");
            flv.setFileSelectedListener(new FileSelectedListener(){

                    @Override
                    public void onFileSelected(File file, String path) {
                        selectedFile.append(path);
                        dialog.dismiss();

                        synchronized (mLockSelectJRE) {
                            mLockSelectJRE.notifyAll();
                        }

                    }
                });
            dialog.setView(flv);
            dialog.show();
        });
        
        synchronized (mLockSelectJRE) {
            mLockSelectJRE.wait();
        }
        
        return new File(selectedFile.toString());
    }

    public void uncompressTarXZ(final File tarFile, final File dest) throws IOException {

        dest.mkdirs();
        TarArchiveInputStream tarIn = null;

        tarIn = new TarArchiveInputStream(
            new XZCompressorInputStream(
                new BufferedInputStream(
                    new FileInputStream(tarFile)
                )
            )
        );

        TarArchiveEntry tarEntry = tarIn.getNextTarEntry();
        // tarIn is a TarArchiveInputStream
        while (tarEntry != null) {
            /*
             * Unpacking very small files in short time cause
             * application to ANR or out of memory, so delay
             * a little if size is below than 20kb (20480 bytes)
             */
            if (tarEntry.getSize() <= 20480) {
                try {
                    // 40 small files per second
                    Thread.sleep(25);
                } catch (InterruptedException e) {}
            }
            final String tarEntryName = tarEntry.getName();
            runOnUiThread(new Runnable(){
                @SuppressLint("StringFormatInvalid")
                @Override
                public void run() {
                    startupTextView.setText(getString(R.string.global_unpacking, tarEntryName));
                }
            });
            // publishProgress(null, "Unpacking " + tarEntry.getName());
            File destPath = new File(dest, tarEntry.getName()); 
            if (tarEntry.isSymbolicLink()) {
                destPath.getParentFile().mkdirs();
                try {
                    // android.system.Os
                    // Libcore one support all Android versions
                    System.out.println(tarEntry.getName() + " " + tarEntry.getLinkName());
                    Os.symlink(tarEntry.getName(), tarEntry.getLinkName());
                } catch (Throwable e) {
                    e.printStackTrace();
                }

            } else if (tarEntry.isDirectory()) {
                destPath.mkdirs();
                destPath.setExecutable(true);
            } else if (!destPath.exists() || destPath.length() != tarEntry.getSize()) {
                destPath.getParentFile().mkdirs();
                destPath.createNewFile();
                
                FileOutputStream os = new FileOutputStream(destPath);
                IOUtils.copy(tarIn, os);
                os.close();

            }
            tarEntry = tarIn.getNextTarEntry();
        }
        tarIn.close();
    }
    
    private static boolean mkdirs(String path) {
        File file = new File(path);
        // check necessary???
        if (file.getParentFile().exists()) {
            return file.mkdir();
        }

        return file.mkdirs();
    }

    public void loginMC(final View v) {
        boolean off = v == null;

        if (!off) {
            String text = edit2.getText().toString();
            if (text.isEmpty()) {
                edit2.setError(getString(R.string.global_error_field_empty));
                return;
            }
            if (text.length() <= 2) {
                edit2.setError(getString(R.string.login_error_short_username));
                return;
            }

            v.setEnabled(false);
        }

        new Thread(() -> {
            try {
                URL url = new URL("https://obvilion.ru/api/auth/login");
                URLConnection con = url.openConnection();
                HttpURLConnection http = (HttpURLConnection) con;
                http.setRequestMethod("POST");
                http.setDoOutput(true);

                JSONObject jsonParam = new JSONObject();
                jsonParam.put("name", edit2.getText());
                jsonParam.put("password", edit3.getText());

                byte[] out = jsonParam.toString().getBytes(StandardCharsets.UTF_8);
                int length = out.length;

                http.setFixedLengthStreamingMode(length);
                http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                http.setRequestProperty("Accept-Charset", "UTF-8");
                http.connect();
                try(OutputStream os = http.getOutputStream()) {
                    os.write(out);
                }

                InputStream is = http.getResponseCode() >= 400 ? http.getErrorStream() : http.getInputStream();

                BufferedReader in = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                JSONTokener tokener = new JSONTokener(response.toString());
                JSONObject result = new JSONObject(tokener);

                if (result.has("token")) {
                    MinecraftAccount builder = new MinecraftAccount();
                    builder.accessToken = result.getString("token");
                    builder.profileId = result.getString("uuid");
                    builder.username = result.has("name") ? result.getString("name") : edit2.getText().toString();
                    builder.isMicrosoft = false;
                    builder.selectedVersion = "1.12.2";

                    // TODO: skin face
                    // builder.updateSkinFace();
                    mProfile = builder;

                    if (!off && sRemember.isChecked()) {
                        if (Vars.CONFIG == null) Vars.CONFIG = new JSONObject();

                        Vars.CONFIG.put("login", Base64.encrypt(edit2.getText().toString()));
                        Vars.CONFIG.put("pass", Base64.encrypt(edit3.getText().toString()));
                        JsonUtils.writeJsonToFile(Vars.CONFIG, Vars.CONFIG_FILE);
                    }

                    runOnUiThread(() -> {
                        if (!off) v.setEnabled(true);

                        if (off) {
                            auth_button.setEnabled(true);
                            auth_button.setText(getString(R.string.login_online_login_label));
                            edit3.setText("");
                        }

                        playProfile(false);
                    });
                } else {
                    if (off) {
                        runOnUiThread(() -> {
                            auth_button.setEnabled(true);
                            auth_button.setText(getString(R.string.login_online_login_label));
                            edit3.setText("");
                        });
                    }

                    if (result.has("error")) {
                        if (result.getString("error").contains("User not found")) {
                            runOnUiThread(() -> {
                                edit2.setError(getString(R.string.login_error_user_not_found));
                                if (!off) v.setEnabled(true);
                            });

                            return;
                        }

                        if (result.getString("error").contains("Invalid password")) {
                            runOnUiThread(() -> {
                                edit3.setError(getString(R.string.login_error_invalid_password));
                                if (!off) v.setEnabled(true);
                            });
                        }
                    } else {
                        if (result.getString("message").contains("Too many requests")) {
                            runOnUiThread(() -> {
                                edit2.setError(getString(R.string.login_error_too_many_req));
                                if (!off) v.setEnabled(true);
                            });
                        }
                    }
                }
            } catch (Exception e) {
                if (off) {
                    runOnUiThread(() -> {
                        auth_button.setEnabled(true);
                        auth_button.setText(getString(R.string.login_online_login_label));
                        edit3.setText("");
                    });
                    return;
                }

                Tools.dialogOnUiThread(LoginActivity.this,
                        getResources().getString(R.string.global_error), e.getLocalizedMessage());
                runOnUiThread(() -> {
                    v.setEnabled(true);
                });
            }
        }).start();
    }
    
    private void playProfile(boolean notOnLogin) {
        if (mProfile != null) {
            try {
                String profileName = null;
                if (sRemember.isChecked() || notOnLogin) {
                    profileName = mProfile.save();
                }
                
                Profile.launch(LoginActivity.this, profileName == null ? mProfile : profileName);
            } catch (IOException e) {
                Tools.showError(this, e);
            }
        }
    }

    // We are calling this method to check the permission status
    private boolean isStorageAllowed() {
        // Getting the permission status
        int result1 = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int result2 = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);


        // If permission is granted returning true
        return result1 == PackageManager.PERMISSION_GRANTED &&
            result2 == PackageManager.PERMISSION_GRANTED;
    }

    // Requesting permission
    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[] {
                Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
        }, REQUEST_STORAGE_REQUEST_CODE);
    }

    // This method will be called when the user will tap on allow or deny
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_STORAGE_REQUEST_CODE){
            synchronized (mLockStoragePerm) {
                mLockStoragePerm.notifyAll();
            }
        }
    }

    public void forgotPassword(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://obvilion.ru/auth/resetpassword"));
        startActivity(browserIntent);
    }

    public void registerButton(View view) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://obvilion.ru/auth/signup"));
        startActivity(browserIntent);
    }
}
