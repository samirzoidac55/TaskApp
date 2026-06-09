package com.taskguard.activities;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.taskguard.R;
import com.taskguard.utils.ZeroTrustManager;

import java.util.HashMap;
import java.util.Map;

public class ContactAdminActivity extends Activity {

    private EditText messageEditText;
    private Button sendButton;
    private ProgressBar progressBar;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_admin);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        messageEditText = findViewById(R.id.messageEditText);
        sendButton = findViewById(R.id.sendMessageButton);
        progressBar = findViewById(R.id.progressBar);

        sendButton.setOnClickListener(v -> sendMessage());
    }

    @Override
    protected void onResume() {
        super.onResume();
        ZeroTrustManager.verifyAccess("Member", this);
    }

    private void sendMessage() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String message = messageEditText.getText().toString().trim();
        if (TextUtils.isEmpty(message)) {
            messageEditText.setError("Message is required");
            messageEditText.requestFocus();
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("from", currentUser.getEmail());
        data.put("message", message);
        data.put("timestamp", Timestamp.now());

        setLoading(true);
        firestore.collection("messages")
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    setLoading(false);
                    Toast.makeText(ContactAdminActivity.this, "Message sent", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(ContactAdminActivity.this, "Failed to send message", Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        sendButton.setEnabled(!isLoading);
    }
}
