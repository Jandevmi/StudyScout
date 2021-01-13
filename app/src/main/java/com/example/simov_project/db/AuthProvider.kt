package com.example.simov_project.db

import android.content.ContentValues
import android.util.Log
import com.example.simov_project.dataclasses.Location
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Class for handling user authentication
 */

class AuthProvider {

    private val auth = FirebaseAuth.getInstance()

    suspend fun createUser(email: String, password: String): String?  {
        return try {
            auth.createUserWithEmailAndPassword(email, password)
                .await().user?.uid
        } catch (e: Exception) {
            Log.w(ContentValues.TAG, "Error creating user", e)
            null
        }
    }

    suspend fun userLogin(email: String, password: String): String? {
        return try {
            auth.signInWithEmailAndPassword(email, password)
                .await().user?.uid
        } catch (e: Exception) {
            Log.w(ContentValues.TAG, "Error login", e)
            null
        }
    }

    fun userLogout(): Boolean {
        return try {
            auth.signOut()
            true
        } catch (e: Exception) {
            Log.w(ContentValues.TAG, "Error logout ", e)
            false
        }
    }

    fun getUserId(): String? {
        return auth.currentUser?.uid
    }
}