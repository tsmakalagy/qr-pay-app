package com.raiza.qrpay;

import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.zxing.Result;

import org.json.JSONObject;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class SimpleScannerActivity extends BaseActivity implements  ZXingScannerView.ResultHandler {

    private static final String FLASH_STATE = "FLASH_STATE";
    private static final String AUTO_FOCUS_STATE = "AUTO_FOCUS_STATE";

    private ZXingScannerView mScannerView;

    private boolean mFlash;
    private boolean mAutoFocus;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState != null) {
            mFlash = savedInstanceState.getBoolean(FLASH_STATE, false);
            mAutoFocus = savedInstanceState.getBoolean(AUTO_FOCUS_STATE, true);
        } else {
            mFlash = false;
            mAutoFocus = true;
        }

        setContentView(R.layout.activity_simple_scanner);
        setupToolbar();

        ViewGroup contentFrame = (ViewGroup) findViewById(R.id.content_frame);
        mScannerView = new ZXingScannerView(this);
        contentFrame.addView(mScannerView);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mScannerView != null)
            mScannerView.stopCamera();           // Stop camera on pause
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mScannerView != null) {
            //mScannerView.resumeCameraPreview(this);
            mScannerView.setResultHandler(this);
            mScannerView.startCamera();
            mScannerView.setFlash(mFlash);
            mScannerView.setAutoFocus(mAutoFocus);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(FLASH_STATE, mFlash);
        outState.putBoolean(AUTO_FOCUS_STATE, mAutoFocus);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem menuItem;


        if(mFlash) {
            menuItem = menu.add(Menu.NONE, R.id.menu_flash, 0, R.string.flash_on);
        } else {
            menuItem = menu.add(Menu.NONE, R.id.menu_flash, 0, R.string.flash_off);
        }
        MenuItemCompat.setShowAsAction(menuItem, MenuItem.SHOW_AS_ACTION_NEVER);


        if(mAutoFocus) {
            menuItem = menu.add(Menu.NONE, R.id.menu_auto_focus, 0, R.string.auto_focus_on);
        } else {
            menuItem = menu.add(Menu.NONE, R.id.menu_auto_focus, 0, R.string.auto_focus_off);
        }
        MenuItemCompat.setShowAsAction(menuItem, MenuItem.SHOW_AS_ACTION_NEVER);


        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_logout).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle presses on the action bar items
        switch (item.getItemId()) {
            case R.id.menu_flash:
                mFlash = !mFlash;
                if(mFlash) {
                    item.setTitle(R.string.flash_on);
                } else {
                    item.setTitle(R.string.flash_off);
                }
                mScannerView.setFlash(mFlash);
                return true;
            case R.id.menu_auto_focus:
                mAutoFocus = !mAutoFocus;
                if(mAutoFocus) {
                    item.setTitle(R.string.auto_focus_on);
                } else {
                    item.setTitle(R.string.auto_focus_off);
                }
                mScannerView.setAutoFocus(mAutoFocus);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void handleResult(Result rawResult) {
        // Do something with the result here
        String resStr = rawResult.getText();

        if (resStr.contains(getString(R.string.base_url_api))) {
            volleyJsonUserRequest(resStr);
        } else {
            String msg = getString(R.string.url_not_found);
            displayError(msg);
        }

    }

    public void volleyJsonUserRequest(String url){

        String  REQUEST_TAG = url;

        JsonObjectRequest jsonObjectReq = new JsonObjectRequest(url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Long usr_id = response.getJSONArray("users").getJSONObject(0).getLong("user_id");
                            String usr_name = response.getJSONArray("users").getJSONObject(0).getString("user_name");
                            Intent intent = new Intent(SimpleScannerActivity.this, PayActivity.class);
                            intent.putExtra("rec_usr_id", usr_id);
                            intent.putExtra("rec_usr_name", usr_name);
                            startActivity(intent);
                            finish();

                        } catch (Exception e) {
                            Log.d("Exception", e.getMessage());
                            String msg = e.getMessage();
                            displayError(msg);
                        }


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d("JSONRequest", "Error: " + error.getMessage());
                String msg = error.getMessage();
                if (msg == null) msg = getString(R.string.unexpected_error);
                displayError(msg);
            }
        });

        // Adding JsonObject request to request queue
        AppSingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectReq,REQUEST_TAG);
    }

    private void displayError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mScannerView.resumeCameraPreview(SimpleScannerActivity.this);
            }
        }, 2000);
    }
}
