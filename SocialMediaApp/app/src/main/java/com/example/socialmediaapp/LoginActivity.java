package com.example.socialmediaapp;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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

public class LoginActivity extends AppCompatActivity {

    EditText mEmailEt , mPasswordEt;
    TextView notHaveAccountTv , mRecoverPassTv;
    Button mLoginBtn;

    //progress bar displayed while registering user
    ProgressDialog pd;


    //Declare FirebaseAuth
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);



        //intialize FirebaseAuth
        mAuth = FirebaseAuth.getInstance();


        //init
        mEmailEt = (EditText) findViewById(R.id.emailET);
        mPasswordEt = (EditText) findViewById(R.id.passwordEt);
        notHaveAccountTv = (TextView) findViewById(R.id.not_have_accountTV);
        mRecoverPassTv = (TextView) findViewById(R.id.recoverPassTV);
        mLoginBtn = (Button) findViewById(R.id.login);


        //login button click
        mLoginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // input email and password
                String email = mEmailEt.getText().toString();
                String passw = mPasswordEt.getText().toString().trim();

                //validate input


                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        //set error and focus to email ET
                        mEmailEt.setError("Invalid Email");
                        mEmailEt.setFocusable(true);
                    } else {
                        loginUser(email, passw); //register the user
                    }

            }
        });

        //not have account textview click
        notHaveAccountTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this,RegisterActivity.class));
                finish();
            }
        });

        //recover password textview click
        mRecoverPassTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRecoverPasswordDialog();
            }
        });


        //init progress bar
        pd = new ProgressDialog( this);


    }

    private void showRecoverPasswordDialog() {
        //alertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Forgot Password");

        //set layout linear
        LinearLayout linearLayout = new LinearLayout(this);

        //view in dialog
        final EditText emailEt= new EditText(this);
        emailEt.setHint("Email");
        emailEt.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        // set min width of editview to fit text of letters regardless of acutal text size
        emailEt.setMinEms(16);

        linearLayout.addView(emailEt);
        linearLayout.setPadding(10,10,10,10);

        builder.setView(linearLayout);

        //buttons recover
        builder.setPositiveButton("Forgot", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                //input email
                String email = emailEt.getText().toString().trim();
                beginRecovery(email);

            }
        });

        //buttons cancel
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                //dismiss dialog
                dialog.dismiss();
            }
        });

        builder.create().show();

    }

    private void beginRecovery(String email) {

        //show progress bar
        pd.setMessage("Sending email ...");
        pd.show();
        mAuth.sendPasswordResetEmail(email).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                pd.dismiss();
                if(task.isSuccessful()){
                    Toast.makeText(LoginActivity.this,"Email Sent",Toast.LENGTH_SHORT).show();

                }
                else {
                    Toast.makeText(LoginActivity.this,"Failed ...",Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {

                pd.dismiss();
                //show error message
                Toast.makeText(LoginActivity.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loginUser(String email, String passw) {

        //show progress bar
        pd.setMessage("Logging In ...");
        pd.show();
        mAuth.signInWithEmailAndPassword(email, passw)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            pd.dismiss();
                            // Sign in success, update UI with the signed-in user's information
                            FirebaseUser user = mAuth.getCurrentUser();

                            if(task.getResult().getAdditionalUserInfo().isNewUser()) {

                                //get user email and uid from auth
                                String email = user.getEmail();
                                String uid = user.getUid();

                                //when user is registered store user info in firebase realtime database

                                //using hashmap
                                HashMap<Object, String> hashMap = new HashMap<>();

                                //put data info in hashmap
                                hashMap.put("email", email);
                                hashMap.put("uid", uid);
                                hashMap.put("name", "");//will be add later (edit profile)
                                hashMap.put("onlineStatus","online");
                                hashMap.put("typingTo","noOne");
                                hashMap.put("phone", "");//will be add later (edit profile)
                                hashMap.put("image", "");//will be add later (edit profile)
                                hashMap.put("cover","");//will be add later (edit profile)

                                //firebase database instances
                                FirebaseDatabase database = FirebaseDatabase.getInstance();

                                //path to stroe user data named "Users"
                                DatabaseReference reference = database.getReference("Users");

                                //put data within hashmap in database
                                reference.child(uid).setValue(hashMap);

                            }

                            //user is logged in ,so start LoginActivity
                            startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                            finish();

                        } else {
                            pd.dismiss();
                            // If sign in fails, display a message to the user.
                            Toast.makeText(LoginActivity.this, "Authentication failed.",Toast.LENGTH_SHORT).show();

                        }


                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //error , dismiss progress dialog and get error message
                pd.dismiss();
                Toast.makeText(LoginActivity.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });


    }


}
