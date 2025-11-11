# Changelog

All notable changes to SplitPay will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Planned Features
- Recurring expenses
- Multi-currency support
- Receipt OCR (automatic amount extraction)
- Web app version
- Push notifications
- Budget limits and alerts
- Expense templates
- Integration with payment apps (Venmo, PayPal)

---

## [1.0.0] - 2024-11-11

### Added - Initial Release

#### Core Features
- **User Authentication**
  - Email/password sign up and login via Firebase Auth
  - User profile creation with profile pictures
  - QR code generation for easy friend discovery
  - Session management

- **Friend Management**
  - Add friends by username search
  - Add friends by QR code scanning
  - View friends list with current balances
  - Friend detail page with expense history
  - Remove friends
  - Block/unblock users

- **Group Management**
  - Create expense-sharing groups
  - Add/remove group members
  - Group detail page with member balances
  - Edit group information (name, icon, photo)
  - Archive groups
  - View non-group (friend-to-friend) expenses

- **Expense Management**
  - Add expenses with multiple split types:
    - Equal split
    - Exact amounts
    - Percentage-based
    - Share-based
  - Support for multiple payers
  - Expense categories (food, transport, utilities, etc.)
  - Attach receipt images
  - Add expense memos
  - Edit and delete expenses
  - Expense detail view

- **Settlement & Payments**
  - Record payments to settle balances
  - Full and partial settlement options
  - Payment history tracking
  - Automatic balance calculations
  - Payment detail view

- **Activity Feed**
  - Real-time activity feed of all expenses and payments
  - Activity types:
    - Expense added/updated/deleted
    - Payment made/updated/deleted
    - Group created/deleted
    - Member added/removed
  - Personalized financial impact display
  - Deep linking to related entities

- **Charts & Analytics**
  - Spending over time charts
  - Category breakdown visualization
  - Balance history tracking
  - Group spending analysis

- **Additional Features**
  - Export expense data (CSV)
  - Payment reminders
  - Profile picture upload and cropping
  - Material Design 3 theming
  - Dark mode support (system-based)
  - Offline support (Firestore persistence)

#### Technical Implementation
- **Architecture**: MVVM with Repository pattern
- **UI Framework**: Jetpack Compose
- **Backend**: Firebase Suite (Auth, Firestore, Storage, Analytics)
- **Language**: 100% Kotlin
- **Navigation**: Navigation Compose
- **Image Loading**: Coil
- **Async**: Kotlin Coroutines + Flow
- **Build System**: Gradle with Kotlin DSL

#### Documentation
- Comprehensive README
- Architecture documentation
- Firestore schema reference
- Development setup guide
- Feature documentation
- Firebase services guide
- Data models reference
- UI components guide
- Testing guidelines
- Contributing guide
- AI context for assistants

---

## Version History

### [1.0.0] - 2024-11-11
- Initial release with core expense-splitting functionality

---

## Future Versions

### [1.1.0] - Planned
**Focus**: Enhanced User Experience

**Planned Features**:
- Push notifications for new expenses
- Improved search and filtering
- Expense sorting options
- Bulk expense operations
- Enhanced error handling
- Performance optimizations

### [1.2.0] - Planned
**Focus**: Advanced Features

**Planned Features**:
- Recurring expenses
- Expense templates
- Multi-currency support
- Currency conversion
- Budget limits and tracking
- Spending insights

### [2.0.0] - Planned
**Focus**: Platform Expansion

**Planned Features**:
- Web application
- iOS app
- Receipt OCR
- Payment app integrations
- Social features
- Gamification elements

---

## Release Process

### Version Numbering

**Format**: MAJOR.MINOR.PATCH

- **MAJOR**: Incompatible API changes or major feature releases
- **MINOR**: New features, backward-compatible
- **PATCH**: Bug fixes, backward-compatible

### Release Checklist

Before each release:
- [ ] All tests passing
- [ ] Documentation updated
- [ ] CHANGELOG.md updated
- [ ] Version bumped in `build.gradle.kts`
- [ ] Release notes prepared
- [ ] APK signed and tested
- [ ] Firebase configuration verified
- [ ] Security rules reviewed

---

## Change Categories

### Added
New features or functionality

### Changed
Changes to existing functionality

### Deprecated
Features that will be removed in future versions

### Removed
Removed features or functionality

### Fixed
Bug fixes

### Security
Security patches or improvements

---

## Detailed Change Log Example

### [1.0.1] - TBD

#### Fixed
- Fix crash when adding expense with no participants
- Resolve image upload issue on Android 14+
- Fix balance calculation for multiple payer scenarios

#### Changed
- Improved expense date picker UX
- Updated error messages for clarity
- Optimized Firestore queries for better performance

#### Security
- Updated Firebase SDK to latest version
- Enhanced input validation

---

## Breaking Changes

No breaking changes in current version.

Future breaking changes will be clearly documented here with migration guides.

---

## Migration Guides

### Migrating to 2.0.0 (Future)

When 2.0.0 is released, migration instructions will be provided here.

---

## Contributors

Thank you to all contributors who have helped build SplitPay!

- [List of contributors will be maintained here]

---

## Feedback & Bug Reports

- **Issues**: [GitHub Issues](https://github.com/your-repo/splitpay/issues)
- **Feature Requests**: [GitHub Discussions](https://github.com/your-repo/splitpay/discussions)
- **Contact**: [Your contact email]

---

**Note**: This changelog is updated with each release. For unreleased changes, see the [Unreleased] section at the top.

Last updated: 2024-11-11
