# SAATHI (साथी) — Smart Assistance & Threat Handling Interface
### Comprehensive Personal Safety & Emergency Response Application

SAATHI is a native Android personal safety application built with **Jetpack Compose (Material 3)**, **Kotlin Coroutines/Flow**, **Room Database**, and **Jetpack DataStore**. Designed specifically for women, students, and solo travelers.

---

## Key Features

1. **Hardware Shake-to-SOS Detection**:
   - Accelerometer-powered `ShakeDetector` running with real-time gravity calibration.
   - Vigorously shaking the physical device instantly triggers the SOS sequence even when panicking.
   - Configurable toggle in Settings with debounce cooldown to prevent accidental pocket activation.

2. **Emergency SOS Flow & Automated Dispatch**:
   - Immediate 3-second abort window with distinct haptic vibration feedback.
   - Automatic live GPS coordinate acquisition and reverse geocoding.
   - **Automated SMS Dispatcher (`EmergencyDispatcher`)**: Auto-dispatches emergency SMS with live Google Maps coordinate links to all Safety Circle contacts marked for SOS alerts.
   - One-tap direct dialing to Primary Contact or Emergency Services (112).
   - De-escalation confirmation dialog to prevent accidental cancellation until the user is confirmed safe.

3. **Persistent Foreground Emergency Service**:
   - `EmergencyService` keeps location broadcast active even if the screen turns off or the app is minimized.
   - Persistent lockscreen notification with instant quick actions: **"CALL 112"** and **"I'M SAFE"**.

4. **Emergency Loud Siren & Deterrent**:
   - High-decibel audio alarm / siren directly on the emergency screen using the system alarm audio stream to attract bystanders and deter aggressors.

5. **Trusted Safety Circle & Radar Visualization**:
   - Interactive radial network canvas displaying trusted relations (Parent, Sibling, Friend, Guardian, Campus Security).
   - Relationship-coded color scheme with star designation for the Primary Responder.
   - Granular notification triggers (notify on SOS, notify on missed travel check-in).

6. **Safety Check-In ("Walk With Me")**:
   - Real-time travel journey tracker (e.g., College Library → Hostel Campus 15).
   - Countdown timer with progress bar and 10-minute time extension option.
   - Overdue threshold detection: automatically flags missed check-ins, logs an active incident, and prompts rapid safety resolution or instant SOS escalation.

7. **AI-Assisted Emergency Assistant**:
   - Situational triage engine for Medical Emergency, Harassment/Stalking, Fire, Theft/Robbery, Road Accident, and Lost/Stranded.
   - Quick one-tap distress scenario chips for low-stress input under duress.
   - Categorization, urgency rating (CRITICAL, HIGH, MODERATE), and immediate actionable guidance.
   - Direct pipeline from AI situational analysis into Emergency SOS with context and category preserved.

8. **Nearby Verified Safe Places**:
   - Categorized directory of nearby safety havens: Hospitals, Police Stations, Campus Security Booths, 24/7 Pharmacies, and Women's Helpdesks.
   - Distance indicators (km) with one-tap Google Maps turn-by-turn navigation and direct phone dialing.

9. **Evaluation Demo Mode**:
   - Risk-free testing and demonstration environment for hackathons, evaluators, and drills.
   - Pre-populates simulated Safety Circle contacts and Bhubaneswar campus coordinates.
   - Simulates SMS and alert dispatch without carrier fees or false 112 alarms.
   - Visual safety badges ensuring clear differentiation from live emergencies.
   - Clean single-tap exit that wipes demo data and restores user configuration.

10. **Accessibility & Bilingual Support**:
    - Full English and Hindi (हिन्दी) bilingual support across all primary flows.
    - Reactive Theme switching (System, Light, Dark).
    - High Contrast accessibility mode with high-legibility palettes.
    - Large Text scaling option with proportional typography expansion.
    - 48dp+ minimum accessible touch targets on all critical buttons.

11. **Privacy-by-Design**:
    - Location is captured **only** during active SOS or explicit travel check-in.
    - No background tracking or continuous data harvesting.
    - All contacts and incident logs remain stored strictly on-device in local encrypted Room database.

---

## Unit Test Suite

The project includes unit test suites verifying safety logic:
- `AIIncidentAnalysisTest`: Triage categorization, urgency levels (CRITICAL/HIGH), Hindi prompt diagnosis, and multi-lingual distress inputs.
- `EmergencyEventTest`: Event duration calculations, location coordinate formatting, and model integrity.

To execute tests:
```powershell
$env:JAVA_HOME="C:\Users\alokj\.jdks\jbr-21.0.11"
.\gradlew.bat testDebugUnitTest
```

---

## Build & Run

### Prerequisites
- JDK 21 (located at `C:\Users\alokj\.jdks\jbr-21.0.11` or standard OpenJDK 21)
- Android SDK Platform 34 (Android 14)

### Build Commands
```powershell
# Set JDK 21 environment
$env:JAVA_HOME="C:\Users\alokj\.jdks\jbr-21.0.11"

# Compile Kotlin & Java sources
.\gradlew.bat compileDebugSources

# Assemble Debug APK
.\gradlew.bat assembleDebug
```

### Generated APK Location
```
app/build/outputs/apk/debug/app-debug.apk
```
