package com.taskguard.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFS_NAME = "taskguard_session";
    private static final String KEY_SESSION_START_TIME = "session_start_time";
    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000;

    private final Context context;

    public SessionManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static boolean isSessionExpired(Context context) {
        return new SessionManager(context).isSessionExpired();
    }

    public boolean isSessionExpired() {
        SharedPreferences sharedPreferences = getSharedPreferences();
        long sessionStartTime = sharedPreferences.getLong(KEY_SESSION_START_TIME, 0);

        if (sessionStartTime == 0) {
            return true;
        }

        return System.currentTimeMillis() - sessionStartTime > SESSION_TIMEOUT_MS;
    }

    public static void refreshSession(Context context) {
        new SessionManager(context).refreshSession();
    }

    public void refreshSession() {
        getSharedPreferences()
                .edit()
                .putLong(KEY_SESSION_START_TIME, System.currentTimeMillis())
                .apply();
    }

    public static void clearSession(Context context) {
        new SessionManager(context).clearSession();
    }

    public void clearSession() {
        getSharedPreferences()
                .edit()
                .remove(KEY_SESSION_START_TIME)
                .apply();
    }

    private SharedPreferences getSharedPreferences() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
