package com.example.naziur.groupchatandroidapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import cz.msebera.android.httpclient.HttpHeaders;
import cz.msebera.android.httpclient.entity.StringEntity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private String groupKey;
    private ArrayList<String> messages;
    private ValueEventListener messageListener;
    public static final String TAG = "ChatActivity";
    private EditText messageEt;
    private Button sendBtn;
    private JSONArray registrationIds;
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();
    public static final String NOTIFICATION_URL = "https://fcm.googleapis.com/fcm/send";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Bundle extra = getIntent().getExtras();
        if (extra == null) {
            finish();
            return;
        }
        registrationIds = new JSONArray();
        messages = new ArrayList<>();
        groupKey = extra.getString("group_id");
        getSupportActionBar().setTitle(groupKey);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        messageListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    messages.clear();
                    for(DataSnapshot snapshot: dataSnapshot.getChildren()){
                        FirebaseMessageModel firebaseMessageModel = snapshot.getValue(FirebaseMessageModel.class);
                        if(firebaseMessageModel.getSenderEmail().equals(mAuth.getCurrentUser().getEmail()))
                            messages.add(firebaseMessageModel.getBody());
                        else {
                            messages.add(firebaseMessageModel.getSenderEmail()+": "+firebaseMessageModel.getBody());
                        }
                    }
                    updateUI();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, databaseError.getMessage());
                databaseError.toException().printStackTrace();
            }
        };

        messageEt = findViewById(R.id.message_et);
        sendBtn = findViewById(R.id.send_btn);
        sendBtn.setEnabled(false);
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!messageEt.getText().toString().isEmpty() && registrationIds != null){
                    sendBtn.setEnabled(false);
                    sendMessage();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        getGroupInfo();
        showAllMessages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAuth.getCurrentUser() == null) {
            Intent home = new Intent(this, MainActivity.class);
            home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(home);
            finish();
        }
    }

    private void showAllMessages(){
        Query reference = database.getReference("messages").child(groupKey);
        reference.addValueEventListener(messageListener);
    }

    private void updateUI(){
        ListView listView = findViewById(R.id.message_list);
        ArrayAdapter arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, messages){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView textView = (TextView) super.getView(position, convertView, parent);
                if(textView.getText().toString().split(":").length == 1)
                    textView.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);
                return textView;
            }
        };
        listView.setAdapter(arrayAdapter);
    }

    private void sendMessage(){
        DatabaseReference newRef = database.getReference("messages").child(groupKey).push();
        newRef.setValue(makeMessageNode(), new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                StringEntity entity = getGroupMessageEntity();
                createAsyncClient().post(getApplicationContext(), NOTIFICATION_URL, entity, RequestParams.APPLICATION_JSON, new TextHttpResponseHandler() {
                    @Override
                    public void onFailure(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString, Throwable throwable) {
                        sendBtn.setEnabled(true);
                        Log.e(TAG, responseString + ": " + throwable.getMessage());
                    }

                    @Override
                    public void onSuccess(int statusCode, cz.msebera.android.httpclient.Header[] headers, String responseString) {
                        sendBtn.setEnabled(true);
                        messageEt.setText("");
                    }
                });

            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public static AsyncHttpClient createAsyncClient () {

        AsyncHttpClient client = new AsyncHttpClient();

        client.addHeader(HttpHeaders.AUTHORIZATION, "key=YOUR SERVER KEY");
        client.addHeader(HttpHeaders.CONTENT_TYPE, RequestParams.APPLICATION_JSON);
        return client;
    }

    private FirebaseMessageModel makeMessageNode(){
        FirebaseMessageModel firebaseMessageModel = new FirebaseMessageModel();
        firebaseMessageModel.setBody(messageEt.getText().toString());
        firebaseMessageModel.setSenderEmail(FirebaseAuth.getInstance().getCurrentUser().getEmail());
        return firebaseMessageModel;
    }

    private void getGroupInfo(){
        Query newRef = database.getReference("groups").orderByChild("groupName").equalTo(groupKey);
        newRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                        FirebaseGroupModel firebaseGroupModel =snapshot.getValue(FirebaseGroupModel.class);
                        if(firebaseGroupModel.getGroupName().equals(groupKey)){
                            getDeviceTokenForGroup(firebaseGroupModel);
                            break;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
            }
        });
    }

    private void getDeviceTokenForGroup(final FirebaseGroupModel firebaseGroupModel){
        Query newRef = database.getReference("users").orderByChild("email");
        newRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    for(DataSnapshot snapshot : dataSnapshot.getChildren()){
                        FirebaseUserModel firebaseUserModel = snapshot.getValue(FirebaseUserModel.class);
                        String[] members = firebaseGroupModel.getMembers().split(",");
                        for(String s : members){
                            if(s.equals(firebaseUserModel.getEmail()) && !s.equals(FirebaseAuth.getInstance().getCurrentUser().getEmail())){
                                System.out.println("Adding token "+firebaseUserModel.getDeviceToken());
                                registrationIds.put(firebaseUserModel.getDeviceToken());
                            }
                        }
                    }
                    sendBtn.setEnabled(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                databaseError.toException().printStackTrace();
            }
        });
    }

    private StringEntity getGroupMessageEntity(){
        JSONObject params = new JSONObject();
        StringEntity entity = null;

        try{
            params.put("registration_ids", registrationIds);
            JSONObject notificationObject = new JSONObject();
            notificationObject.put("click_action", ".MainActivity");
            notificationObject.put("body", mAuth.getCurrentUser().getDisplayName()+": "+messageEt.getText().toString());
            notificationObject.put("title", groupKey);
            params.put("notification", notificationObject);
            entity = new StringEntity(params.toString());
        } catch(Exception e){
            e.printStackTrace();
        }

        return entity;
    }
}
