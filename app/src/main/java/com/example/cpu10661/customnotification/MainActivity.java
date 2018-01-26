package com.example.cpu10661.customnotification;

import android.animation.LayoutTransition;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IntDef;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.Switch;

import com.bumptech.glide.Glide;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String CHANNEL_ID = "CHANNEL_01";
    private static final int NOTIFICATION_ID = 1;

    private static final int TYPE_BIG_PICTURE_STYLE = 0;
    private static final int TYPE_REMOTE_VIEWS = 1;
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TYPE_BIG_PICTURE_STYLE, TYPE_REMOTE_VIEWS})
    private @interface NOTIFICATION_TYPE{}
    @NOTIFICATION_TYPE
    private int mNotificationType = TYPE_REMOTE_VIEWS;

    private Switch mCompactModeSwitch, mLandscapeSwitch;
    private AdjustNumberView mTotalButtonsANV, mTotalPhotosANV, mPhotosPerRowANV;

    private NotificationManager mNotificationManager;
    private int mMaxGridHeight;
    private Bitmap mPhotoItem;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeNotificationComponents();

        initializeUiComponents();
    }

    private void initializeNotificationComponents() {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "channel_01";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mNotificationManager.createNotificationChannel(mChannel);
        }
    }

    private void initializeUiComponents() {
        findViewById(R.id.btn_show_notifications).setOnClickListener(this);
        findViewById(R.id.rb_big_picture_style).setOnClickListener(this);
        findViewById(R.id.rb_remote_views).setOnClickListener(this);

        mTotalButtonsANV = findViewById(R.id.anv_total_buttons);
        mTotalPhotosANV = findViewById(R.id.anv_total_photos);
        mPhotosPerRowANV = findViewById(R.id.anv_photos_per_row);

        mCompactModeSwitch = findViewById(R.id.sw_compact_mode);
        mLandscapeSwitch = findViewById(R.id.sw_landscape);

        LinearLayout optionsLinearLayout = findViewById(R.id.ll_options);
        optionsLinearLayout.setLayoutTransition(new LayoutTransition());
    }

    /**
     * calculate bitmap's width and height in order to perfectly fit them into each row
     * of GridView layout
     * this method is synchronously executed, hence comes the "Sync" suffix
     *
     * @param itemsPerRow number of items (photos) per row
     */
    private void retrievePhotoItemSync(int itemsPerRow) {
        // get screen width
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        float width = displayMetrics.widthPixels;

        // get padding
        float outerPadding = Utils.dpToPx(this, 16) * 2;
        float gridSpace = getResources().getDimension(R.dimen.grid_item_space);
        float padding = outerPadding + gridSpace * (itemsPerRow - 1);

        int imgRes = mLandscapeSwitch.isChecked() ?
                R.drawable.grid_photo_item_land :
                R.drawable.grid_photo_item_port;
        // get image width and height
        BitmapFactory.Options dimensions = new BitmapFactory.Options();
        dimensions.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(), imgRes, dimensions);

        // retrieve photo with calculated size
        int photoWidth = (int) ((width - padding) / itemsPerRow);
        int photoHeight = photoWidth * dimensions.outHeight / dimensions.outWidth;
        try {
            mPhotoItem = Glide.with(this).asBitmap().load(imgRes)
                    .submit(photoWidth, photoHeight)
                    .get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        // calculate maximum grid view height
        float titleHeight = Utils.dpToPx(this, 16);
        mMaxGridHeight = (int) (Utils.dpToPx(this, 256) - (outerPadding + titleHeight));
    }

    private NotificationCompat.Builder getNotificationBuilder() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                .setContentIntent(getNotificationActionPendingIntent())
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            builder.setPriority(Notification.PRIORITY_HIGH)
                    .setDefaults(Notification.DEFAULT_ALL);
        }

        int totalButtons = mTotalButtonsANV.getNumberValue();
        for (int i = 0; i < totalButtons; i++) {
            String text = "button " + i;
            builder.addAction(new NotificationCompat.Action(
                    R.drawable.ic_notifications_black_24dp, text, null));
        }

        return builder;
    }

    private PendingIntent getNotificationActionPendingIntent() {
        Intent resultIntent = new Intent(this, MainActivity.class);
        return PendingIntent.getActivity(this, 0,
                resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private void showNotificationBigPictureStyle() {

        final NotificationCompat.Builder builder = getNotificationBuilder();
        builder.setContentTitle(getString(R.string.big_picture_style_demo));

        final NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle();
        style.bigPicture(mPhotoItem);
        builder.setStyle(style);

        if (mNotificationManager != null) {
            mNotificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private void showNotificationRemoteViews() {
        final NotificationCompat.Builder builder = getNotificationBuilder();

        RemoteViews bigContentView = getPhotosGridView(mPhotosPerRowANV.getNumberValue(),
                mTotalPhotosANV.getNumberValue());
        builder.setContentTitle(getString(R.string.custom_notification_demo))
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomBigContentView(bigContentView);

        if (mNotificationManager != null) {
            mNotificationManager.notify(NOTIFICATION_ID, builder.build());
        }
    }

    private RemoteViews getPhotosGridView(int photoPerRow, int totalPhotos) {

        // recalculate grid's height if it has action buttons
        int maxGridHeight = mMaxGridHeight;
        if (mTotalButtonsANV.getNumberValue() > 0) {
            maxGridHeight -= getSystemActionBarHeight();
        }

        // add photos part
        float gridSpace = getResources().getDimension(R.dimen.grid_item_space);
        float curHeight = 0, rowHeight = (int) (mPhotoItem.getHeight() + gridSpace);
        RemoteViews gridView = new RemoteViews(getPackageName(), R.layout.notification_layout_expanded);
        Bitmap photoItem = mPhotoItem;
        RemoteViews lastItem = null;
        int morePhotos = totalPhotos;
        for (int i = 0; i < totalPhotos; i += photoPerRow) {
            RemoteViews rowView = new RemoteViews(getPackageName(), R.layout.horizontal_linear_layout);

            // TODO: 1/25/18 fix portrait images not showing
            // check if it has reached the last row
            int residualSpace = (int) (maxGridHeight - curHeight);
            if (residualSpace < rowHeight) {
                // and in compact mode
                if (mCompactModeSwitch.isChecked() && i > 0) {
                    break;
                } /* or not */ else {
                    photoItem = Bitmap.createBitmap(mPhotoItem, 0, 0,
                            mPhotoItem.getWidth(), residualSpace);
                }
            }

            // add photos to grid
            RemoteViews photoView = null;
            for (int j = 0; j < photoPerRow && i + j < totalPhotos; j++) {

                photoView = new RemoteViews(getPackageName(),
                        j < photoPerRow - 1 ? R.layout.grid_item : R.layout.grid_last_row_item);
                photoView.setImageViewBitmap(R.id.iv_photo, photoItem);
                setOnClickPendingIntent(photoView, i + j, String.valueOf(i + j));

                rowView.addView(R.id.ll_row, photoView);
                morePhotos--;
            }
            lastItem = photoView;
            gridView.addView(R.id.ll_root, rowView);

            // if it exceeds acceptable limit
            curHeight += rowHeight;
            if (curHeight > maxGridHeight) {
                break;
            }
        }

        // update last item if it is in compact mode
        if (mCompactModeSwitch.isChecked() && morePhotos > 0) {
            photoItem = getCompactModeBitmap(photoItem, morePhotos);
            if (lastItem != null) {
                lastItem.setImageViewBitmap(R.id.iv_photo, photoItem);
                setOnClickPendingIntent(lastItem, totalPhotos, "+" + morePhotos);
            }
        }

        return gridView;
    }

    private int getSystemActionBarHeight() {
        TypedValue tv = new TypedValue();
        if (getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
        }
        return 0;
    }

    private void setOnClickPendingIntent(RemoteViews remoteViews, int requestCode, String extra) {
        Intent intent = new Intent(this, PopupActivity.class);
        intent.putExtra(PopupActivity.ARG_EXTRA, extra);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, requestCode,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.iv_photo, pendingIntent);
    }

    /**
     * get a bitmap with darkened background and overlaid text indicating the number of hidden photos
     * this method is only used in compact mode, and hence comes the method's name.
     *
     * @param src the original bitmap
     * @param morePhotos the number of hidden photos
     * @return the designated bitmap
     */
    private Bitmap getCompactModeBitmap(Bitmap src, int morePhotos) {
        src = Utils.darkenBitmap(src);
        return Utils.addTextToBitmap(this, src, "+" + morePhotos);
    }

    @Override
    public void onClick(View view) {
        HandlerThread handlerThread = new HandlerThread(getPackageName());
        handlerThread.start();
        Handler handler = new Handler(handlerThread.getLooper());
        switch (view.getId()) {
            case R.id.btn_show_notifications:
                switch (mNotificationType) {
                    case TYPE_BIG_PICTURE_STYLE:
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                retrievePhotoItemSync(mPhotosPerRowANV.getNumberValue());
                                showNotificationBigPictureStyle();
                            }
                        });
                        break;
                    case TYPE_REMOTE_VIEWS:
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                retrievePhotoItemSync(mPhotosPerRowANV.getNumberValue());
                                showNotificationRemoteViews();
                            }
                        });
                        break;
                    default:
                        throw new UnsupportedOperationException("Unknown notification type");
                }
                break;
            case R.id.rb_big_picture_style:
                mNotificationType = TYPE_BIG_PICTURE_STYLE;
                setRemoteViewsOptionsEnabled(false);
                break;
            case R.id.rb_remote_views:
                mNotificationType = TYPE_REMOTE_VIEWS;
                setRemoteViewsOptionsEnabled(true);
                break;
        }
    }

    @SuppressLint({"WrongConstant", "PrivateApi"})
    private void expandNotification() {
        Object statusBarService = getSystemService( "statusbar" );
        try {
            Class<?> statusBarManager = Class.forName( "android.app.StatusBarManager" );
            Method showStatusBarMethod = Build.VERSION.SDK_INT >= 17 ?
                    statusBarManager.getMethod("expandNotificationsPanel") :
                    statusBarManager.getMethod("expand");
            showStatusBarMethod.invoke( statusBarService );
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void setRemoteViewsOptionsEnabled(boolean enabled) {
        int visibility = enabled ? View.VISIBLE : View.GONE;
        mCompactModeSwitch.setVisibility(visibility);
        mTotalPhotosANV.setVisibility(visibility);
        mPhotosPerRowANV.setVisibility(visibility);
    }
}
