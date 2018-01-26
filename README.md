# PhotoGridNotification
A demonstration of notification capability to contain photos

# Approaches
This demo consists of 2 approaches:
1. [BigPictureStyle](https://developer.android.com/reference/android/support/v4/app/NotificationCompat.BigPictureStyle.html): this is an Android built-in style of notification. It's easy and simple to implement, required only a few additional lines of code. 
```java 
final NotificationCompat.BigPictureStyle style = new NotificationCompat.BigPictureStyle();
style.bigPicture(mPhotoItem);
builder.setStyle(style);
```
Because of the easy implementation, the feature with be limited to merely one picture for the whole notification.

2. [RemoteViews](https://developer.android.com/reference/android/widget/RemoteViews.html): this approach is a little more complicated than the previous method. Think of this as a custom view implementation, but with RemoteViews (the view used in app widget).  
Notwithstanding the complicated implementation of appropriate RemoteViews (not covered in here), it is actually quite straightforward to insert the view.
```java
RemoteViews bigContentView = getPhotosGridView(photosPerRow, totalPhotos);
builder.setContentTitle(getString(R.string.custom_notification_demo))
        .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
        .setCustomBigContentView(bigContentView);
```
Similarly, the nofication collapsed view can be set with setCustomContentView.  
Since it is RemoteViews, each view can be tied with an onClick event as long as it has its own distinguished ID. This can be done with [setOnClickPendingIntent](https://developer.android.com/reference/android/widget/RemoteViews.html).

Keep in mind that in some circumstances, you will have to thoughtfully think about how you layout your view to perfectly suit the notification space.

# Additional info
* The maximum number of action buttons is 3 per notification.
* The maximum height of expanded notification is 256dp.
* The height of action buttons is the same as the actionBarSize.
