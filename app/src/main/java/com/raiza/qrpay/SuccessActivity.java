package com.raiza.qrpay;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

import static com.raiza.qrpay.R.id.confirmText;

public class SuccessActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        setupToolbar();
        hideProgressView();

        Intent intent = getIntent();
        String rec_usr_name = intent.getStringExtra("rec_usr_name");
        float trans_amount = intent.getFloatExtra("trans_amount", -1);
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.FRENCH);
        format.setCurrency(Currency.getInstance("MGA"));
        String amount = format.format(trans_amount);

        TextView successText = (TextView)findViewById(R.id.successText);
        successText.setText(Html.fromHtml(getString(R.string.success_text, "<b>"+amount+"</b>", "<b>"+rec_usr_name.toUpperCase()+"</b>")));
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        //startActivity(new Intent(SuccessActivity.this, MainActivity.class));
        //finish();

    }

    public void checkBalance(View v) {
        Long usr_id = getUserId();
        if (usr_id != null && usr_id > 0) {
            showProgressView();
            String url = getString(R.string.base_url_api) + "balance/" + usr_id;
            volleyJsonObjectRequest(url);
        }
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
                                Intent intent = new Intent(SuccessActivity.this, BalanceActivity.class);
                                intent.putExtra("balance", balance);
                                startActivity(intent);
                                finish();
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
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
