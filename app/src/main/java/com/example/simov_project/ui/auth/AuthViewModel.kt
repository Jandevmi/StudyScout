package com.example.simov_project.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.simov_project.dataclasses.User
import com.example.simov_project.db.FirebaseProvider
import kotlinx.coroutines.launch

/**
 * ViewModel to persist users
 */
class AuthViewModel : ViewModel() {

    private val _user = MutableLiveData<User>().apply { value = User() }
    private val _userId = MutableLiveData<String>().apply { value = "-1" }
    private val _loading = MutableLiveData<Boolean>().apply { value = true }
    val user: LiveData<User> = _user
    val userId: LiveData<String> = _userId
    val loading: LiveData<Boolean> = _loading

    init {
        _loading.value = true
        _userId.value = "loggedOut"
    }

    /**
     * Log in user with email and password. Update userId or set it to "failed"
     */
    fun login(email: String, password: String) {
        _loading.value = true
        viewModelScope.launch {
            val userId = FirebaseProvider.auth.userLogin(email, password)
            if (userId != null) {
                _userId.value = userId
            } else {
                _userId.value = "failed"
                _loading.value = false
            }
        }
    }

    /**
     * Log out user and set userId to "loggedOut"
     */
    fun logout(): Boolean {
        return if (FirebaseProvider.auth.userLogout()) {
            _userId.value = "loggedOut"
            _user.value = User()
            true
        } else false
    }

    /**
     * Get user from Firebase and set it to local val
     * Sets the user as not location owner
     */
    fun getUser() {
        viewModelScope.launch {
            userId.value?.let {
                _user.value = FirebaseProvider.db.getUser(it)
                _user.value?.locationOwner = false
            }
        }
    }

    /**
     * Updates the user in Firebase and local if it succeeded
     * Creates the user it does not exist
     * @param user The user to be created or updated
     */
    fun updateUser(user: User) {
        viewModelScope.launch {
            FirebaseProvider.db.updateUser(user)?.let {
                _user.value = user
            }
        }
    }

    /**
     * Adds the reservationID to the user locally and to Firebase
     * @param reservationId The ID to be added to user.reservationIds
     */
    fun addReservationId(reservationId: String) {
        user.value?.let {
            it.reservationIds.apply {
                add(reservationId)
                distinct()
            }
            updateUser(it)
        }
    }

    fun removeReservationId(reservationId: String) {
        user.value?.let {
            it.reservationIds.apply {
                remove(reservationId)
                distinct()
            }
            updateUser(it)
        }
    }

    /**
     * Adds the locationID to the user locally and to Firebase
     * @param locationId The ID to be added to user.locationIds
     */
    fun addLocationId(locationId: String) {
        user.value?.let {
            it.locationIds.apply {
                add(locationId)
                distinct()
            }
            updateUser(it)
        }
    }

    /**
     * Set userId to the one of the current user or to "loggedOut"
     * Result turns loading off
     */
    fun checkIfUserLoggedIn() {
        _userId.value = FirebaseProvider.auth.getUserId() ?: kotlin.run {
            _loading.value = false
            "loggedOut"
        }
    }

    /**
     * Register user with email and password. Update userId or set it to "failed"
     */
    fun registerUser(email: String, name: String, password: String) {
        viewModelScope.launch {
            _loading.value = true
            FirebaseProvider.auth.createUser(email, password)?.let { userId ->
                updateUser(
                    User(
                        userId = userId,
                        name = name,
                        email = email
                    )
                )
                true
            } ?: kotlin.run {
                _loading.value = false
                _userId.value = "failed"
            }
        }
    }
}