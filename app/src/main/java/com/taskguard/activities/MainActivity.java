package com.taskguard.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.taskguard.utils.RoleManager;

public class MainActivity extends Activity {

    private FirebaseAuth firebaseAuth;
    private RoleManager roleManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();
        roleManager = new RoleManager();

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            openLoginActivity();
            return;
        }

        fetchRoleAndRedirect();
    }

    private void fetchRoleAndRedirect() {
        roleManager.getCurrentUserRole(new RoleManager.RoleCallback() {
            @Override
            public void onRoleLoaded(String role) {
                if (roleManager.isAdmin(role)) {
                    openRoleActivity(AdminActivity.class);
                } else if (roleManager.isManager(role)) {
                    openRoleActivity(ManagerActivity.class);
                } else {
                    openRoleActivity(MemberActivity.class);
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
                finish();
            }
        });
    }

    private void openRoleActivity(Class<? extends Activity> activityClass) {
        Intent intent = new Intent(this, activityClass);
        startActivity(intent);
        finish();
    }

    private void openLoginActivity() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
