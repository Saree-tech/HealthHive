# HealthHive 🌿

![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg)
![Platform](https://img.shields.io/badge/Platform-Android-green.svg)
![Firebase](https://img.shields.io/badge/Backend-Firebase-orange.svg)

**HealthHive** is a comprehensive, AI-powered health monitoring Android application designed to centralize personal vitals, medical history, and intelligent health insights. Built using modern Android development practices, it provides secure real-time synchronization and personalized health guidance.

---

## ✨ Key Features

* **👤 Smart Profile Management**
  Real-time synchronization of user information, medical history, and allergies across devices.

* **📊 Vitals Tracking**
  Monitor heart rate, blood pressure, steps, and sleep with interactive charts and a dynamic health score.

* **🤖 Lumi AI**
  An AI-powered health assistant and symptom checker that provides personalized recommendations.

* **📰 Global Health Insights**
  Live health news and medical articles powered by a curated News API.

* **📅 Health Calendar**
  Track daily progress, moods, medication reminders, and medical appointments.

* **🔒 Secure Authentication**
  Secure login and data protection using Firebase Authentication and App Check.

---

## 🛠 Tech Stack

* **Language:** Kotlin
* **UI:** Jetpack Compose (Declarative UI)
* **Architecture:** MVVM (Model–View–ViewModel)
* **State Management:** StateFlow / Flow
* **Asynchronous Tasks:** Kotlin Coroutines
* **Networking:** Retrofit & OkHttp
* **Backend & Services:**

  * Firebase Firestore (NoSQL Database)
  * Firebase Authentication
  * Firebase Storage
* **Image Loading:** Coil
* **Local Storage:** DataStore / SharedPreferences

---

## 🚀 Getting Started

### Prerequisites

* Android Studio Iguana or newer
* Android Emulator or Physical Device
* Firebase Project (google-services.json required)

---

## 📥 Installation

### 1️⃣ Clone the Repository

Clone the project using Git:

git clone [https://github.com/your-username/HealthHive.git](https://github.com/your-username/HealthHive.git)

---

### 2️⃣ Firebase Setup

1. Create a project in the Firebase Console.
2. Add an Android app with the package name:
   com.example.healthhive
3. Download the `google-services.json` file.
4. Place it inside the `app/` directory.
5. Enable the following Firebase services:
* Email/Password Authentication
* Cloud Firestore
* Firebase Storage

---

### 3️⃣ Configure News API

1. Obtain an API key from NewsAPI.org.
2. Add the API key to the `local.properties` file:
   NEWS_API_KEY=your_actual_api_key_here

---

### 4️⃣ Build & Run

1. Sync the project with Gradle files.

2. Run the app on an emulator or physical device.

---

## 🏗 Project Structure

com.example.healthhive

├── data/      # Repositories, API services, Firebase logic

│   └── model/   # Data models (User, Vitals, HealthArticle)

├── ui/       # Jetpack Compose screens & UI components

│   ├── screens   # Home, Profile, Auth, AI Assistant, etc.

│   └── theme    # Colors, Typography, Shapes

└── viewmodel   # ViewModels and UI state management

---

## 🤝 Contributing

Contributions are welcome and appreciated ❤️

1. Fork the repository
2. Create your feature branch
   git checkout -b feature/AmazingFeature
3. Commit your changes
   git commit -m "Add AmazingFeature"
4. Push to your branch
   git push origin feature/AmazingFeature
5. Open a Pull Request

---

## 📄 License

This project is licensed under the **MIT License**.
See the LICENSE file for more details.

---

## 📩 Contact

**Your Name**
LinkedIn: [www.linkedin.com/in/sareen-fatima-960a96325](www.linkedin.com/in/sareen-fatima-960a96325)


**Project Repository:**
[https://github.com/Saree-tech/HealthHive](https://github.com/Saree-tech/HealthHive)


