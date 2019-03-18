package com.tanwir.qrcodescanner;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SignInActivity extends AppCompatActivity {

    private EditText email;
    private EditText password;
    private Button mLoginBtn;
    private CheckBox rememberMe;
    private FirebaseAuth mAuth;

    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                if (firebaseAuth.getCurrentUser() != null) {
                    startActivity(new Intent(SignInActivity.this, ScanActivity.class));
                }
            }
        };
        email = (EditText) findViewById(R.id.email);
        password = (EditText) findViewById(R.id.password);
        rememberMe = (CheckBox) findViewById(R.id.rememberMe);
        rememberMe.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked) {
                   changeLoginPrefs(false, email.getText().toString(), password.getText().toString());
                } else {
                    changeLoginPrefs(false, null, null);
                }
            }
        });
        mLoginBtn = (Button) findViewById(R.id.loginButton);

        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startSignIn();
            }
        });
    }

    private void changeLoginPrefs(boolean shouldSave, String email, String password) {
    }

    private void startSignIn() {
        String emailaddress = email.getText().toString();
        String pass = password.getText().toString();
        mAuth.signInWithEmailAndPassword(emailaddress, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(!task.isSuccessful()) {
                    createLoginFailureDialog("Sign In Error", "Unable to Sign In, Please check your email or password");
                } else {
                    startActivity(new Intent(SignInActivity.this, ScanActivity.class));
                }
            }
        });

    }

    public void createLoginFailureDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .create()
                .show();
    }
}
