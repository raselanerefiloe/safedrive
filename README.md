# SafeDrive

## Overview

SafeDrive is an Android application developed to enhance road safety by providing real-time solutions for drivers. The app aims to curb accidents by offering alternative routes when traffic is detected. It utilizes a pre-trained SSD MobileNet V1 model for real-time object detection, enabling drivers to be aware of their surroundings. Additionally, SafeDrive allows users to send reports regarding road conditions, which are then dispersed to other drivers through push notifications.

## Features

- **Traffic Monitoring**: The app detects traffic conditions and provides alternative routes to help drivers avoid congestion.
- **Real-Time Object Detection**: Utilizing the SSD MobileNet V1 TensorFlow Lite model, the app identifies objects on the road to enhance driver awareness.
- **User Reporting**: Drivers can report various road conditions, including accidents, construction zones, and traffic jams.
- **Push Notifications**: Alerts are sent to users based on reports from other drivers to keep everyone informed about current road conditions.
- **Backend Integration**: The app leverages Appwrite as the backend service for data management and user authentication.

## Technologies Used

- **Programming Language**: Kotlin
- **Layout**: XML
- **Machine Learning Model**: SSD MobileNet V1 (TensorFlow Lite)
- **Backend**: Appwrite
- **Android SDK**: Android Studio

## Getting Started

### Prerequisites

- Android Studio installed on your machine.
- Kotlin support enabled in your Android Studio.
- An Appwrite account and setup (for backend services).

### Cloning the Repository

To clone this repository, run:

```bash
git clone https://github.com/raselanerefiloe/safedrive.git
