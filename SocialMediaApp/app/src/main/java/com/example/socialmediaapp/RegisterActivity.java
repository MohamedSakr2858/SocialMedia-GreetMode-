package com.example.socialmediaapp;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    EditText mEmailEt , mPasswordEt;
    Button mRegisterBtn;
    TextView mHaveAccountTV;

    //progress bar displayed while registering user
    ProgressDialog progressDialog;

    //Declare FirebaseAuth
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);





        //initialize
        mEmailEt = (EditText) findViewById(R.id.emailET);
        mPasswordEt = (EditText) findViewById(R.id.passwordEt);
        mRegisterBtn = (Button) findViewById(R.id.register_btn);
        mHaveAccountTV = (TextView) findViewById(R.id.have_accountTV);




        //intialize FirebaseAuth
        mAuth = FirebaseAuth.getInstance();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registering User...");



        //handle rster btn click
        mRegisterBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // input email and password
                String email = mEmailEt.getText().toString().trim();
                String password = mPasswordEt.getText().toString().trim();

                //validate input
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    //set error and focus to email ET
                    mEmailEt.setError("Invalid Email");
                    mEmailEt.setFocusable(true);
                }
                else if (password.length()<6){
                    //set error and focus to password ET
                    mPasswordEt.setError("Password must be at least 6 characters");
                    mPasswordEt.setFocusable(true);
                }
                else
                {
                    registerUser(email,password); //register the user
                }
            }
        });

        //handle login textview click listener
        mHaveAccountTV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(RegisterActivity.this,LoginActivity.class));
                finish();

            }
        });

    }

    private void registerUser(String email, String password) {

        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success, dismiss dialog and start register activity
                            progressDialog.dismiss();
                            FirebaseUser user = mAuth.getCurrentUser();

                            //get user email and uid from auth
                            String email = user.getEmail();
                            String uid = user.getUid();

                            //when user is registered store user info in firebase realtime database

                            //using hashmap
                            HashMap<Object,String> hashMap = new HashMap<>();

                            //put data info in hashmap
                            hashMap.put("email",email);
                            hashMap.put("uid",uid);
                            hashMap.put("name","");//will be add later (edit profile)
                            hashMap.put("onlineStatus","online");
                            hashMap.put("typingTo","noOne");
                            hashMap.put("phone","");//will be add later (edit profile)
                            hashMap.put("image","");//will be add later (edit profile)
                            hashMap.put("cover","");//will be add later (edit profile)

                            //firebase database instances
                            FirebaseDatabase database = FirebaseDatabase.getInstance();

                            //path to stroe user data named "Users"
                            DatabaseReference reference = database.getReference("Users");

                            //put data within hashmap in database
                            reference.child(uid).setValue(hashMap);




                            Toast.makeText(RegisterActivity.this,"Registered...\n"+user.getEmail(),Toast.LENGTH_SHORT).show();

                        } else {
                            // If sign in fails, display a message to the user.
                            progressDialog.dismiss();
                            Toast.makeText(RegisterActivity.this, "Authentication failed.",Toast.LENGTH_SHORT).show();

                        }


                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //error , dismiss progress dialog and get error message
                progressDialog.dismiss();
                Toast.makeText(RegisterActivity.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });


    }


}
