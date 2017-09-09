package com.raiza.qrpay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import static android.R.attr.phoneNumber;

public class PhoneAuthActivity extends BaseActivity implements
        View.OnClickListener {

    EditText mPhoneNumberField, mVerificationField;
    Button mStartButton, mVerifyButton, mResendButton, cEmailAuthButton;
    TextView textPhoneCode;

    private FirebaseAuth mAuth;
    private PhoneAuthProvider.ForceResendingToken mResendToken;
    private PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks;
    String mVerificationId, user_fb_id, phone_number, app_token, user_email;
    Long user_id;


    private static final String TAG = "PhoneAuthActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phone_auth);

        hideProgressView();
        setupToolbar();

        app_token = FirebaseInstanceId.getInstance().getToken();

        mPhoneNumberField = (EditText) findViewById(R.id.field_phone_number);
        mVerificationField = (EditText) findViewById(R.id.field_verification_code);

        mVerificationField.setVisibility(View.GONE);

        mStartButton = (Button) findViewById(R.id.button_start_verification);
        mVerifyButton = (Button) findViewById(R.id.button_verify_phone);
        mResendButton = (Button) findViewById(R.id.button_resend);
        cEmailAuthButton = (Button) findViewById(R.id.c_email_auth_button);

        textPhoneCode = (TextView) findViewById(R.id.text_phone_code);

        mVerifyButton.setVisibility(View.GONE);
        mResendButton.setVisibility(View.GONE);

        mStartButton.setOnClickListener(this);
        mVerifyButton.setOnClickListener(this);
        mResendButton.setOnClickListener(this);
        cEmailAuthButton.setOnClickListener(this);

        mAuth = FirebaseAuth.getInstance();
        mCallbacks = new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override
            public void onVerificationCompleted(PhoneAuthCredential credential) {
                Log.d(TAG, "onVerificationCompleted:" + credential);
                hideProgressView();
                signInWithPhoneAuthCredential(credential);
            }

            @Override
            public void onVerificationFailed(FirebaseException e) {
                Log.w(TAG, "onVerificationFailed", e);
                hideProgressView();
                if (e instanceof FirebaseAuthInvalidCredentialsException) {
                    mPhoneNumberField.setError("Invalid phone number.");
                } else if (e instanceof FirebaseTooManyRequestsException) {
                    Snackbar.make(findViewById(android.R.id.content), "Quota exceeded.",
                            Snackbar.LENGTH_SHORT).show();
                } else {
                    displayError(getString(R.string.unexpected_error));
                }
            }

            @Override
            public void onCodeSent(String verificationId,
                                   PhoneAuthProvider.ForceResendingToken token) {
                Log.d(TAG, "onCodeSent:" + verificationId);
                mVerificationId = verificationId;
                mResendToken = token;
                mPhoneNumberField.setVisibility(View.GONE);
                findViewById(R.id.text_phone_code).setVisibility(View.GONE);
                mVerificationField.setVisibility(View.VISIBLE);
                mVerificationField.requestFocus();
                mStartButton.setVisibility(View.GONE);
                mVerifyButton.setVisibility(View.VISIBLE);
                hideProgressView();
            }
        };


    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_logout).setVisible(false);
        return true;
    }

    private void signInWithPhoneAuthCredential(final PhoneAuthCredential credential) {
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = task.getResult().getUser();
                            //phone_number = mPhoneNumberField.getText().toString();
                            user_fb_id = user.getUid();
                            Log.d(TAG, "signInWithCredential:User_Email: " + user.getEmail());

                            String url = getString(R.string.base_url_api) + "check_user_phone";
                            volleyJsonCheckUserRequest(url);

                            //startActivity(new Intent(PhoneAuthActivity.this, MainActivity.class));
                            //finish();
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                                mVerificationField.setError("Invalid code.");
                            }
                        }
                        hideProgressView();
                    }
                });
    }

    private void volleyJsonCheckUserRequest(String url){
        String  REQUEST_TAG = url;
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("phone_number", phone_number);
        params.put("fb_id", user_fb_id);
        params.put("app_token", app_token);

        JsonObjectRequest jsonObjectReq = new JsonObjectRequest(url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            //Log.w("Response", response.toString(4));
                            // Web server return JSON of format {"user_id": 1}
                            user_id = response.getLong("user_id");
                            user_email= response.getString("user_email");
                            if (user_id != null && user_id > 0 && user_email != null) { // Launch MainActivity
                                setSessions();
                                Intent intent = new Intent(PhoneAuthActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            } else { // user with phone number doesnt exist yet, send to MailAuthActivity
                                Intent intent = new Intent(PhoneAuthActivity.this, MailAuthActivity.class);
                                intent.putExtra("verification_id", mVerificationId);
                                intent.putExtra("phone_number", phone_number);
                                startActivity(intent);
                                finish();
                            }


                        } catch (Exception e) {
                            Log.w("Exception", e.getMessage());
                            String msg = e.getMessage();
                            displayError(msg);
                        }


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d("JSONRequest", "Error: " + error.getMessage());
                Log.w("JSONRequest", error.getMessage());
                String msg = error.getMessage();
                if (msg == null) msg = getString(R.string.unexpected_error);
                displayError(msg);
                hideProgressView();
            }
        });

        // Adding JsonObject request to request queue
        AppSingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectReq,REQUEST_TAG);
    }


    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks);        // OnVerificationStateChangedCallbacks
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        signInWithPhoneAuthCredential(credential);
    }

    private void resendVerificationCode(String phoneNumber,
                                        PhoneAuthProvider.ForceResendingToken token) {
        PhoneAuthProvider.getInstance().verifyPhoneNumber(
                phoneNumber,        // Phone number to verify
                60,                 // Timeout duration
                TimeUnit.SECONDS,   // Unit of timeout
                this,               // Activity (for callback binding)
                mCallbacks,         // OnVerificationStateChangedCallbacks
                token);             // ForceResendingToken from callbacks
    }

    private boolean validatePhoneNumber() {
        phone_number = mPhoneNumberField.getText().toString();
        if (TextUtils.isEmpty(phone_number)) {
            mPhoneNumberField.setError("Empty phone number.");
            return false;
        }
        if (phone_number.length() < 9) {
            mPhoneNumberField.setError("Invalid length.");
            return false;
        }
        if (!phone_number.startsWith("0")) {
            if (!phone_number.startsWith("3")) {
                mPhoneNumberField.setError("Invalid phone number.");
                return false;
            } else {
                phone_number = getString(R.string.text_phone_code) + phone_number;
            }
        } else {
            String tmp_str = phone_number.substring(0,0)+getString(R.string.text_phone_code)+phone_number.substring(1);
            phone_number = tmp_str;
        }
        return true;
    }
    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(PhoneAuthActivity.this, MainActivity.class));
            finish();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_start_verification:
                if (!validatePhoneNumber()) {
                    return;
                }
                showProgressView();
                startPhoneNumberVerification(phone_number);
                break;
            case R.id.button_verify_phone:
                String code = mVerificationField.getText().toString();
                if (TextUtils.isEmpty(code)) {
                    mVerificationField.setError("Cannot be empty.");
                    return;
                }
                showProgressView();
                verifyPhoneNumberWithCode(mVerificationId, code);
                break;
            case R.id.button_resend:
                resendVerificationCode(phone_number, mResendToken);
                break;
            case R.id.c_email_auth_button:
                startActivity(new Intent(PhoneAuthActivity.this, MailAuthActivity.class));
                finish();
                break;
        }

    }

    private void setSessions() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        if (user_fb_id != null && user_id != null) {
            editor.putString("user_fb_id", user_fb_id);
            editor.putLong("user_id", user_id);
            if (user_email != null) editor.putString("user_email", user_email);
            editor.apply();
        }

    }

    private void displayError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}
