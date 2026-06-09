package com.taskguard.utils;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.taskguard.R;
import com.taskguard.activities.AdminActivity;
import com.taskguard.activities.ContactAdminActivity;
import com.taskguard.activities.LoginActivity;
import com.taskguard.activities.ManagerActivity;
import com.taskguard.activities.MemberActivity;
import com.taskguard.activities.ProfileActivity;
import com.taskguard.activities.TaskListActivity;
import com.taskguard.views.CircleImageView;
import com.taskguard.views.DrawerLayout;

public class DrawerController {

    private static final String USERS_COLLECTION = "users";

    private DrawerController() {
    }

    public static void setup(Activity activity, String currentRole) {
        DrawerLayout drawerLayout = activity.findViewById(R.id.drawerLayout);
        TextView hamburgerButton = activity.findViewById(R.id.hamburgerButton);

        if (drawerLayout == null || hamburgerButton == null) {
            return;
        }

        hamburgerButton.setOnClickListener(v -> drawerLayout.toggleDrawer());
        configureMenuVisibility(activity, currentRole);
        configureNavigation(activity, drawerLayout, currentRole);
        loadHeader(activity);
    }

    public static void loadHeader(Activity activity) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }

        CircleImageView imageView = activity.findViewById(R.id.drawerProfileImageView);
        if (imageView != null) {
            UserPhotoUtils.loadPhoto(imageView, null);
        }

        FirebaseFirestore.getInstance()
                .collection(USERS_COLLECTION)
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    TextView nameTextView = activity.findViewById(R.id.drawerUserNameTextView);
                    TextView roleTextView = activity.findViewById(R.id.drawerUserRoleTextView);

                    String name = documentSnapshot.getString("name");
                    String role = normalizeRole(documentSnapshot.getString("role"));
                    String photoBase64 = documentSnapshot.getString("photoBase64");

                    if (nameTextView != null) {
                        nameTextView.setText(name == null || name.trim().isEmpty() ? "TaskGuard User" : name);
                    }
                    if (roleTextView != null) {
                        roleTextView.setText(role);
                    }
                    if (imageView != null) {
                        UserPhotoUtils.loadPhoto(imageView, photoBase64);
                    }
                });
    }

    private static void configureMenuVisibility(Activity activity, String currentRole) {
        View contactItem = activity.findViewById(R.id.nav_contact);
        View deleteUsersItem = activity.findViewById(R.id.nav_delete_users);

        if (contactItem != null) {
            contactItem.setVisibility(RoleManager.ROLE_MEMBER.equals(currentRole) ? View.VISIBLE : View.GONE);
        }
        if (deleteUsersItem != null) {
            deleteUsersItem.setVisibility(RoleManager.ROLE_ADMIN.equals(currentRole) ? View.VISIBLE : View.GONE);
        }
    }

    private static void configureNavigation(Activity activity, DrawerLayout drawerLayout, String currentRole) {
        setClick(activity, R.id.nav_profile, drawerLayout, () -> openActivity(activity, ProfileActivity.class));
        setClick(activity, R.id.nav_tasks, drawerLayout, () -> openTasks(activity, currentRole));
        setClick(activity, R.id.nav_contact, drawerLayout, () -> openActivity(activity, ContactAdminActivity.class));
        setClick(activity, R.id.nav_delete_users, drawerLayout, () -> openActivity(activity, AdminActivity.class));
        setClick(activity, R.id.nav_logout, drawerLayout, () -> logout(activity));
    }

    private static void setClick(Activity activity, int viewId, DrawerLayout drawerLayout, Runnable action) {
        View view = activity.findViewById(viewId);
        if (view == null) {
            return;
        }

        view.setOnClickListener(v -> {
            drawerLayout.closeDrawer();
            action.run();
        });
    }

    private static void openTasks(Activity activity, String currentRole) {
        if (RoleManager.ROLE_MEMBER.equals(currentRole)) {
            openActivity(activity, MemberActivity.class);
        } else if (RoleManager.ROLE_MANAGER.equals(currentRole)) {
            openActivity(activity, ManagerActivity.class);
        } else {
            openActivity(activity, TaskListActivity.class);
        }
    }

    private static void openActivity(Activity activity, Class<? extends Activity> activityClass) {
        if (activity.getClass().equals(activityClass)) {
            return;
        }

        Intent intent = new Intent(activity, activityClass);
        activity.startActivity(intent);
    }

    private static void logout(Activity activity) {
        SessionManager.clearSession(activity);
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }

    private static String normalizeRole(String role) {
        if (role == null) {
            return RoleManager.ROLE_MEMBER;
        }

        if (RoleManager.ROLE_ADMIN.equalsIgnoreCase(role.trim())) {
            return RoleManager.ROLE_ADMIN;
        }

        if (RoleManager.ROLE_MANAGER.equalsIgnoreCase(role.trim())) {
            return RoleManager.ROLE_MANAGER;
        }

        return RoleManager.ROLE_MEMBER;
    }
}
