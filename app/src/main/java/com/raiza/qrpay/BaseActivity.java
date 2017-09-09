package com.raiza.qrpay;

/**
 * Created by raiza on 8/31/17.
 */

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

import static com.raiza.qrpay.R.id.successText;

public class BaseActivity extends AppCompatActivity {

    private ProgressBar progressView;

    public void setupToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);
        /*final ActionBar ab = getSupportActionBar();
        if(ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }*/
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the main; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:
                signOut();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void signOut() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signOut();
        Long user_id = getUserId();
        if (user_id != null && user_id > 0) {
            removeSessions();
        }
        Intent intent = new Intent(this, MailAuthActivity.class);
        startActivity(intent);
        finish();
    }

    public void removeSessions() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.apply();

    }

    public Long getUserId() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        return preferences.getLong("user_id", -1);
    }

    public void hideProgressView() {
        // ProgressView
        if (progressView == null)
            progressView = (ProgressBar) findViewById(R.id.progress_view);
        progressView.setVisibility(View.INVISIBLE);
    }

    public void showProgressView() {
        if (progressView == null)
            progressView = (ProgressBar) findViewById(R.id.progress_view);
        progressView.setVisibility(View.VISIBLE);
    }



}