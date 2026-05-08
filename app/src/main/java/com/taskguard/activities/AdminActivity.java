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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.taskguard.R;
import com.taskguard.utils.RoleManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminActivity extends Activity {

    private static final String USERS_COLLECTION = "users";
    private static final String NAME_FIELD = "name";
    private static final String ROLE_FIELD = "role";
    private static final String EMAIL_FIELD = "email";
    private static final List<String> ROLES = Arrays.asList(
            RoleManager.ROLE_ADMIN,
            RoleManager.ROLE_MANAGER,
            RoleManager.ROLE_MEMBER
    );

    private final List<AppUser> users = new ArrayList<>();
    private UserAdapter userAdapter;
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private FirebaseFirestore firestore;
    private RoleManager roleManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);

        firestore = FirebaseFirestore.getInstance();
        roleManager = new RoleManager();

        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);
        Button logoutButton = findViewById(R.id.logoutButton);

        RecyclerView usersRecyclerView = findViewById(R.id.usersRecyclerView);
        usersRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        userAdapter = new UserAdapter(users, this::changeUserRole);
        usersRecyclerView.setAdapter(userAdapter);

        logoutButton.setOnClickListener(v -> logout());
        checkAdminAccess();
    }

    private void checkAdminAccess() {
        setLoading(true);

        roleManager.getCurrentUserRole(new RoleManager.RoleCallback() {
            @Override
            public void onRoleLoaded(String role) {
                if (roleManager.isAdmin(role)) {
                    fetchUsers();
                } else {
                    setLoading(false);
                    Toast.makeText(AdminActivity.this, "Only Admins can manage user roles", Toast.LENGTH_LONG).show();
                    finish();
                }
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(AdminActivity.this, message, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void fetchUsers() {
        firestore.collection(USERS_COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    users.clear();

                    for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots.getDocuments()) {
                        String name = documentSnapshot.getString(NAME_FIELD);
                        String email = documentSnapshot.getString(EMAIL_FIELD);
                        String role = normalizeRole(documentSnapshot.getString(ROLE_FIELD));
                        users.add(new AppUser(documentSnapshot.getId(), name, email, role));
                    }

                    userAdapter.notifyDataSetChanged();
                    emptyTextView.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
                    setLoading(false);
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(AdminActivity.this, "Failed to load users", Toast.LENGTH_LONG).show();
                });
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return RoleManager.ROLE_MEMBER;
        }

        String normalizedRole = role.trim();

        for (String validRole : ROLES) {
            if (validRole.equalsIgnoreCase(normalizedRole)) {
                return validRole;
            }
        }

        return RoleManager.ROLE_MEMBER;
    }

    private void setLoading(boolean isLoading) {
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void changeUserRole(AppUser user) {
        String nextRole = getNextRole(user.getRole());
        setLoading(true);

        firestore.collection(USERS_COLLECTION)
                .document(user.getId())
                .update(ROLE_FIELD, nextRole)
                .addOnSuccessListener(unused -> {
                    user.setRole(nextRole);
                    userAdapter.notifyDataSetChanged();
                    setLoading(false);
                    Toast.makeText(AdminActivity.this, "Role updated to " + nextRole, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(AdminActivity.this, "Failed to update role", Toast.LENGTH_LONG).show();
                });
    }

    private String getNextRole(String currentRole) {
        String normalizedRole = normalizeRole(currentRole);
        int currentIndex = ROLES.indexOf(normalizedRole);
        int nextIndex = (currentIndex + 1) % ROLES.size();
        return ROLES.get(nextIndex);
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private interface OnRoleChangeClickListener {
        void onRoleChangeClicked(AppUser user);
    }

    private static class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

        private final List<AppUser> users;
        private final OnRoleChangeClickListener roleChangeClickListener;

        UserAdapter(List<AppUser> users, OnRoleChangeClickListener roleChangeClickListener) {
            this.users = users;
            this.roleChangeClickListener = roleChangeClickListener;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user_role, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            holder.bind(users.get(position), roleChangeClickListener);
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        private static class UserViewHolder extends RecyclerView.ViewHolder {

            private final TextView nameTextView;
            private final TextView emailTextView;
            private final TextView roleTextView;
            private final Button changeRoleButton;

            UserViewHolder(@NonNull View itemView) {
                super(itemView);
                nameTextView = itemView.findViewById(R.id.userNameTextView);
                emailTextView = itemView.findViewById(R.id.userEmailTextView);
                roleTextView = itemView.findViewById(R.id.userRoleTextView);
                changeRoleButton = itemView.findViewById(R.id.changeRoleButton);
            }

            void bind(AppUser user, OnRoleChangeClickListener roleChangeClickListener) {
                nameTextView.setText(valueOrFallback(user.getName(), "No name"));
                emailTextView.setText("Email: " + valueOrFallback(user.getEmail(), "No email"));
                roleTextView.setText("Role: " + valueOrFallback(user.getRole(), RoleManager.ROLE_MEMBER));
                changeRoleButton.setText("Change to " + getNextRoleLabel(user.getRole()));
                changeRoleButton.setOnClickListener(v -> roleChangeClickListener.onRoleChangeClicked(user));
            }

            private String getNextRoleLabel(String role) {
                if (RoleManager.ROLE_ADMIN.equals(role)) {
                    return RoleManager.ROLE_MANAGER;
                }

                if (RoleManager.ROLE_MANAGER.equals(role)) {
                    return RoleManager.ROLE_MEMBER;
                }

                return RoleManager.ROLE_ADMIN;
            }

            private String valueOrFallback(String value, String fallback) {
                return value == null || value.isEmpty() ? fallback : value;
            }
        }
    }

    private static class AppUser {

        private final String id;
        private final String name;
        private final String email;
        private String role;

        AppUser(String id, String name, String email, String role) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.role = role;
        }

        String getId() {
            return id;
        }

        String getName() {
            return name;
        }

        String getEmail() {
            return email;
        }

        String getRole() {
            return role;
        }

        void setRole(String role) {
            this.role = role;
        }
    }
}
