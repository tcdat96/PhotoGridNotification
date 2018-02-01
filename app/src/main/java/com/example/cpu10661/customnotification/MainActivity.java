package com.example.cpu10661.customnotification;

import android.animation.LayoutTransition;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.Switch;

import com.example.cpu10661.customnotification.Utils.BitmapUtils;
import com.example.cpu10661.customnotification.Utils.NotificationUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final Uri.Builder BASE_URI_BUILDER =
            Uri.parse("https://source.unsplash.com/random/?").buildUpon();

    private static final int TYPE_BIG_PICTURE_STYLE = 0;
    private static final int TYPE_REMOTE_VIEWS = 1;
    private static final int TYPE_GRID_LAYOUT = 2;
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TYPE_BIG_PICTURE_STYLE, TYPE_REMOTE_VIEWS, TYPE_GRID_LAYOUT})
    private @interface NOTIFICATION_TYPE{}
    @NOTIFICATION_TYPE
    private int mNotificationType = TYPE_REMOTE_VIEWS;

    private Switch mAsyncSwitch;
    private AdjustNumberView mTotalButtonsANV, mTotalPhotosANV, mPhotosPerRowANV;
    private GridLayout mGridLayout;         // only used in GridLayout option

    private Handler mHandler;
    private NotificationCompat.Builder mBuilder;
    private Bitmap mPlaceHolder;
    private int mPhotoSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        NotificationUtils.initializeNotificationComponents(this);

        initializeUiComponents();

        HandlerThread handlerThread = new HandlerThread(getPackageName());
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());
    }

    private void initializeUiComponents() {
        findViewById(R.id.btn_show_notifications).setOnClickListener(this);
        findViewById(R.id.rb_big_picture_style).setOnClickListener(this);
        findViewById(R.id.rb_remote_views).setOnClickListener(this);
        findViewById(R.id.rb_grid_layout).setOnClickListener(this);

        mTotalButtonsANV = findViewById(R.id.anv_total_buttons);
        mTotalPhotosANV = findViewById(R.id.anv_total_photos);
        mPhotosPerRowANV = findViewById(R.id.anv_photos_per_row);

        mAsyncSwitch = findViewById(R.id.sw_load_async);

        LinearLayout optionsLinearLayout = findViewById(R.id.ll_options);
        optionsLinearLayout.setLayoutTransition(new LayoutTransition());
    }

    private void computePhotoSize(int itemsPerRow) {
        // get screen width
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        float screenWidth = displayMetrics.widthPixels;

        // get padding
        float outerPadding = Utils.dpToPx(this, 16) * 2;
        float gridSpace = getResources().getDimension(R.dimen.grid_item_space);
        float padding = outerPadding + gridSpace * (itemsPerRow - 1);

//        int imgRes = mLandscapeSwitch.isChecked() ?
//                R.drawable.grid_photo_item_land :
//                R.drawable.grid_photo_item_port;
//        // get image width and height
//        BitmapFactory.Options dimensions = new BitmapFactory.Options();
//        dimensions.inJustDecodeBounds = true;
//        BitmapFactory.decodeResource(getResources(), imgRes, dimensions);

        // calc photo new size
        mPhotoSize = (int) ((screenWidth - padding) / itemsPerRow);
        getPlaceHolder();
    }

    private void showNotificationBigPictureStyle() {

        mBuilder = NotificationUtils.getNotificationBuilder(this, mTotalButtonsANV.getValue());
        mBuilder.setContentTitle(getString(R.string.big_picture_style_demo));

        final NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = getDemoBitmap(true);
                style.bigPicture(bitmap);
                mBuilder.setStyle(style);
                NotificationUtils.updateNotification(mBuilder);
            }
        });
    }

    private void showNotificationGridLayout() {
        mBuilder = NotificationUtils.getNotificationBuilder(this, mTotalButtonsANV.getValue());

        // get cell IDs
        int[] viewIds = new int[mGridLayout.getChildCount()];
        for (int i = 0; i < mGridLayout.getChildCount(); i++) {
            viewIds[i] = mGridLayout.getChildAt(i).getId();
        }

        // add photos to grid
        RemoteViews bigContentView = new RemoteViews(getPackageName(), R.layout.grid_layout);
        int totalPhotos = mGridLayout.getChildCount() - 1;      // arbitrary value
        for (int photoIdx = 0; photoIdx < totalPhotos;) {
            for (int j = 0; j < 2 && photoIdx < totalPhotos; photoIdx++, j++) {
                RemoteViews photoView = new RemoteViews(getPackageName(), R.layout.grid_item);
                setRemoteViewsImage(photoView);
                bigContentView.addView(viewIds[photoIdx], photoView);
            }
        }

        // add grid to notification
        mBuilder.setContentTitle(getString(R.string.use_grid_layout))
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomBigContentView(bigContentView);

        NotificationUtils.updateNotification(mBuilder);
    }

    private void showNotificationRemoteViews() {
        mBuilder = NotificationUtils.getNotificationBuilder(this, mTotalButtonsANV.getValue());

        int photosPerRow = mPhotosPerRowANV.getValue();
        int totalPhotos = mTotalPhotosANV.getValue();
        RemoteViews bigContentView = getPhotosGridView(photosPerRow, totalPhotos);

        mBuilder.setContentTitle(getString(R.string.use_remote_views))
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomBigContentView(bigContentView);

        NotificationUtils.updateNotification(mBuilder);
    }

    private RemoteViews getPhotosGridView(int photosPerRow, final int totalPhotos) {

        int maxGridHeight = computeMaxGridHeight();
        float gridSpace = getResources().getDimension(R.dimen.grid_item_space);
        float rowHeight = (int) (mPhotoSize + gridSpace);

        RemoteViews gridView = new RemoteViews(getPackageName(), R.layout.notification_layout_expanded);
        // add photos part
        RemoteViews lastItem = null;
        int photoIdx = 0;
        for (float curHeight = 0; photoIdx < totalPhotos;) {
            RemoteViews rowView = new RemoteViews(getPackageName(), R.layout.horizontal_linear_layout);

            // add photos to grid
            RemoteViews photoView = null;
            for (int j = 0; j < photosPerRow && photoIdx < totalPhotos; photoIdx++, j++) {
                photoView = new RemoteViews(getPackageName(),
                        j < photosPerRow - 1 ? R.layout.grid_item : R.layout.grid_last_row_item);
                setRemoteViewsImage(photoView);
                setOnClickPendingIntent(photoView, photoIdx, String.valueOf(photoIdx));

                rowView.addView(R.id.ll_row, photoView);
            }
            lastItem = photoView;
            gridView.addView(R.id.ll_root, rowView);

            // if it exceeds acceptable limit
            curHeight += rowHeight;
            if (curHeight + rowHeight > maxGridHeight) {
                break;
            }
        }

        // update last item if it is in compact mode
        final int morePhotos = totalPhotos - photoIdx;
        if (morePhotos > 0) {
            if (lastItem != null) {
                final RemoteViews finalLastItem = lastItem;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Bitmap src = getDemoBitmap(false);
                        if (src != null) {
                            Bitmap photoItem = getCompactModeBitmap(src, morePhotos);
                            setOnClickPendingIntent(finalLastItem, totalPhotos, "+" + morePhotos);
                            setRemoteViewsImage(finalLastItem, photoItem);
                        }
                    }
                });
            }
        }

        return gridView;
    }

    private int computeMaxGridHeight() {

        int maxGridHeight;

        // title
        float titleMargins = Utils.dpToPx(this, 16) * 2;
        float titleHeight = Utils.dpToPx(this, 16);
        maxGridHeight = (int) (Utils.dpToPx(this, 256) - (titleMargins + titleHeight));

        // recalculate grid's height if it has action buttons
        if (mTotalButtonsANV.getValue() > 0) {
            maxGridHeight -= Utils.getSystemActionBarHeight(this);
        }

        return maxGridHeight;
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
     * Keep in mind that this method will be executed SYNCHRONOUSLY
     *
     * @param morePhotos the number of hidden photos
     * @return the designated bitmap
     */
    private Bitmap getCompactModeBitmap(@NonNull Bitmap src, final int morePhotos) {
        Bitmap bitmap = BitmapUtils.darkenBitmap(src);
        return BitmapUtils.addTextToBitmap(MainActivity.this, bitmap, "+" + morePhotos);
    }

    private void setRemoteViewsImage(@NonNull RemoteViews remoteViews) {
        setRemoteViewsImage(remoteViews, R.id.iv_photo);
    }

    private void setRemoteViewsImage(@NonNull RemoteViews remoteViews, @NonNull final Bitmap bitmap) {
        setRemoteViewsImage(remoteViews, R.id.iv_photo, bitmap);
    }

    private void setRemoteViewsImage(@NonNull final RemoteViews remoteViews,
                                     @IdRes final int viewId) {
        remoteViews.setImageViewBitmap(viewId, mPlaceHolder);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                final Bitmap bitmap = getDemoBitmap(false);
                if (bitmap != null) {
                    remoteViews.setImageViewBitmap(viewId, bitmap);
                    // mute sound before notifying
                    mBuilder.setDefaults(Notification.DEFAULT_LIGHTS);
                    NotificationUtils.updateNotification(mBuilder);
                }
            }
        });
    }

    private void setRemoteViewsImage(@NonNull final RemoteViews remoteViews,
                                     @IdRes final int viewId, @NonNull Bitmap bitmap) {
        remoteViews.setImageViewBitmap(viewId, bitmap);
        NotificationUtils.updateNotification(mBuilder);
    }

    /**
     * get demo bitmap corresponding to the the async option
     *
     * Keep in mind that this method will be executed SYNCHRONOUSLY
     * and must be called in a background thread
     *
     *
     * @param originalSize use bitmap's original size if true
     * @return bitmap from resource if sync, bitmap from url if async
     */
    private Bitmap getDemoBitmap(boolean originalSize) {
        if (originalSize) {
            return mAsyncSwitch.isChecked() ?
                    BitmapUtils.getThumbnail(MainActivity.this, BASE_URI_BUILDER.build()) :
                    BitmapUtils.getThumbnail(MainActivity.this, R.drawable.grid_photo_item_land);
        } else
            return mAsyncSwitch.isChecked() ?
                    BitmapUtils.getThumbnail(MainActivity.this,
                            BASE_URI_BUILDER.build(), mPhotoSize, mPhotoSize) :
                    BitmapUtils.getThumbnail(MainActivity.this,
                            R.drawable.grid_photo_item_land, mPhotoSize, mPhotoSize);
    }

    private void getPlaceHolder() {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.placeholder_300);
        mPlaceHolder = ThumbnailUtils.extractThumbnail(bitmap, mPhotoSize, mPhotoSize);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_show_notifications:
                NotificationUtils.removeAllNotifications();
                showNotification();
                break;
            case R.id.rb_big_picture_style:
                mNotificationType = TYPE_BIG_PICTURE_STYLE;
                setRemoteViewsOptionsVisibility(View.GONE);
                break;
            case R.id.rb_remote_views:
                mNotificationType = TYPE_REMOTE_VIEWS;
                setRemoteViewsOptionsVisibility(View.VISIBLE);
                break;
            case R.id.rb_grid_layout:
                mNotificationType = TYPE_GRID_LAYOUT;
                setRemoteViewsOptionsVisibility(View.GONE);
                break;
        }
    }

    private void showNotification() {
        switch (mNotificationType) {
            case TYPE_BIG_PICTURE_STYLE:
                computePhotoSize(1);
                showNotificationBigPictureStyle();
                break;
            case TYPE_REMOTE_VIEWS:
                computePhotoSize(mPhotosPerRowANV.getValue());
                showNotificationRemoteViews();
                break;
            case TYPE_GRID_LAYOUT:
                mGridLayout = (GridLayout) View.inflate(
                        MainActivity.this, R.layout.grid_layout, null);
                computePhotoSize(mGridLayout.getColumnCount());
                showNotificationGridLayout();
                break;
            default:
                throw new UnsupportedOperationException("Unknown notification type");
        }
    }

    private void setRemoteViewsOptionsVisibility(int visibility) {
        mTotalPhotosANV.setVisibility(visibility);
        mPhotosPerRowANV.setVisibility(visibility);
    }
}
