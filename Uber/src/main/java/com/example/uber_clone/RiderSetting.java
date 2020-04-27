package com.example.uber_clone;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RiderSetting extends AppCompatActivity {

    private EditText mName, mPhone;
    private Button confirm, back;
    private FirebaseAuth mAuth;
    private DatabaseReference mRiderDatabase;
    private String userId;
    private String name_info;
    private String phone_info;
    private ImageView mprofile_image;
    private Uri result_uri;
    private String profile_image_ulr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rider_setting);

        mprofile_image = (ImageView) findViewById(R.id.profileImage);
        mName = (EditText) findViewById(R.id.name);
        mPhone = (EditText) findViewById(R.id.phone);
        confirm = (Button) findViewById(R.id.confirm_button);
        back = (Button) findViewById(R.id.back_button);

        mAuth = FirebaseAuth.getInstance();
        userId = mAuth.getCurrentUser().getUid();
        mRiderDatabase = FirebaseDatabase.getInstance().getReference().child("Users").child("Riders").child(userId);



        getUserInfo();

        mprofile_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,1);
            }
        });


        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserInformation();
            }
        });

        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                return;
            }
        });

    }

        private void getUserInfo(){

            mRiderDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()&&dataSnapshot.getChildrenCount()>0){
                    Map<String,Object> map = (Map<String,Object>) dataSnapshot.getValue();
                    if(map.get("name")!= null){
                        name_info = map.get("name").toString();
                        mName.setText(name_info);

                    }
                    if(map.get("phone")!= null){
                        phone_info = map.get("phone").toString();
                        mPhone.setText(phone_info);

                    }
                    if(map.get("profileImageUrl")!= null){
                        profile_image_ulr = map.get("profileImageUrl").toString();

                       //Glide.with(getApplication()).load(profile_image_ulr).into(mprofile_image);
                        Picasso.get().load(profile_image_ulr).into(mprofile_image);
                    }

                }
            }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {

                 }
            });
        }

        private void saveUserInformation(){
            name_info = mName.getText().toString();
            phone_info = mPhone.getText().toString();
            Map userInfo = new HashMap();
            userInfo.put("name",name_info);
            userInfo.put("phone",phone_info);
            mRiderDatabase.updateChildren(userInfo);

            if(result_uri != null){
                final StorageReference filePath = FirebaseStorage.getInstance().getReference().child("profile_images").child(userId);
                Bitmap bitmap = null;
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(),result_uri);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 20,baos);
                byte[] data = baos.toByteArray();



                   UploadTask uploadTask = filePath.putBytes(data);
                   uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                       @Override
                       public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                           if (!task.isSuccessful()) {
                               throw Objects.requireNonNull(task.getException());
                           }
                            return  filePath.getDownloadUrl();
                       }
                   }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                       @Override
                       public void onComplete(@NonNull Task<Uri> task) {
                           if (task.isSuccessful()) {
                               String URL = task.getResult().toString();
                               Map newImage = new HashMap();
                               newImage.put("profileImageUrl",URL);
                               mRiderDatabase.updateChildren(newImage);
                               finish();


                           } else {
                               finish();
                           }
                       }
                   });


            }else{

                finish();

            }


        }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == Activity.RESULT_OK){
            final Uri imageUri = data.getData();
            result_uri = imageUri;
            mprofile_image.setImageURI(result_uri);
        }

    }



}
