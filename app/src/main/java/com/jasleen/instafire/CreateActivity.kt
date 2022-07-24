package com.jasleen.instafire

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.jasleen.instafire.models.Post
import com.jasleen.instafire.models.User
import kotlinx.android.synthetic.main.activity_create.*
import java.io.FileInputStream
import kotlin.math.sign

private const val TAG = "CreateActivity"
private const val PICK_PHOTO_CODE = 1234
class CreateActivity : AppCompatActivity() {
    private var photoUri: Uri? = null
    private var signedInUser: User? = null
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var storageReference: StorageReference


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)
        storageReference = FirebaseStorage.getInstance().reference
        firestoreDb = FirebaseFirestore.getInstance()

        firestoreDb.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid as String)    // firebase auth gets the uid of the current user signed in and the document function fetches the user doc corresponding to that uid
            .get()
            .addOnSuccessListener { userSnapshot ->
                signedInUser = userSnapshot.toObject(User::class.java)
                Log.i(TAG, "Signed in user: $signedInUser")
            }
            .addOnFailureListener{ exception ->
                Log.i(TAG, "Failure fetching signed in user", exception)
            }

        btnPickImage.setOnClickListener {
            Log.i(TAG, "Open up image picker on device")
            // implicit intent - this means we want to open any application which handles this intent
            val imagePickerIntent= Intent(Intent.ACTION_GET_CONTENT)
            imagePickerIntent.type = "image/*"
            if(imagePickerIntent.resolveActivity(packageManager) != null){
                startActivityForResult(imagePickerIntent, PICK_PHOTO_CODE)
            }
        }

        btnSubmit.setOnClickListener {
            handleSubmitButtonClick()
        }
    }

    private fun handleSubmitButtonClick() {
        if(photoUri == null){
            Toast.makeText(this, "No Photo Selected", Toast.LENGTH_SHORT).show()
            return
        }

        if(etDescription.text.isBlank()){
            Toast.makeText(this, "Description Cannot Be Empty", Toast.LENGTH_SHORT).show()
            return
        }

        if(signedInUser == null){
            Toast.makeText(this, "No Signed In User, Please Wait", Toast.LENGTH_SHORT).show()
            return
        }

        btnSubmit.isEnabled = false

        val photoUploadUri = photoUri as Uri
        // to handle chained asynch operations we use tasks api from fb
        // we need all the images to be sotred in the images/ file and with a unque name so we use currentimemillis
        val photoReference = storageReference.child("images/${System.currentTimeMillis()}-photo.jpg")
        // upload image user selected to firebase storage
        photoReference.putFile(photoUploadUri)
            .continueWithTask { photoUploadTask ->  // returns a task, if the task is succeeded then continue but it fails then the task api will propragte the failure to next task
                Log.i(TAG, "uploaded byts: ${photoUploadTask.result?.bytesTransferred}")
                // Retrieve the image url of the uploaded iamge
                photoReference.downloadUrl
            }.continueWithTask{downloadUrlTask ->
                // create a post object with the image url and add that to the ppst collection
                val post = Post(
                    etDescription.text.toString(),
                    downloadUrlTask.result.toString(),
                    System.currentTimeMillis(),
                    signedInUser
                )
                firestoreDb.collection("posts").add(post)
            }.addOnCompleteListener { postCreationTask ->
                btnSubmit.isEnabled = true
                if(!postCreationTask.isSuccessful){
                    Log.e(TAG, "Exception during Firebase operations", postCreationTask.exception)
                    Toast.makeText(this, "Failed to save posts", Toast.LENGTH_SHORT).show()
                }
                etDescription.text.clear()
                ivImage.setImageResource(0)
//                Toast.makeText(this, "success!", Toast.LENGTH_SHORT).show()
                val profileIntent = Intent(this, ProfileActivity::class.java)
                profileIntent.putExtra(EXTRA_USERNAME, signedInUser?.username)
                startActivity(profileIntent)
                finish()
            }



    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_PHOTO_CODE){ //tells us if this is the result coming back from the intent that we launched at button click
            if(resultCode == Activity.RESULT_OK){   // result code indicates what the user actually did in the application that opened up
                photoUri = data?.data   // photo uri gives the location of the photo user selected
                Log.i(TAG, "PhotoURI $photoUri")
                ivImage.setImageURI(photoUri)
            }else{
                Toast.makeText(this, "Image Pick Action Cancelled", Toast.LENGTH_SHORT).show()
            }
        }
    }
}