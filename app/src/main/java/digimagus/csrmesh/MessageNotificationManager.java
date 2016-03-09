package digimagus.csrmesh;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import digimagus.csrmesh.acplug.NotificationActivity;

/**
 * Notification
 */
public class MessageNotificationManager {

    public NotificationManager manager = null;
    private static final int NOTIFICATION_FLAG = 1;

    private static class LazyHolder {
        private static final MessageNotificationManager INSTANCE = new MessageNotificationManager();
    }

    public static final MessageNotificationManager getInstance() {
        return LazyHolder.INSTANCE;
    }

    public void notificationManager(NotificationManager manager, Context context) {
        this.manager = manager;
        PendingIntent pendingIntent2 = PendingIntent.getActivity(context, 0, new Intent(context, NotificationActivity.class), 0);
        Notification notify2 = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.arrow_up_float)
                .setTicker("您有新短消息，请注意查收！")
                .setContentTitle("Notification Title")
                .setContentText("This is the notification message")
                .setContentIntent(pendingIntent2)
                .setNumber(1).build();
        notify2.flags |= Notification.FLAG_AUTO_CANCEL;
        manager.notify(NOTIFICATION_FLAG, notify2);
    }
}
