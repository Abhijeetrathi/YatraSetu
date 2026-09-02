document.addEventListener("DOMContentLoaded", function () {

    console.log("YatraSetu app.js loaded successfully");

    // =====================================================
    // SEARCH BUTTON
    // =====================================================

    const searchButton = document.getElementById("searchTrains");

    if (searchButton) {

        searchButton.addEventListener("click", searchTrains);

    } else {

        console.error("Search button not found!");

    }


    // =====================================================
    // SET TODAY'S DATE
    // =====================================================

    const dateInput = document.getElementById("journeyDate");

    if (dateInput) {

        const today = new Date();

        const year = today.getFullYear();

        const month = String(today.getMonth() + 1).padStart(2, "0");

        const day = String(today.getDate()).padStart(2, "0");

        dateInput.value = `${year}-${month}-${day}`;

    }

});


// =====================================================
// SEARCH TRAINS
// =====================================================

async function searchTrains() {

    console.log("Searching trains...");


    // Get HTML elements

    const fromElement = document.getElementById("from");

    const toElement = document.getElementById("to");

    const dateElement = document.getElementById("journeyDate");

    const resultsContainer =
        document.getElementById("trainResults");


    // Get selected values

    const from = fromElement.value;

    const to = toElement.value;

    const date = dateElement.value;


    console.log("From:", from);

    console.log("To:", to);

    console.log("Journey Date:", date);


    // =====================================================
    // VALIDATE DATE
    // =====================================================

    if (!date) {

        alert("Please select a journey date.");

        return;

    }


    // =====================================================
    // SHOW LOADING
    // =====================================================

    resultsContainer.innerHTML = `

        <div class="loading">

            <div class="loading-icon">
                🚆
            </div>

            <h3>
                Searching trains...
            </h3>

            <p>
                Please wait.
            </p>

        </div>

    `;


    try {

        // =================================================
        // CALL SPRING BOOT API
        // =================================================

        const apiUrl =
            `/api/trains?source=${encodeURIComponent(from)}` +
            `&destination=${encodeURIComponent(to)}` +
            `&date=${date}`;


        console.log("API URL:", apiUrl);


        const response = await fetch(apiUrl);


        console.log("API response:", response);


        // =================================================
        // CHECK RESPONSE
        // =================================================

        if (!response.ok) {

            throw new Error(
                "API request failed: " + response.status
            );

        }


        // =================================================
        // GET TRAIN DATA
        // =================================================

        const trains = await response.json();


        console.log(
            "Trains returned by API:",
            trains
        );


        // =================================================
        // NO TRAINS FOUND
        // =================================================

        if (trains.length === 0) {

            resultsContainer.innerHTML = `

                <div class="no-results">

                    <div class="no-results-icon">
                        🚫
                    </div>

                    <h3>
                        No trains found
                    </h3>

                    <p>
                        No trains are available for
                        ${from} → ${to}
                    </p>

                    <p>
                        Journey Date:
                        ${formatDate(date)}
                    </p>

                </div>

            `;

            return;

        }


        // =================================================
        // CLEAR OLD RESULTS
        // =================================================

        resultsContainer.innerHTML = "";


        // =================================================
        // CREATE TRAIN CARDS
        // =================================================

        trains.forEach(function (train) {

            const card =
                document.createElement("div");


            card.className = "train-card";


            card.innerHTML = `

                <div class="train-card-top">

                    <div class="train-name">

                        <div class="train-icon-small">
                            🚆
                        </div>

                        <div>

                            <h3>
                                ${train.trainName}
                            </h3>

                            <p>
                                Train No: ${train.trainNumber}
                            </p>

                        </div>

                    </div>


                    <div class="class-badge">

                        ${train.trainClass}

                    </div>

                </div>


                <div class="journey-info">

                    <div class="station">

                        <strong>
                            ${formatTime(train.departureTime)}
                        </strong>

                        <span>
                            ${train.source}
                        </span>

                    </div>


                    <div class="journey-line">

                        <span>
                            ${train.duration}
                        </span>

                        <div class="line">
                            ─────────▶
                        </div>

                    </div>


                    <div class="station">

                        <strong>
                            ${formatTime(train.arrivalTime)}
                        </strong>

                        <span>
                            ${train.destination}
                        </span>

                    </div>

                </div>


                <div class="train-details">

                    <div>

                        <span>
                            Available Seats
                        </span>

                        <strong>
                            ${train.availableSeats}
                        </strong>

                    </div>


                    <div>

                        <span>
                            Class
                        </span>

                        <strong>
                            ${train.trainClass}
                        </strong>

                    </div>


                    <div>

                        <span>
                            Fare
                        </span>

                        <strong class="fare">
                            ₹${Number(train.fare).toFixed(2)}
                        </strong>

                    </div>


                    <button
                        class="book-btn"
                        onclick="bookTrain(${train.id})">

                        Book Now

                    </button>

                </div>

            `;


            resultsContainer.appendChild(card);

        });

    }


    // =====================================================
    // ERROR HANDLING
    // =====================================================

    catch (error) {

        console.error(
            "Error loading trains:",
            error
        );


        resultsContainer.innerHTML = `

            <div class="no-results">

                <div class="no-results-icon">
                    ⚠️
                </div>

                <h3>
                    Unable to load trains
                </h3>

                <p>
                    Something went wrong while
                    searching for trains.
                </p>

                <p>
                    Please make sure your Spring Boot
                    application is running.
                </p>

                <p>
                    API: /api/trains
                </p>

            </div>

        `;

    }

}


// =====================================================
// FORMAT DATE
// =====================================================

function formatDate(dateString) {

    const date =
        new Date(dateString + "T00:00:00");


    const day =
        String(date.getDate()).padStart(2, "0");


    const month =
        String(date.getMonth() + 1).padStart(2, "0");


    const year =
        date.getFullYear();


    return `${day}-${month}-${year}`;

}


// =====================================================
// FORMAT TIME
// =====================================================

function formatTime(timeString) {

    if (!timeString) {
        return "";
    }


    // Spring Boot normally returns HH:mm:ss

    const parts =
        timeString.split(":");


    return `${parts[0]}:${parts[1]}`;

}


// =====================================================
// SWAP FROM / TO
// =====================================================

function swapStations() {

    const from =
        document.getElementById("from");

    const to =
        document.getElementById("to");


    const temp =
        from.value;


    from.value =
        to.value;


    to.value =
        temp;

}


// =====================================================
// BOOK TRAIN
// =====================================================

// =====================================================
// BOOK TRAIN
// =====================================================

function bookTrain(trainId) {

    const confirmLogin = confirm(
        "🔐 Login Required\n\n" +
        "Please login or register before booking a ticket.\n\n" +
        "Would you like to continue to Login?"
    );

    if (confirmLogin) {

        window.location.href = "/login.html";

    }

}