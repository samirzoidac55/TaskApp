package com.taskguard.utils;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.taskguard.activities.LoginActivity;

public class ZeroTrustManager {

    private static final String USERS_COLLECTION = "users";
    private static final String ROLE_FIELD = "role";

    private ZeroTrustManager() {
    }

    public static void verifyAccess(String requiredRole, Activity activity) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            redirectToLogin(activity, "Please log in first");
            return;
        }

        if (SessionManager.isSessionExpired(activity)) {
            redirectToLogin(activity, "Session expired. Please log in again");
            return;
        }

        currentUser.getIdToken(true)
                .addOnSuccessListener(getTokenResult -> verifyRole(requiredRole, activity, currentUser))
                .addOnFailureListener(e -> redirectToLogin(activity, "Session could not be verified"));
    }

    private static void verifyRole(String requiredRole, Activity activity, FirebaseUser currentUser) {
        FirebaseFirestore.getInstance()
                .collection(USERS_COLLECTION)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        redirectToLogin(activity, "User profile was not found");
                        return;
                    }

                    String currentRole = normalizeRole(documentSnapshot.getString(ROLE_FIELD));
                    if (!requiredRole.equals(currentRole)) {
                        redirectToLogin(activity, "Access denied");
                        return;
                    }

                    SessionManager.refreshSession(activity);
                })
                .addOnFailureListener(e -> redirectToLogin(activity, "Access could not be verified"));
    }

    private static String normalizeRole(String role) {
        if (role == null) {
            return RoleManager.ROLE_MEMBER;
        }

        String normalizedRole = role.trim();

        if (RoleManager.ROLE_ADMIN.equalsIgnoreCase(normalizedRole)) {
            return RoleManager.ROLE_ADMIN;
        }

        if (RoleManager.ROLE_MANAGER.equalsIgnoreCase(normalizedRole)) {
            return RoleManager.ROLE_MANAGER;
        }

        return RoleManager.ROLE_MEMBER;
    }

    private static void redirectToLogin(Activity activity, String message) {
        SessionManager.clearSession(activity);
        FirebaseAuth.getInstance().signOut();
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();

        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}
