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
import com.taskguard.utils.UserPhotoUtils;
import com.taskguard.views.CircleImageView;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ProfileActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 1001;
    private static final String USERS_COLLECTION = "users";

    private CircleImageView profileImageView;
    private EditText nameEditText;
    private TextView emailTextView;
    private TextView roleTextView;
    private Button saveButton;
    private ProgressBar progressBar;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private String photoBase64;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        profileImageView = findViewById(R.id.profileImageView);
        nameEditText = findViewById(R.id.profileNameEditText);
        emailTextView = findViewById(R.id.profileEmailTextView);
        roleTextView = findViewById(R.id.profileRoleTextView);
        saveButton = findViewById(R.id.saveProfileButton);
        progressBar = findViewById(R.id.progressBar);

        profileImageView.setOnClickListener(v -> openGallery());
        saveButton.setOnClickListener(v -> saveProfile());

        loadProfile();
    }

    private void loadProfile() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setLoading(true);
        emailTextView.setText(currentUser.getEmail());

        firestore.collection(USERS_COLLECTION)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    setLoading(false);
                    nameEditText.setText(documentSnapshot.getString("name"));
                    roleTextView.setText(documentSnapshot.getString("role"));
                    photoBase64 = documentSnapshot.getString("photoBase64");
                    UserPhotoUtils.loadPhoto(profileImageView, photoBase64);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    UserPhotoUtils.loadPhoto(profileImageView, null);
                    Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_LONG).show();
                });
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
            }
        } catch (Exception e) {
            Toast.makeText(this, "Could not load selected image", Toast.LENGTH_LONG).show();
        }
    }

    private void saveProfile() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String name = nameEditText.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            nameEditText.setError("Name is required");
            nameEditText.requestFocus();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        if (photoBase64 != null) {
            updates.put("photoBase64", photoBase64);
        }

        setLoading(true);
        firestore.collection(USERS_COLLECTION)
                .document(currentUser.getUid())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    setLoading(false);
                    Toast.makeText(ProfileActivity.this, "Profile saved", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(ProfileActivity.this, "Failed to save profile", Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!isLoading);
    }
}
