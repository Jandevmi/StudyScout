package com.example.simov_project.ui.addLocation

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.example.simov_project.R

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
        locationImage = root.findViewById(R.id.addImage_location_imageView)
        locationIcon = root.findViewById(R.id.addImage_viewHolder_imageView)
        val loadingProgressBar: ProgressBar = root.findViewById(R.id.addImage_progress_loader)
        val uploadButton: Button = root.findViewById(R.id.addImage_upload_button)
        val confirmButton: Button = root.findViewById(R.id.addImage_confirm_button)
        val pickImageButton: Button = root.findViewById(R.id.addImage_image_pick_button)
        val pickIconButton: Button = root.findViewById(R.id.addImage_icon_pick_button)

        addLocationViewModel.loading.observe(viewLifecycleOwner, Observer {
            if (it > 0) loadingProgressBar.visibility = View.VISIBLE
            else loadingProgressBar.visibility = View.GONE
        })

        addLocationViewModel.icon.observe(viewLifecycleOwner, Observer {
            locationIcon.setImageBitmap(it)
        })

        addLocationViewModel.image.observe(viewLifecycleOwner, Observer {
            locationImage.setImageBitmap(it)
        })

        pickImageButton.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, pickImage)
        }

        pickIconButton.setOnClickListener {
            val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
            startActivityForResult(gallery, pickIcon)
        }

        uploadButton.setOnClickListener {
            addLocationViewModel.uploadImage("icon", locationIcon)
            addLocationViewModel.uploadImage("image", locationImage)
        }

        confirmButton.setOnClickListener {
            addLocationViewModel.updateFirebaseLocation()
            findNavController().navigate(R.id.action_navigation_addLocationImage_to_location)
        }

        return root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            imageUri = data?.data
            when (requestCode) {
                pickImage -> {
                    locationImage.setImageURI(imageUri)
                    locationImageUri = imageUri.toString()
                }
                pickIcon -> {
                    locationIcon.setImageURI(imageUri)
                    locationIconUri = imageUri.toString()
                }
            }
        }
    }
}