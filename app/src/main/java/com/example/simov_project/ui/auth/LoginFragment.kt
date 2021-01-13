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
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.simov_project.R


class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_login, container, false)
        val authViewModel: AuthViewModel by activityViewModels()
        val emailView: EditText = root.findViewById(R.id.login_email_editText)
        val passwordView: EditText = root.findViewById(R.id.login_password_editText)
        val loginButton: Button = root.findViewById(R.id.login_login_button)
        val registerButton: Button = root.findViewById(R.id.login_register_button)
        val loadingView: ProgressBar = root.findViewById(R.id.progress_loader)

        authViewModel.loading.observe(viewLifecycleOwner, Observer { loading ->
            if (loading) {
                loadingView.visibility = View.VISIBLE
                emailView.visibility = View.INVISIBLE
                passwordView.visibility = View.INVISIBLE
                loginButton.visibility = View.INVISIBLE
                registerButton.visibility = View.INVISIBLE
            } else {
                loadingView.visibility = View.INVISIBLE
                emailView.visibility = View.VISIBLE
                passwordView.visibility = View.VISIBLE
                loginButton.visibility = View.VISIBLE
                registerButton.visibility = View.VISIBLE
            }
        })

        authViewModel.userId.observe(viewLifecycleOwner, Observer { userId ->
            when (userId) {
                "failed" -> Toast.makeText(requireContext(),
                    "Email/Password don´t match", Toast.LENGTH_SHORT).show()
                "loggedOut" -> print("")
                "-1" -> println("userId should not be -1")
                null -> println("userId should not be null")
                else -> authViewModel.getUser()
            }
        })

        authViewModel.user.observe(viewLifecycleOwner, Observer { user ->
            if (user.userId != "-1")
                findNavController().navigate(R.id.action_navigation_login_to_location)
        })

        authViewModel.checkIfUserLoggedIn()

        loginButton.setOnClickListener() {
            val email = emailView.text.toString().trim()
            val password = passwordView.text.toString()

            if (!isValidEmail(email)) {
                Toast.makeText(requireContext(), "E-mail address not valid", Toast.LENGTH_SHORT)
                    .show()
            } else if (!isValidPassword(password)) {
                Toast.makeText(
                    requireContext(),
                    "Password needs at least 6 figures",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                authViewModel.login(email, password)
            }
        }

        registerButton.setOnClickListener {
            val action = LoginFragmentDirections.actionNavigationLoginToRegister()
            NavHostFragment.findNavController(this).navigate(action)
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
     * @return true if password has at least 6 figures
     */
    private fun isValidPassword(password: String): Boolean {
        return !TextUtils.isEmpty(password) && password.length > 5
    }
}