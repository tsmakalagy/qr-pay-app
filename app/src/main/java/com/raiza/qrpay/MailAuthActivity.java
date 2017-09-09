package com.raiza.qrpay;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.iid.FirebaseInstanceId;

import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.HashMap;
import java.util.regex.Pattern;

public class MailAuthActivity extends BaseActivity implements View.OnClickListener {

    EditText mNameField, mEmailAddressField, mPasswordField;
    Button mLoginButton, mSignupButton, cSigninButton, cSignupButton, cPhoneAuthButton;
    ImageButton clearName, clearEmail, clearPassword;

    TextView titleText;
    private FirebaseAuth mAuth;

    private String user_fb_id, user_pwd, user_email, user_name, app_token;
    private Long user_id;
    private boolean isCreated = false;

    String phone_number, verification_id;
    private FirebaseUser phoneUser;

    private static final String TAG = "MailAuthActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mail_auth);

        // Set toolbar
        setupToolbar();

        // Hide ProgressView
        hideProgressView();

        // To be displayed when Creating new account
        titleText = (TextView)findViewById(R.id.title_text_email_auth);
        titleText.setText(getString(R.string.sign_in));



        mEmailAddressField = (EditText)findViewById(R.id.field_email_address);
        mPasswordField = (EditText)findViewById(R.id.field_password);

        mLoginButton = (Button)findViewById(R.id.login_button);
        mSignupButton = (Button)findViewById(R.id.signup_button);
        cSigninButton = (Button)findViewById(R.id.c_signin_button);
        cSignupButton = (Button)findViewById(R.id.c_signup_button);
        cPhoneAuthButton = (Button)findViewById(R.id.c_phone_auth_button);

        mLoginButton.setOnClickListener(this);
        mSignupButton.setOnClickListener(this);
        cSigninButton.setOnClickListener(this);
        cSignupButton.setOnClickListener(this);
        cPhoneAuthButton.setOnClickListener(this);

        // Name is hidden during signin
        mNameField = (EditText)findViewById(R.id.field_name);

        Intent intent = getIntent();
        phone_number = intent.getStringExtra("phone_number");
        verification_id = intent.getStringExtra("verification_id");

        if (mAuth == null)
            mAuth = FirebaseAuth.getInstance();

        if (isFromPhoneAuth()) {
            showSignupFields();
            phoneUser = mAuth.getCurrentUser();
        } else {
            showSigninFields();
        }

        clearEmail = (ImageButton)findViewById(R.id.clear_email);
        clearEmail.setOnClickListener(this);
        clearEmail.setVisibility(View.GONE);

        clearName = (ImageButton)findViewById(R.id.clear_name);
        clearName.setOnClickListener(this);
        clearName.setVisibility(View.GONE);

        clearPassword = (ImageButton)findViewById(R.id.clear_password);
        clearPassword.setOnClickListener(this);
        clearPassword.setVisibility(View.GONE);

        setUpClearFields();

        app_token = FirebaseInstanceId.getInstance().getToken();



    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.findItem(R.id.action_logout).setVisible(false);
        return true;
    }


    private void createAccount(String name, String email, String password) {
        Log.d(TAG, "createAccount:" + email);

        if (!validateForm()) {
            return;
        }

        // Validate name field
        if (TextUtils.isEmpty(name)) {
            mNameField.setError("Required");
            return;
        } else {
            mNameField.setError(null);
        }


        user_pwd = password;
        user_email = email;
        user_name = name;

        // Show progressview during account creation
        showProgressView();

        if (isFromPhoneAuth()) {
            AuthCredential credential = EmailAuthProvider.getCredential(user_email, user_pwd);
            phoneUser.linkWithCredential(credential)
                    .addOnCompleteListener(MailAuthActivity.this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                Log.d(TAG, "linkWithCredential:success");
                                FirebaseUser user = task.getResult().getUser();
                                // INSERT INTO LOCAL DB
                                user_fb_id = user.getUid();
                                Log.d(TAG, "UserId After Linking: " + user_fb_id);
                                String url = getString(R.string.base_url_api) + "create_user_phone";
                                volleyJsonSignupPhoneRequest(url);

                            } else {
                                Log.w(TAG, "linkWithCredential:failure", task.getException());

                            }

                            // ...
                        }
                    });
        } else {
            // [START create_user_with_email]
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                // Sign in success, update UI with the signed-in user's information
                                Log.d(TAG, "createUserWithEmail:success");
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    user_fb_id = user.getUid();
                                    isCreated = true;
                                    Log.d(TAG, "UserId Before Linking: " + user_fb_id);
                                }
                                if (isCreated) { // Send user info to web server for persistence
                                    String url = getString(R.string.base_url_api) + "create_user_email";
                                    volleyJsonSignupRequest(url);
                                }

                            } else {
                                // If sign in fails, display a message to the user.
                                Log.w(TAG, "createUserWithEmail:failure", task.getException());
                                Exception e = task.getException();
                                if (e instanceof FirebaseAuthUserCollisionException) {
                                    displayError(getString(R.string.email_in_use));
                                } else {
                                    displayError(getString(R.string.auth_failed));
                                }
                            }

                            // [START_EXCLUDE]
                            hideProgressView();
                            // [END_EXCLUDE]
                        }
                    });
            // [END create_user_with_email]

        }


    }

    private void signIn(String email, final String password) {
        if (!validateForm()) {
            return;
        }

        user_email = email;
        user_pwd = password;

        //show ProgressDialog;
        showProgressView();

        // [START sign_in_with_email]
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithEmail:success");
                            FirebaseUser user = mAuth.getCurrentUser();
                            user_fb_id = user.getUid();

                            // Get web server user_id
                            String url = getString(R.string.base_url_api) + "get_user_email";
                            volleyJsonLoginRequest(url);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithEmail:failure", task.getException());
                            Exception e = task.getException();
                            if (e instanceof FirebaseAuthInvalidUserException) { // User doesn't exist; create new one
                                displayError(getString(R.string.user_not_exist));
                            } else if (e instanceof FirebaseAuthInvalidCredentialsException) { // invalid password
                                displayError(getString(R.string.auth_failed));
                            }
                        }

                        // [START_EXCLUDE]
                        if (!task.isSuccessful()) {
                            //mStatusTextView.setText(R.string.auth_failed);
                            Exception e = task.getException();
                            if (e instanceof FirebaseAuthInvalidUserException) { // User doesn't exist; create new one
                                displayError(getString(R.string.user_not_exist));
                            } else if (e instanceof FirebaseAuthInvalidCredentialsException) { // invalid password
                                displayError(getString(R.string.auth_failed));
                            }
                        }
                        //progressView.setVisibility(View.INVISIBLE);
                        hideProgressView();
                        //hideProgressDialog();
                        // [END_EXCLUDE]
                    }
                });
        // [END sign_in_with_email]
    }

    private void volleyJsonSignupRequest(String url){
        String  REQUEST_TAG = url;
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("name", user_name);
        params.put("password", user_pwd);
        params.put("email", user_email);
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
                            if (user_id != null && user_id > 0) { // Launch MainActivity
                                setSessions();
                                Intent intent = new Intent(MailAuthActivity.this, MainActivity.class);
                                intent.putExtra("usr_id", user_id);
                                startActivity(intent);
                                finish();
                            }


                        } catch (Exception e) {
                            Log.w("Exception", e.getMessage());
                            String msg = e.getMessage();
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

    private void volleyJsonSignupPhoneRequest(String url){
        String  REQUEST_TAG = url;
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("name", user_name);
        params.put("password", user_pwd);
        params.put("email", user_email);
        params.put("fb_id", user_fb_id);
        params.put("phone_number", phone_number);
        params.put("app_token", app_token);

        JsonObjectRequest jsonObjectReq = new JsonObjectRequest(url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            //Log.w("Response", response.toString(4));
                            // Web server return JSON of format {"user_id": 1}
                            user_id = response.getLong("user_id");
                            if (user_id != null && user_id > 0) { // Launch MainActivity
                                setSessions();
                                Intent intent = new Intent(MailAuthActivity.this, MainActivity.class);
                                intent.putExtra("usr_id", user_id);
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

    private void volleyJsonLoginRequest(String url){
        String  REQUEST_TAG = url;
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("email", user_email);
        params.put("fb_id", user_fb_id);
        params.put("app_token", app_token);

        JsonObjectRequest jsonObjectReq = new JsonObjectRequest(url, new JSONObject(params),
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            // Web server return JSON of format {"user_id": 1}
                            user_id = response.getLong("user_id");
                            if (user_id != null && user_id > 0) { // Launch MainActivity
                                setSessions();
                                Intent intent = new Intent(MailAuthActivity.this, MainActivity.class);
                                intent.putExtra("usr_id", user_id);
                                startActivity(intent);
                                finish();
                            }


                        } catch (Exception e) {
                            Log.w("Exception", e.getMessage());
                        }


                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                VolleyLog.d("JSONRequest", "Error: " + error.getMessage());
                String msg = error.getMessage();
                if (msg == null) msg = getString(R.string.unexpected_error);
                displayError(msg);
                hideProgressView();
            }
        });

        // Adding JsonObject request to request queue
        AppSingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectReq,REQUEST_TAG);
    }

    private boolean validateForm() {
        boolean valid = true;


        String email = mEmailAddressField.getText().toString();
        if (!isValidEmail(email)) {
            mEmailAddressField.setError("Required.");
            valid = false;
        } else {
            mEmailAddressField.setError(null);
        }

        String password = mPasswordField.getText().toString();
        if (TextUtils.isEmpty(password)) {
            mPasswordField.setError("Required.");
            valid = false;
        } else if (!isValidPassword(password)) {
            mPasswordField.setError("Enter 6 digits.");
            valid = false;
        } else {
            mPasswordField.setError(null);
        }

        return valid;
    }

    private boolean isValidEmail(CharSequence target) {
        return !TextUtils.isEmpty(target) && Patterns.EMAIL_ADDRESS.matcher(target).matches();
    }

    private boolean isValidPassword(CharSequence target) {
        String regex = "\\d{6}";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(target).matches();
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        user_id = getUserId();
        if (currentUser != null && user_id > 0) {
            Intent intent = new Intent(MailAuthActivity.this, MainActivity.class);
            intent.putExtra("usr_id", user_id);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.login_button:
                signIn(mEmailAddressField.getText().toString(), mPasswordField.getText().toString());
                break;
            case R.id.signup_button:
                createAccount(mNameField.getText().toString(), mEmailAddressField.getText().toString(), mPasswordField.getText().toString());
                break;
            case R.id.c_signup_button:
                showSignupFields();
                break;
            case R.id.c_signin_button:
                showSigninFields();
                break;
            case R.id.clear_email:
                mEmailAddressField.getText().clear();
                clearEmail.setVisibility(View.GONE);
                break;
            case R.id.clear_name:
                mNameField.getText().clear();
                clearName.setVisibility(View.GONE);
                break;
            case R.id.clear_password:
                mPasswordField.getText().clear();
                clearPassword.setVisibility(View.GONE);
                break;
            case R.id.c_phone_auth_button:
                startActivity(new Intent(MailAuthActivity.this, PhoneAuthActivity.class));
                finish();
                break;

        }
    }


    private void setSessions() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        if (user_email != null && user_fb_id != null && user_id != null) {
            editor.putString("user_email", user_email);
            editor.putString("user_fb_id", user_fb_id);
            editor.putLong("user_id", user_id);
            editor.apply();
        }
    }

    private void showSignupFields() {
        mSignupButton.setVisibility(View.VISIBLE);
        mLoginButton.setVisibility(View.GONE);
        titleText.setVisibility(View.VISIBLE);
        titleText.setText(getString(R.string.create_account));
        mNameField.setVisibility(View.VISIBLE);
        mNameField.requestFocus();
        findViewById(R.id.c_signin).setVisibility(View.VISIBLE);
        findViewById(R.id.c_signup).setVisibility(View.GONE);
    }

    private void showSigninFields() {
        mSignupButton.setVisibility(View.GONE);
        mLoginButton.setVisibility(View.VISIBLE);
        titleText.setVisibility(View.VISIBLE);
        titleText.setText(getString(R.string.sign_in));
        mNameField.setVisibility(View.GONE);
        findViewById(R.id.c_signin).setVisibility(View.GONE);
        findViewById(R.id.c_signup).setVisibility(View.VISIBLE);
    }

    private void setUpClearFields() {
        mEmailAddressField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                clearEmail.setVisibility(View.VISIBLE);
            }
        });
        mNameField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                clearName.setVisibility(View.VISIBLE);
            }
        });
        mPasswordField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                clearPassword.setVisibility(View.VISIBLE);
            }
        });
    }

    private void displayError(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private boolean isFromPhoneAuth() {
        return !TextUtils.isEmpty(phone_number) && !TextUtils.isEmpty(verification_id);
    }

}

