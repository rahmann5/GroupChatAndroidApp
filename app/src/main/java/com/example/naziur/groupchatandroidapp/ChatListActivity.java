package com.example.naziur.groupchatandroidapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatListActivity extends AppCompatActivity {

    private FirebaseAuth mAuth = FirebaseAuth.getInstance();;
    private ListView allGroups;
    private ArrayAdapter<FirebaseGroupModel> adapter;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_list);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final DatabaseReference databaseRef = database.getReference().child("groups");
        allGroups = (ListView) findViewById( R.id.all_groups );
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<FirebaseGroupModel>());
        allGroups.setAdapter(adapter);
        allGroups.setEmptyView(findViewById(android.R.id.empty));

        allGroups.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                FirebaseGroupModel groupModel = (FirebaseGroupModel) adapterView.getItemAtPosition(i);
                Intent chat = new Intent(ChatListActivity.this, ChatActivity.class);
                chat.putExtra("group_id", groupModel.getGroupName());
                chat.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(chat);
            }
        });

        databaseRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<FirebaseGroupModel> groups = new ArrayList<>();
                if (dataSnapshot.exists()) {
                    for (com.google.firebase.database.DataSnapshot groupSnapshot : dataSnapshot.getChildren()) {
                        FirebaseGroupModel groupModel =  groupSnapshot.getValue(FirebaseGroupModel.class);
                        String[] members  = groupModel.getMembers().split(",");
                        for (String member : members) {
                            if (member.equals(mAuth.getCurrentUser().getEmail())) {
                                groups.add(groupModel);
                            }
                        }

                    }
                    adapter.addAll(groups);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.i(ChatListActivity.class.getSimpleName(), databaseError.getMessage());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.user_menu, menu);
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
            case R.id.users:
                Intent usersActivity = new Intent(this, UsersActivity.class);
                usersActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(usersActivity);
                break;
            case android.R.id.home:
                this.finish();
                break;
        }
        return true;
    }
}
