package com.jasleen.instafire

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    lateinit var etEmail: EditText
    lateinit var etPassword: EditText
    lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        val auth = FirebaseAuth.getInstance()
        if(auth.currentUser != null){
            goPostsActivity()
        }

        btnLogin = findViewById(R.id.btnLogin)
        etPassword = findViewById(R.id.etPassword)
        etEmail = findViewById(R.id.etEmail)

        btnLogin.setOnClickListener {
            btnLogin.isEnabled = false //
            val email = etEmail.text.toString()
            val password = etPassword.text.toString()

            if(email.isBlank() || password.isBlank()){
                Toast.makeText(this, "Email/Password is Empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Firebase authentication check
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                btnLogin.isEnabled = false //
                if(task.isSuccessful){
                    Toast.makeText(this, "Success", Toast.LENGTH_SHORT).show()
                    goPostsActivity()
                }else{
                    Log.e(TAG, "signInWithEmail failed", task.exception)
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun goPostsActivity() {
        Log.i(TAG, "goPostsActivity")
        val intent = Intent(this, PostsActivity::class.java)
        startActivity(intent)
        finish()
    }
}