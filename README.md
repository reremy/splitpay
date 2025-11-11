# SplitPay

**A modern Android app for tracking shared expenses and settling up with friends and groups.**

SplitPay simplifies the hassle of splitting bills, tracking debts, and managing group expenses. Whether you're sharing rent with roommates, splitting vacation costs, or tracking expenses with friends, SplitPay makes it easy to keep everyone on the same page.

---

## Features

### Core Functionality
- **Expense Tracking**: Add expenses with multiple split types (equal, exact amounts, percentages, shares)
- **Group Management**: Create groups for recurring expense sharing (trips, roommates, events)
- **Friend-to-Friend**: Track expenses directly with individual friends without groups
- **Settle Up**: Record payments and automatically calculate who owes whom
- **Activity Feed**: Real-time feed of all expense and payment activities
- **Multiple Payers**: Support for expenses paid by multiple people

### Additional Features
- **Expense Categories**: Organize expenses by type (food, groceries, utilities, etc.)
- **Expense Images**: Attach photos of receipts or bills
- **Charts & Analytics**: Visualize spending patterns and balances
- **Payment Reminders**: Set reminders for outstanding debts
- **Export**: Export expense data for record-keeping
- **Profile Pictures & QR Codes**: Easy friend discovery via QR code scanning
- **Block Users**: Privacy control to block unwanted users

---

## Tech Stack

### Languages & Frameworks
- **Kotlin**: 100% Kotlin codebase
- **Jetpack Compose**: Modern declarative UI framework
- **Material Design 3**: Latest Material Design components

### Architecture
- **MVVM Pattern**: Clean separation of concerns
- **Repository Pattern**: Abstracted data layer
- **Use Cases**: Business logic encapsulation
- **Kotlin Coroutines**: Asynchronous programming

### Backend & Services
- **Firebase Authentication**: Secure user authentication
- **Cloud Firestore**: Real-time NoSQL database
- **Firebase Storage**: Image and file storage
- **Firebase Analytics**: Usage tracking and insights

### Key Dependencies
- **Navigation Compose**: Type-safe navigation
- **Coil**: Efficient image loading
- **DataStore**: Modern preference storage
- **Android Image Cropper**: Profile picture cropping

---

## Project Structure

```
app/src/main/java/com/example/splitpay/
├── data/
│   ├── model/              # Data models (User, Expense, Group, Activity)
│   └── repository/         # Data layer repositories
├── domain/
│   └── usecase/            # Business logic use cases
├── ui/
│   ├── login/              # Authentication screens
│   ├── signup/
│   ├── welcome/
│   ├── home/               # Main navigation hub
│   ├── friends/            # Friend management
│   ├── friendsDetail/      # Friend detail and balances
│   ├── groups/             # Group management
│   ├── expense/            # Add/edit expenses
│   ├── settleUp/           # Payment recording
│   ├── activity/           # Activity feed
│   ├── profile/            # User profile
│   ├── theme/              # App theming
│   └── common/             # Reusable UI components
├── navigation/             # Navigation configuration
└── logger/                 # Custom logging utilities
```

---

## Quick Start

### Prerequisites
- Android Studio Ladybug or later
- JDK 11 or higher
- Android SDK (API 24+)
- Firebase account

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd splitpay
   ```

2. **Set up Firebase**
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Enable Authentication (Email/Password)
   - Create a Firestore database
   - Enable Firebase Storage
   - Download `google-services.json` and place it in `app/`

3. **Open in Android Studio**
   ```bash
   # Open the project in Android Studio
   # Let Gradle sync dependencies
   ```

4. **Run the app**
   - Connect an Android device or start an emulator
   - Click "Run" or press Shift+F10

For detailed setup instructions, see [docs/SETUP.md](docs/SETUP.md)

---

## Documentation

Comprehensive documentation is available in the `/docs` folder:

- **[Architecture](docs/ARCHITECTURE.md)**: Detailed architecture overview and design patterns
- **[Firestore Schema](docs/FIRESTORE_SCHEMA.md)**: Database structure and relationships
- **[Setup Guide](docs/SETUP.md)**: Step-by-step development environment setup
- **[Features](docs/FEATURES.md)**: Complete feature documentation with user flows
- **[Firebase Services](docs/FIREBASE_SERVICES.md)**: Firebase integration details
- **[UI Components](docs/UI_COMPONENTS.md)**: Reusable component library
- **[Data Models](docs/DATA_MODELS.md)**: Core data structures explained
- **[Testing](docs/TESTING.md)**: Testing strategy and guidelines
- **[AI Context](docs/AI_CONTEXT.md)**: Quick reference for AI assistants

---

## Build Information

- **Application ID**: `com.example.splitpay`
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Compile SDK**: 36
- **Version**: 1.0 (versionCode 1)

---

## Contributing

Contributions are welcome! Please read our [Contributing Guidelines](CONTRIBUTING.md) before submitting pull requests.

### Development Workflow
1. Create a feature branch from `main`
2. Make your changes following our code style
3. Write/update tests as needed
4. Submit a pull request

---

## License

This project is licensed under the MIT License - see the LICENSE file for details.

---

## Contact & Support

For questions, issues, or feature requests:
- Open an issue on GitHub
- Contact: [Your contact information]

---

## Acknowledgments

Built with:
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Firebase](https://firebase.google.com/)
- [Material Design 3](https://m3.material.io/)
- [Coil](https://coil-kt.github.io/coil/)

---

**Made with ❤️ for easier expense splitting**
