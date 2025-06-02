package com.example.mycookbook.ui.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.example.mycookbook.MainActivity
import com.example.mycookbook.R
import com.example.mycookbook.data.local.AppDatabase
import com.example.mycookbook.data.local.entity.User
import com.example.mycookbook.utils.SessionManager
import com.facebook.*
import com.facebook.appevents.AppEventsLogger
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*

class LoginActivity : ComponentActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var db: AppDatabase
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 1001
    private lateinit var callbackManager: CallbackManager
    private lateinit var fbLoginButton: ImageButton
    private lateinit var sessionManager: SessionManager
    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        com.google.firebase.FirebaseApp.initializeApp(this)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        super.onCreate(savedInstanceState)
        FirebaseFirestore.setLoggingEnabled(true)
        firestore = FirebaseFirestore.getInstance()
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        FacebookSdk.sdkInitialize(applicationContext)
        AppEventsLogger.activateApp(application)
        setContentView(R.layout.activity_login)
        db = AppDatabase.getInstance(applicationContext)
        sessionManager = SessionManager(this)
        val guestId = sessionManager.getGuestId()
        Log.d("GuestSession", "Guest ID onCreate: $guestId")

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnGoogleSignIn = findViewById<ImageButton>(R.id.btnGoogleSignIn)
        val tvGoToRegister = findViewById<TextView>(R.id.tvGoToRegister)
        fbLoginButton = findViewById<ImageButton>(R.id.btnFacebookLogin)

        callbackManager = CallbackManager.Factory.create()
        updateFacebookButtonIcon()

        LoginManager.getInstance().registerCallback(callbackManager, object : FacebookCallback<LoginResult> {
            override fun onSuccess(result: LoginResult) {
                handleFacebookAccessToken(result.accessToken)
                Toast.makeText(this@LoginActivity, getString(R.string.login_success_facebook), Toast.LENGTH_SHORT).show()
            }

            override fun onCancel() {
                val bundle = Bundle().apply {
                    putString("login_type", "facebook")
                    putString("result", "cancelled")
                }
                firebaseAnalytics.logEvent("facebook_login", bundle)
                Toast.makeText(this@LoginActivity, getString(R.string.login_cancelled_facebook), Toast.LENGTH_SHORT).show()
            }

            override fun onError(error: FacebookException) {
                val bundle = Bundle().apply {
                    putString("login_type", "facebook")
                    putString("result", "error")
                    putString("error_message", error.message)
                }
                firebaseAnalytics.logEvent("facebook_login", bundle)
                Toast.makeText(this@LoginActivity, getString(R.string.login_failed_facebook), Toast.LENGTH_SHORT).show()
            }
        })

        fbLoginButton.setOnClickListener {
            val accessToken = AccessToken.getCurrentAccessToken()
            val isLoggedIn = accessToken != null && !accessToken.isExpired
            if (isLoggedIn) {
                LoginManager.getInstance().logOut()
                clearFacebookUserPrefs()
                updateFacebookButtonIcon()
            } else {
                LoginManager.getInstance().logInWithReadPermissions(this, listOf("email", "public_profile"))
            }
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, getString(R.string.empty_email_password), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                val user = withContext(Dispatchers.IO) {
                    db.userDao().getUserByEmail(email)
                }
                if (user != null && user.password == password) {
                    sessionManager.clearGuestSession()
                    Log.d("GuestSession", "Guest session cleared on email login")
                    val prefs = PreferenceManager.getDefaultSharedPreferences(this@LoginActivity)
                    prefs.edit()
                        .putString("login_type", "email")
                        .putInt("user_id", user.id)
                        .putString("user_email", user.email)
                        .remove("user_profile_pic_url")
                        .apply()
                    val sharedPrefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                    sharedPrefs.edit().putBoolean("isLoggedIn", true).apply()
                    withContext(Dispatchers.Main) {
                        uploadUserAndLogAnalytics(user, "email", password)
                        Toast.makeText(this@LoginActivity, getString(R.string.login_success_email), Toast.LENGTH_SHORT).show()
                        startMainAndFinish()
                    }
                } else {
                    val bundle = Bundle().apply {
                        putString("login_type", "email")
                        putString("result", "invalid_credentials")
                        putString("email", email)
                    }
                    firebaseAnalytics.logEvent("login_failure", bundle)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@LoginActivity, getString(R.string.login_failed_email), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnGoogleSignIn.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                startActivityForResult(signInIntent, RC_SIGN_IN)
            }
        }

        val btnGuestLogin = findViewById<ImageButton>(R.id.btnAnonymousLogin)
        btnGuestLogin.setOnClickListener {
            val guestId = sessionManager.getGuestId()
            Log.d("GuestSession", "Logging in as guest with ID: $guestId")
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            prefs.edit()
                .putString("login_type", "guest")
                .putString("guest_id", guestId)
                .remove("user_profile_pic_url")
                .apply()
            val sharedPrefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
            sharedPrefs.edit().putBoolean("isLoggedIn", true).apply()
            val savedGuestId = prefs.getString("guest_id", "NOT FOUND")
            Log.d("GuestSession", "Saved guest_id in SharedPreferences: $savedGuestId")
            val bundle = Bundle()
            bundle.putString("login_type", "guest")
            firebaseAnalytics.logEvent("guest_login", bundle)
            Toast.makeText(this, getString(R.string.login_success_guest), Toast.LENGTH_SHORT).show()
            startMainAndFinish()
        }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        testSimpleFirestoreWrite()
    }

    private fun updateFacebookButtonIcon() {
        val accessToken = AccessToken.getCurrentAccessToken()
        val isLoggedIn = accessToken != null && !accessToken.isExpired
        fbLoginButton.setImageResource(R.drawable.ic_fb)
    }

    fun testSimpleFirestoreWrite() {
        val firestore = FirebaseFirestore.getInstance()
        val testData = hashMapOf("testField" to "testValue")
        firestore.collection("testCollection")
            .document("testDoc")
            .set(testData)
            .addOnSuccessListener {
                Log.d("FirestoreTest", "Simple Firestore write succeeded.")
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreTest", "Simple Firestore write failed", e)
            }
    }

    private fun clearFacebookUserPrefs() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.edit()
            .remove("user_profile_pic_url")
            .remove("user_email")
            .remove("user_id")
            .remove("login_type")
            .apply()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        callbackManager.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                if (account != null) {
                    val email = account.email ?: ""
                    val displayName = account.displayName ?: "Unknown"
                    val nameParts = displayName.trim().split("\\s+".toRegex(), limit = 2)
                    val firstName = nameParts.getOrNull(0) ?: ""
                    val lastName = nameParts.getOrNull(1) ?: ""
                    lifecycleScope.launch {
                        val prefs = PreferenceManager.getDefaultSharedPreferences(this@LoginActivity)
                        var user = withContext(Dispatchers.IO) {
                            db.userDao().getUserByEmail(email)
                        }
                        if (user == null) {
                            user = User(
                                firstName = firstName,
                                lastName = lastName,
                                email = email,
                                password = ""
                            )
                            val newId = withContext(Dispatchers.IO) {
                                db.userDao().insert(user)
                            }
                            user.id = newId.toInt()
                        } else {
                            user.firstName = firstName
                            user.lastName = lastName
                            withContext(Dispatchers.IO) {
                                db.userDao().update(user)
                            }
                        }
                        prefs.edit()
                            .putString("login_type", "google")
                            .putInt("user_id", user.id)
                            .putString("user_email", user.email)
                            .remove("user_profile_pic_url")
                            .apply()
                        val sharedPrefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                        sharedPrefs.edit().putBoolean("isLoggedIn", true).apply()
                        withContext(Dispatchers.Main) {
                            uploadUserAndLogAnalytics(user, "google", "")
                            Toast.makeText(this@LoginActivity, getString(R.string.login_success_google), Toast.LENGTH_SHORT).show()
                            startMainAndFinish()
                        }
                    }
                }
            } catch (e: ApiException) {
                val bundle = Bundle().apply {
                    putString("login_type", "google")
                    putString("result", "error")
                    putString("error_message", e.message)
                }
                firebaseAnalytics.logEvent("google_login", bundle)
                Toast.makeText(this, getString(R.string.login_failed_google), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleFacebookAccessToken(token: AccessToken) {
        lifecycleScope.launch {
            val request = GraphRequest.newMeRequest(token) { obj, _ ->
                if (obj != null) {
                    val email = obj.optString("email", "")
                    val name = obj.optString("name", "")
                    val nameParts = name.trim().split("\\s+".toRegex(), limit = 2)
                    val firstName = nameParts.getOrNull(0) ?: ""
                    val lastName = nameParts.getOrNull(1) ?: ""
                    lifecycleScope.launch {
                        val prefs = PreferenceManager.getDefaultSharedPreferences(this@LoginActivity)
                        var user = withContext(Dispatchers.IO) {
                            db.userDao().getUserByEmail(email)
                        }
                        if (user == null) {
                            user = User(
                                firstName = firstName,
                                lastName = lastName,
                                email = email,
                                password = ""
                            )
                            val newId = withContext(Dispatchers.IO) {
                                db.userDao().insert(user)
                            }
                            user.id = newId.toInt()
                        } else {
                            user.firstName = firstName
                            user.lastName = lastName
                            withContext(Dispatchers.IO) {
                                db.userDao().update(user)
                            }
                        }
                        prefs.edit()
                            .putString("login_type", "facebook")
                            .putInt("user_id", user.id)
                            .putString("user_email", user.email)
                            .putString("user_profile_pic_url", obj.optString("picture"))
                            .apply()
                        val sharedPrefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                        sharedPrefs.edit().putBoolean("isLoggedIn", true).apply()
                        withContext(Dispatchers.Main) {
                            uploadUserAndLogAnalytics(user, "facebook", "")
                            startMainAndFinish()
                        }
                    }
                }
            }
            val parameters = Bundle()
            parameters.putString("fields", "id,name,email,picture.type(large)")
            request.parameters = parameters
            request.executeAsync()
        }
    }

    private fun uploadUserAndLogAnalytics(user: User, loginType: String, password: String) {
        lifecycleScope.launch {
            val userMap = hashMapOf(
                "id" to user.id,
                "email" to user.email,
                "firstName" to user.firstName,
                "lastName" to user.lastName,
                "loginType" to loginType
            )
            firestore.collection("users").document(user.id.toString())
                .set(userMap)
                .addOnSuccessListener {
                    Log.d("Firestore", "User uploaded successfully")
                }
                .addOnFailureListener {
                    Log.e("Firestore", "Failed to upload user", it)
                }
            val bundle = Bundle().apply {
                putString(FirebaseAnalytics.Param.METHOD, loginType)
                putString("email", user.email)
                putString("password_length", password.length.toString())
            }
            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN, bundle)
        }
    }

    private fun startMainAndFinish() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
