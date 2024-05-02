package com.example.kharchakhata;

import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;

import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private boolean shouldAuthenticateWithBiometrics = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        emailEditText = findViewById(R.id.email_edittext);
        passwordEditText = findViewById(R.id.password_edittext);
        loginButton = findViewById(R.id.login_button);

        BiometricManager biometricManager = BiometricManager.from(MainActivity.this);
        if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS && shouldAuthenticateWithBiometrics) {
            // Create biometric prompt
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Biometric Login")
                    .setNegativeButtonText("Cancel")
                    .build();

            BiometricPrompt biometricPrompt = new BiometricPrompt(MainActivity.this,
                    Executors.newSingleThreadExecutor(),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override
                        public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                            super.onAuthenticationError(errorCode, errString);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Handle authentication error on the main UI thread
                                    Toast.makeText(MainActivity.this, "Biometric error: " + errString, Toast.LENGTH_SHORT).show();
                                }
                            });
                        }


                        @Override
                        public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                            super.onAuthenticationSucceeded(result);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Biometric authentication successful, navigate to the next activity
                                    Toast.makeText(MainActivity.this, "Biometric Login successful!", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                                    startActivity(intent);
                                }
                            });
                        }


                        @Override
                        public void onAuthenticationFailed() {
                            super.onAuthenticationFailed();
                            // Biometric authentication failed
                            Toast.makeText(MainActivity.this, "Biometric Login failed!", Toast.LENGTH_SHORT).show();
                        }
                    });

            // Show biometric prompt
            biometricPrompt.authenticate(promptInfo);
        } else {
            if (!shouldAuthenticateWithBiometrics) {
                // User cancelled biometric authentication, handle login using loginButton
                Toast.makeText(MainActivity.this, "Biometric Login cancelled, please use Login button", Toast.LENGTH_SHORT).show();
            } else {
                // Biometric authentication not available, handle accordingly
                Toast.makeText(MainActivity.this, "Biometric authentication not available", Toast.LENGTH_SHORT).show();
            }
        }

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shouldAuthenticateWithBiometrics = false;
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (isValidLogin(email, password)) {
                    // Login successful, navigate to the next activity
                    Toast.makeText(MainActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                    startActivity(intent);
                } else {
                    // Invalid credentials, show error message
                    Toast.makeText(MainActivity.this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private boolean isValidLogin(String email, String password) {
        return email.equals("mrinaldas969@gmail.com") && password.equals("password");
    }
}
