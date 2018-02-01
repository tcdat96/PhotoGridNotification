package com.example.cpu10661.customnotification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.RawRes;
import android.support.v4.app.NotificationCompat;
import android.util.TypedValue;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.target.Target;

import java.util.concurrent.ExecutionException;

class Utils {
    static float dpToPx(Context context, int dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dp, context.getResources().getDisplayMetrics());
    }

    static int getSystemActionBarHeight(Context context) {
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
            return TypedValue.complexToDimensionPixelSize(tv.data,
                    context.getResources().getDisplayMetrics());
        }
        return 0;
    }

    static class NotificationUtils {

        private static final String CHANNEL_ID = "CHANNEL_01";
        private static final int NOTIFICATION_ID = 1;

        private static NotificationManager mNotificationManager;

        static void initializeNotificationComponents(@NonNull Context context) {
            mNotificationManager = (NotificationManager)
                    context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                CharSequence name = "channel_01";
                int importance = NotificationManager.IMPORTANCE_HIGH;
                NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
                mNotificationManager.createNotificationChannel(mChannel);
            }
        }

        static NotificationCompat.Builder getNotificationBuilder(@NonNull Context context,
                                                                 int totalButtons) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notifications_black_24dp)
                    .setContentIntent(getNotificationActionPendingIntent(context))
                    .setAutoCancel(true);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                builder.setPriority(Notification.PRIORITY_HIGH)
                        .setDefaults(Notification.DEFAULT_ALL);
            }

            for (int i = 0; i < totalButtons; i++) {
                String text = "button " + i;
                builder.addAction(new NotificationCompat.Action(
                        R.drawable.ic_notifications_black_24dp, text, null));
            }

            return builder;
        }

        private static PendingIntent getNotificationActionPendingIntent(@NonNull Context context) {
            Intent resultIntent = new Intent(context, MainActivity.class);
            return PendingIntent.getActivity(context, 0,
                    resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        static void updateNotification(NotificationCompat.Builder builder) {
            if (mNotificationManager != null) {
                mNotificationManager.notify(NOTIFICATION_ID, builder.build());
            }
        }

        static void removeAllNotifications() {
            if (mNotificationManager != null) {
                mNotificationManager.cancelAll();
            }
        }

//        @SuppressLint({"WrongConstant", "PrivateApi"})
//        private void expandNotification(Context context) {
//            Object statusBarService = context.getSystemService( "statusbar" );
//            try {
//                Class<?> statusBarManager = Class.forName( "android.app.StatusBarManager" );
//                Method showStatusBarMethod = Build.VERSION.SDK_INT >= 17 ?
//                        statusBarManager.getMethod("expandNotificationsPanel") :
//                        statusBarManager.getMethod("expand");
//                showStatusBarMethod.invoke( statusBarService );
//            } catch (ClassNotFoundException e) {
//                e.printStackTrace();
//            } catch (NoSuchMethodException e) {
//                e.printStackTrace();
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//            } catch (InvocationTargetException e) {
//                e.printStackTrace();
//            }
//        }
    }

    static class BitmapUtils {

        static final int SIZE_ORIGINAL = Target.SIZE_ORIGINAL;

        static Bitmap getThumbnail(@NonNull Context context, @NonNull Uri uri,
                                   int width, int height) {
            Bitmap bitmap = null;
            try {
                bitmap = GlideApp.with(context).asBitmap().load(uri)
                        .skipMemoryCache(true)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .submit()
                        .get();
                if (width != SIZE_ORIGINAL && height != SIZE_ORIGINAL) {
                    return ThumbnailUtils.extractThumbnail(bitmap, width, height,
                            ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        static Bitmap getThumbnail(@NonNull Context context, @RawRes @DrawableRes int resId,
                                   int width, int height) {
            Bitmap bitmap = null;
            try {
                bitmap = Glide.with(context).asBitmap().load(resId)
                        .submit(width, height)
                        .get();
                if (width != SIZE_ORIGINAL && height != SIZE_ORIGINAL) {
                    bitmap = ThumbnailUtils.extractThumbnail(bitmap, width, height);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
            return bitmap;
        }

        static Bitmap getThumbnail(@NonNull Context context, @NonNull Uri uri) {
            return getThumbnail(context, uri, SIZE_ORIGINAL, SIZE_ORIGINAL);
        }

        static Bitmap getThumbnail(@NonNull Context context, @RawRes @DrawableRes int resId) {
            return getThumbnail(context, resId, SIZE_ORIGINAL, SIZE_ORIGINAL);
        }

        static Bitmap darkenBitmap(@NonNull Bitmap src) {
            Bitmap result = src.copy(src.getConfig(), true);
            Canvas canvas = new Canvas(result);
            Paint p = new Paint(Color.RED);

            ColorFilter filter = new LightingColorFilter(0xFF666666, 0x00000000);    // darken
            p.setColorFilter(filter);
            canvas.drawBitmap(result, new Matrix(), p);

            return result;
        }

        static Bitmap addTextToBitmap(@NonNull Context context,
                                      @NonNull Bitmap src, @NonNull String text) {

            Resources resources = context.getResources();
            float scale = resources.getDisplayMetrics().density;

            android.graphics.Bitmap.Config bitmapConfig = src.getConfig();
            if(bitmapConfig == null) {
                bitmapConfig = android.graphics.Bitmap.Config.ARGB_8888;
            }
            src = src.copy(bitmapConfig, true);

            Canvas canvas = new Canvas(src);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.WHITE);
            paint.setTextSize((int) (src.getHeight() / 10 * scale));
            paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

            Rect bounds = new Rect();
            paint.getTextBounds(text, 0, text.length(), bounds);
            int x = (src.getWidth() - bounds.width())/2;
            int y = (src.getHeight() + bounds.height())/2;
            canvas.drawText(text, x, y, paint);

            return src;
        }
    }
}
