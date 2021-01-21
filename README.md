# README #

Hello Evenly

### Open Issues im main repo ###

* Tests !!!
* Handle old reservations
* Logic check if location is fully booked


### Create Location 
* Settings -> Press Switch owner mode on -> Create new location
* Finding a place with google Place API not possible without API Key
* Ask for key if needed

### Edit Location
* Settings -> Press Switch owner mode on
* Locations -> click on location now opens fragment to edit location
* First page for general data
* Second to edit operating times
* Third to upload icon / image an to get QR-Code

### QR-Code
The QR-code is needed to activate the reservation.
QR-code data is the locationId.

### Activate Reservations
Reservations can be activated from start to 30 minutes after start.
Click on a reservation to go to detailPage.
Button activate Reservation starts QR-code scanner.
If Reservation was not activated it gets cancelled.