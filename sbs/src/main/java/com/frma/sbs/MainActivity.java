package com.frma.sbs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import java.util.logging.Logger;

public class MainActivity extends Activity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {
    private Button install;
    private CheckBox enable;
    private SeekBar seekBar;
    private TextView zoomFactor;
    private SharedPreferences mPrefs;

    // What to do when power button is pressed
    enum PendingOperation {
        NONE,
        INSTALL,
        UNINSTALL,
        RESTART
    };
    private PendingOperation mPendingOperation = PendingOperation.NONE;
    private boolean mOverrideInstalledTest = false;
    private int mZoom;
    private boolean mEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        install = (Button) findViewById(R.id.install);
        enable = (CheckBox) findViewById(R.id.enable);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        zoomFactor = (TextView) findViewById(R.id.zommFactor);
        install.setOnClickListener(this);
        enable.setOnCheckedChangeListener(this);
        seekBar.setOnSeekBarChangeListener(this);
        mZoom = mPrefs.getInt("zoom", 255);
        seekBar.setProgress(mZoom);

        mEnabled = mPrefs.getBoolean("enabled", false);
        enable.setChecked(mEnabled);
        updateStatus();


/*
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Unsupported hardware")
                .setMessage("SBS support has not been tested on this hardware, it might still work and " +
                        "the worst thing that can happen is that you need to reboot the device")
                .setPositiveButton("Go !", null)
                .setNegativeButton("Bail", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
              .show();
*/
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch(id) {
            case R.id.action_restart_framework:
                promptRestartFramework();
                return true;
            case R.id.action_override_install_check:
                mOverrideInstalledTest = true;
                updateStatus();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onClick(View v) {
        if(isInstalled())
            uninstall();
        else
            install();
        updateStatus();
    }
    private void updateStatus() {
        boolean inst = isInstalled();
        enable.setEnabled(inst);
        if(inst)
            install.setText("Uninstall");
        else
            install.setText("Install");
    }
    private void commit()
    {
        int val = 0;
        if(mEnabled) {
            val = (mZoom << 4) + 1;
        }
        setSBSValue(val);
    }
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        //if(enable.isChecked()) MainNotification.notify(this, "Nothing", 0);
        //else MainNotification.cancel(this);
        mEnabled = isChecked;
        mPrefs.edit().putBoolean("enabled", mEnabled);
        commit();
    }
    private void uninstall() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Uninstall and reboot device ?")
                .setMessage("Do you want to uninstall SBS support and restart the device")
                .setPositiveButton("Go", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doUninstall();
                    }

                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void install() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Install and restart UI ?")
                .setMessage("Do you want to install SBS support and restart the UI")
                .setPositiveButton("Go", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doInstall();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void promptRestartFramework() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Restart framework")
                .setMessage("You are about to restart the android framework, it's used to confirm if it's" +
                            " the SBS functionality that is broken or if the android framework doesn't restart correctly.")
                .setPositiveButton("Go", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doRestartFramework();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void promptInstall() {
	//getWindow().addFlags(0x80000000);
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Install SBS")
                .setMessage("This might fail, if it fails ten times in a row try restart framework instead to to help diagnose. Now press the power button to install")
                .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
			            mPendingOperation = PendingOperation.NONE;
                    }

                })
                .show();
	    mPendingOperation = PendingOperation.INSTALL;
    }
    private void promptUninstall() {
	//getWindow().addFlags(0x80000000);
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Uninstall SBS")
                .setMessage("Now press the power button")
                .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
			            mPendingOperation = PendingOperation.NONE;
                    }

                })
                .show();
                mPendingOperation = PendingOperation.UNINSTALL;
    }
    private int runAsRoot(String cmd) {
        int rv = -1;
        logi("RunAsRoot: " + cmd);
        try {
            Process process =
                Runtime.getRuntime().exec(new String[] {"su", "-mm", "-c", cmd});
            rv = process.waitFor();
            logi("runAsRoot returned " + rv);
        } catch (Exception e) {
            logi("Failed to run as root with exception: " + e.getMessage());
            Toast.makeText(this, "Failed to run as root", Toast.LENGTH_LONG).show();
        }
        return rv;
    }
    private void doInstall() {
        String srcso = this.getApplicationInfo().nativeLibraryDir + "/libsurfaceflinger.so";
        logi("srcso at " + srcso);
        runAsRoot("mount -o bind " + srcso + " /system/lib/libsurfaceflinger.so ; restart &");
    }
    private void doUninstall() {
        runAsRoot("reboot &");
    }
    private void doRestartFramework() {
        runAsRoot("restart & ");
    }
    private boolean isInstalled() {
        int rv = -1;
        if(mOverrideInstalledTest)
            return true;
        rv = runAsRoot("grep /system/lib/libsurfaceflinger.so /proc/mounts");
        return rv == 0;
    }
    private void setSBSValue(int val)  {
        runAsRoot("service call SurfaceFlinger 4711 i32 " + val);
    }
    private void setSBSValues(int flags, int portraitZoom, int landscapeZoom)  {
        int rv = -1;

        //Toast.makeText(this, "Send value " + val, Toast.LENGTH_LONG).show();
        try {

        } catch (Exception e) {
            Toast.makeText(this, "Failed to run as root", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
        mZoom = progress;
        if(mZoom < 128)
            mZoom = 128;
        mPrefs.edit().putInt("zoom", mZoom);
        zoomFactor.setText("" + 100*mZoom/255 + "%");
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        commit();
    }

    @Override 
    public void onPause() {
        switch (mPendingOperation) {
            case INSTALL:
                doInstall();
                break;
            case UNINSTALL:
                doUninstall();
                break;
            case RESTART:
                doRestartFramework();
                break;
        }
    	super.onPause();
    }
    private void logi(String msg) {
        Logger.getLogger("SBS").info(msg);
    }
}
