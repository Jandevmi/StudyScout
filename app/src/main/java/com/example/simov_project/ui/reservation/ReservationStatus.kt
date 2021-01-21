package com.example.simov_project.ui.reservation

/**
 * Enum Class for different states of a reservation
 *
 * Active: Reservation has been activated and is not over
 * Activate: Reservation can be activated (QR-code scan) now
 * Cancelled: Reservation was not activated in time
 * Reserved: Reservation is in the future
 * Unknown: Should not be reached
 */
enum class ReservationStatus(val status: String) {
    ACTIVE("Active"),
    ACTIVATE("Activate now"),
    CANCELLED("Cancelled"),
    RESERVED("Reserved"),
    ENDED("Ended"),
    UNKNOWN("Unknown status")
}