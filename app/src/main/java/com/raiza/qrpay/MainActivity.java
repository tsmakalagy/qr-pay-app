package com.raiza.qrpay;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.json.JSONObject;

public class MainActivity extends BaseActivity {

    private static final int ZXING_CAMERA_PERMISSION = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
        hideProgressView();

    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Intent intent = new Intent(MainActivity.this, PhoneAuthActivity.class);
            startActivity(intent);
            finish();
        }

    }

    public void launchScanner(View view) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA}, ZXING_CAMERA_PERMISSION);
        } else {
            Intent intent = new Intent(this, SimpleScannerActivity.class);
            startActivity(intent);

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case ZXING_CAMERA_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Intent intent = new Intent(this, SimpleScannerActivity.class);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Please grant camera permission to use the QR Scanner", Toast.LENGTH_SHORT).show();
                }
                return;
        }
    }

    public void checkBalance(View v) {
        Long usr_id = getUserId();
        showProgressView();
        if (usr_id != null && usr_id > 0) {
            String url = getString(R.string.base_url_api) + "balance/" + usr_id;
            volleyJsonObjectRequest(url);
        }
    }

    public void createQRCode(View v) {
        Toast.makeText(this, getString(R.string.generating_qr_code), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, GenerateQRActivity.class);
        startActivity(intent);

    }

    public void volleyJsonObjectRequest(String url){

        String  REQUEST_TAG = url;

        JsonObjectRequest jsonObjectReq = new JsonObjectRequest(url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Log.e("Response", response.toString(4));
                            Double balance = response.getDouble("balance");
                            if (balance != null) {
                                Intent intent = new Intent(MainActivity.this, BalanceActivity.class);
                                intent.putExtra("balance", balance);
                                startActivity(intent);
                                //finish();
                            }

                        } catch (Exception e) {
                            Log.e("JSONException", e.getMessage());
                            displayError(e.getMessage());
                        }
                        hideProgressView();


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d("JSONRequest", "Error: " + error.getMessage());
                String msg = error.getMessage();
                if (msg == null) msg = "Unexpted error were encountered!";
                displayError(msg);
            }
        });

        // Adding JsonObject request to request queue
        AppSingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectReq,REQUEST_TAG);

    }

    private void displayError(String msg) {
        hideProgressView();
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }


}
