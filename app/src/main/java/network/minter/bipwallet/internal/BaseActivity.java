package network.minter.bipwallet.internal;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.arch.lifecycle.LifecycleOwner;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MenuItem;

import static network.minter.bipwallet.internal.common.Preconditions.checkNotNull;


/**
 * Dogsy. 2017
 *
 * @author Eduard Maximovich <edward.vstock@gmail.com>
 */
@SuppressLint("Registered")
public class BaseActivity extends AppCompatActivity implements LifecycleOwner {

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void setupToolbar(@NonNull final Toolbar toolbar, @NonNull final DrawerLayout drawerLayout) {
        checkNotNull(toolbar, "Toolbar can't be null!");
        setSupportActionBar(toolbar);

        assert (getSupportActionBar() != null);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(Gravity.START, true));
    }

    public void setupToolbar(@NonNull final Toolbar toolbar) {
        checkNotNull(toolbar, "Toolbar can't be null!");
        setSupportActionBar(toolbar);

        assert (getSupportActionBar() != null);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void setActionBarVisible(boolean isVisible) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            if (isVisible) {
                actionBar.show();
            }
            else {
                actionBar.hide();
            }
        }
    }

    public void startActivityClearTop(Activity from, Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);

        from.startActivity(intent);
        from.finish();
    }

    public void startActivityClearTop(Activity from, Class<?> toClass) {
        Intent intent = new Intent(from, toClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);

        from.startActivity(intent);
        from.finish();
    }

    public void startActivityClearTop(Activity from, Class<?> toClass, Bundle options) {
        Intent intent = new Intent(from, toClass);
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TASK |
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);

        from.startActivity(intent, options);
        from.finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
