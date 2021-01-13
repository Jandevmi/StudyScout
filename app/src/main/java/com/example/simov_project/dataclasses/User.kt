package com.example.simov_project.dataclasses

data class User(
    var userId: String = "-1",
    val email: String = "-1",
    val name: String = "-1",
    var locationOwner: Boolean = false,
    val reservationIds: MutableList<String> = mutableListOf(),
    val locationIds: MutableList<String> = mutableListOf()
)
