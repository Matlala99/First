package com.firstword.app.ui.create

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.firstword.app.R
import com.firstword.app.databinding.FragmentCreatePostBinding
import com.firstword.app.models.Location as PostLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.util.Locale
import java.util.UUID

class CreatePostFragment : Fragment() {

    private var _binding: FragmentCreatePostBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var selectedImageUri: Uri? = null
    private var currentUser: FirebaseUser? = null
    private var currentLocation: PostLocation? = null

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                getCurrentLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                getCurrentLocation()
            }
            else -> {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val imagePicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedUri ->
            if (isUriAccessible(selectedUri)) {
                selectedImageUri = selectedUri
                binding.imagePreview.setImageURI(selectedUri)
                binding.imagePreview.visibility = View.VISIBLE
                binding.btnRemoveImage.visibility = View.VISIBLE
            } else {
                Toast.makeText(requireContext(), "Cannot access selected image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreatePostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        checkAuthentication()
        setupClickListeners()
        setupCategorySpinner()
        requestLocationPermission()
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            getCurrentLocation()
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        binding.locationProgress.visibility = View.VISIBLE
        binding.tvLocationStatus.text = "Getting your location..."

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                // Get address from coordinates
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(it.latitude, it.longitude, 1)

                val address = addresses?.firstOrNull()
                currentLocation = PostLocation(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    city = address?.locality ?: "",
                    country = address?.countryName ?: "",
                    address = address?.getAddressLine(0) ?: ""
                )

                binding.tvLocationStatus.text = "📍 ${address?.locality ?: "Location captured"}"
                binding.btnRemoveLocation.visibility = View.VISIBLE
            } ?: run {
                binding.tvLocationStatus.text = "❌ Could not get location"
            }
            binding.locationProgress.visibility = View.GONE
        }.addOnFailureListener { exception ->
            binding.tvLocationStatus.text = "❌ Location error"
            binding.locationProgress.visibility = View.GONE
            Toast.makeText(requireContext(), "Location error: ${exception.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAuthentication() {
        currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please sign in to create a post", Toast.LENGTH_LONG).show()
            findNavController().navigateUp()
        }
    }

    private fun setupCategorySpinner() {
        val categories = arrayOf("General", "News", "Technology", "Politics", "Entertainment", "Sports", "Science", "Health", "Business")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnSelectImage.setOnClickListener {
            imagePicker.launch("image/*")
        }

        binding.btnRemoveImage.setOnClickListener {
            selectedImageUri = null
            binding.imagePreview.setImageURI(null)
            binding.imagePreview.visibility = View.GONE
            binding.btnRemoveImage.visibility = View.GONE
        }

        binding.btnGetLocation.setOnClickListener {
            requestLocationPermission()
        }

        binding.btnRemoveLocation.setOnClickListener {
            currentLocation = null
            binding.tvLocationStatus.text = "No location added"
            binding.btnRemoveLocation.visibility = View.GONE
        }

        binding.btnPost.setOnClickListener {
            createPost()
        }

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun createPost() {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Please sign in to create a post", Toast.LENGTH_SHORT).show()
            return
        }

        val title = binding.etTitle.text?.toString()?.trim() ?: ""
        val content = binding.etContent.text?.toString()?.trim() ?: ""
        val category = binding.spinnerCategory.selectedItem?.toString() ?: "General"

        if (title.isEmpty() || content.isEmpty() || title.length < 5 || content.length < 10) {
            // Validation logic (same as before)
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        binding.btnPost.isEnabled = false

        if (selectedImageUri != null) {
            uploadImageAndCreatePost(currentUser!!.uid, title, content, category)
        } else {
            createPostInFirestore(currentUser!!.uid, title, content, category, null)
        }
    }

    private fun isUriAccessible(uri: Uri): Boolean {
        return try {
            requireContext().contentResolver.openInputStream(uri)?.use { true } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun uploadImageAndCreatePost(userId: String, title: String, content: String, category: String) {
        val uri = selectedImageUri ?: run {
            createPostInFirestore(userId, title, content, category, null)
            return
        }

        if (!isUriAccessible(uri)) {
            binding.progressBar.visibility = View.GONE
            binding.btnPost.isEnabled = true
            Toast.makeText(requireContext(), "Cannot access image file", Toast.LENGTH_SHORT).show()
            return
        }

        val filename = "posts/${userId}/${UUID.randomUUID()}.jpg"
        val imageRef = storage.reference.child(filename)

        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .setCustomMetadata("uploadedBy", userId)
            .build()

        imageRef.putFile(uri, metadata)
            .addOnSuccessListener { taskSnapshot ->
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    createPostInFirestore(userId, title, content, category, downloadUrl.toString())
                }.addOnFailureListener { exception ->
                    createPostInFirestore(userId, title, content, category, null)
                }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                binding.btnPost.isEnabled = true
                Toast.makeText(requireContext(), "Upload failed: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun createPostInFirestore(userId: String, title: String, content: String, category: String, imageUrl: String?) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { userDocument ->
                val userHandle = userDocument.getString("handle") ?: "user_${userId.take(8)}"
                val userDisplayName = userDocument.getString("displayName") ?: "User"
                val userAvatarUrl = userDocument.getString("avatarUrl") ?: ""

                val postId = firestore.collection("posts").document().id
                val post = hashMapOf(
                    "id" to postId,
                    "title" to title,
                    "content" to content,
                    "imageUrl" to imageUrl,
                    "userId" to userId,
                    "userHandle" to userHandle,
                    "userDisplayName" to userDisplayName,
                    "userAvatarUrl" to userAvatarUrl,
                    "category" to category,
                    "location" to currentLocation?.let { location ->
                        hashMapOf(
                            "latitude" to location.latitude,
                            "longitude" to location.longitude,
                            "city" to location.city,
                            "country" to location.country,
                            "address" to location.address
                        )
                    },
                    "likesCount" to 0,
                    "commentsCount" to 0,
                    "sharesCount" to 0,
                    "viewsCount" to 0,
                    "authenticityVotes" to hashMapOf(
                        "trueCount" to 0,
                        "fakeCount" to 0,
                        "aiCount" to 0,
                        "totalCount" to 0
                    ),
                    "isVerified" to false,
                    "isReported" to false,
                    "isHidden" to false,
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                    "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )

                firestore.collection("posts").document(postId)
                    .set(post)
                    .addOnSuccessListener {
                        binding.progressBar.visibility = View.GONE
                        binding.btnPost.isEnabled = true
                        Toast.makeText(requireContext(), "Post created with location!", Toast.LENGTH_SHORT).show()
                        findNavController().navigateUp()
                    }
                    .addOnFailureListener { exception ->
                        binding.progressBar.visibility = View.GONE
                        binding.btnPost.isEnabled = true
                        Toast.makeText(requireContext(), "Failed to create post: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { exception ->
                binding.progressBar.visibility = View.GONE
                binding.btnPost.isEnabled = true
                Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}