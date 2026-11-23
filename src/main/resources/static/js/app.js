
/* =====================================================
   NAVBAR / LOGIN STATUS
===================================================== */

async function updateNavbar() {
  const navButtons = document.getElementById("navButtons");

  if (!navButtons) return;

  try {
    const response = await fetch("/api/auth/me");

    if (!response.ok) {
      navButtons.innerHTML = `
        <a href="/login.html">
          <button class="login-btn">Login</button>
        </a>

        <a href="/register.html">
          <button class="register-btn">Register</button>
        </a>
      `;
      return;
    }

    const user = await response.json();

    localStorage.setItem("userId", user.userId);
    localStorage.setItem("userName", user.name);
    localStorage.setItem("userEmail", user.email);

    navButtons.innerHTML = `
      <a href="/account.html">
        <button class="login-btn">My Account</button>
      </a>

      <button class="register-btn" onclick="logoutUser()">
        Logout
      </button>
    `;
  } catch (error) {
    console.error("Unable to check login status:", error);
  }
}


/* =====================================================
   LOGOUT
===================================================== */

async function logoutUser() {
  try {
    const response = await fetch("/api/auth/logout", {
      method: "POST"
    });

    if (response.ok) {
      localStorage.removeItem("userId");
      localStorage.removeItem("userName");
      localStorage.removeItem("userEmail");

      window.location.reload();
    } else {
      alert("Unable to logout. Please try again.");
    }
  } catch (error) {
    console.error("Logout error:", error);
    alert("Something went wrong while logging out.");
  }
}


/* =====================================================
   PAGE LOAD
===================================================== */

// Prevent browsers from restoring a previous scroll position on refresh.
if ("scrollRestoration" in history) {
  history.scrollRestoration = "manual";
}

document.addEventListener("DOMContentLoaded", function () {
  // Start each fresh page load with empty station fields and at the top.
  const fromField = document.getElementById("from");
  const toField = document.getElementById("to");
  if (fromField) fromField.value = "";
  if (toField) toField.value = "";
  window.scrollTo(0, 0);

  console.log("YatraSetu app.js loaded successfully");

  updateNavbar();

  const searchButton = document.getElementById("searchTrains");
  const swapButton = document.getElementById("swapStations");

  if (searchButton) {
    searchButton.addEventListener("click", searchTrains);
  } else {
    console.error("Search button not found!");
  }

  if (swapButton) {
    swapButton.addEventListener("click", swapStations, { capture: true });
  } else {
    console.error("Swap stations button not found!");
  }

  // Journey date intentionally remains blank.
});

window.addEventListener("load", function () {
  window.scrollTo({ top: 0, left: 0, behavior: "instant" });
});


/* =====================================================
   SCROLL TO TRAIN RESULTS
===================================================== */

function scrollToTrainResults() {
  const trainsSection = document.getElementById("trains");

  if (!trainsSection) return;

  trainsSection.scrollIntoView({
    behavior: "smooth",
    block: "start"
  });
}


/* =====================================================
   SEARCH TRAINS
===================================================== */

async function searchTrains() {
  console.log("Searching trains...");

  const fromElement = document.getElementById("from");
  const toElement = document.getElementById("to");
  const dateElement = document.getElementById("journeyDate");
  const resultsContainer = document.getElementById("trainResults");

  if (!fromElement || !toElement || !dateElement || !resultsContainer) {
    console.error("Search elements not found!");
    return;
  }

  const from = fromElement.value.trim().replace(/\s+/g, " ");
  const to = toElement.value.trim().replace(/\s+/g, " ");
  const date = dateElement.value;
  fromElement.value = from;
  toElement.value = to;

  console.log("From:", from);
  console.log("To:", to);
  console.log("Journey Date:", date);

  /* VALIDATION */

  if (!from) {
    alert("Please enter your departure station.");
    fromElement.focus();
    return;
  }

  if (!to) {
    alert("Please enter your destination.");
    toElement.focus();
    return;
  }

  if (from.toLowerCase() === to.toLowerCase()) {
    alert("Source and destination cannot be the same.");
    toElement.focus();
    return;
  }

  if (!date) {
    alert("Please select a journey date.");
    dateElement.focus();
    return;
  }

  /* SHOW LOADING */

  resultsContainer.innerHTML = `
    <div class="loading">
      <div class="loading-icon">🚆</div>
      <h3>Searching trains...</h3>
      <p>Please wait.</p>
    </div>
  `;

  // Scroll to the Available Trains section immediately
  scrollToTrainResults();

  try {
    /* API REQUEST */

    const apiUrl =
      `/api/trains?source=${encodeURIComponent(from)}` +
      `&destination=${encodeURIComponent(to)}` +
      `&date=${date}`;

    console.log("API URL:", apiUrl);

    const response = await fetch(apiUrl);

    if (!response.ok) {
      throw new Error("API request failed: " + response.status);
    }

    /* GET TRAINS */

    const trains = await response.json();

    console.log("Trains returned by API:", trains);

    /* NO TRAINS FOUND */

    if (!trains || trains.length === 0) {
      resultsContainer.innerHTML = `
        <div class="no-results">
          <div class="no-results-icon">🚫</div>

          <h3>No trains found</h3>

          <p>
            No trains are available for
            ${escapeHtml(from)} → ${escapeHtml(to)}
          </p>

          <p>
            Journey Date: ${formatDate(date)}
          </p>
        </div>
      `;

      scrollToTrainResults();
      return;
    }

    /* CLEAR OLD RESULTS */

    resultsContainer.innerHTML = "";

    /* CREATE TRAIN CARDS */

    for (const train of trains) {
      const card = document.createElement("div");
      card.className = "train-card";
      card.dataset.trainName = String(train.trainName || "");
      card.dataset.departureTime = String(train.departureTime || "");

      /* LOAD CLASS DETAILS */

      let classes = [];

      try {
        const classResponse = await fetch(
          `/api/trains/${train.id}/classes`
        );

        if (classResponse.ok) {
          classes = await classResponse.json();
        }
      } catch (classError) {
        console.error(
          `Unable to load classes for train ${train.id}:`,
          classError
        );
      }

      /* CLASS CARDS */

      const classCards = classes.length
        ? classes.map(function (trainClass) {
            const soldOut =
              Number(trainClass.availableSeats) <= 0;

            return `
              <div class="class-option ${soldOut ? "class-sold-out" : ""}">

                <div class="class-code">
                  ${escapeHtml(trainClass.classCode)}
                </div>

                <div class="class-fare">
                  ₹${Number(trainClass.fare).toLocaleString("en-IN")}
                </div>

                <div class="class-seats">
                  ${Number(trainClass.availableSeats)} seats
                </div>

                ${
                  soldOut
                    ? `<div class="class-full-label">Sold out</div>`
                    : ""
                }

              </div>
            `;
          }).join("")
        : `
          <div class="classes-unavailable">
            Class information unavailable
          </div>
        `;

      /* TRAIN CARD HTML */

      card.innerHTML = `
        <div class="train-card-layout">

          <!-- TRAIN INFO -->

          <div class="train-info">

            <div class="train-icon-small">
              🚆
            </div>

            <div class="train-details">

              <span class="train-number">
                ${escapeHtml(train.trainNumber)}
              </span>

              <h3>
                ${escapeHtml(train.trainName)}
              </h3>

            </div>

          </div>

          <!-- JOURNEY DETAILS -->

          <div class="journey-info">

            <div class="station station-departure">
              <strong>
                ${formatTime(train.departureTime)}
              </strong>

              <span>
                ${escapeHtml(train.source)}
              </span>
            </div>

            <div class="journey-line">

              <span class="journey-duration">
                ${escapeHtml(train.duration || "")}
              </span>

              <div class="line">
                ─────────▶
              </div>

            </div>

            <div class="station station-arrival">
              <strong>
                ${formatTime(train.arrivalTime)}
              </strong>

              <span>
                ${escapeHtml(train.destination)}
              </span>
            </div>

          </div>

          <!-- AVAILABLE CLASSES -->

          <div class="classes-section">

            <h4 class="train-classes-heading">
              Available Classes
            </h4>

            <div class="train-class-grid">
              ${classCards}
            </div>

          </div>

          <!-- BOOK BUTTON -->

          <div class="train-card-actions">

            <button
              class="book-btn"
              onclick="bookTrain(${train.id})"
            >
              ✦ Book Now
            </button>

          </div>

        </div>
      `;

      resultsContainer.appendChild(card);
    }

    // Add the interactive filter/sort toolbar after train cards are rendered
    buildTrainFilters(resultsContainer);

    // Scroll to results again after all cards are loaded
    setTimeout(function () {
      scrollToTrainResults();
    }, 150);

  } catch (error) {
    console.error("Error loading trains:", error);

    resultsContainer.innerHTML = `
      <div class="no-results">
        <div class="no-results-icon">⚠️</div>

        <h3>Unable to load trains</h3>

        <p>
          Something went wrong while searching for trains.
        </p>

        <p>
          Please make sure your Spring Boot application is running.
        </p>

        <p>
          API: /api/trains
        </p>
      `;

    scrollToTrainResults();
  }
}


/* =====================================================
   AVAILABLE TRAINS FILTER & SORT TOOLBAR
===================================================== */
function buildTrainFilters(resultsContainer) {
  const cards = Array.from(resultsContainer.querySelectorAll(".train-card"));
  if (!cards.length) return;

  const toolbar = document.createElement("div");
  toolbar.className = "train-filter-toolbar";
  toolbar.innerHTML = `
    <div class="train-filter-left">
      <span class="train-filter-label"><span class="filter-symbol">☷</span> Filter By:</span>
      <label class="train-filter-control"><select id="filterClass" aria-label="Filter by class"><option value="all">All Classes</option></select></label>
      <label class="train-filter-control"><select id="filterDeparture" aria-label="Filter by departure time"><option value="all">Departure Time</option><option value="morning">Morning (5 AM–12 PM)</option><option value="afternoon">Afternoon (12 PM–5 PM)</option><option value="evening">Evening (5 PM–9 PM)</option><option value="night">Night (9 PM–5 AM)</option></select></label>
      <label class="train-filter-control"><select id="filterTrainType" aria-label="Filter by train type"><option value="all">Train Type</option><option value="premium">Premium / Vande Bharat / Shatabdi</option><option value="superfast">Superfast</option><option value="express">Express / Other</option></select></label>
    </div>
    <div class="train-filter-right">
      <span class="train-filter-label">Sort By:</span>
      <label class="train-filter-control train-sort-control"><select id="sortTrains" aria-label="Sort trains"><option value="departure">↕ Earliest Departure</option><option value="duration">Shortest Duration</option><option value="name">Train Name (A–Z)</option></select></label>
    </div>
    <div class="train-filter-count" aria-live="polite"></div>
  `;
  resultsContainer.insertBefore(toolbar, resultsContainer.firstChild);


  const classSelect = toolbar.querySelector("#filterClass");
  const classSet = new Set();
  cards.forEach(card => card.querySelectorAll(".class-code").forEach(node => classSet.add(node.textContent.trim())));
  Array.from(classSet).sort().forEach(code => {
    const option = document.createElement("option"); option.value = code; option.textContent = code;
    classSelect.appendChild(option);
  });

  function parseTime(value) {
    const match = String(value || "").match(/(\d{1,2}):(\d{2})/);
    if (!match) return null;
    return (Number(match[1]) % 24) * 60 + Number(match[2]);
  }
  function trainType(card) {
    const name = (card.dataset.trainName || "").toLowerCase();
    if (/vande bharat|shatabdi|rajdhani|duronto/.test(name)) return "premium";
    if (/superfast|sf|fast/.test(name)) return "superfast";
    return "express";
  }
  function durationMinutes(card) {
    const duration = card.querySelector(".journey-duration")?.textContent || "";
    const h = duration.match(/(\d+)\s*h/i), m = duration.match(/(\d+)\s*m/i);
    return (h ? Number(h[1]) * 60 : 0) + (m ? Number(m[1]) : 0);
  }
  function applyFilters() {
    const selectedClass = classSelect.value;
    const departureRange = toolbar.querySelector("#filterDeparture").value;
    const type = toolbar.querySelector("#filterTrainType").value;
    const sort = toolbar.querySelector("#sortTrains").value;
    const visible = [];
    cards.forEach(card => {
      const classCodes = Array.from(card.querySelectorAll(".class-code")).map(node => node.textContent.trim());
      const time = parseTime(card.dataset.departureTime);
      const matchesClass = selectedClass === "all" || classCodes.includes(selectedClass);
      let matchesTime = true;
      if (departureRange !== "all" && time !== null) {
        if (departureRange === "morning") matchesTime = time >= 300 && time < 720;
        if (departureRange === "afternoon") matchesTime = time >= 720 && time < 1020;
        if (departureRange === "evening") matchesTime = time >= 1020 && time < 1260;
        if (departureRange === "night") matchesTime = time >= 1260 || time < 300;
      }
      const matchesType = type === "all" || trainType(card) === type;
      card.hidden = !(matchesClass && matchesTime && matchesType);
      if (!card.hidden) visible.push(card);
    });
    visible.sort((a,b) => {
      if (sort === "name") return (a.dataset.trainName || "").localeCompare(b.dataset.trainName || "");
      if (sort === "duration") return durationMinutes(a) - durationMinutes(b);
      return (parseTime(a.dataset.departureTime) ?? 0) - (parseTime(b.dataset.departureTime) ?? 0);
    });
    visible.forEach(card => resultsContainer.appendChild(card));
    toolbar.querySelector(".train-filter-count").textContent = `${visible.length} of ${cards.length} trains shown`;
  }
  toolbar.querySelectorAll("select").forEach(select => select.addEventListener("change", applyFilters));
  applyFilters();
}


/* =====================================================
   SAFE HTML TEXT
===================================================== */

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, function (character) {
    return {
      "&": "&amp;",
      "<": "&lt;",
      ">": "&gt;",
      '"': "&quot;",
      "'": "&#39;"
    }[character];
  });
}


/* =====================================================
   FORMAT DATE
===================================================== */

function formatDate(dateString) {
  const date = new Date(dateString + "T00:00:00");

  const day = String(date.getDate()).padStart(2, "0");
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const year = date.getFullYear();

  return `${day}-${month}-${year}`;
}


/* =====================================================
   FORMAT TIME
===================================================== */

function formatTime(timeString) {
  if (!timeString) return "";

  const parts = timeString.split(":");

  return `${parts[0]}:${parts[1]}`;
}


/* =====================================================
   SWAP STATIONS
===================================================== */

function swapStations() {
  const from = document.getElementById("from");
  const to = document.getElementById("to");

  if (!from || !to) return;

  const temp = from.value;

  from.value = to.value;
  to.value = temp;

  // Notify any UI logic listening to station input changes.
  from.dispatchEvent(new Event("input", { bubbles: true }));
  from.dispatchEvent(new Event("change", { bubbles: true }));
  to.dispatchEvent(new Event("input", { bubbles: true }));
  to.dispatchEvent(new Event("change", { bubbles: true }));
}


/* =====================================================
   BOOK TRAIN
===================================================== */

function bookTrain(trainId) {
  fetch("/api/auth/me")
    .then(function (response) {
      if (response.ok) {
        window.location.href =
          "/booking.html?trainId=" + trainId;
      } else {
        const confirmLogin = confirm(
          "🔐 Login Required\n\n" +
          "Please login or register before booking a ticket.\n\n" +
          "Would you like to continue to Login?"
        );

        if (confirmLogin) {
          window.location.href = "/login.html";
        }
      }
    })
    .catch(function (error) {
      console.error("Login check failed:", error);
      alert("Unable to check login status.");
    });
}
