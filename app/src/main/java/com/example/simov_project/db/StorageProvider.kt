package com.example.simov_project.db

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.util.Log
import android.widget.ImageView
import androidx.core.graphics.scale
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream

class StorageProvider {

    private val storage = Firebase.storage
    private val locationsRef = storage.reference.child("locations")

    suspend fun uploadLocationImage(
        locationId: String,
        fileName: String,
        imageView: ImageView
    ): String? {
        return try {
            val imageRef = locationsRef.child(locationId).child(fileName)
            // Get the data from an ImageView as bytes

            imageView.isDrawingCacheEnabled = true
            imageView.buildDrawingCache()
            var width = 240
            var height = 240
            if (fileName == "image") {
                width = 800
                height = 400
            }
            val bitmap = (imageView.drawable as BitmapDrawable).bitmap.scale(width, height, true)
            val baos = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val data = baos.toByteArray()
            val uploadTask = imageRef.putBytes(data)
            uploadTask.await().storage.downloadUrl.toString()
        } catch (e: Exception) {
            Log.e(ContentValues.TAG, "Error uploading $fileName to location: $locationId", e)
            null
        }
    }

    suspend fun downloadLocationImage(locationId: String, fileName: String): Bitmap? {
        return try {
        val imageRef = locationsRef.child(locationId).child(fileName)
        val ONE_MEGABYTE: Long = 1024 * 1024
        imageRef.getBytes(ONE_MEGABYTE)
            .await().let {
                BitmapFactory.decodeByteArray(it, 0, it.size)
            }
        } catch (e: Exception) {
            Log.e(ContentValues.TAG, "Error downloading $fileName for location: $locationId", e)
            null
        }
    }
}