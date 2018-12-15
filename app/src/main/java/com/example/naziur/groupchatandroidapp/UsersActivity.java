package com.example.naziur.groupchatandroidapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends AppCompatActivity {
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();;
    private ListView allUsers;
    private ArrayAdapter<String> adapter;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final DatabaseReference databaseRef = database.getReference().child("users");
        allUsers = (ListView) findViewById( R.id.all_contacts );
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_checked, new ArrayList<String>());
        allUsers.setAdapter(adapter);
        allUsers.setEmptyView(findViewById(android.R.id.empty));
        databaseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<String> users = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (com.google.firebase.database.DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        FirebaseUserModel mUser =  userSnapshot.getValue(FirebaseUserModel.class);
                        if (!mAuth.getCurrentUser().getEmail().equals(mUser.getEmail())) {
                            users.add(mUser.getEmail());
                        }
                    }
                    adapter.addAll(users);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.i(UsersActivity.class.getSimpleName(), databaseError.getMessage());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.chat_menu, menu);
        return true;
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.done:
                createGroup();
                break;
            case android.R.id.home:
                this.finish();
                break;
        }
        return true;
    }

    private void createGroup() {
        final SparseBooleanArray checkedItems = allUsers.getCheckedItemPositions();
        final FirebaseGroupModel groupModel = new FirebaseGroupModel();
        Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        // unique ID and name of group
        groupModel.setGroupName("group_"+timestamp.getTime());
        StringBuilder members = new StringBuilder();
        members.append(mAuth.getCurrentUser().getEmail());
        int membersCount = 0;
        for (int i = 0; i < checkedItems.size(); i++){
            int key = checkedItems.keyAt(i);
            if (checkedItems.get(key)) {
                membersCount++;
                members.append(",") ;
                members.append(adapter.getItem(i)) ;
            }
        }

        groupModel.setMembers(members.toString());
        if (membersCount > 0) {
            //create group
            DatabaseReference newRef = database.getReference("groups").push();
            newRef.setValue(groupModel, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                    if(databaseError != null){
                        Log.i(UsersActivity.class.getSimpleName(), databaseError.getMessage());
                    } else {
                        Intent chatActivity = new Intent(UsersActivity.this, ChatActivity.class);
                        chatActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        chatActivity.putExtra("group_id", groupModel.getGroupName());
                        startActivity(chatActivity);
                    }
                }
            });
        } else {
            Toast.makeText(this, "No members selected", Toast.LENGTH_SHORT).show();
        }

    }
}
