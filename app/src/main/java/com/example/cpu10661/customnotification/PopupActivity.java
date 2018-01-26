package com.example.cpu10661.customnotification;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;

public class PopupActivity extends Activity {

    public static final String ARG_EXTRA = "extra";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // get title
        String extra = getIntent().getStringExtra(ARG_EXTRA);
        String title;
        if (extra.startsWith("+")) {
            int morePhotos = Integer.valueOf(extra.substring(1));
            title = getResources().getQuantityString(R.plurals.more_photos, morePhotos, morePhotos);
        } else {
            title = String.format(getString(R.string.photo_clicked), extra);
        }

        // show dialog
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setIcon(R.mipmap.ic_launcher);
        alertDialog.setButton(Dialog.BUTTON_POSITIVE, getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                });
        alertDialog.show();
    }
}
