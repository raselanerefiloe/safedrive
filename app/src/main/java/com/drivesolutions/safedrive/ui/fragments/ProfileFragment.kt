package com.drivesolutions.safedrive.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import coil3.load
import coil3.request.transformations
import coil3.transform.RoundedCornersTransformation
import com.drivesolutions.safedrive.R
import com.drivesolutions.safedrive.SignInActivity
import com.drivesolutions.safedrive.data.models.User
import com.drivesolutions.safedrive.data.sources.remote.Appwrite
import io.appwrite.Client
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private lateinit var client: Client
    private lateinit var users: User

    private lateinit var userAvatar: ImageView
    private lateinit var userName: TextView
    private lateinit var userEmail: TextView
    private lateinit var logoutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        userAvatar = view.findViewById(R.id.user_avatar)
        userName = view.findViewById(R.id.user_name)
        userEmail = view.findViewById(R.id.user_email)
        logoutButton = view.findViewById(R.id.logout_button)

        // Load current user information inside a coroutine
        lifecycleScope.launch {
            loadCurrentUser()
        }

        // Set up logout button to call logoutUser() within a coroutine
        logoutButton.setOnClickListener {
            lifecycleScope.launch {
                logoutUser()
            }
        }

        return view
    }

    private suspend fun loadCurrentUser() {
        // Get the current user
        val user = Appwrite.getUserDetails()
        Log.d("User Details", "Fragment fetched user $user")
        user?.let {
            userName.text = it.names // Assuming user.name is available
            userEmail.text = it.email // Assuming user.email is available

            // Load user avatar using Coil
            if (it.avatar != null) {
                userAvatar.load(it.avatar){
                    transformations(RoundedCornersTransformation(50f))
                }
            } else {
                // If no avatar is set, show default avatar
                userAvatar.setImageResource(R.drawable.ic_avatar)
            }
        }
    }

    private suspend fun logoutUser() {
        // Handle user logout
        Appwrite.onLogout()
        startActivity(Intent(requireContext(), SignInActivity::class.java))
        activity?.finish()
    }
}
