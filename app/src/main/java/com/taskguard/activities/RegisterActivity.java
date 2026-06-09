package com.taskguard.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.taskguard.R;
import com.taskguard.utils.RoleManager;
import com.taskguard.utils.SessionManager;
import com.taskguard.utils.UserPhotoUtils;
import com.taskguard.views.CircleImageView;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 1002;

    private CircleImageView profileImageView;
    private TextView selectProfileImageTextView;
    private EditText nameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button registerButton;
    private ProgressBar progressBar;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private String photoBase64;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        profileImageView = findViewById(R.id.registerProfileImageView);
        selectProfileImageTextView = findViewById(R.id.selectProfileImageTextView);
        nameEditText = findViewById(R.id.nameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);
        progressBar = findViewById(R.id.progressBar);

        UserPhotoUtils.loadPhoto(profileImageView, null);
        profileImageView.setOnClickListener(v -> openGallery());
        selectProfileImageTextView.setOnClickListener(v -> openGallery());
        registerButton.setOnClickListener(v -> registerUser());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != PICK_IMAGE_REQUEST || resultCode != RESULT_OK || data == null) {
            return;
        }

        Uri imageUri = data.getData();
        if (imageUri == null) {
            return;
        }

        try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap != null) {
                profileImageView.setImageBitmap(bitmap);
                photoBase64 = UserPhotoUtils.bitmapToBase64(bitmap);
                selectProfileImageTextView.setText("Change profile image");
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not load selected image", Toast.LENGTH_LONG).show();
        }
    }

    private void registerUser() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            nameEditText.setError("Name is required");
            nameEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return;
        }

        setLoading(true);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        if (user == null) {
                            setLoading(false);
                            Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_LONG).show();
                            return;
                        }

                        saveUserProfile(user.getUid(), name, email);
                    } else {
                        setLoading(false);
                        showError("Registration failed", task.getException());
                    }
                });
    }

    private void saveUserProfile(String userId, String name, String email) {
        Map<String, Object> userProfile = new HashMap<>();
        userProfile.put("name", name);
        userProfile.put("email", email);
        userProfile.put("role", RoleManager.ROLE_MEMBER);
        if (photoBase64 != null) {
            userProfile.put("photoBase64", photoBase64);
        }

        firestore.collection("users")
                .document(userId)
                .set(userProfile)
                .addOnSuccessListener(unused -> {
                    SessionManager.refreshSession(RegisterActivity.this);
                    setLoading(false);
                    Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    showError("Could not save user profile", e);
                });
    }

    private void showError(String fallbackMessage, Exception exception) {
        String message = fallbackMessage;
        if (exception != null && exception.getMessage() != null) {
            message = exception.getMessage();
        }
        Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        registerButton.setEnabled(!isLoading);
        profileImageView.setEnabled(!isLoading);
        selectProfileImageTextView.setEnabled(!isLoading);
        nameEditText.setEnabled(!isLoading);
        emailEditText.setEnabled(!isLoading);
        passwordEditText.setEnabled(!isLoading);
    }
}
