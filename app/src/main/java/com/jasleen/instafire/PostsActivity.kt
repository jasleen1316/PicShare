package com.jasleen.instafire

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.jasleen.instafire.models.Post
import com.jasleen.instafire.models.User
import kotlinx.android.synthetic.main.activity_posts.*

private const val TAG = "PostsActivity"
const val EXTRA_USERNAME = "EXTRA_USERNAME"
open class PostsActivity : AppCompatActivity() {

    private var signedInUser: User? = null
    private lateinit var firestoreDb: FirebaseFirestore
    private lateinit var posts: MutableList<Post>
    private lateinit var adapter: PostsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_posts)


        //Create the layout file for one post - DONE
        //Create data source
        posts = mutableListOf() // when asynchronous call to firestore backend succeeds then we will upadte this list of posts to have the most recent info
        //Create the adapter
        adapter = PostsAdapter(this, posts)
        rvPosts.adapter = adapter
        rvPosts.layoutManager = LinearLayoutManager(this)
        //Bind adapter and layout manager to recycler view
        firestoreDb = FirebaseFirestore.getInstance()

        // using a query to fetch the currently signed in user
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
        var postsReference = firestoreDb
            .collection("posts")
            .limit(20)
            .orderBy("creation_time_ms", Query.Direction.DESCENDING)

        // if username (EXTRA USERNAME STRING FROM INTENT) is not null then we are in the profile activity
        val username = intent.getStringExtra(EXTRA_USERNAME)
        if(username != null){
            // user.username is a field path that goes into the user attribute of the post and goes into its username attribute
            supportActionBar?.title = username
            postsReference = postsReference.whereEqualTo("user.username", username)
        }
        // snapshot listener informs us whenever there is a change in the DB and we'll get notified about the new data
        postsReference.addSnapshotListener { snapshot, exception ->
// when a change is made in any atribute of a post, a callback is triggered on the snapshotlistener and it immediately updates the output with the new data
            if(exception!=null || snapshot == null){
                Log.e(TAG, "Exception when querying posts", exception)
                return@addSnapshotListener
            }

            // mapping the list of posts that we get back (snapshot.documents) and mapping that into a list of post data class objects
            val postList = snapshot.toObjects(Post::class.java)

            // clearing old posts and updating w new info
            posts.clear()
            posts.addAll(postList)
            adapter.notifyDataSetChanged()  // updates the adapter on any change in the database
            for(post in postList){
                Log.i(TAG, "Post ${post}") // printing out the id of the documents along with the map (printout out a map of  all the atributes associated with the document)
            }

        }

        fabCreate.setOnClickListener{
            val intent = Intent(this, CreateActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {        // inflates the menu to create the options
        menuInflater.inflate(R.menu.menu_posts, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {   // checks if a certain item has been selected
        if(item.itemId == R.id.menu_profile){
            val intent = Intent(this, ProfileActivity::class.java)
            intent.putExtra(EXTRA_USERNAME, signedInUser?.username)
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }
}