package com.example.simov_project.ui.auth

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.simov_project.R

class RegisterFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val root = inflater.inflate(R.layout.fragment_register, container, false)
        val authViewModel: AuthViewModel by activityViewModels()
        val emailView: EditText = root.findViewById(R.id.register_email_editText)
        val passwordView: EditText = root.findViewById(R.id.register_password_editText)
        val nameView: EditText = root.findViewById(R.id.register_name_editText)
        val registerButton: Button = root.findViewById(R.id.register_confirm_button)
        val loadingView: ProgressBar = root.findViewById(R.id.progress_loader)

        authViewModel.loading.observe(viewLifecycleOwner, Observer { loading ->
            if (loading) {
                loadingView.visibility = View.VISIBLE
                emailView.visibility = View.INVISIBLE
                passwordView.visibility = View.INVISIBLE
                registerButton.visibility = View.INVISIBLE
            } else {
                loadingView.visibility = View.INVISIBLE
                emailView.visibility = View.VISIBLE
                passwordView.visibility = View.VISIBLE
                registerButton.visibility = View.VISIBLE
            }
        })

        authViewModel.userId.observe(viewLifecycleOwner, Observer { userId ->
            println("Observe userID: $userId")
            when (userId) {
                "failed" -> Toast.makeText(
                    requireContext(),
                    "Email already registered",
                    Toast.LENGTH_SHORT
                ).show()
                "loggedOut" -> print("")
                "-1" -> println("userId should not be -1")
                null -> println("userId should not be null")
                else -> {
                    authViewModel.getUser()
                    findNavController().navigate(R.id.action_navigation_register_to_location)
                }
            }
        })

        authViewModel.user.observe(viewLifecycleOwner, Observer { user ->
            if (user != null && user.userId != "-1") {
                findNavController().navigate(R.id.action_navigation_register_to_location)
            }
        })

        registerButton.setOnClickListener {
            val email = emailView.text.toString()
            val name = nameView.text.toString()
            val password = passwordView.text.toString()
            val toastText = when {
                !isValidEmail(email) -> "E-mail address not valid"
                !isValidPassword(password) -> "Enter password"
                name.isEmpty() -> "Enter name"
                else -> null
            }
            if (toastText != null)
                Toast.makeText(requireContext(), toastText, Toast.LENGTH_SHORT).show()
            else
                authViewModel.registerUser(email, name, password)
        }
        return root
    }

    /**
     * @return true if email is not empty and matches an email pattern
     */
    private fun isValidEmail(email: String): Boolean {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email)
            .matches()
    }

    /**
     * @return true if password is not empty and matches an email pattern
     */
    private fun isValidPassword(password: String): Boolean {
        return !TextUtils.isEmpty(password)
    }

}