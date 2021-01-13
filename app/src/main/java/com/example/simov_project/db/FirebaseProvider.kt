package com.example.simov_project.db

/**
 * Singelton object that can be accessed from all classes
 *
 * This object is used to call auth und db functions
 */

object FirebaseProvider {

    val auth = AuthProvider()
    val db = DatabaseProvider()
    val storage = StorageProvider()

}