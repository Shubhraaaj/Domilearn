package com.example.shubhraj.domilearn;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;

public class MainActivity extends AppCompatActivity
{
    private static final int CAMERA_REQUEST = 1;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private static final String TAG = "MAIN_ACTIVITY_TAG";
    private StorageReference mStorageRef;
    private CircleImageView mImage;
    private TextView mText;
    private Button mButton;
    private Uri resultUri;
    private Bitmap photo;
    private ProgressDialog dialog;
    private DatabaseReference mDatabase;

    private String pushID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pushID = "shubhraj";
        mImage = (CircleImageView) findViewById(R.id.profile_image);
        mText  = (TextView) findViewById(R.id.upload_text);
        mButton = (Button) findViewById(R.id.upload_button);
        mStorageRef = FirebaseStorage.getInstance().getReference().child("domilearn");
        mDatabase = FirebaseDatabase.getInstance().getReference().child("domilearn");
        dialog = new ProgressDialog(MainActivity.this);

        mImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CropImage.activity().setAspectRatio(1,1)
                        .start(MainActivity.this);
            }
        });

        mButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (resultUri != null)
                {
                    try {
                        photo = MediaStore.Images.Media.getBitmap(getContentResolver(), resultUri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    dialog.setTitle("Uploading Image");
                    dialog.setCancelable(false);
                    dialog.show();
                    uploadImage();
                }
                else
                {
                    Toast.makeText(MainActivity.this, "Please select an Image", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void uploadImage()
    {
        DatabaseReference waterItemRef = mDatabase.push();
        String pushID = waterItemRef.getKey();
        Log.d(TAG, pushID);
        ByteArrayOutputStream imageBaos = new ByteArrayOutputStream();
        photo.compress(Bitmap.CompressFormat.JPEG, 20, imageBaos);
        byte[] imageData = imageBaos.toByteArray();
        StorageReference newStorageRef = mStorageRef.child(pushID);
        UploadTask imageUploadTask = newStorageRef.putBytes(imageData);
        imageUploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                String imageURL = taskSnapshot.getDownloadUrl().toString();
                mText.setText("Image URL - " + imageURL);
                dialog.hide();
                Toast.makeText(MainActivity.this, "Image Successfully uploaded", Toast.LENGTH_SHORT).show();
            }
            }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                Log.e(TAG, exception.getMessage());
                Toast.makeText(MainActivity.this, "Error occured while uploading Image", Toast.LENGTH_SHORT).show();
                dialog.hide();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "CAMERA PERMISSION GRANTED", Toast.LENGTH_LONG).show();
                Intent cameraIntent = new
                        Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            } else {
                Toast.makeText(MainActivity.this, "CAMERA PERMISSION DENIED", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK)
            {
                resultUri = result.getUri();
                Glide.with(MainActivity.this).load(resultUri).into(mImage);
            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE)
            {
                Exception error = result.getError();
            }
        }
    }
}
