// Global variables for CTU time (seconds) and dawn/dusk (seconds since midnight)
let currentCTUSeconds = 0;
let dawnSeconds = null;
let duskSeconds = null;
// Clock mode: 0 = UTC, 1 = Local, 2 = CTU
let currentState = 0; // Will be set by setDisplayState from Android (if available)
let isAnimating = false; // Flag to prevent overlapping animations
const fadeDuration = 500; // Match CSS transition duration for mode switch in milliseconds
const dateToggleDuration = 300; // Match CSS transition duration for date toggle in milliseconds

const utcSection = document.getElementById("utc-section");
const localSection = document.getElementById("local-section");
const ctuSection = document.getElementById("ctu-section");
const sunElement = document.getElementById("sun");
const weekLabelElement = document.getElementById('week-label');
const fullDateElement = document.getElementById('full-date');

const sections = [utcSection, localSection, ctuSection]; // Array of sections for easy access
const dateElements = [weekLabelElement, fullDateElement]; // Array of date elements

// Function to set the initial display state without animation
// Called once on page load, potentially with state provided by Android
function setDisplayState(initialState = 0) {
    currentState = initialState;
    isAnimating = true; // Prevent interaction during initial setup

    // Hide all sections by default
    sections.forEach(section => {
        section.style.display = "none";
        section.style.visibility = "hidden";
        section.style.opacity = "0";
        section.classList.remove('active');
    });

     // Hide all date elements by default
    dateElements.forEach(el => {
        el.style.display = "none";
        el.style.visibility = "hidden";
        el.style.opacity = "0";
        el.classList.remove('active');
    });


    // Set the initially active main section
    const initialSection = sections[currentState];
    initialSection.classList.add('active');
    initialSection.style.display = "block"; // Show immediately
    initialSection.style.visibility = "visible"; // Make visible
    initialSection.style.opacity = "1"; // Set opacity (CSS transition won't run as it's instant)

    // Set the initial active date element (Week Label by default)
    weekLabelElement.classList.add('active');
    weekLabelElement.style.display = "block"; // Show immediately
    weekLabelElement.style.visibility = "visible"; // Make visible
    weekLabelElement.style.opacity = "1"; // Set opacity

    // Handle initial state of sun and solar times
    document.body.classList.remove("show-ctu"); // Ensure hidden
    sunElement.style.display = "none";
    sunElement.style.visibility = "hidden";
    sunElement.style.opacity = "0";

    if (currentState === 2) { // If initial state is CTU
        // Calculate and set the sun's position before making it visible
        updateSunPosition();
        sunElement.style.display = "none"; // Hide sun initially
        sunElement.style.visibility = "hidden"; // Ensure hidden
        sunElement.style.opacity = "0"; // Start with 0 opacity

        setTimeout(() => {
            updateSunPosition(); // Calculate position
            sunElement.style.display = "block"; // Make sun element visible
            sunElement.style.visibility = "visible"; // Ensure visibility
            sunElement.style.opacity = "1"; // Fade in the sun
            document.body.classList.add("show-ctu"); // Add class to trigger solar times visibility
        }, 10); // Small delay to ensure position is calculated
    }

     // Allow interaction after a short delay
    setTimeout(() => {
        isAnimating = false;
    }, fadeDuration); // Wait for potential initial CTU fade-in
}

// Initialize display state (default to 0 if not set by Android)
// setDisplayState(currentState); // Call with initial currentState (which defaults to 0)
// Call setDisplayState() on DOMContentLoaded to ensure elements exist
document.addEventListener('DOMContentLoaded', () => {
    // Check if AndroidBridge exists and has a method to get initial mode
    if (window.AndroidBridge && typeof window.AndroidBridge.getInitialClockMode === 'function') {
        // Assume getInitialClockMode returns the desired initial state (0, 1, or 2)
        // It might be asynchronous, so handle that if necessary.
        // For now, assume it returns synchronously or call setDisplayState from Android callback
         try {
             const initialMode = window.AndroidBridge.getInitialClockMode();
             setDisplayState(initialMode);
         } catch (e) {
             console.error("Error getting initial clock mode from AndroidBridge:", e);
             setDisplayState(0); // Default to UTC if bridge call fails
         }
    } else {
        setDisplayState(0); // Default to UTC if AndroidBridge is not available
    }
});


// --- Event Listeners for Switching Modes (Swipe Only) ---
 let startX = null;
 let startTime = null;
 const swipeDistanceThreshold = 50; // Minimum distance for a swipe (pixels)
 const swipeTimeThreshold = 500; // Maximum time for a gesture to be considered a swipe (milliseconds)


 document.body.addEventListener("touchstart", function (event) {
   // Only track if not animating and it's a single touch
   if (event.touches.length === 1 && !isAnimating) {
     startX = event.touches[0].clientX;
     startTime = new Date().getTime();
   }
 });

 document.body.addEventListener("touchend", function (event) {
   // Only process if a gesture was started (startX is not null), not animating, and it's a single touch end
   if (startX === null || isAnimating || event.changedTouches.length !== 1) return;

   const endX = event.changedTouches[0].clientX;
   const swipeDistance = endX - startX;
   const swipeTime = new Date().getTime() - startTime;

   // Check if the gesture is a swipe based on distance and time
   if (swipeTime < swipeTimeThreshold && Math.abs(swipeDistance) > swipeDistanceThreshold) {
     // It's a swipe!
     if (swipeDistance > 0) {
       switchMode(1); // Swipe right (forward mode)
     } else {
       switchMode(-1); // Swipe left (backward mode)
     }
   }
   // No click/tap handling here for mode switching

   startX = null; // Reset tracking after gesture ends
   startTime = null;
 });

 // Add mouse events for desktop compatibility (simplified for swipe detection)
 document.body.addEventListener("mousedown", function (event) {
     // Only track if not animating
     if (!isAnimating) {
          startX = event.clientX;
          startTime = new Date().getTime();
     }
 });

 document.body.addEventListener("mouseup", function (event) {
     // Only process if a gesture was started (startX is not null) and not animating
     if (startX === null || isAnimating) return;

     const endX = event.clientX;
     const swipeDistance = endX - startX;
     const swipeTime = new Date().getTime() - startTime;

     // Check if the gesture is a swipe based on distance and time (using the same thresholds)
     if (swipeTime < swipeTimeThreshold && Math.abs(swipeDistance) > swipeDistanceThreshold) {
          // It's a swipe!
          if (swipeDistance > 0) {
              switchMode(1); // Swipe right (forward mode)
          } else {
              switchMode(-1); // Swipe left (backward mode)
          }
     }
      // No click handling needed here for mode switching

     startX = null; // Reset tracking after gesture ends
     startTime = null;
 });


  // Function to handle the mode switching animation
  function switchMode(delta) {
    if (isAnimating) {
        return; // Prevent new animation if one is already running
    }
    isAnimating = true;

    const prevState = currentState;
    const nextState = (currentState + delta + 3) % 3;

    const prevSection = sections[prevState];
    const nextSection = sections[nextState];

    // --- Step 1: Start Fade Out of current elements ---
    prevSection.style.opacity = "0"; // Start fade out of previous main section
    prevSection.classList.remove('active'); // Remove active class immediately for state tracking

    // Find the currently active date element and start its fade out
    const activeDateElement = document.querySelector('.week-label.active, .full-date.active');
    if (activeDateElement) {
        activeDateElement.style.opacity = "0"; // Start fade out
        activeDateElement.classList.remove('active'); // Remove active class immediately
    }


    // If switching *from* CTU (prevState === 2)
    if (prevState === 2) {
         // Start fading out sun and solar times
         sunElement.style.opacity = "0"; // Start sun fade out
         document.body.classList.remove("show-ctu"); // CSS handles solar times fade out
    }


    // --- Step 2: Wait for Fade Out to complete, then hide/show elements ---
    setTimeout(() => {
        // After fade out: Hide previous elements using display/visibility
        prevSection.style.display = "none";
        prevSection.style.visibility = "hidden";


         // Hide the date elements that just faded out
        dateElements.forEach(el => {
             el.style.display = "none";
             el.style.visibility = "hidden";
             // Opacity is already 0, keep it there
         });

         // If switching *from* CTU, ensure sun is hidden and not visible during the gap
         if (prevState === 2) {
             sunElement.style.display = "none";
             sunElement.style.visibility = "hidden";
             // Opacity is already 0, keep it there
         }


        // --- Step 3: Set up next elements for Fade In ---
        currentState = nextState; // Update the state after hiding old elements

        // Save mode to Android preferences if bridge is available
        if (window.AndroidBridge && window.AndroidBridge.saveClockMode) {
            window.AndroidBridge.saveClockMode(currentState);
        }

        nextSection.classList.add('active'); // Add active class to the next section
        nextSection.style.display = "block"; // Show next main section
        nextSection.style.visibility = "visible"; // Make it visible
        nextSection.style.opacity = "0"; // Set opacity to 0 before fading in

         // Set the default date element (Week Label) as active and visible for fade in
         weekLabelElement.classList.add('active');
         weekLabelElement.style.display = 'block';
         weekLabelElement.style.visibility = 'visible';
         weekLabelElement.style.opacity = '0'; // Set opacity to 0 before fading in


        // If switching *to* CTU (currentState === 2)
        if (currentState === 2) {
             // Calculate and set the sun's position *before* making it visible and fading in
            updateSunPosition();

            // Ensure sun is displayed and fades in properly
            sunElement.style.display = "block"; // Make sun element visible
            sunElement.style.visibility = "visible"; // Ensure visibility
            sunElement.style.opacity = "0"; // Start with 0 opacity for fade-in effect

            setTimeout(() => {
                updateSunPosition(); // Calculate position before fading in
                sunElement.style.opacity = "1"; // Fade in the sun
                document.body.classList.add("show-ctu"); // Add class to trigger solar times visibility
            }, 10); // Small delay to ensure position is calculated
        }


        // --- Step 4: Start Fade In ---
        // Use a small delay to ensure display/visibility takes effect before opacity transition
        setTimeout(() => {
            nextSection.style.opacity = "1"; // Start fading in next section
            weekLabelElement.style.opacity = '1'; // Start fading in week label

            // If switching *to* CTU, start sun fade in
            if (currentState === 2) {
                 sunElement.style.opacity = "1";
            }

            isAnimating = false; // Animation finished
        }, 10); // Small delay before starting fade in


    }, fadeDuration); // Match the CSS transition duration for fade out
  }


  // Function to update time strings. Assumes time strings in "HH:MM:SS" format for main times.
  function formatTime(timeString, elementId) {
    const parentElement = document.getElementById(elementId);
    if (!parentElement) return; // Defensive check
    const hmElement = parentElement.querySelector(".time-hm");
    const secElement = parentElement.querySelector(".time-sec");
    if (!hmElement || !secElement) return; // Defensive check

    const parts = timeString.split(':');
    if (parts.length < 3) {
        console.error("Invalid time format:", timeString);
        return; // Handle invalid input
    }
    const hoursMinutes = parts[0] + ':' + parts[1]; // HH:MM
    const seconds = parts[2]; // SS

    hmElement.innerText = hoursMinutes;
    secElement.innerText = seconds;
  }

  // Called from Kotlin to update the times.
  // Expects:
  // - utcTime, localTime, ctuTime in "HH:MM:SS" format;
  // - localLabel string (e.g., "Local Time", "Berlin");
  // - dawnTime and duskTime in "HH:mm" format.
  function updateTimes(utcTime, localLabel, localTime, ctuTime, dawnTime, duskTime) {
    // Update the labels and main time values
    const localTimeLabelElement = document.getElementById("local-time-label");
    if (localTimeLabelElement) {
        localTimeLabelElement.innerText = localLabel;
    }

    formatTime(utcTime, "utc-time");
    formatTime(localTime, "local-time");
    formatTime(ctuTime, "ctu-time");

    // Update Dawn and Dusk times (assumes HH:mm format)
    const dawnTimeElement = document.querySelector("#dawn-time .time");
    const duskTimeElement = document.querySelector("#dusk-time .time");
    if (dawnTimeElement) dawnTimeElement.innerText = dawnTime;
    if (duskTimeElement) duskTimeElement.innerText = duskTime;


    // Update global variables for sun position calculation.
    const parts = ctuTime.split(":");
    if (parts.length === 3) {
      const hours = parseInt(parts[0], 10);
      const minutes = parseInt(parts[1], 10);
      const seconds = parseInt(parts[2], 10);
       // Check for NaN before assigning
      if (!isNaN(hours) && !isNaN(minutes) && !isNaN(seconds)) {
         currentCTUSeconds = hours * 3600 + minutes * 60 + seconds;
      } else {
         currentCTUSeconds = 0; // Reset if invalid
         console.warn("Invalid CTU time received:", ctuTime);
      }
    } else {
       currentCTUSeconds = 0; // Reset if invalid format
       console.warn("Invalid CTU time format:", ctuTime);
    }

    const dawnParts = dawnTime.split(":");
    if (dawnParts.length === 2) {
      const dawnHours = parseInt(dawnParts[0], 10);
      const dawnMinutes = parseInt(dawnParts[1], 10);
       // Check for NaN
      if (!isNaN(dawnHours) && !isNaN(dawnMinutes)) {
         dawnSeconds = dawnHours * 3600 + dawnMinutes * 60;
      } else {
         dawnSeconds = null; // Reset if invalid
         console.warn("Invalid dawn time received:", dawnTime);
      }
    } else {
      dawnSeconds = null; // Reset if invalid format
      console.warn("Invalid dawn time format:", dawnTime);
    }

    const duskParts = duskTime.split(":");
    if (duskParts.length === 2) {
       const duskHours = parseInt(duskParts[0], 10);
       const duskMinutes = parseInt(duskParts[1], 10);
       // Check for NaN
       if (!isNaN(duskHours) && !isNaN(duskMinutes)) {
           duskSeconds = duskHours * 3600 + duskMinutes * 60;
       } else {
          duskSeconds = null; // Reset if invalid
          console.warn("Invalid dusk time received:", duskTime);
       }
    } else {
      duskSeconds = null; // Reset if invalid format
      console.warn("Invalid dusk time format:", duskTime);
    }
  }

  // Update the sun's position and color based on CTU time.
  // This function calculates and sets the position/color.
  // Visibility (display, opacity, transition) is managed by switchMode and CSS.
  function updateSunPosition() {
    const sun = document.getElementById("sun");
    if (!sun) return; // Defensive check

     // Only update position if CTU mode is potentially active
    if (currentState !== 2 && !document.body.classList.contains('show-ctu')) {
        // Don't update position if sun is completely hidden
        return;
    }

     // Get screen dimensions and calculate radii
    const centerX = window.innerWidth / 2;
    const centerY = window.innerHeight / 2;
    const screenWidth = window.innerWidth;
    const screenHeight = window.innerHeight;

    const minDimension = Math.min(screenWidth, screenHeight);
    const maxDimension = Math.max(screenWidth, screenHeight);

    // Use a slightly smaller ellipse to keep sun off the absolute edge
    const ellipsePadding = 40; // Pixels from the edge
    const adjustedMinDimension = Math.max(0, minDimension - ellipsePadding);
    const adjustedMaxDimension = Math.max(0, maxDimension - ellipsePadding);


    let rx, ry;
     // Determine ellipse radii based on screen orientation
    if (screenWidth > screenHeight) { // Landscape
        rx = adjustedMaxDimension / 2; // Horizontal radius is larger
        ry = adjustedMinDimension / 2; // Vertical radius is smaller
    } else { // Portrait or Square
        rx = adjustedMinDimension / 2; // Horizontal radius is smaller
        ry = adjustedMaxDimension / 2; // Vertical radius is larger
    }

    // Ensure radii are not negative
    rx = Math.max(0, rx);
    ry = Math.max(0, ry);


    // Use currentCTUSeconds (time from 00:00 CTU)
    const secondsOfDay = (typeof currentCTUSeconds === "number" && !isNaN(currentCTUSeconds))
      ? currentCTUSeconds
      : 0;

    // --- Recalculate Angle based on new requirements ---
    // 00:00 => bottom (angle PI from upward vertical, clockwise)
    // 12:00 => top (angle 0 from upward vertical, clockwise)
    // 06:00 => left (angle 3*PI/2 from upward vertical, clockwise)
    // 18:00 => right (angle PI/2 from upward vertical, clockwise)

    const secondsInDay = 24 * 3600; // 86400 seconds

    // Map secondsOfDay to an angle from 0 to 2*PI, starting at 12:00 (angle 0)
    // 12:00 (43200s) corresponds to angle 0
    // Time in seconds relative to 12:00 PM
    const secondsFrom1200 = (secondsOfDay - 12 * 3600 + secondsInDay) % secondsInDay;

    // Angle calculation: Scale secondsFrom1200 (0 to 86400) to 0 to 2*PI
    const angle = (secondsFrom1200 / secondsInDay) * 2 * Math.PI; // Angle in radians (0 to 2*PI)


    // Position the sun along the ellipse
    // x = centerX + rx * sin(angle)
    // y = centerY - ry * cos(angle) because screen y is downwards and we want y up
    const x = centerX + rx * Math.sin(angle);
    const y = centerY - ry * Math.cos(angle);


    // Set sun position
    // We are already using transform: translate(-50%, -50%) in CSS to center the element itself
    // So we set the top-left corner of the element to the calculated (x, y)
    sun.style.left = x + "px";
    sun.style.top = y + "px";

    // Set sun color according to CTU seconds relative to dawn/dusk.
    let sunColor = "yellow";
    let sunBoxShadow = "0 0 10px 5px rgba(255, 223, 0, 0.7)";

    if (dawnSeconds !== null && duskSeconds !== null && typeof secondsOfDay === "number" && !isNaN(secondsOfDay)) {
      const transitionDurationSec = 600; // 10 minutes transition time (in seconds)

      const timeToDawn = Math.abs(secondsOfDay - dawnSeconds);
      const timeToDusk = Math.abs(secondsOfDay - duskSeconds);

      // Check if within 10 minutes of dawn or dusk
      if (timeToDawn <= transitionDurationSec || timeToDusk <= transitionDurationSec) {
         sunColor = "red"; // Transitioning color
         sunBoxShadow = "0 0 10px 5px rgba(255, 0, 0, 0.7)";
      } else if (secondsOfDay < dawnSeconds || secondsOfDay >= duskSeconds) {
         // Night time (or before dawn/after dusk)
         sunColor = "white"; // Night color
         sunBoxShadow = "0 0 10px 5px rgba(255, 255, 255, 0.7)";
      } else { // Daytime
         sunColor = "yellow"; // Day color
         sunBoxShadow = "0 0 10px 5px rgba(255, 223, 0, 0.7)";
      }
    }

    sun.style.background = sunColor;
    sun.style.boxShadow = sunBoxShadow;
  }

  // Function to update the week label and full date dynamically
function updateWeekLabel() {
    const now = new Date();
    // Use toLocaleDateString to get the day name and week number based on local settings
    // Note: ISO week calculation might differ based on locale. getISOWeek function is more consistent.
    const weekLabel = now.toLocaleDateString(undefined, { weekday: 'short' }) + ' ' + getISOWeek(now);
    const fullDate = now.toISOString().split('T')[0];

    if(weekLabelElement) weekLabelElement.textContent = weekLabel;
    if(fullDateElement) fullDateElement.textContent = fullDate;
}

// Function to calculate ISO week number (provided logic)
function getISOWeek(date) {
    const target = new Date(date.valueOf());
    const dayNumber = (date.getUTCDay() + 6) % 7;
    target.setUTCDate(target.getUTCDate() - dayNumber + 3);
    const firstThursday = target.valueOf();
    target.setUTCMonth(0, 1);
    if (target.getUTCDay() !== 4) {
        target.setUTCMonth(0, 1 + ((4 - target.getUTCDay()) + 7) % 7);
    }
    return 1 + Math.ceil((firstThursday - target) / 604800000);
}

// Add event listener for toggling week label and full date visibility (internal toggle)
weekLabelElement.addEventListener('click', () => {
    // Only toggle if not animating and week label is currently active
    if (isAnimating || !weekLabelElement.classList.contains('active')) {
        return;
    }

    // Start fade out of the week label
    weekLabelElement.style.opacity = "0";
    weekLabelElement.classList.remove('active'); // Remove active class immediately

    // Wait for fade out to complete
    setTimeout(() => {
        // Hide the week label after it has faded out
        weekLabelElement.style.display = 'none';
        weekLabelElement.style.visibility = 'hidden';

        // Set up the full date element for fade in
        fullDateElement.classList.add('active'); // Set active class
        fullDateElement.style.display = 'block'; // Make block
        fullDateElement.style.visibility = 'visible'; // Make visible
        fullDateElement.style.opacity = '0'; // Set opacity to 0 before fading in


        // Start fade in of the full date
        // Use a small delay to ensure display/visibility takes effect before opacity transition
        setTimeout(() => {
             fullDateElement.style.opacity = '1';
        }, 10); // Small delay

    }, dateToggleDuration); // Match CSS transition duration for date elements
});

fullDateElement.addEventListener('click', () => {
     // Only toggle if not animating and full date is currently active
     if (isAnimating || !fullDateElement.classList.contains('active')) {
         return;
     }

    // Start fade out of the full date
     fullDateElement.style.opacity = "0";
     fullDateElement.classList.remove('active'); // Remove active class immediately

     // Wait for fade out to complete
     setTimeout(() => {
          // Hide the full date after it has faded out
          fullDateElement.style.display = 'none';
          fullDateElement.style.visibility = 'hidden';

          // Set up the week label element for fade in (default state)
          weekLabelElement.classList.add('active'); // Set active class
          weekLabelElement.style.display = 'block'; // Make block
          weekLabelElement.style.visibility = 'visible'; // Make visible
          weekLabelElement.style.opacity = '0'; // Set opacity to 0 before fading in

          // Start fade in of the week label
          // Use a small delay to ensure display/visibility takes effect before opacity transition
          setTimeout(() => {
               weekLabelElement.style.opacity = '1';
          }, 10); // Small delay

     }, dateToggleDuration); // Match CSS transition duration for date elements
});


// Remove any background or border styling when toggling the date elements
    weekLabelElement.style.background = "none";
    weekLabelElement.style.border = "none";
    fullDateElement.style.background = "none";
    fullDateElement.style.border = "none";


// Update sun position periodically (every second)
  setInterval(updateSunPosition, 1000);
  // Update position immediately on load (called within setDisplayState)
  // updateSunPosition();
  // Update position on window resize
  window.addEventListener("resize", updateSunPosition);
  // Initial update for week label and date (called within setDisplayState)
  // updateWeekLabel();
  // Update week label and date every second
  setInterval(updateWeekLabel, 1000);