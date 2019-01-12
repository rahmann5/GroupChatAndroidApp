package com.example.naziur.groupchatandroidapp;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class CustomFirebaseMessageService extends FirebaseMessagingService {
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private static final String TAG = "FirebaseMessageService";
    private static final int NOTIFICATION_ID = 100;
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        showNotification(
                remoteMessage.getNotification().getBody(),
                remoteMessage.getNotification().getTitle()
                );
    }

    private void showNotification(String message, String title){
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.putExtra("group_id",title);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationManager mNotificationManager =
                (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), title)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setLargeIcon(BitmapFactory.decodeResource(getApplicationContext().getResources(),
                        R.mipmap.ic_launcher_round))
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent);


        //dToken to ensure same chat doesn't create different notification each time
        mNotificationManager.notify(title, NOTIFICATION_ID, notificationBuilder.build());
    }

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        if (s != null) {
            Log.d(TAG, "Refreshed token: " + s);
            updateTokenOnServer(s);
        }
    }

    private void updateTokenOnServer(final String strToken){
        final FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if(user != null){
        final DatabaseReference usersRef = database.getReference("users").orderByChild("email").equalTo(user.getEmail()).getRef();
        usersRef.runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                for (MutableData data : mutableData.getChildren()) {
                    FirebaseUserModel userModel = data.getValue(FirebaseUserModel.class);

                    if (userModel == null) return Transaction.success(mutableData);

                    if (strToken != null && userModel.getEmail().equals(user.getEmail()) && !strToken.equals(userModel.getDeviceToken())) {
                        userModel.setDeviceToken(strToken);
                        data.setValue(userModel);
                        break;
                    }
                }
                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (databaseError != null) {
                    Log.e(TAG, databaseError.getMessage());
                }
            }
        });
        }
    }

}


