package com.taskguard.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import com.taskguard.R;

public class DrawerLayout extends FrameLayout {

    private View drawerView;
    private boolean drawerOpen;

    public DrawerLayout(Context context) {
        super(context);
    }

    public DrawerLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DrawerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        drawerView = findViewById(R.id.drawerNavigationView);
        if (drawerView != null) {
            drawerView.setVisibility(INVISIBLE);
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        positionDrawer(false);
    }

    public void openDrawer() {
        drawerOpen = true;
        positionDrawer(true);
    }

    public void closeDrawer() {
        drawerOpen = false;
        positionDrawer(true);
    }

    public void toggleDrawer() {
        if (drawerOpen) {
            closeDrawer();
        } else {
            openDrawer();
        }
    }

    public boolean isDrawerOpen() {
        return drawerOpen;
    }

    private void positionDrawer(boolean animate) {
        if (drawerView == null) {
            return;
        }

        int drawerWidth = drawerView.getWidth();
        if (drawerWidth == 0) {
            return;
        }

        float targetTranslation = drawerOpen ? 0 : -drawerWidth;
        drawerView.setVisibility(drawerOpen ? VISIBLE : INVISIBLE);

        if (animate) {
            drawerView.animate()
                    .translationX(targetTranslation)
                    .setDuration(180)
                    .withStartAction(() -> drawerView.setVisibility(VISIBLE))
                    .withEndAction(() -> drawerView.setVisibility(drawerOpen ? VISIBLE : INVISIBLE))
                    .start();
        } else {
            drawerView.setTranslationX(targetTranslation);
        }
    }
}
