package com.github.martoreto.aauto.vex;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

public class PermissionsActivity extends Activity {
    private static final int REQUEST_PERMISSION = 1;

    @Override
    protected void onStart() {
        super.onStart();

        if (VexProxyService.needsPermissions(this)) {
            requestPermission();
        } else {
            finish();
        }
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this,
                new String[] {VexProxyService.PERMISSION_VEX},
                REQUEST_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_PERMISSION:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    finish();
                } else {
                    Toast.makeText(this, R.string.vex_permission_not_granted, Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

}
