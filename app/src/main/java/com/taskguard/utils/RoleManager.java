package com.taskguard.utils;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class RoleManager {

    public static final String ROLE_ADMIN = "Admin";
    public static final String ROLE_MANAGER = "Manager";
    public static final String ROLE_MEMBER = "Member";

    private static final String USERS_COLLECTION = "users";
    private static final String ROLE_FIELD = "role";

    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;

    public RoleManager() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
    }

    public void getCurrentUserRole(RoleCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            callback.onError("No user is currently signed in");
            return;
        }

        firestore.collection(USERS_COLLECTION)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> handleRoleDocument(documentSnapshot, callback))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public boolean isAdmin(String role) {
        return ROLE_ADMIN.equals(role);
    }

    public boolean isManager(String role) {
        return ROLE_MANAGER.equals(role);
    }

    public boolean isMember(String role) {
        return ROLE_MEMBER.equals(role);
    }

    private void handleRoleDocument(DocumentSnapshot documentSnapshot, RoleCallback callback) {
        if (!documentSnapshot.exists()) {
            callback.onError("User profile was not found");
            return;
        }

        String role = normalizeRole(documentSnapshot.getString(ROLE_FIELD));
        callback.onRoleLoaded(role);
    }

    private String normalizeRole(String role) {
        if (role == null) {
            return ROLE_MEMBER;
        }

        String normalizedRole = role.trim();

        if (ROLE_ADMIN.equalsIgnoreCase(normalizedRole)) {
            return ROLE_ADMIN;
        }

        if (ROLE_MANAGER.equalsIgnoreCase(normalizedRole)) {
            return ROLE_MANAGER;
        }

        return ROLE_MEMBER;
    }

    public interface RoleCallback {
        void onRoleLoaded(String role);

        void onError(String message);
    }
}
