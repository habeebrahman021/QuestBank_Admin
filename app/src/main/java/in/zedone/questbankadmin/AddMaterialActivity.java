package in.zedone.questbankadmin;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.common.internal.Constants;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.vincent.filepicker.Constant;
import com.vincent.filepicker.activity.NormalFilePickActivity;
import com.vincent.filepicker.filter.entity.NormalFile;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.transform.Result;


public class AddMaterialActivity extends AppCompatActivity {

    private static final String TAG = AddMaterialActivity.class.getName();
    final private String FCM_API = "https://fcm.googleapis.com/fcm/send";
    final private String serverKey = "key=" + "AAAAlU7WCUo:APA91bEsB9hW2TBROV7WxLi6J6T6kgWQag7M8O4tem0uEPlSGC1g9eTwuZa3JEPnaoZ0LblmwllFe0aalnlTZssXw7l1kzNYdLau4J4Tdo5LMzKsyNBVfdqONIEwsEKHSsz658rA0qAy";
    final private String contentType = "application/json";


    Spinner spinClass, spinBoard, spinMedium, spinSubject;
    Button btnUpload;
    TextView txtStatus;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_material);

        txtStatus = findViewById(R.id.txt_status);
        spinClass = findViewById(R.id.spin_class);
        spinBoard = findViewById(R.id.spin_board);
        spinMedium = findViewById(R.id.spin_medium);
        spinSubject = findViewById(R.id.spin_subject);
        btnUpload = findViewById(R.id.btn_upload);
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent4 = new Intent(getApplicationContext(), NormalFilePickActivity.class);
                intent4.putExtra(Constant.MAX_NUMBER, 15);
                intent4.putExtra(NormalFilePickActivity.SUFFIX, new String[] {"pdf"});
                startActivityForResult(intent4, Constant.REQUEST_CODE_PICK_FILE);
            }
        });

    }
    private void uploadFile(final String name, Uri data, final String subject, final String board, final String class_type) {
        StorageReference mStorageReference = FirebaseStorage.getInstance().getReference();
        StorageReference sRef = mStorageReference.child("materials/" + name);
        sRef.putFile(data)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @SuppressWarnings("VisibleForTests")
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Task<Uri> uri = taskSnapshot.getStorage().getDownloadUrl();
                        while(!uri.isComplete());
                        Uri download_url = uri.getResult();

                        // Create a new user with a first and last name
                        Map<String, Object> material = new HashMap<>();
                        material.put("name",name);
                        material.put("url",download_url.toString());
                        material.put("subject",subject);
                        //material.put("medium",spinMedium.getSelectedItem());
                        material.put("board",board);
                        material.put("class_type",class_type);

                        FirebaseFirestore db = FirebaseFirestore.getInstance();
                        // Add a new document with a generated ID
                        db.collection("materials")
                                .add(material)
                                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                                    @Override
                                    public void onSuccess(DocumentReference documentReference) {
                                        Log.d("TAG", "DocumentSnapshot added with ID: " + documentReference.getId());
                                        sendNotification(name,subject);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.w("TAG", "Error adding document", e);
                                    }
                                });

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Toast.makeText(getApplicationContext(), exception.getMessage(), Toast.LENGTH_LONG).show();
                    }
                })
                .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @SuppressWarnings("VisibleForTests")
                    @Override
                    public void onProgress(UploadTask.TaskSnapshot taskSnapshot) {
                        double progress = (100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                        txtStatus.setText(name + " : " + (int) progress + "% Uploading...");
                    }
                });

    }

    public void sendNotification(String NOTIFICATION_TITLE, String NOTIFICATION_MESSAGE){
        String TOPIC = "/topics/TOPIC_ALL";

        JSONObject notification = new JSONObject();
        JSONObject notifcationBody = new JSONObject();
        try {
            notifcationBody.put("title", NOTIFICATION_TITLE);
            notifcationBody.put("message", NOTIFICATION_MESSAGE);

            notification.put("to", TOPIC);
            notification.put("data", notifcationBody);
        } catch (JSONException e) {
            Log.e("AddMaterialActivity", "onCreate: " + e.getMessage() );
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(FCM_API, notification,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.e(TAG,"Notification Send Suucessfully");
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(AddMaterialActivity.this, "Request error", Toast.LENGTH_LONG).show();
                        Log.i(TAG, error.getMessage());
                    }
                }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", serverKey);
                params.put("Content-Type", contentType);
                return params;
            }
        };
        MySingleton.getInstance(getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constant.REQUEST_CODE_PICK_FILE && resultCode== RESULT_OK){
            ArrayList<NormalFile> list = data.getParcelableArrayListExtra(Constant.RESULT_PICK_FILE);
            for (int i=0; i<list.size(); i++){

                File file=new File(list.get(i).getPath());
                uploadFile(file.getName(), Uri.fromFile(file), spinSubject.getSelectedItem().toString(), spinBoard.getSelectedItem().toString(), spinClass.getSelectedItem().toString());
            }
        }
    }
}
