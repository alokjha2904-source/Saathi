package com.saathi.app.util

object SaathiStrings {
    fun isHindi(lang: String): Boolean {
        return lang.equals("hi", ignoreCase = true) || 
               lang.equals("hindi", ignoreCase = true) || 
               lang.contains("हिन्दी")
    }

    // Common
    fun appName(isHi: Boolean) = if (isHi) "साथी (SAATHI)" else "SAATHI"
    fun appTagline(isHi: Boolean) = if (isHi) "स्मार्ट सहायता और आपातकालीन सुरक्षा" else "Smart Assistance & Threat Handling"
    fun cancel(isHi: Boolean) = if (isHi) "रद्द करें" else "Cancel"
    fun save(isHi: Boolean) = if (isHi) "सहेजें" else "Save"
    fun delete(isHi: Boolean) = if (isHi) "हटाएं" else "Delete"
    fun back(isHi: Boolean) = if (isHi) "पीछे जाएं" else "Back"
    fun settings(isHi: Boolean) = if (isHi) "सेटिंग्स" else "Settings"

    // Home Screen
    fun youAreSafe(isHi: Boolean) = if (isHi) "आप सुरक्षित हैं।" else "You are safe."
    fun emergencyActive(isHi: Boolean) = if (isHi) "आपातकालीन स्थिति सक्रिय!" else "Emergency Active"
    fun safetyNetworkReady(isHi: Boolean) = if (isHi) "सुरक्षा नेटवर्क सक्रिय और तैयार" else "Safety Network Active & Ready"
    fun holdForSos(isHi: Boolean) = if (isHi) "SOS के लिए दबाए रखें" else "HOLD FOR SOS"
    fun inCircle(isHi: Boolean, count: Int) = if (isHi) "सर्कल में $count संपर्क" else "$count in Circle"
    fun online(isHi: Boolean) = if (isHi) "ऑनलाइन" else "Online"
    fun offline(isHi: Boolean) = if (isHi) "ऑफ़लाइन" else "Offline"

    // Quick Actions
    fun safetyCheckInTitle(isHi: Boolean) = if (isHi) "सुरक्षा चेक-इन" else "Safety Check-In"
    fun safetyCheckInSub(isHi: Boolean) = if (isHi) "मेरे साथ चलें" else "Walk With Me"
    fun safetyCircleTitle(isHi: Boolean) = if (isHi) "सुरक्षा चक्र" else "Safety Circle"
    fun safetyCircleSub(isHi: Boolean) = if (isHi) "विश्वसनीय संपर्क" else "Trusted Contacts"
    fun safePlacesTitle(isHi: Boolean) = if (isHi) "सुरक्षित स्थान" else "Safe Places"
    fun safePlacesSub(isHi: Boolean) = if (isHi) "निकटवर्ती सहायता" else "Nearby Help"
    fun incidentHistoryTitle(isHi: Boolean) = if (isHi) "घटना इतिहास" else "Incident History"
    fun incidentHistorySub(isHi: Boolean) = if (isHi) "पुराने रिकॉर्ड" else "Past Logs"
    fun aiAssistant(isHi: Boolean) = if (isHi) "AI सहायक" else "AI Assistant"
    fun demoMode(isHi: Boolean) = if (isHi) "डेमो मोड" else "Demo Mode"

    // SOS & Emergency
    fun activatingSos(isHi: Boolean) = if (isHi) "आपातकालीन सेवा सक्रिय हो रही है" else "EMERGENCY ACTIVATING"
    fun sosActive(isHi: Boolean) = if (isHi) "🚨 SOS सक्रिय" else "🚨 SOS ACTIVE"
    fun endSos(isHi: Boolean) = if (isHi) "SOS समाप्त करें" else "END SOS"
    fun callEmergency(isHi: Boolean) = if (isHi) "प्राथमिक संपर्क / 112 पर कॉल करें" else "CALL PRIMARY CONTACT / 112"
    fun shareAlert(isHi: Boolean) = if (isHi) "स्थान / अलर्ट साझा करें" else "SHARE LOCATION / ALERT"
    fun nearbySafePlaces(isHi: Boolean) = if (isHi) "निकटवर्ती सुरक्षित स्थान" else "NEARBY SAFE PLACES"

    // Check-in
    fun arrivedSafely(isHi: Boolean) = if (isHi) "मैं सुरक्षित पहुँच गया/गई हूँ" else "I'VE ARRIVED SAFELY"
    fun checkInMissed(isHi: Boolean) = if (isHi) "चेक-इन का समय समाप्त" else "CHECK-IN MISSED"
    fun areYouSafe(isHi: Boolean) = if (isHi) "क्या आप सुरक्षित हैं?" else "Are you safe?"
    fun yesSafe(isHi: Boolean) = if (isHi) "हाँ, मैं सुरक्षित हूँ" else "YES, I'M SAFE"
    fun triggerSos(isHi: Boolean) = if (isHi) "सहायता चाहिए (SOS भेजें)" else "NEED HELP (TRIGGER SOS)"

    // Accessibility
    fun accessibility(isHi: Boolean) = if (isHi) "अभिगम्यता (एक्सेसिबिलिटी)" else "Accessibility"
    fun largeText(isHi: Boolean) = if (isHi) "बड़ा टेक्स्ट" else "Large Text"
    fun highContrast(isHi: Boolean) = if (isHi) "उच्च कंट्रास्ट" else "High Contrast"
    fun reducedMotion(isHi: Boolean) = if (isHi) "कम एनिमेशन" else "Reduced Motion"
    fun language(isHi: Boolean) = if (isHi) "भाषा (Language)" else "Language / भाषा"
}
