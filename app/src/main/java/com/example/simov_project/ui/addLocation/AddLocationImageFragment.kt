package com.example.simov_project.ui.addLocation

import android.app.Activity.RESULT_OK
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.example.simov_project.R
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

/**
 * Fragment to add icon + image to location and to get the QR-code
 */
class AddLocationImageFragment : Fragment() {

    lateinit var locationImage: ImageView
    lateinit var locationImageUri: String
    lateinit var locationIcon: ImageView
    lateinit var locationIconUri: String
    lateinit var button: Button
    private val pickImage = 100
    private val pickIcon = 101
    private var imageUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_add_location_image, container, false)

        val addLocationViewModel: AddLocationViewModel by navGraphViewModels(R.id.editLocation_nav_graph)
        val location = addLocationViewModel.location.value!!
        locationImage = root.findViewById(R.id.addImage_location_imageView)
        locationIcon = root.findViewById(R.id.addImage_viewHolder_imageView)
        val locationNameView: TextView = root.findViewById(R.id.addImage_name_textView)
        val locationAddressView: TextView = root.findViewById(R.id.addImage_address_textView)
        val loadingProgressBar: ProgressBar = root.findViewById(R.id.addImage_progress_loader)
        val confirmButton: Button = root.findViewById(R.id.addImage_confirm_button)
        val pickImageButton: Button = root.findViewById(R.id.addImage_image_pick_button)
        val pickIconButton: Button = root.findViewById(R.id.addImage_icon_pick_button)
        val qrImageView: ImageView = root.findViewById(R.id.addImage_qr_imageView)
        val qrTextView: TextView = root.findViewById(R.id.addImage_qr_textView)

        addLocationViewModel.loading.observe(viewLifecycleOwner, Observer {
            if (it > 0) {
                loadingProgressBar.visibility = View.VISIBLE
                confirmButton.visibility = View.INVISIBLE
                confirmButton.isEnabled = false
            } else {
                loadingProgressBar.visibility = View.GONE
                confirmButton.visibility = View.VISIBLE
                confirmButton.isEnabled = true
            }
        })

        addLocationViewModel.icon.observe(viewLifecycleOwner, Observer {
            locationIcon.setImageBitmap(it)
        })

        addLocationViewModel.image.observe(viewLifecycleOwner, Observer {
            locationImage.setImageBitmap(it)
        })

        locationNameView.text = location.name
        locationAddressView.text = location.addressString

        // Create QR-code with locationID as Information
        val barcodeEncoder = BarcodeEncoder()
        val imageName = location.name?.substring(0 .. 4) + location.locationId
        val bitmap = barcodeEncoder.encodeBitmap(
            location.locationId,
            BarcodeFormat.QR_CODE,
            400,
            400
        )
        // Set QR-code to image and start download with onClick
        qrImageView.setImageBitmap(bitmap)
        qrImageView.setOnClickListener { saveMediaToStorage(bitmap, imageName) }
        qrTextView.setOnClickListener { saveMediaToStorage(bitmap, imageName) }

        // Pick image from gallery
        pickImageButton.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, pickImage)
        }
        // Pick Icon from gallery
        pickIconButton.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, pickIcon)
        }

        // Upload image / icon and navigate to locations FixMe: Don´t upload if no change
        confirmButton.setOnClickListener {
            addLocationViewModel.updateFirebaseLocation()
            findNavController().navigate(R.id.action_navigation_addLocationImage_to_location)
        }

        return root
    }

    /**
     * Function from Belal Khan
     * https://www.simplifiedcoding.net/android-save-bitmap-to-gallery/
     */
    private fun saveMediaToStorage(bitmap: Bitmap, imageName: String) {
        //Generating a file name
        val filename = "$imageName.jpg"

        //Output stream
        var fos: OutputStream? = null

        //For devices running android >= Q
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            //getting the contentResolver
            context?.contentResolver?.also { resolver ->

                //Content resolver will process the contentvalues
                val contentValues = ContentValues().apply {

                    //putting file information in content values
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                }

                //Inserting the contentValues to contentResolver and getting the Uri
                val imageUri: Uri? =
                    resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

                //Opening an outputstream with the Uri that we got
                fos = imageUri?.let { resolver.openOutputStream(it) }
            }
        } else {
            //These for devices running on android < Q
            //So I don't think an explanation is needed here
            val imagesDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val image = File(imagesDir, filename)
            fos = FileOutputStream(image)
        }

        fos?.use {
            //Finally writing the bitmap to the output stream that we opened
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
            Toast.makeText(this.context, "Saved to Photos", Toast.LENGTH_SHORT).show()
        }
    }

    // Set image / icon to imageView after being picked
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val addLocationViewModel: AddLocationViewModel by navGraphViewModels(R.id.editLocation_nav_graph)
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            imageUri = data?.data
            when (requestCode) {
                pickImage -> {
                    locationImage.setImageURI(imageUri)
                    locationImageUri = imageUri.toString()
                    addLocationViewModel.uploadImage("icon", locationIcon)
                }
                pickIcon -> {
                    locationIcon.setImageURI(imageUri)
                    locationIconUri = imageUri.toString()
                    addLocationViewModel.uploadImage("image", locationImage)
                }
            }
        }
    }
}