package com.tanwir.qrcodescanner;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.text.InputType;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class SignInActivity extends AppCompatActivity {

    private EditText email;
    private EditText password;
    private Button mLoginBtn;
    private FirebaseAuth mAuth;
    private ProgressBar pbar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        mAuth = FirebaseAuth.getInstance();
        email = (EditText) findViewById(R.id.email);
        password = (EditText) findViewById(R.id.password);
        mLoginBtn = (Button) findViewById(R.id.loginButton);
        pbar = (ProgressBar) findViewById(R.id.progressBar);
        pbar.setVisibility(View.GONE);
        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLoginBtn.setVisibility(View.GONE);
                pbar.setVisibility(View.VISIBLE);
                pbar.setIndeterminate(true);
                startSignIn();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        mLoginBtn.setVisibility(View.VISIBLE);
    }

    private void changeLoginPrefs(boolean shouldSave, String email) {
        //TO-DO Maybe? only for email tho.
    }

    private void startSignIn() {
        String emailAddress = email.getText().toString();
        String pass = password.getText().toString();
        mAuth.signInWithEmailAndPassword(emailAddress, pass).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                pbar.setVisibility(View.GONE);
                if(!task.isSuccessful()) {
                    mLoginBtn.setVisibility(View.VISIBLE);
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
