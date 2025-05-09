// Global variables for CTU time (seconds) and dawn/dusk (seconds since midnight)
  let currentCTUSeconds = 0;
  let dawnSeconds = null;
  let duskSeconds = null;
  // Clock mode: 0 = UTC, 1 = Local, 2 = CTU
let currentState = 0; // Will be set by setDisplayState from Android
  let isAnimating = false;
  const fadeDuration = 500; // Match CSS transition duration in milliseconds

  const utcSection = document.getElementById("utc-section");
  const localSection = document.getElementById("local-section");
  const ctuSection = document.getElementById("ctu-section");
  const sunElement = document.getElementById("sun");

  const sections = [utcSection, localSection, ctuSection]; // Array of sections for easy access

  // Function to set the display state without animation (for initial load)
  function setDisplayState(state) {
    currentState = state;
    sections.forEach((section, index) => {
        section.style.display = (index === state) ? "block" : "none";
        section.style.opacity = (index === state) ? "1" : "0";
    });

    // Set initial display/opacity for sun and solar times
    document.body.classList.remove("show-ctu");
    sunElement.style.display = "none";
    sunElement.style.opacity = "0";

    if (state === 2) { // If initial state is CTU
        // Calculate and set initial sun position before making it visible
        updateSunPosition();

        document.body.classList.add("show-ctu"); // Show solar times
        sunElement.style.display = "block"; // Show sun block
        sunElement.style.opacity = "1"; // Show sun opacity
    }
  }

  // Initialize to the default mode (UTC)
  setDisplayState(currentState);

  // --- Event Listeners for Switching Modes (Swipe Only) ---
   let startX = null;
   let startTime = null;
   const clickThreshold = 50; // Minimum distance for a swipe to be registered (in pixels)
   const swipeTimeThreshold = 500; // Maximum time for a gesture to be considered a swipe (in milliseconds)


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
     if (swipeTime < swipeTimeThreshold && Math.abs(swipeDistance) > clickThreshold) {
       // It's a swipe!
       if (swipeDistance > 0) {
         switchMode(1); // Swipe right (forward mode)
       } else {
         switchMode(-1); // Swipe left (backward mode)
       }
     }
     // No click/tap handling here

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
       if (swipeTime < swipeTimeThreshold && Math.abs(swipeDistance) > clickThreshold) {
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
        return;
    }
    isAnimating = true;

    const prevState = currentState;
    currentState = (currentState + delta + 3) % 3;
    // Save mode to Android preferences if bridge is available
    if (window.AndroidBridge && window.AndroidBridge.saveClockMode) {
        window.AndroidBridge.saveClockMode(currentState);
    }
    const prevSection = sections[prevState];
    const nextSection = sections[currentState];

    // --- Start Fade Out ---
    prevSection.style.opacity = "0";
    if (prevState === 2) {
        sunElement.style.opacity = "0";
        document.body.classList.remove("show-ctu");
    }


    // Wait for fade out to complete
    setTimeout(() => {
        prevSection.style.display = "none"; // Hide previous section

        // --- Start Fade In ---
        nextSection.style.display = "block"; // Show next section
        nextSection.style.opacity = "0"; // Set opacity to 0 before fading in

        // If switching *to* CTU (currentState === 2)
        if (currentState === 2) {
             // Calculate and set the sun's position *before* making it visible and fading in
            updateSunPosition();

            sunElement.style.display = "block"; // Make sun element block
            // Use a small delay before setting opacity to 1
            setTimeout(() => {
                 sunElement.style.opacity = "1"; // Start fading in sun
                 document.body.classList.add("show-ctu"); // Start fading in solar times
            }, 10); // Small delay

        } else {
             // If switching *from* CTU, ensure sun is hidden after fading out
             sunElement.style.display = "none";
             sunElement.style.opacity = "0";
        }

        // Trigger fade in for next section
        setTimeout(() => {
            nextSection.style.opacity = "1"; // Start fading in next section
            isAnimating = false; // Animation finished
        }, 10);


    }, fadeDuration); // Match the CSS transition duration for fade out
  }


  // Function to update time strings. Assumes time strings in "HH:MM:SS" format for main times.
  function formatTime(timeString, elementId) {
    const parentElement = document.getElementById(elementId);
    const hmElement = parentElement.querySelector(".time-hm");
    const secElement = parentElement.querySelector(".time-sec");

    const parts = timeString.split(':');
    const hoursMinutes = parts[0] + ':' + parts[1]; // HH:MM
    const seconds = parts[2]; // SS

    hmElement.innerText = hoursMinutes;
    secElement.innerText = seconds;
  }

  // Called from Kotlin to update the times.
  // Expects:
  // - utcTime, localTime, ctuTime in "HH:MM:SS" format;
  // - dawnTime and duskTime in "HH:mm" format.
  function updateTimes(utcTime, localLabel, localTime, ctuTime, dawnTime, duskTime) {
    // Update the labels and main time values
    document.getElementById("local-time-label").innerText = localLabel;
    formatTime(utcTime, "utc-time");
    formatTime(localTime, "local-time");
    formatTime(ctuTime, "ctu-time");

    // Update Dawn and Dusk times (assumes HH:mm format)
    document.querySelector("#dawn-time .time").innerText = dawnTime;
    document.querySelector("#dusk-time .time").innerText = duskTime;

    // Update global variables for sun position calculation.
    const parts = ctuTime.split(":");
    if (parts.length === 3) {
      const hours = parseInt(parts[0], 10);
      const minutes = parseInt(parts[1], 10);
      const seconds = parseInt(parts[2], 10);
      currentCTUSeconds = hours * 3600 + minutes * 60 + seconds;
    } else {
       currentCTUSeconds = 0;
    }

    const dawnParts = dawnTime.split(":");
    if (dawnParts.length === 2) {
      dawnSeconds = parseInt(dawnParts[0], 10) * 3600 + parseInt(dawnParts[1], 10) * 60;
    } else {
      dawnSeconds = null;
    }

    const duskParts = duskTime.split(":");
    if (duskParts.length === 2) {
      duskSeconds = parseInt(duskParts[0], 10) * 3600 + parseInt(duskParts[1], 10) * 60;
    } else {
      duskSeconds = null;
    }
  }

  // Update the sun's position and color based on CTU time.
  // This function calculates and sets the position/color.
  // Visibility (display, opacity, transition) is managed by switchMode.
  function updateSunPosition() {
    const sun = document.getElementById("sun");

     // Get screen dimensions and calculate radii
    const centerX = window.innerWidth / 2;
    const centerY = window.innerHeight / 2;
    const screenWidth = window.innerWidth;
    const screenHeight = window.innerHeight;

    const minDimension = Math.min(screenWidth, screenHeight);
    const maxDimension = Math.max(screenWidth, screenHeight);

    const smallRadius = minDimension / 2; // 1/2 of min
    const bigRadius = maxDimension / 2; // 1/2 of max

    let rx, ry;
    if (screenWidth > screenHeight) { // Landscape
        rx = bigRadius; // Horizontal radius is big
        ry = smallRadius; // Vertical radius is small
    } else { // Portrait
        rx = smallRadius; // Horizontal radius is small
        ry = bigRadius; // Vertical radius is big
    }

    // Use currentCTUSeconds (time from 00:00 CTU)
    const secondsOfDay = (typeof currentCTUSeconds === "number" && !isNaN(currentCTUSeconds))
      ? currentCTUSeconds
      : 0;

    // --- Recalculate Angle based on new requirements ---
    // 00:00 => bottom (angle 180 deg from upward vertical, clockwise)
    // 12:00 => top (angle 0 deg from upward vertical, clockwise)
    // 06:00 => left (angle 270 deg from upward vertical, clockwise)
    // 18:00 => right (angle 90 deg from upward vertical, clockwise)

    const secondsInDay = 24 * 3600; // 86400 seconds

    // Angle (clockwise from upward vertical) mapping:
    // 12:00 (43200s) -> 0
    // 18:00 (64800s) -> PI/2
    // 00:00 (0s)     -> PI
    // 06:00 (21600s) -> 3*PI/2
    // 12:00 (43200s) -> 2*PI (or 0)

    // Time in seconds from 12:00 PM (wrapping around midnight)
    const secondsFrom1200 = (secondsOfDay - 12 * 3600 + secondsInDay) % secondsInDay;

    // Angle (clockwise from upward vertical) based on time from 12:00 PM
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

    if (dawnSeconds !== null && duskSeconds !== null && typeof secondsOfDay === "number") {
      const transitionDuration = 600;

      if (Math.abs(secondsOfDay - dawnSeconds) <= transitionDuration || Math.abs(secondsOfDay - duskSeconds) <= transitionDuration) {
         sunColor = "red";
         sunBoxShadow = "0 0 10px 5px rgba(255, 0, 0, 0.7)";
      } else if (secondsOfDay < dawnSeconds || secondsOfDay >= duskSeconds) {
         sunColor = "white";
         sunBoxShadow = "0 0 10px 5px rgba(255, 255, 255, 0.7)";
      } else { // Daytime
         sunColor = "yellow";
         sunBoxShadow = "0 0 10px 5px rgba(255, 223, 0, 0.7)";
      }
    }

    sun.style.background = sunColor;
    sun.style.boxShadow = sunBoxShadow;
  }

  // Update sun position periodically (every second)
  setInterval(updateSunPosition, 1000);
  // Update position immediately on load
  updateSunPosition();
  // Update position on window resize
  window.addEventListener("resize", updateSunPosition);