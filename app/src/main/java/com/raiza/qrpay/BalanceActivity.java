package com.raiza.qrpay;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class BalanceActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_balance);

        setupToolbar();

        Intent intent = getIntent();
        Double balance = intent.getDoubleExtra("balance", -1);


        TextView tv = (TextView)findViewById(R.id.balanceText);
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.FRENCH);
        format.setCurrency(Currency.getInstance("MGA"));
        String amount = format.format(balance);
        tv.setText(Html.fromHtml(getString(R.string.balance_text, "<b>"+amount+"</b>")));

    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        //startActivity(new Intent(BalanceActivity.this, MainActivity.class));
        //finish();

    }
}
