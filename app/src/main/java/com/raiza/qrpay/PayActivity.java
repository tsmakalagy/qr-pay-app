package com.raiza.qrpay;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

import static android.R.attr.password;

public class PayActivity extends BaseActivity {

    private Long rec_usr_id;
    private String rec_usr_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay);
        setupToolbar();

        Intent intent = getIntent();
        rec_usr_id = intent.getLongExtra("rec_usr_id", -1);
        rec_usr_name = intent.getStringExtra("rec_usr_name");
    }

    public void payNow(View view) {
        EditText mPayField = (EditText) findViewById(R.id.pay_value);
        String p_value_str = mPayField.getText().toString();
        if (TextUtils.isEmpty(p_value_str)) {
            mPayField.setError("Required.");
            return;
        } else {
            mPayField.setError(null);
        }


        float pay_value = Float.valueOf(p_value_str);

        if (rec_usr_id != null && rec_usr_id > 0 && rec_usr_name != null && pay_value > 0) {
            Intent intent = new Intent(this, ConfirmActivity.class);
            intent.putExtra("rec_usr_id", rec_usr_id);
            intent.putExtra("rec_usr_name", rec_usr_name);
            intent.putExtra("trans_amount", pay_value);
            startActivity(intent);
            finish();
        }
    }
}
