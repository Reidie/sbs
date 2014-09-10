package com.frma.sbs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

class SBSException extends Exception {
    public int mErrcode;
    SBSException(String msg, int errcode) {
        super(msg);
        mErrcode = errcode;
    }
    public int getErrCode() {
        return mErrcode;
    }
}

public class MainActivity extends Activity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener, SeekBar.OnSeekBarChangeListener {
    private SharedPreferences mPrefs;
    private int mScreenHeight;
    private int mScreenWidth;

    private Button mInstallBtn;
    private Button mRebootBtn;
    private CheckBox mLoadNextCB;
    private CheckBox mPermanentCB;
    private TextView mCurrentStatusTV;
    private ToggleButton mActivateTB;
    private SeekBar mZoomSeekBar;
    private TextView mZoomFactor;
    private SeekBar mImgDistSeekBar;
    private TextView mImgDistValue;
    private int mImgDistance;
    private DisplayMetrics mDisplayMetrics;

    private int mZoom;
    private boolean mEnabled = false;
    private ProgressDialog mProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get screen width and height
        mDisplayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(mDisplayMetrics);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);


        mInstallBtn = (Button)findViewById(R.id.install);
        mRebootBtn =  (Button)findViewById(R.id.reboot);
        mLoadNextCB = (CheckBox)findViewById(R.id.loadNextBoot);
        mPermanentCB = (CheckBox)findViewById(R.id.enablePermanent);
        mCurrentStatusTV = (TextView)findViewById(R.id.activeStatus);
        mActivateTB = (ToggleButton)findViewById(R.id.activateTB);
        mZoomSeekBar = (SeekBar) findViewById(R.id.zoomSeekBar);
        mZoomFactor = (TextView) findViewById(R.id.zoomValue);

        mImgDistSeekBar = (SeekBar) findViewById(R.id.marginSeekBar);
        mImgDistValue = (TextView) findViewById(R.id.marginValue);

        mInstallBtn.setOnClickListener(this);
        mRebootBtn.setOnClickListener(this);

        mLoadNextCB.setOnCheckedChangeListener(this);
        mPermanentCB.setOnCheckedChangeListener(this);

        mActivateTB.setOnCheckedChangeListener(this);

        mZoomSeekBar.setOnSeekBarChangeListener(this);
        mZoom = mPrefs.getInt("zoom", 255);
        mZoomSeekBar.setProgress(mZoom);

        mImgDistSeekBar.setOnSeekBarChangeListener(this);
        mImgDistance = mPrefs.getInt("imgdist", 60);
        mImgDistSeekBar.setProgress(mImgDistance);

        mImgDistSeekBar.setMax((int) ((float) mDisplayMetrics.heightPixels / mDisplayMetrics.ydpi * 25.4));

        mProgress = new ProgressDialog(this);

        copyAssets("armeabi");

        updateStatus();
    }
    boolean mInItemUpdate = false;
    private void updateStatus() {
        new AsyncTaskWUI("Reading status...") {
            boolean installed;
            boolean loadOnBoot;
            boolean loadOnBootPermanent;
            boolean loaded;

            @Override
            protected Integer doInBackground(Void... voids) {
                installed = isInstalled();
                loadOnBoot = isLoadOnBootEnabled();
                loadOnBootPermanent = isLoadOnBootPermanent();
                loaded = isLoaded();
                return 0;
            }

            @Override
            protected void onPostExecute(Integer integer) {
                super.onPostExecute(integer);
                mInItemUpdate = true;
                if (installed)
                    mInstallBtn.setText("Uninstall");
                else
                    mInstallBtn.setText("Install");
                mLoadNextCB.setChecked(loadOnBoot);
                mPermanentCB.setChecked(loadOnBootPermanent);
                String statusText = "SBS ";
                statusText += (installed ?
                               ("is installed" + (loaded ? " and loaded": " but not loaded")) :
                               "is not installed");
                mCurrentStatusTV.setText(statusText);
                mZoomSeekBar.setEnabled(loaded);
                mImgDistSeekBar.setEnabled(loaded);
                mActivateTB.setEnabled(loaded);
                mInItemUpdate = false;
            }
        }.execute();
    }
    private void commit() {
        new AsyncTaskWUIandE("Updating...") {
            @Override
            protected Integer doInBackgroundE(Void... voids) throws SBSException {
                int rv = -1;
                rv = runAndCheckSBSCmd(String.format("set %d %d %d", mEnabled ? 1 : 0, mZoom,
                        (int) (mImgDistance / 25.4 * mDisplayMetrics.xdpi)));
                return rv;
            }

            @Override
            protected void onPostExecute(Integer integer) {
                super.onPostExecute(integer);
            }
        }.execute();
    }
    @Override
    public void onClick(View v) {
        if(v.equals(mInstallBtn)) {
            if (isInstalled())
                uninstall();
            else
                install();
        }
        else if(v.equals(mRebootBtn)) {
            new AsyncTaskWUIandE("Rebooting...") {
                @Override
                protected Integer doInBackgroundE(Void... voids) throws Exception {
                    doReboot();
                    Thread.sleep(100000);
                    return 0;
                }
            }.execute();

        }
        updateStatus();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        final boolean fIsChecked = isChecked;

        if(mInItemUpdate)
            return;
        if(buttonView.equals(mLoadNextCB)) {
            new AsyncTaskWUIandE(fIsChecked?"Enabling...":"Disabling...")  {
                @Override
                protected Integer doInBackgroundE(Void... voids) throws SBSException {
                    int rv = -1;
                    if (fIsChecked) rv = runAndCheckSBSCmd("enable");
                    else rv = runAndCheckSBSCmd("disable");
                    return rv;
                }

                @Override
                protected void onPostExecute(Integer integer) {
                    super.onPostExecute(integer);
                    mInItemUpdate = true;
                    if(!mLoadNextCB.isChecked())
                        mPermanentCB.setChecked(false);
                    mInItemUpdate = false;
                }
            }.execute();
        }
        if(buttonView.equals(mPermanentCB)) {
            new AsyncTaskWUIandE(isChecked?"Enabling...":"Disabling...") {
                @Override
                protected Integer doInBackgroundE(Void... voids) throws Exception {
                    int rv = -1;
                    if (fIsChecked) rv = runAndCheckSBSCmd("enablepermanent");
                    else rv = runAndCheckSBSCmd("disable");
                    return rv;
                }

                    @Override
                protected void onPostExecute(Integer integer) {
                    super.onPostExecute(integer);
                        mInItemUpdate = true;
                        mLoadNextCB.setChecked(mPermanentCB.isChecked());
                        mInItemUpdate = false;
                }
            }.execute();
        };
        if(buttonView.equals(mActivateTB)) {
            mEnabled = isChecked;
            mPrefs.edit().putBoolean("enabled", mEnabled).commit();
            commit();
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean b) {
        if (seekBar == mZoomSeekBar) {
            mZoom = progress;
            if (mZoom < 128)
                mZoom = 128;
            mPrefs.edit().putInt("zoom", mZoom).commit();
            mZoomFactor.setText("" + 100 * mZoom / 255 + "%");
        } else if (seekBar == mImgDistSeekBar) {
            mImgDistance = progress;
            mPrefs.edit().putInt("imgdist", mImgDistance).commit();
            mImgDistValue.setText(mImgDistance + "mm");
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if(mEnabled)
            commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        sendReport();
        return super.onMenuItemSelected(featureId, item);
    }

    private void uninstall() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Uninstall and reboot device ?")
                .setMessage("Do you want to uninstall SBS support and restart the device")
                .setPositiveButton("Go", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new AsyncTaskWUIandE("Uninstalling...") {
                            @Override
                            protected Integer doInBackgroundE(Void... voids) throws SBSException {
                                doUninstall();
                                return null;
                            }
                        }.execute();
                    }

                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void install() {
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Install and restart UI ?")
                .setMessage("Do you want to install SBS support and restart the device")
                .setPositiveButton("Go", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new AsyncTaskWUIandE("Installing...") {

                            @Override
                            protected Integer doInBackgroundE(Void... voids) throws Exception {
                                doInstall();
                                return 0;
                            }


                        }.execute();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
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
    private int runSBSCmd(String cmd) {
        String path = getFilesDir().getAbsolutePath();
        return runAsRoot(path + "/sbs.sh " + cmd);
    }
    private int runAndCheckSBSCmd(String cmd) throws SBSException {
        int rv = runSBSCmd(cmd);
        if(rv != 0) {
            throw new SBSException("SBS Command failed with error " + rv, rv);
        }
        return rv;
    }
    private void doInstall() throws SBSException {
        int rv = 0;
        if(!isInstalled())
            rv = runAndCheckSBSCmd("install");
        if(rv == 0)
            rv = runAndCheckSBSCmd("enable");
        if(rv == 0)
            rv = runAndCheckSBSCmd("reboot");
    }
    private void doUninstall() throws SBSException
    {
        int rv = 0;
        if(isInstalled())
            rv = runAndCheckSBSCmd("uninstall");
        if(rv == 0)
            rv = runAndCheckSBSCmd("reboot");
    }
    private void doReboot() throws SBSException
    {
        runAndCheckSBSCmd("reboot");
    }
    private boolean isInstalled() {
        int rv = -1;
        rv = runSBSCmd("isinstalled");
        return rv == 0;
    }
    private boolean isLoadOnBootEnabled() {
        int rv = -1;
        rv = runSBSCmd("isenabled");
        return rv == 0;
    }
    private boolean isLoadOnBootPermanent() {
        int rv = -1;
        rv = runSBSCmd("ispermanent");
        return rv == 0;
    }
    private boolean isLoaded() {
        int rv = -1;
        rv = runSBSCmd("isloaded");
        return rv == 0;
    }

    // Asset stuff
    private void copyAssets(String path) {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list(path);
        } catch (IOException e) {
            loge("Failed to get asset file list.");
        }
        for(String filename : files) {
            InputStream in = null;
            OutputStream out = null;
            try {

                in = assetManager.open(path + "/" + filename);
                File outFile = new File(getFilesDir(), filename);
                out = new FileOutputStream(outFile);
                logd("copy " + outFile);
                copyFile(in, out);
                in.close();
                in = null;
                out.flush();
                out.close();
                out = null;
                Runtime.getRuntime().exec( "chmod 755 " + outFile.getAbsolutePath());
            } catch(IOException e) {
                loge("Failed to copy asset file: " + filename);
            }
        }
    }
    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    // Logg stuff

    private void logd(String msg) {
        Log.d("SBS", msg);
    }
    private void logi(String msg) {
        Log.i("SBS", msg);
    }
    private void loge(String msg)
    {
        Log.e("SBS", msg);
    }

    // Async  helper classes
    abstract class AsyncTaskWUI extends AsyncTask<Void,String,Integer> {
        ProgressDialog mProgress;
        String mText;
        AsyncTaskWUI(String text) {
            super();
            mText = text;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgress = ProgressDialog.show(MainActivity.this, "Wait...", mText, true);
        }

        @Override
        protected void onPostExecute(Integer integer) {
            mProgress.dismiss();
        }
    };
    abstract class AsyncTaskWUIandE extends AsyncTaskWUI {
        protected Exception mException;

        AsyncTaskWUIandE(String text) {
            super(text);
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mException = null;
        }
        abstract protected Integer doInBackgroundE(Void... voids) throws Exception;
        @Override
        protected Integer doInBackground(Void... voids) {
            try {
                doInBackgroundE(voids);
            }
            catch(Exception e) {
                mException = e;
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            if(mException != null) {
                new AlertDialog.Builder(MainActivity.this)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle("SBS Failed")
                        .setMessage(mException.getMessage())
                        .show();
                //sendReport();
            }
        }
    };
    private void sendReport() {
        final Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
        emailIntent.setType("plain/text");
        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL,
                new String[] { "fredrik.markstrom@gmail.com" });
        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                "A log from SBS");
        Uri uri = Uri.parse("file:///"+getFilesDir()+"/sbs-log.txt");
        emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
        emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,
                "This is a log from SBS");
        this.startActivity(Intent.createChooser(emailIntent, "Sending email..."));

    }
}
