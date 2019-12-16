package com.example.socialmediaapp;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.support.v7.widget.Toolbar;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.socialmediaapp.adapters.AdapterChat;
import com.example.socialmediaapp.models.ModelChat;
import com.example.socialmediaapp.models.ModelUser;
import com.example.socialmediaapp.notifications.APIService;
import com.example.socialmediaapp.notifications.Client;
import com.example.socialmediaapp.notifications.Data;
import com.example.socialmediaapp.notifications.Response;
import com.example.socialmediaapp.notifications.Sender;
import com.example.socialmediaapp.notifications.Token;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;


public class ChatActivity extends AppCompatActivity {

    //views from xml
Toolbar toolbar;
RecyclerView recyclerView;
ImageView profileIv;
TextView nameTv,userStatusTv;
EditText messageEt;
ImageButton sendBtn;





    FirebaseAuth firebaseAuth;
FirebaseDatabase firebaseDatabase;
DatabaseReference usersDbRef;

//for checking if user has seen message or not
ValueEventListener seenListener;
DatabaseReference userRefForSeen;
List<ModelChat> chatList;
AdapterChat adapterChat;



String hisUid;
String myUid;
String hisImage;


APIService apiService;
boolean notify =false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        Toolbar toolbar=findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("");
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);


        recyclerView=findViewById(R.id.chat_recyclerView);
        profileIv=findViewById(R.id.profileIv);
        nameTv=findViewById(R.id.nameTv);
        userStatusTv=findViewById(R.id.userStatusTv);
        messageEt=findViewById(R.id.messageEt);
        sendBtn=findViewById(R.id.sendBtn);

//layout for recycler view
        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        //recycler view properties
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(linearLayoutManager);


        //create api service
        apiService = Client.getRetrofit("https://fcm.googleapis.com/").create(APIService.class);




//use intent to get profile picture and name to start chat with that


        Intent intent=getIntent();
        hisUid=intent.getStringExtra("hisUid");

//firebase auth instance
firebaseAuth=FirebaseAuth.getInstance();

firebaseDatabase=FirebaseDatabase.getInstance();
usersDbRef=firebaseDatabase.getReference("Users");

//serach user to get user"s info
        Query userQuery=usersDbRef.orderByChild("uid").equalTo(hisUid);
        //get user picture and name
        userQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//check until required info is recieved
                for (DataSnapshot ds:dataSnapshot.getChildren()) {

                    //get data
                    String name=""+ds.child("name").getValue();
                    hisImage=""+ds.child("image").getValue();
                    String typingStatus=""+ds.child("typingTo").getValue();

                     //check typing status
                    if (typingStatus.equals(myUid)){
                        userStatusTv.setText("typing...");
                    }
                    else{
                        //get value of online status
                        String onlineStatus=""+ds.child("onlineStatus").getValue();

                        if(onlineStatus.equals("online")){
                            userStatusTv.setText(onlineStatus);
                        }
                        else{
                            //convert timestamp to proper time date
                            //convert time stamp to dd /mm/yy/am/pm
                            Calendar cal=Calendar.getInstance(Locale.ENGLISH);
                            cal.setTimeInMillis(Long.parseLong(onlineStatus));
                            String dateTime= DateFormat.format("dd/MM/yyyy hh:mm aa",  cal).toString();
                            userStatusTv.setText("Last seen at: "+dateTime);
                        }



                    }




                    //set data
                    nameTv.setText(name);

                    try {
//image recieved ,set it to image view in toolbar
                        Picasso.get().load(hisImage).placeholder(R.drawable.ic_default_img_white).into(profileIv);
                    }
                    catch (Exception e){

//there is exception getting picture ,set default picture
Picasso.get().load(R.drawable.ic_default_img_white).into(profileIv);

                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
sendBtn.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View view) {
        notify = true;
        //get text from edit text
        String message=messageEt.getText().toString().trim();
        //check iof text is empty or not
        if (TextUtils.isEmpty(message)){
            //textempty
            Toast.makeText(ChatActivity.this,"cannot send empty message",Toast.LENGTH_SHORT).show();

        }
        else{
            //text not empty
            sendMessage(message);
        }
        //reset edittext after sending message
        messageEt.setText("");





    }
});

messageEt.addTextChangedListener(new TextWatcher() {
    @Override
    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
      if (s.toString().trim().length()==0){
          checkTypingStatus("noOne");
      }
      else{
          checkTypingStatus(hisUid);
      }
    }

    @Override
    public void afterTextChanged(Editable editable) {

    }
});





readMessages();
seenMessage();

    }

    private void seenMessage() {
        userRefForSeen=FirebaseDatabase.getInstance().getReference("Chats");
        seenListener=userRefForSeen.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot ds:dataSnapshot.getChildren()) {
                    ModelChat chat = ds.getValue(ModelChat.class);
                    if (chat.getReceiver().equals(myUid) && chat.getSender().equals(hisUid))
                    {
                       HashMap<String,Object>hasSeenHashMap=new HashMap<>();
                       hasSeenHashMap.put("isSeen",true);
                       ds.getRef().updateChildren(hasSeenHashMap);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }


    private void readMessages(){

        chatList=new ArrayList<>();
        DatabaseReference dbRef=FirebaseDatabase.getInstance().getReference("Chats");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatList.clear();
                for (DataSnapshot ds:dataSnapshot.getChildren()){
                    ModelChat chat=ds.getValue(ModelChat.class);
                    if (chat.getReceiver().equals(myUid)&&chat.getSender().equals(hisUid)||
                            chat.getReceiver().equals(hisUid)&&chat.getSender().equals(myUid)){
                        chatList.add(chat);
                    }
                   //adapter

                    adapterChat=new AdapterChat(ChatActivity.this,chatList,hisImage);
                    adapterChat.notifyDataSetChanged();
                    //set adapter to recyclerview

                    recyclerView.setAdapter(adapterChat);




                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }


    private void sendMessage(final String message){

        /*chats will be created that will contain all chats
        when ever user sends message it will create new child in "chats" node and that child will contain
        the following key values
        sender:uid of sender
        reciever:uid of reciever
        message:actual messgae*/

        DatabaseReference databaseReference=FirebaseDatabase.getInstance().getReference();




        String timestamp=String.valueOf(System.currentTimeMillis());


        HashMap<String,Object> hashMap=new HashMap<>();
        hashMap.put("sender",myUid);
        hashMap.put("receiver",hisUid);
        hashMap.put("message",message);
        hashMap.put("timestamp",timestamp );
        hashMap.put("isSeen",false);

        databaseReference.child("Chats").push().setValue(hashMap);





        //message appear in notification

        String msg = message;
        DatabaseReference database = FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        database.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                ModelUser user = dataSnapshot.getValue(ModelUser.class);

                if(notify){
                    sendNotification(hisUid , user.getName() , message);
                }
                notify =false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


        //create chat list child in firebase
        final DatabaseReference chatRef1=FirebaseDatabase.getInstance().getReference("Chatlist").child(myUid).child(hisUid);
        chatRef1.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    chatRef1.child("id").setValue(hisUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        final DatabaseReference chatRef2=FirebaseDatabase.getInstance().getReference("Chatlist").child(hisUid).child(myUid);

        chatRef2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (!dataSnapshot.exists()) {
                    chatRef2.child("id").setValue(myUid);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });


    }

    private void sendNotification(final String hisUid, final String name, final String message) {
        DatabaseReference allTokens = FirebaseDatabase.getInstance().getReference("Tokens");
        Query query = allTokens.orderByKey().equalTo(hisUid);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                for (DataSnapshot ds: dataSnapshot.getChildren()){
                    Token token = ds.getValue(Token.class);
                    Data data = new Data(myUid , name+" : "+message,"New Message" ,hisUid , R.drawable.ic_default_img);

                    Sender sender = new Sender(data , token.getToken());
                    apiService.sendNotification(sender).enqueue(new Callback<Response>() {
                        @Override
                        public void onResponse(Call<Response> call, retrofit2.Response<Response> response) {
                            Toast.makeText(ChatActivity.this,""+response.message(),Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onFailure(Call<Response> call, Throwable t) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }


    private void CheckUserStatus(){
        FirebaseUser user = firebaseAuth.getCurrentUser();

        if (user != null){
            //user is signed in stay here
            myUid=user.getUid();//currently signed in user uid


        }
        else{
            //user not signed in go to main activivty
            startActivity(new Intent(this,MainActivity.class));
            finish();

        }

    }


    private void checkonlineStatus(String status){
        DatabaseReference dbRef=FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String,Object>hashMap=new HashMap<>();
        hashMap.put("onlineStatus",status);
        //update status of current user
        dbRef.updateChildren(hashMap);



    }




    private void checkTypingStatus(String typing){
        DatabaseReference dbRef=FirebaseDatabase.getInstance().getReference("Users").child(myUid);
        HashMap<String,Object>hashMap=new HashMap<>();
        hashMap.put("typingTo",typing);
        //update status of current user
        dbRef.updateChildren(hashMap);



    }



    @Override
    protected void onStart() {
        CheckUserStatus();
        //set online
        checkonlineStatus("online");
        super.onStart();
    }


    @Override
    protected void onPause() {
        super.onPause();

        //gettimestamp
        String timestamp=String.valueOf(System.currentTimeMillis());
        //set offline with last seen
        checkonlineStatus(timestamp);
        checkTypingStatus("noOne");
        userRefForSeen.removeEventListener(seenListener);
    }

    @Override
    protected void onResume() {
        //set online
        checkonlineStatus("online");
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

      getMenuInflater().inflate(R.menu.menu_main, menu);

      //hide search view,add post
        menu.findItem(R.id.action_search).setVisible(false);
        menu.findItem(R.id.action_add_post).setVisible(false);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {


        int id = item.getItemId();
        if (id == R.id.action_logout){

            firebaseAuth.signOut();
            CheckUserStatus();
        }
        return super.onOptionsItemSelected(item);




    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }
}
