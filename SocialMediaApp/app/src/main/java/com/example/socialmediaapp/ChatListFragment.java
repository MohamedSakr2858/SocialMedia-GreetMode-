package com.example.socialmediaapp;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.example.socialmediaapp.adapters.AdapterChatlist;
import com.example.socialmediaapp.models.ModelChat;
import com.example.socialmediaapp.models.ModelChatlist;
import com.example.socialmediaapp.models.ModelUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChatListFragment extends Fragment {

    FirebaseAuth firebaseAuth;
    RecyclerView recyclerView;
    List<ModelChatlist>chatlistList;
    List<ModelUser>userList;
    DatabaseReference reference;
    FirebaseUser currentUser;
    AdapterChatlist adapterChatlist;



    public ChatListFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view= inflater.inflate(R.layout.fragment_chat_list, container, false);

        //init firebase auth
        firebaseAuth = FirebaseAuth.getInstance();

        currentUser=FirebaseAuth.getInstance().getCurrentUser();
        recyclerView=view.findViewById(R.id.recyclerViewChatList);
        chatlistList=new ArrayList<>();

        reference= FirebaseDatabase.getInstance().getReference("Chatlist").child(currentUser.getUid());
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                chatlistList.clear();
                for (DataSnapshot ds:dataSnapshot.getChildren()){
                    ModelChatlist chatlist=ds.getValue(ModelChatlist.class);
                    chatlistList.add(chatlist);
                }
                loadChats();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        return view;
    }

    private void loadChats() {
        userList=new ArrayList<>();
        reference=FirebaseDatabase.getInstance().getReference("Users");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                   userList.clear();
                   for (DataSnapshot ds:dataSnapshot.getChildren()){
                       ModelUser user=ds.getValue(ModelUser.class);
                       for (ModelChatlist chatlist:chatlistList){
                           if (user.getUid()!=null&&user.getUid().equals(chatlist.getId())){

                               userList.add(user);
                               break;
                           }
                       }
                       //adapter
                       adapterChatlist=new AdapterChatlist(getContext(),userList);
                       //set adapter
                       recyclerView.setAdapter(adapterChatlist);
                       //set last message
                       for (int i=0;i<userList.size();i++){

                           lastMessage(userList.get(i).getUid());
                       }

                   }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void lastMessage(final String userId) {
        DatabaseReference reference=FirebaseDatabase.getInstance().getReference("Chats");
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
              String theLastMessage="default";
              for (DataSnapshot ds: dataSnapshot.getChildren()){
                  ModelChat chat=ds.getValue(ModelChat.class);
                  if (chat==null){
                      continue;
                  }
                  String sender=chat.getSender();
                  String receiver=chat.getReceiver();

                  if (sender==null||receiver==null){

                      continue;
                  }

                  if (chat.getReceiver().equals(currentUser.getUid())&&
                  chat.getSender().equals(userId)||chat.getReceiver().equals(userId)&&
                  chat.getSender().equals(currentUser.getUid())){

                      theLastMessage=chat.getMessage();

                  }

              }
              adapterChatlist.setLastMessageMap(userId,theLastMessage);
               adapterChatlist.notifyDataSetChanged();
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


        }
        else{
            //user not signed in go to main activivty
            startActivity(new Intent(getActivity(),MainActivity.class));
            getActivity().finish();

        }

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true); //to show menu in fragment
        super.onCreate(savedInstanceState);
    }

    //inflate options menu
    @Override
    public void onCreateOptionsMenu(Menu menu , MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main ,menu);

        //hide add post icon
        menu.findItem(R.id.action_add_post).setVisible(false);


        super.onCreateOptionsMenu(menu , inflater);
    }



    //handle menu item click
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_logout){

            firebaseAuth.signOut();
            CheckUserStatus();
        }



        return super.onOptionsItemSelected(item);
    }

}
