package com.example.uber_clone;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class RiderActivity extends AppCompatActivity {

    private EditText email_input, password_input;
    private Button registration_button, login_button;
    private FirebaseAuth authentication;
    private FirebaseAuth.AuthStateListener firebaseAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider);


        email_input = (EditText) findViewById(R.id.email);
        password_input = (EditText) findViewById(R.id.password);
        registration_button = (Button) findViewById(R.id.registration);
        login_button = (Button) findViewById(R.id.login);

        authentication = FirebaseAuth.getInstance();
        firebaseAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                if(user!= null){
                    Intent intent = new Intent(RiderActivity.this, RiderMap.class);
                    startActivity(intent);
                    finish();
                    return;
                }


            }
        };



        registration_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = email_input.getText().toString();
                final String password = password_input.getText().toString();
                authentication.createUserWithEmailAndPassword(email,password).addOnCompleteListener(RiderActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){

                            Toast.makeText(RiderActivity.this, "Sign up Error", Toast.LENGTH_SHORT).show();

                        }else{
                            String  user_id = authentication.getCurrentUser().getUid();
                            DatabaseReference current_user_db = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(user_id);
                            current_user_db.setValue(true);

                        }
                    }
                });
            }
        });


        login_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = email_input.getText().toString();
                final String password = password_input.getText().toString();
                authentication.createUserWithEmailAndPassword(email,password).addOnCompleteListener(RiderActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if(!task.isSuccessful()){

                            Toast.makeText(RiderActivity.this, "Sign up Error", Toast.LENGTH_SHORT).show();

                        }
                    }
                });

            }
        });


    }

    @Override
    protected void onStart(){
        super.onStart();
        authentication.addAuthStateListener(firebaseAuthListener);

    }
    @Override
    protected void onStop(){
        super.onStop();
        authentication.removeAuthStateListener( firebaseAuthListener);

    }



}
