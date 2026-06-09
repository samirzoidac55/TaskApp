package com.taskguard.activities;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.taskguard.R;
import com.taskguard.utils.RoleManager;
import com.taskguard.utils.ZeroTrustManager;

import java.util.HashMap;
import java.util.Map;

public class CreateTaskActivity extends Activity {

    private EditText titleEditText;
    private EditText descriptionEditText;
    private EditText assignedToEditText;
    private Button saveTaskButton;
    private ProgressBar progressBar;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private RoleManager roleManager;
    private boolean managerCanCreateTasks;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_task);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        roleManager = new RoleManager();

        titleEditText = findViewById(R.id.titleEditText);
        descriptionEditText = findViewById(R.id.descriptionEditText);
        assignedToEditText = findViewById(R.id.assignedToEditText);
        saveTaskButton = findViewById(R.id.saveTaskButton);
        progressBar = findViewById(R.id.progressBar);

        saveTaskButton.setEnabled(false);
        saveTaskButton.setOnClickListener(v -> createTask());

        checkManagerAccess();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ZeroTrustManager.verifyAccess("Manager", this);
    }

    private void checkManagerAccess() {
        setLoading(true);

        roleManager.getCurrentUserRole(new RoleManager.RoleCallback() {
            @Override
            public void onRoleLoaded(String role) {
                setLoading(false);
                managerCanCreateTasks = roleManager.isManager(role);
                saveTaskButton.setEnabled(managerCanCreateTasks);

                if (!managerCanCreateTasks) {
                    Toast.makeText(CreateTaskActivity.this, "Only Managers can create tasks", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                managerCanCreateTasks = false;
                saveTaskButton.setEnabled(false);
                Toast.makeText(CreateTaskActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void createTask() {
        if (!managerCanCreateTasks) {
            Toast.makeText(this, "Only Managers can create tasks", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in before creating a task", Toast.LENGTH_LONG).show();
            return;
        }

        String title = titleEditText.getText().toString().trim();
        String description = descriptionEditText.getText().toString().trim();
        String assignedTo = assignedToEditText.getText().toString().trim();
        String createdBy = currentUser.getEmail();

        if (TextUtils.isEmpty(title)) {
            titleEditText.setError("Title is required");
            titleEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(description)) {
            descriptionEditText.setError("Description is required");
            descriptionEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(assignedTo)) {
            assignedToEditText.setError("Assigned user is required");
            assignedToEditText.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(createdBy)) {
            Toast.makeText(this, "Current user email is not available", Toast.LENGTH_LONG).show();
            return;
        }

        Map<String, Object> task = new HashMap<>();
        task.put("title", title);
        task.put("description", description);
        task.put("assignedTo", assignedTo);
        task.put("status", "pending");
        task.put("createdBy", createdBy);

        setLoading(true);

        firestore.collection("tasks")
                .add(task)
                .addOnSuccessListener(documentReference -> {
                    setLoading(false);
                    Toast.makeText(CreateTaskActivity.this, "Task created", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(CreateTaskActivity.this, "Failed to create task", Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        saveTaskButton.setEnabled(!isLoading && managerCanCreateTasks);
    }
}
