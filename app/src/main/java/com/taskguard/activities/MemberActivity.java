package com.taskguard.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.taskguard.R;
import com.taskguard.models.Task;

import java.util.ArrayList;
import java.util.List;

public class MemberActivity extends Activity {

    private final List<Task> tasks = new ArrayList<>();
    private TaskAdapter taskAdapter;
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_member);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);
        Button logoutButton = findViewById(R.id.logoutButton);

        RecyclerView assignedTasksRecyclerView = findViewById(R.id.assignedTasksRecyclerView);
        assignedTasksRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        taskAdapter = new TaskAdapter(tasks);
        assignedTasksRecyclerView.setAdapter(taskAdapter);

        logoutButton.setOnClickListener(v -> logout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAssignedTasks();
    }

    private void fetchAssignedTasks() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in to view assigned tasks", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String currentUserEmail = currentUser.getEmail();
        if (currentUserEmail == null || currentUserEmail.trim().isEmpty()) {
            Toast.makeText(this, "Current user email is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        currentUserEmail = currentUserEmail.trim();

        setLoading(true);

        firestore.collection("tasks")
                .whereEqualTo("assignedTo", currentUserEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    tasks.clear();

                    for (QueryDocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                        Task task = documentSnapshot.toObject(Task.class);
                        task.setId(documentSnapshot.getId());
                        tasks.add(task);
                    }

                    taskAdapter.notifyDataSetChanged();
                    emptyTextView.setVisibility(tasks.isEmpty() ? View.VISIBLE : View.GONE);
                    setLoading(false);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(MemberActivity.this, "Failed to load assigned tasks", Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private static class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

        private final List<Task> tasks;

        TaskAdapter(List<Task> tasks) {
            this.tasks = tasks;
        }

        @NonNull
        @Override
        public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_task, parent, false);
            return new TaskViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
            holder.bind(tasks.get(position));
        }

        @Override
        public int getItemCount() {
            return tasks.size();
        }

        private static class TaskViewHolder extends RecyclerView.ViewHolder {

            private final TextView titleTextView;
            private final TextView descriptionTextView;
            private final TextView assignedToTextView;
            private final TextView statusTextView;

            TaskViewHolder(@NonNull View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.taskTitleTextView);
                descriptionTextView = itemView.findViewById(R.id.taskDescriptionTextView);
                assignedToTextView = itemView.findViewById(R.id.taskAssignedToTextView);
                statusTextView = itemView.findViewById(R.id.taskStatusTextView);
            }

            void bind(Task task) {
                titleTextView.setText(valueOrEmpty(task.getTitle()));
                descriptionTextView.setText(valueOrEmpty(task.getDescription()));
                assignedToTextView.setText("Assigned to: " + valueOrEmpty(task.getAssignedTo()));
                statusTextView.setText("Status: " + valueOrEmpty(task.getStatus()));
            }

            private String valueOrEmpty(String value) {
                return value == null ? "" : value;
            }
        }
    }
}
