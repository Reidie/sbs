package com.frma.sbs;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import java.util.logging.Logger;

public class MainActivity extends Activity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {
    Button install;
    CheckBox enable;
    CheckBox fillL;
    CheckBox fillP;
    SeekBar seekBar;
    TextView zoomFactor;
    int mZoom = 255;
    boolean pendingInstall = false;
    boolean pendingUninstall = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        install = (Button) findViewById(R.id.install);
        if (isInstalled()) {
            install.setText("Uninstall");
        }
        enable = (CheckBox) findViewById(R.id.enable);
        fillL = (CheckBox) findViewById(R.id.filll);
        fillP = (CheckBox) findViewById(R.id.fillp);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        zoomFactor = (TextView) findViewById(R.id.zommFactor);
        install.setOnClickListener(this);
        enable.setOnCheckedChangeListener(this);
        fillL.setOnCheckedChangeListener(this);
        fillP.setOnCheckedChangeListener(this);
        seekBar.setOnSeekBarChangeListener(this);
        seekBar.setProgress(255);
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
        if (id == R.id.action_settings) {
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
        fillL.setEnabled(inst);
        fillP.setEnabled(inst);
    }
    private void commit()
    {
        int val = 0;
        if(enable.isChecked()) {
            val = (mZoom << 4) + 1;
            if(fillL.isChecked()) {
                val |= 2;
            }
            if(fillP.isChecked()) {
                val |= 4;
            }
        }
        setSBSValue(val);

    }
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        //if(enable.isChecked()) MainNotification.notify(this, "Nothing", 0);
        //else MainNotification.cancel(this);
        commit();
    }
    private void uninstall() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Uninstall and restart UI ?")
                .setMessage("Do you want to uninstall SBS support and restart the UI")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doUninstall();
                    }

                })
                .setNegativeButton("No", null)
                .show();
    }
    private void install() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Install and restart UI ?")
                .setMessage("Do you want to install SBS support and restart the UI")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        doInstall();
                    }

                })
                .setNegativeButton("No", null)
                .show();
    }

    private void doInstall() {
	//getWindow().addFlags(0x80000000);
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Install SBS")
                .setMessage("Now press the power button")
                .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
			pendingInstall = false;
                    }

                })
                .show();
	pendingInstall = true;
    }
    private void doUninstall() {
	//getWindow().addFlags(0x80000000);
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Uninstall SBS")
                .setMessage("Now press the power button")
                .setPositiveButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
			pendingUninstall = false;
                    }

                })
                .show();
	pendingUninstall = true;
    }
    private void doInstall2() {
        int rv = -1;

        try {
            String srcso = this.getApplicationInfo().nativeLibraryDir + "/libsurfaceflinger.so";
            Logger.getLogger("SBS").info("srcso at " + srcso);
            Process process = Runtime.getRuntime().exec(new String[] {"su", "-mm", "-c",
                        "sleep 2 ; stop surfaceflinger && mount -o bind " + srcso + " /system/lib/libsurfaceflinger.so ; start surfaceflinger"});
            rv = process.waitFor();
            if(rv != 0)
                Toast.makeText(this, "Failed to install SBS, return code " + rv, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to run as root", Toast.LENGTH_LONG).show();
        }
    }
    private void doUninstall2() {
        int rv = -1;

        try {
            Process process = Runtime.getRuntime().exec(new String[] {"su", "-mm", "-c",
                    "sleep 2 ; pkill -9 surfaceflinger ; umount /system/lib/libsurfaceflinger.so "});
            rv = process.waitFor();
            if(rv != 0 && rv != 1)
                Toast.makeText(this, "Failed to uninstall SBS, return code " + rv, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to run as root", Toast.LENGTH_LONG).show();
        }
    }
    private boolean isInstalled() {
        int rv = -1;
        try {
            Process process = Runtime.getRuntime().exec(new String[] {"su", "-mm", "-c",
                    "grep /system/lib/libsurfaceflinger.so /proc/mounts"});
            rv = process.waitFor();
            if(rv != 0 && rv != 1)
                Toast.makeText(this, "Failed to check if SBS is installed, return code " + rv, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to run as root", Toast.LENGTH_LONG).show();
        }
        return rv == 0;
    }
    private void setSBSValue(int val)  {
        int rv = -1;
        //Toast.makeText(this, "Send value " + val, Toast.LENGTH_LONG).show();
        try {
            Process process = Runtime.getRuntime().exec(new String[] {"su", "-mm", "-c",
                    "service call SurfaceFlinger 4711 i32 " + val });
            rv = process.waitFor();
            if(rv != 0)
                Toast.makeText(this, "Failed to change SBS mode, return code " + rv, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Failed to run as root", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
        mZoom = progress;
        if(mZoom < 128)
            mZoom = 128;
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
	if(pendingInstall) {
	    doInstall2();
	}
	if(pendingUninstall) {
	    doUninstall2();
	}
	super.onPause();
    }
}
