package com.raiza.qrpay;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;


public class ConfirmActivity extends BaseActivity {

    private Long rec_usr_id;
    private String rec_usr_name;
    private float trans_amount;
    private String usr_password;
    private int type = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_confirm);
        setupToolbar();

        Intent intent = getIntent();

        rec_usr_id = intent.getLongExtra("rec_usr_id", -1);
        rec_usr_name = intent.getStringExtra("rec_usr_name");
        trans_amount = intent.getFloatExtra("trans_amount", -1);

        hideProgressView();
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.FRENCH);
        format.setCurrency(Currency.getInstance("MGA"));

        TextView confirmText = (TextView)findViewById(R.id.confirmText);
        if (rec_usr_name == null || TextUtils.isEmpty(rec_usr_name)) rec_usr_name = "Unknown";
        if (rec_usr_id != null && rec_usr_id > 0 && trans_amount > 0) {
            String amount = format.format(trans_amount);
            confirmText.setText(Html.fromHtml(getString(R.string.pay_confirm_text, "<b>"+amount+"</b>", "<b>"+rec_usr_name.toUpperCase()+"</b>")));
        }
    }

    public void finalizeTrans(View view) {
        EditText editText = (EditText) findViewById(R.id.passwordText);
        usr_password = editText.getText().toString();
        String url = getString(R.string.base_url_api) + "create_transaction";

        if (rec_usr_name == null || TextUtils.isEmpty(rec_usr_name)) rec_usr_name = "Unknown";

        if (rec_usr_id != null && rec_usr_id > 0 && usr_password != null && trans_amount > 0) {
            showProgressView();
            volleyJsonObjectRequest(url);
        }
    }

    public void volleyJsonObjectRequest(String url){

        String  REQUEST_TAG = url;
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("password", usr_password);
        params.put("amount", String.valueOf(trans_amount));
        params.put("emitter", String.valueOf(getUserId()));
        params.put("receiver", String.valueOf(rec_usr_id));
        params.put("type", String.valueOf(type));

        JsonObjectRequest jsonObjectReq = new JsonObjectRequest(url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            VolleyLog.v("Response:%n %s", response.toString(4));
                            Log.e("Response", response.toString(4));
                            boolean success = false;
                            String msg = "Error";

                            Iterator<String> keys = response.keys();
                            while (keys.hasNext()) {
                                String key = keys.next();
                                if (!key.contains("error")) {
                                    success = true;
                                } else {
                                    success = false;
                                }
                                msg = response.get(key).toString();
                            }
                            if (!success) {
                                TextView errorConfirmText = (TextView)findViewById(R.id.errorConfirmText);
                                errorConfirmText.setText(msg);
                            } else {
                                Intent intent = new Intent(ConfirmActivity.this, SuccessActivity.class);
                                intent.putExtra("rec_usr_name", rec_usr_name);
                                intent.putExtra("trans_amount", trans_amount);
                                startActivity(intent);
                                finish();
                            }
                            Log.e("JSONObject", msg);
                        } catch (JSONException e) {
                            Log.e("JSONException", e.getMessage());
                        }
                        hideProgressView();


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.e("Error: ", error.getMessage());
                Log.e("ResponseError", error.getMessage());

            }
        });


        // Adding JsonObject request to request queue
        AppSingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectReq,REQUEST_TAG);
    }
}
