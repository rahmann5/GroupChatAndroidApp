package com.example.naziur.groupchatandroidapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

public class MainActivity extends AppCompatActivity {
    private FirebaseAuth mAuth = FirebaseAuth.getInstance();;
    private static final String TAG = MainActivity.class.getCanonicalName();
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;
    private FirebaseUserModel firebaseUserModel;
    private LinearLayout loginContainer;
    private Button signOutButton, chatsButton;
    private DatabaseReference usersRef;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        signOutButton = findViewById(R.id.sign_out_button);
        chatsButton = findViewById(R.id.chats_button);
        loginContainer = findViewById(R.id.login_container);
        usersRef = database.getReference("users");
        chatsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                moveToChat();
            }
        });
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
            }
        });
        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signIn();
            }
        });
    }

    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    private void signOut() {
        // Google sign out
        mGoogleSignInClient.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        updateUIButtons(false);
                    }
                });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign In was successful, authenticate with Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                // Google Sign In failed, update UI appropriately
                Log.d(TAG, "Google Sign In failed");
                e.printStackTrace();
                // ...
            }
        }
    }

    private void firebaseAuthWithGoogle(final GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            firebaseUserModel = new FirebaseUserModel();
                            firebaseUserModel.setEmail(account.getEmail());
                            firebaseUserModel.setName(account.getDisplayName());
                            FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
                                @Override
                                public void onSuccess(InstanceIdResult instanceIdResult) {
                                    firebaseUserModel.setDeviceToken(instanceIdResult.getToken());
                                    updateUI();
                                }
                            });
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in and update UI accordingly.
        FirebaseAuth user = FirebaseAuth.getInstance();
        if(user.getCurrentUser() != null){
            updateUIButtons(true);
        } else {
            updateUIButtons(false);
        }
    }

    private void updateUI(){
        FirebaseAuth user = FirebaseAuth.getInstance();
        if(user.getCurrentUser() != null){
            updateUIButtons(true);
            verifyUser();
        } else {
            updateUIButtons(false);
        }
    }

    private void updateUIButtons (boolean show) {
        if (show) {
            loginContainer.setVisibility(View.GONE);
            signOutButton.setVisibility(View.VISIBLE);
            chatsButton.setVisibility(View.VISIBLE);
        } else {
            loginContainer.setVisibility(View.VISIBLE);
            signOutButton.setVisibility(View.GONE);
            chatsButton.setVisibility(View.GONE);
        }

    }

    private void verifyUser(){
        usersRef.orderByChild("email").equalTo(firebaseUserModel.getEmail()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(!dataSnapshot.exists()){
                    addUserToDatabase(firebaseUserModel);
                } else {
                    moveToChat();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.d(TAG, databaseError.getMessage());
            }
        });
    }

    public void moveToChat() {
        Intent intent;
        Bundle extra = getIntent().getExtras();
        if (extra != null) {
            if (extra.getString("group_id") != null) {
                intent = new Intent(MainActivity.this, ChatActivity.class);
                intent.putExtra("group_id", extra.getString("group_id"));
                startActivity(intent);
            }
        } else {
            intent = new Intent(MainActivity.this, ChatListActivity.class);
            startActivity(intent);
        }
    }

    private void addUserToDatabase(FirebaseUserModel firebaseUserModel){
        usersRef.push().setValue(firebaseUserModel, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError == null) {
                    moveToChat();
                } else {
                    Log.d(TAG, databaseError.getMessage());
                }
            }
        });
    }

}
