package com.drivesolutions.safedrive.data.models


import com.drivesolutions.safedrive.R

sealed class NavigationItem(val route: String, val title: String, val icon: Int?) {
    object Home : NavigationItem("home", "Home", R.drawable.ic_home_60dp)
    object ObjectDetection : NavigationItem("object_detection", "Object Detection", R.drawable.ic_center_focus_strong_60dp)
    object Profile : NavigationItem("profile", "Profile", R.drawable.ic_person_60dp)
    object SignIn : NavigationItem("signIn", "SignIn",null)
    object SignUp : NavigationItem("signUp", "SignUp",null)
}
