package net.kdt.pojavlaunch;

import android.animation.ValueAnimator;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.VerticalTabLayout.ViewPagerAdapter;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.Guideline;
import androidx.viewpager.widget.ViewPager;

import net.kdt.pojavlaunch.fragments.ConsoleFragment;
import net.kdt.pojavlaunch.fragments.ServerFragment;
import net.kdt.pojavlaunch.prefs.LauncherPreferenceFragment;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.value.MinecraftAccount;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ru.obvilion.launcher.Vars;
import ru.obvilion.launcher.tasks.ImageLoadTask;

import static android.os.Build.VERSION_CODES.P;
import static net.kdt.pojavlaunch.Tools.ignoreNotch;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_HIDE_SIDEBAR;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_IGNORE_NOTCH;
import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_NOTCH_SIZE;

public class LauncherActivity extends BaseLauncherActivity {

    private ViewPager viewPager;
    private ViewPagerAdapter viewPageAdapter;
    private final Button[] Tabs = new Button[3];
    private View selected;

    private Button logoutBtn; // MineButtons

    public LauncherActivity() {
    }

    @Override

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launcher_menu);

        if (BuildConfig.DEBUG) {
            Toast.makeText(this, "Launcher process id: " + android.os.Process.myPid(), Toast.LENGTH_LONG).show();
        }

        viewPager = findViewById(R.id.launchermainTabPager);
        selected = findViewById(R.id.viewTabSelected);

        mConsoleView = new ConsoleFragment();

        viewPageAdapter = new ViewPagerAdapter(getSupportFragmentManager());

        viewPageAdapter.addFragment(new ServerFragment(), 0, getString(R.string.mcl_option_servers));
        viewPageAdapter.addFragment(new ServerFragment(), 0, getString(R.string.mcl_option_servers));
        viewPageAdapter.addFragment(new ServerFragment(), 0, getString(R.string.mcl_option_servers));
        viewPageAdapter.addFragment(new ServerFragment(), 0, getString(R.string.mcl_option_servers));
        viewPageAdapter.addFragment(new ServerFragment(), 0, getString(R.string.mcl_option_servers));
        viewPageAdapter.addFragment(new ServerFragment(), 0, getString(R.string.mcl_option_servers));
        viewPageAdapter.addFragment(new ServerFragment(), 0, getString(R.string.mcl_option_servers));
        viewPageAdapter.addFragment(new ServerFragment(), 0, getString(R.string.mcl_option_servers));
        viewPageAdapter.addFragment(new ServerFragment(), 0, getString(R.string.mcl_option_servers));
        viewPageAdapter.addFragment(new ServerFragment(), 0, getString(R.string.mcl_option_servers));
        viewPageAdapter.addFragment(new ServerFragment(), 0, getString(R.string.mcl_option_servers));
        viewPageAdapter.addFragment(new ServerFragment(), 0, getString(R.string.mcl_option_servers));

        viewPageAdapter.addFragment(mConsoleView, 0, getString(R.string.mcl_tab_console));
        viewPageAdapter.addFragment(new LauncherPreferenceFragment(), 0, getString(R.string.mcl_option_settings));

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                int serversCount = viewPageAdapter.getCount() - 2;

                if (position > serversCount - 1) {
                    setTabActive(position - serversCount + 1);
                } else {
                    Vars.LAST_SERVER_TAB = position;
                    setTabActive(0);
                }
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        viewPager.setAdapter(viewPageAdapter);

        Tabs[0] = findViewById(R.id.btnTab1);
        Tabs[1] = findViewById(R.id.btnTab2);
        Tabs[2] = findViewById(R.id.btnTab3);

        pickAccount();

        final List<String> accountList = new ArrayList<String>();
        final MinecraftAccount tempProfile = Profile.getTempProfileContent(this);
        if (tempProfile != null) {
            accountList.add(tempProfile.username);
        }
        for (String s : new File(Tools.DIR_ACCOUNT_NEW).list()) {
            accountList.add(s.substring(0, s.length() - 5));
        }

        TextView username = findViewById(R.id.launchermain_account_name);
        username.setText(accountList.get(0));
        ImageView avatar = findViewById(R.id.account_avatar);
        new ImageLoadTask(
                "https://obvilionnetwork.ru/api/users/get/" + accountList.get(0) + "/avatar",
                avatar
        ).execute();

        List<String> versions = new ArrayList<String>();
        final File fVers = new File(Tools.DIR_HOME_VERSION);

        try {
            if (fVers.listFiles().length < 1) {
                throw new Exception(getString(R.string.error_no_version));
            }

            for (File fVer : fVers.listFiles()) {
                if (fVer.isDirectory())
                    versions.add(fVer.getName());
            }
        } catch (Exception e) {
            versions.add(getString(R.string.global_error) + ":");
            versions.add(e.getMessage());

        } finally {
            mAvailableVersions = versions.toArray(new String[0]);
        }

        //mAvailableVersions;
        ArrayAdapter<String> adapterVer = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, mAvailableVersions);
        adapterVer.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        mVersionSelector = (Spinner) findViewById(R.id.launchermain_spinner_version);
        mVersionSelector.setAdapter(adapterVer);

        mLaunchProgress = (ProgressBar) findViewById(R.id.progressDownloadBar);
        mLaunchTextStatus = (TextView) findViewById(R.id.progressDownloadText);
        logoutBtn = (Button) findViewById(R.id.switchUserBtn);

        mPlayButton = (Button) findViewById(R.id.launchermainPlayButton);

        statusIsLaunching(false);


        initTabs(0);
        LauncherPreferences.DEFAULT_PREF.registerOnSharedPreferenceChangeListener((sharedPreferences, key) -> {
            if(key.equals("hideSidebar")){
                changeLookAndFeel(sharedPreferences.getBoolean("hideSidebar",false));
                return;
            }

            if(key.equals("ignoreNotch")){
                ignoreNotch(sharedPreferences.getBoolean("ignoreNotch", true), LauncherActivity.this);
                return;
            }
        });
        changeLookAndFeel(PREF_HIDE_SIDEBAR);
        ignoreNotch(PREF_IGNORE_NOTCH, LauncherActivity.this);
    }


    private void selectTabPage(int pageIndex) {
        if (pageIndex == 0) {
            if (viewPager.getCurrentItem() < (viewPageAdapter.getCount() - Tabs.length + 1)) {
                return;
            }

            viewPager.setCurrentItem(Vars.LAST_SERVER_TAB);
            setTabActive(pageIndex);
            return;
        }

        int page = (viewPageAdapter.getCount() - Tabs.length + 1) + pageIndex - 1;

        viewPager.setCurrentItem(page);
        setTabActive(pageIndex);
    }

    private void pickAccount() {
        try {
            mProfile = Profile.getCurrentProfileContent(this);
        } catch(Exception e) {
            mProfile = new MinecraftAccount();
            Tools.showError(this, e, true);
        }
    }

    public void statusIsLaunching(boolean isLaunching) {
        int launchVisibility = isLaunching ? View.VISIBLE : View.GONE;
        mLaunchProgress.setVisibility(launchVisibility);
        mLaunchTextStatus.setVisibility(launchVisibility);

        logoutBtn.setEnabled(!isLaunching);
        mVersionSelector.setEnabled(!isLaunching);
        canBack = !isLaunching;
    }

    public void onTabClicked(View view) {
        for (int i = 0; i < Tabs.length; i++) {
            if (view.getId() == Tabs[i].getId()) {
                selectTabPage(i);
                return;
            }
        }
    }

    private void setTabActive(int index) {
        for (Button tab : Tabs) {
            tab.setTypeface(null, Typeface.NORMAL);
            tab.setTextColor(Color.rgb(220,220,220)); //Slightly less bright white.
        }

        Tabs[index].setTypeface(Tabs[index].getTypeface(), Typeface.BOLD);
        Tabs[index].setTextColor(Color.WHITE);

        //Animating the white bar on the left
        ValueAnimator animation = ValueAnimator.ofFloat(selected.getX(), Tabs[index].getX());
        animation.setDuration(250);
        animation.addUpdateListener(animation1 -> selected.setX((float) animation1.getAnimatedValue()));
        animation.start();
    }

    protected void initTabs(int activeTab){
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            //Do something after 100ms
            selectTabPage(activeTab);
        }, 500);
    }

    private void changeLookAndFeel(boolean useOldLook){
        Guideline guideLine = findViewById(R.id.guidelineLeft);
        ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) guideLine.getLayoutParams();

        if(useOldLook || getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            //UI v1 Style
            //Hide the sidebar
            params.guidePercent = 0; // 0%, range: 0 <-> 1
            guideLine.setLayoutParams(params);

            //Remove the selected Tab
            selected.setVisibility(View.GONE);

            //Enlarge the button, but just a bit.
            params = (ConstraintLayout.LayoutParams) mPlayButton.getLayoutParams();
            params.matchConstraintPercentWidth = 0.35f;
        }else{
            //UI v2 Style
            //Show the sidebar back
            params.guidePercent = 0.23f; // 23%, range: 0 <-> 1
            guideLine.setLayoutParams(params);

            //Show the selected Tab
            selected.setVisibility(View.VISIBLE);

            //Set the default button size
            params = (ConstraintLayout.LayoutParams) mPlayButton.getLayoutParams();
            params.matchConstraintPercentWidth = 0.25f;
        }
        mPlayButton.setLayoutParams(params);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (Build.VERSION.SDK_INT >= P){
            //Get the fucking notch height:
            try {
                PREF_NOTCH_SIZE = getWindow().getDecorView().getRootWindowInsets().getDisplayCutout().getBoundingRects().get(0).width();
            }catch (Exception e){
                Log.i("NOTCH DETECTION", "No notch detected, or the device if in split screen mode");
            }
            Tools.updateWindowSize(this);
        }
    }

    public void toLeft(View view) {
        int cur = viewPager.getCurrentItem();
        if (cur - 1 < 0) return;

        viewPager.setCurrentItem(cur - 1, true);
    }

    public void toRight(View view) {
        int cur = viewPager.getCurrentItem();
        if (cur + 2 > viewPageAdapter.getCount()) return;

        viewPager.setCurrentItem(cur + 1, true);
    }
}

