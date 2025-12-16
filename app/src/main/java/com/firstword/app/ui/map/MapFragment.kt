package com.firstword.app.ui.map

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.firstword.app.R
import com.firstword.app.databinding.FragmentMapBinding
import com.firstword.app.models.AuthenticityVotes
import com.firstword.app.models.Post
import com.firstword.app.models.PostLocation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var firestore: FirebaseFirestore
    private var postsListener: ListenerRegistration? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.fabMyLocation.setOnClickListener {
            getCurrentLocation()
        }

        binding.btnMapType.setOnClickListener {
            toggleMapType()
        }

        // Hide legend when tapped
        binding.legendCard.setOnClickListener {
            binding.legendCard.visibility = View.GONE
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = false

        // Set up marker click listener
        googleMap.setOnMarkerClickListener { marker ->
            val post = marker.tag as? Post
            post?.let {
                showPostDetails(it)
            }
            true
        }

        // Set default location
        val defaultLocation = LatLng(37.7749, -122.4194)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 3f))

        checkLocationPermission()
        setupRealTimePostsListener()
    }

    private fun setupRealTimePostsListener() {
        postsListener = firestore.collection("posts")
            .whereNotEqualTo("location", null)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                error?.let {
                    Toast.makeText(context, "Error loading posts: ${it.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                snapshots?.let { querySnapshot ->
                    googleMap.clear() // Clear existing markers

                    for (document in querySnapshot.documents) {
                        try {
                            // Parse post data
                            val data = document.data ?: continue

                            // Check if location exists in the data
                            val locationData = data["location"] as? Map<String, Any>
                            if (locationData != null) {
                                val location = extractLocation(locationData)
                                val post = createPostFromData(document.id, data)

                                addPostMarker(post, location)
                            }
                        } catch (e: Exception) {
                            // Skip this document if there's an error
                            continue
                        }
                    }

                    binding.legendCard.visibility = if (querySnapshot.documents.isEmpty()) View.GONE else View.VISIBLE
                }
            }
    }

    private fun createPostFromData(documentId: String, data: Map<String, Any>): Post {
        // Extract authenticity votes
        val votesData = data["authenticityVotes"] as? Map<String, Any>
        val authenticityVotes = AuthenticityVotes(
            trueCount = (votesData?.get("trueCount") as? Long)?.toInt() ?: 0,
            fakeCount = (votesData?.get("fakeCount") as? Long)?.toInt() ?: 0,
            aiCount = (votesData?.get("aiCount") as? Long)?.toInt() ?: 0
        )

        return Post(
            id = documentId,
            userId = data["userId"] as? String ?: "",
            userDisplayName = data["userDisplayName"] as? String ?: "",
            userHandle = data["userHandle"] as? String ?: "",
            title = data["title"] as? String ?: "",
            content = data["content"] as? String ?: "",
            category = data["category"] as? String ?: "general",
            likesCount = (data["likesCount"] as? Long)?.toInt() ?: 0,
            commentsCount = (data["commentsCount"] as? Long)?.toInt() ?: 0,
            sharesCount = (data["sharesCount"] as? Long)?.toInt() ?: 0,
            authenticityVotes = authenticityVotes
        )
    }

    private fun extractLocation(locationData: Map<String, Any>): PostLocation {
        return PostLocation(
            latitude = (locationData["latitude"] as? Double) ?: 0.0,
            longitude = (locationData["longitude"] as? Double) ?: 0.0,
            address = locationData["address"] as? String,
            placeName = locationData["placeName"] as? String,
            accuracy = (locationData["accuracy"] as? Double) ?: 0.0
        )
    }

    private fun addPostMarker(post: Post, location: PostLocation) {
        val latLng = LatLng(location.latitude, location.longitude)

        // Calculate marker size based on engagement
        val engagementScore = post.likesCount + post.commentsCount * 2 + post.sharesCount * 3
        val markerSize = when {
            engagementScore > 100 -> 1.5f
            engagementScore > 50 -> 1.2f
            engagementScore > 10 -> 1.0f
            else -> 0.8f
        }

        // Choose marker color based on authenticity
        val authenticityScore = calculateAuthenticityScore(post.authenticityVotes)
        val markerColor = when {
            authenticityScore > 0.7 -> BitmapDescriptorFactory.HUE_GREEN
            authenticityScore > 0.4 -> BitmapDescriptorFactory.HUE_ORANGE
            else -> BitmapDescriptorFactory.HUE_RED
        }

        val title = if (post.category == "News") {
            "📰 ${post.title}"
        } else {
            "💬 ${post.userDisplayName}"
        }

        val locationName = location.placeName ?: location.address ?: "Unknown location"

        val snippet = """
            ${post.content.take(80)}${if (post.content.length > 80) "..." else ""}
            👍 ${post.likesCount} 💬 ${post.commentsCount} 🔄 ${post.sharesCount}
            $locationName
            Trust: ${(authenticityScore * 100).toInt()}%
        """.trimIndent()

        val marker = googleMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(title)
                .snippet(snippet)
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
        )

        marker?.tag = post
        marker?.setAnchor(0.5f, 0.5f) // Center the marker
    }

    private fun calculateAuthenticityScore(authenticityVotes: AuthenticityVotes): Double {
        val totalVotes = authenticityVotes.getTotalCount().toDouble()
        return if (totalVotes == 0.0) {
            0.5 // Default neutral score when no votes
        } else {
            // Calculate score based on true votes vs fake/ai votes
            val trueScore = authenticityVotes.trueCount.toDouble() / totalVotes
            val fakeAiScore = (authenticityVotes.fakeCount.toDouble() + authenticityVotes.aiCount.toDouble()) / totalVotes
            // Normalize to 0-1 scale where 1 is most authentic
            trueScore / (trueScore + fakeAiScore)
        }
    }

    private fun showPostDetails(post: Post) {
        // You can implement a bottom sheet or dialog here
        Toast.makeText(
            context,
            "${post.title}\nby ${post.userDisplayName}",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun toggleMapType() {
        googleMap.mapType = when (googleMap.mapType) {
            GoogleMap.MAP_TYPE_NORMAL -> GoogleMap.MAP_TYPE_SATELLITE
            GoogleMap.MAP_TYPE_SATELLITE -> GoogleMap.MAP_TYPE_TERRAIN
            else -> GoogleMap.MAP_TYPE_NORMAL
        }
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
            getCurrentLocation()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
                getCurrentLocation()
            } else {
                Toast.makeText(
                    context,
                    "Location permission denied. Some features may not work.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            googleMap.isMyLocationEnabled = true
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

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(currentLatLng, 12f)
                )
            } ?: run {
                Toast.makeText(
                    context,
                    "Unable to get current location",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(
                context,
                "Failed to get location: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        postsListener?.remove()
        _binding = null
    }
}