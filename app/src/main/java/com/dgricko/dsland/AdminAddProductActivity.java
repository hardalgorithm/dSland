package com.dgricko.dsland;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;

public class AdminAddProductActivity extends AppCompatActivity {

    private String categoryName, productName, productDescription, productPrice, saveCurrentDate, saveCurrentTime;
    private String productRandomKey, downloadImageUrl;

    private static final int GALLERY_PICK = 1;
    private Uri imageUri;
    private StorageReference productImageRef;
    private DatabaseReference productRef;

    private Button addNewProductButton;
    private ImageView inputProductImage;
    private EditText inputProductName, inputProductDescription,inputProductPrice;
    private ProgressDialog loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_addproduct);

        categoryName = getIntent().getExtras().get("category").toString();

        productImageRef = FirebaseStorage.getInstance().getReference().child("Product Images");
        productRef = FirebaseDatabase.getInstance().getReference().child("Products");

        Toast.makeText(this, categoryName, Toast.LENGTH_SHORT).show();

        addNewProductButton = (Button)findViewById(R.id.add_new_product_btn);
        inputProductImage = (ImageView)findViewById(R.id.select_product_image);
        inputProductName = (EditText)findViewById(R.id.product_name_input);
        inputProductDescription = (EditText)findViewById(R.id.product_description_input);
        inputProductPrice = (EditText)findViewById(R.id.product_price_input);
        loadingBar = new ProgressDialog(this);
        
        inputProductImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openGallery();
            }
        });


        addNewProductButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                validateProductData();
            }
        });
    }

    private void validateProductData() {
        productDescription = inputProductDescription.getText().toString();
        productName = inputProductName.getText().toString();
        productPrice = inputProductPrice.getText().toString();
        
        if (imageUri == null){
            Toast.makeText(this, "Product image is empty", Toast.LENGTH_SHORT).show();
        }else if (TextUtils.isEmpty(productName)){
            inputProductName.setBackgroundResource(R.drawable.error_design);
        }else if (TextUtils.isEmpty(productDescription)){
            inputProductDescription.setBackgroundResource(R.drawable.error_design);
        }else if (TextUtils.isEmpty(productPrice)){
            inputProductPrice.setBackgroundResource(R.drawable.error_design);
        }else {
            storeProductInformation(); 
        }
    }

    private void storeProductInformation() {

        loadingBar.setTitle("Add new Product");
        loadingBar.setMessage("Please wait ,while we are adding new product");
        loadingBar.setCanceledOnTouchOutside(false);
        loadingBar.show();


        Calendar calendar = Calendar.getInstance();

        @SuppressLint("SimpleDateFormat") SimpleDateFormat currentDate = new SimpleDateFormat("MM:dd:yyyy ");
        saveCurrentDate = currentDate.format(calendar.getTime());

        @SuppressLint("SimpleDateFormat") SimpleDateFormat currentTime = new SimpleDateFormat("HH:mm:ss a");
        saveCurrentTime = currentTime.format(calendar.getTime());

        productRandomKey = saveCurrentDate + saveCurrentTime;

        //save Image
        final StorageReference filePath = productImageRef.child(imageUri.getLastPathSegment()+productRandomKey+".jpg");

        final UploadTask uploadTask = filePath.putFile(imageUri);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                String message = e.toString();
                Toast.makeText(AdminAddProductActivity.this, "Error: "+message, Toast.LENGTH_LONG).show();
                loadingBar.dismiss();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(AdminAddProductActivity.this, "Product Image upload successfully!", Toast.LENGTH_SHORT).show();

                Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                    @Override
                    public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                        if  (!task.isSuccessful()){
                            throw task.getException();

                        }

                        downloadImageUrl = filePath.getDownloadUrl().toString();
                        return filePath.getDownloadUrl();
                    }
                }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                    @Override
                    public void onComplete(@NonNull Task<Uri> task) {

                        if (task.isSuccessful()){
                            downloadImageUrl = task.getResult().toString();

                            Toast.makeText(AdminAddProductActivity.this, "Product Image save to Database successfully", Toast.LENGTH_SHORT).show();
                            saveProductInfoToDatabase();
                        }
                    }
                });
            }
        });
    }

    private void saveProductInfoToDatabase() {

        HashMap<String, Object> productMap = new HashMap<>();
        productMap.put("pid",productRandomKey);
        productMap.put("date",saveCurrentDate);
        productMap.put("time",saveCurrentTime);
        productMap.put("description",productDescription);
        productMap.put("image",downloadImageUrl);
        productMap.put("category",categoryName);
        productMap.put("price",productPrice);
        productMap.put("name",productName);

        productRef.child(productRandomKey).updateChildren(productMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){

                            Intent intent = new Intent(AdminAddProductActivity.this, AdminCategoryActivity.class);
                            startActivity(intent);

                            loadingBar.dismiss();
                            Toast.makeText(AdminAddProductActivity.this, "Product is added successfully", Toast.LENGTH_SHORT).show();
                        }else{
                            loadingBar.dismiss();
                            String message = task.getException().toString();
                            Toast.makeText(AdminAddProductActivity.this, "Error: "+message, Toast.LENGTH_SHORT).show();
                        }
                    }
                });

    }

    private void openGallery() {
        Intent galleryIntent = new Intent();
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent,GALLERY_PICK);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==GALLERY_PICK && resultCode == RESULT_OK && data!=null){
            imageUri = data.getData();
            inputProductImage.setImageURI(imageUri);
        }
    }
}