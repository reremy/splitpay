# Contributing to SplitPay

Thank you for your interest in contributing to SplitPay! This document provides guidelines and instructions for contributing.

---

## Table of Contents

1. [Code of Conduct](#code-of-conduct)
2. [Getting Started](#getting-started)
3. [Development Workflow](#development-workflow)
4. [Coding Standards](#coding-standards)
5. [Commit Guidelines](#commit-guidelines)
6. [Pull Request Process](#pull-request-process)
7. [Testing Requirements](#testing-requirements)
8. [Documentation](#documentation)

---

## Code of Conduct

### Our Pledge

We are committed to providing a welcoming and inclusive environment for all contributors, regardless of experience level, background, or identity.

### Expected Behavior

- Be respectful and constructive in all interactions
- Welcome newcomers and help them get started
- Accept constructive criticism gracefully
- Focus on what is best for the community and project

### Unacceptable Behavior

- Harassment, discrimination, or offensive comments
- Trolling or deliberately derailing discussions
- Publishing others' private information
- Any conduct that would be inappropriate in a professional setting

---

## Getting Started

### Prerequisites

Before contributing, ensure you have:

1. Read the [README.md](README.md)
2. Set up your development environment following [docs/SETUP.md](docs/SETUP.md)
3. Familiarized yourself with the [architecture](docs/ARCHITECTURE.md)
4. Reviewed existing issues and pull requests

### Finding an Issue

1. Browse [open issues](https://github.com/your-repo/splitpay/issues)
2. Look for issues labeled:
   - `good first issue` - Great for newcomers
   - `help wanted` - Need contributors
   - `bug` - Bug fixes needed
   - `enhancement` - New features
3. Comment on the issue to express interest and get assigned

### Reporting Bugs

When reporting bugs, include:

- Clear, descriptive title
- Steps to reproduce
- Expected behavior
- Actual behavior
- Screenshots/logs (if applicable)
- Environment details:
  - Android version
  - Device model
  - App version

**Template**:
```markdown
## Bug Description
A clear description of the bug.

## Steps to Reproduce
1. Go to '...'
2. Click on '...'
3. Scroll down to '...'
4. See error

## Expected Behavior
What you expected to happen.

## Actual Behavior
What actually happened.

## Screenshots
If applicable, add screenshots.

## Environment
- Android Version: [e.g., Android 13]
- Device: [e.g., Pixel 5]
- App Version: [e.g., 1.0]
```

### Suggesting Features

When suggesting features, include:

- Clear, descriptive title
- Problem statement (what user need does this address?)
- Proposed solution
- Alternatives considered
- Additional context (mockups, examples, etc.)

---

## Development Workflow

### 1. Fork and Clone

```bash
# Fork the repository on GitHub, then clone your fork
git clone https://github.com/YOUR_USERNAME/splitpay.git
cd splitpay

# Add upstream remote
git remote add upstream https://github.com/original-repo/splitpay.git
```

### 2. Create a Branch

```bash
# Update your main branch
git checkout main
git pull upstream main

# Create a feature branch
git checkout -b feature/your-feature-name

# Or for bug fixes
git checkout -b fix/bug-description
```

**Branch Naming Convention**:
- `feature/feature-name` - New features
- `fix/bug-description` - Bug fixes
- `docs/update-description` - Documentation updates
- `refactor/component-name` - Code refactoring
- `test/test-description` - Adding tests

### 3. Make Changes

- Write clean, readable code
- Follow coding standards (see below)
- Add tests for new functionality
- Update documentation as needed
- Test your changes thoroughly

### 4. Commit Changes

```bash
git add .
git commit -m "type: brief description"
```

See [Commit Guidelines](#commit-guidelines) for commit message format.

### 5. Push to Your Fork

```bash
git push origin feature/your-feature-name
```

### 6. Create Pull Request

1. Go to your fork on GitHub
2. Click "New Pull Request"
3. Select your branch
4. Fill out the PR template
5. Submit the PR

---

## Coding Standards

### Kotlin Code Style

Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):

#### Naming
- **Classes**: PascalCase (e.g., `UserRepository`)
- **Functions**: camelCase (e.g., `getUserById`)
- **Variables**: camelCase (e.g., `userName`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_RETRY_COUNT`)

#### Formatting
- **Indentation**: 4 spaces (no tabs)
- **Line length**: Max 120 characters
- **Blank lines**: One blank line between functions

#### Example:

```kotlin
class UserRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    suspend fun getUser(uid: String): Result<User> {
        return try {
            val snapshot = firestore.collection("users")
                .document(uid)
                .get()
                .await()

            val user = snapshot.toObject(User::class.java)
            Result.success(user ?: throw Exception("User not found"))
        } catch (e: Exception) {
            logE("Error fetching user: ${e.message}")
            Result.failure(e)
        }
    }
}
```

### Compose Guidelines

- **State Hoisting**: Hoist state to the lowest common ancestor
- **Modifiers**: Always accept a `modifier` parameter
- **Previews**: Add `@Preview` for all composables
- **Single Responsibility**: Each composable should do one thing well

```kotlin
@Composable
fun UserCard(
    user: User,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        // Card content
    }
}

@Preview
@Composable
fun PreviewUserCard() {
    SplitPayTheme {
        UserCard(
            user = User(fullName = "John Doe"),
            onClick = {}
        )
    }
}
```

### Comments

- **When to comment**: Explain "why", not "what"
- **KDoc**: Use for public APIs
- **TODO**: Mark with TODO for future improvements

```kotlin
/**
 * Calculates how to split an expense among participants.
 *
 * @param totalAmount The total expense amount
 * @param splitType How to split (EQUALLY, EXACT_AMOUNTS, etc.)
 * @param participants List of participant UIDs
 * @return List of ExpenseParticipant with calculated amounts
 */
fun calculateSplit(
    totalAmount: Double,
    splitType: String,
    participants: List<String>
): List<ExpenseParticipant> {
    // TODO: Add support for custom split ratios
    // ...
}
```

### Error Handling

- Use `Result<T>` for operations that can fail
- Log errors with custom Logger
- Provide meaningful error messages

```kotlin
suspend fun addExpense(expense: Expense): Result<String> {
    return try {
        val docRef = expensesCollection.document()
        docRef.set(expense).await()
        logI("Expense added successfully: ${expense.id}")
        Result.success(docRef.id)
    } catch (e: FirebaseException) {
        logE("Failed to add expense: ${e.message}")
        Result.failure(e)
    }
}
```

---

## Commit Guidelines

### Commit Message Format

```
type(scope): subject

body (optional)

footer (optional)
```

### Types

- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, no logic change)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks (dependencies, build, etc.)

### Examples

```bash
# Feature
git commit -m "feat(expense): add support for multiple payers"

# Bug fix
git commit -m "fix(login): resolve email validation issue"

# Documentation
git commit -m "docs: update README with setup instructions"

# Refactoring
git commit -m "refactor(repository): simplify error handling"

# Test
git commit -m "test(expense): add unit tests for split calculation"
```

### Detailed Commit

```
feat(expense): add percentage-based split option

Allow users to split expenses by percentage instead of just
equal amounts. Each participant can specify their percentage
of the total expense.

Closes #42
```

---

## Pull Request Process

### PR Title

Follow commit message format:
```
type(scope): brief description
```

### PR Description

Use this template:

```markdown
## Description
Brief description of changes.

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Related Issue
Closes #[issue number]

## Changes Made
- Change 1
- Change 2
- Change 3

## Testing
- [ ] Unit tests added/updated
- [ ] UI tests added/updated
- [ ] Manual testing completed

## Screenshots (if applicable)
[Add screenshots or GIFs]

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex code
- [ ] Documentation updated
- [ ] No new warnings generated
- [ ] Tests pass locally
```

### Review Process

1. **Automated Checks**: CI must pass (tests, linting)
2. **Code Review**: At least one approval required
3. **Testing**: Reviewers should test changes locally
4. **Feedback**: Address all review comments
5. **Approval**: Merge when approved

### Merge Strategy

- **Squash and Merge**: Preferred for feature branches
- **Rebase**: For keeping history clean
- **No force push**: After PR is created

---

## Testing Requirements

### Required Tests

1. **Unit Tests**:
   - All new business logic
   - ViewModels
   - Use cases
   - Utilities

2. **Integration Tests**:
   - Repository operations
   - Database interactions

3. **UI Tests**:
   - Critical user flows
   - New UI components

### Running Tests

```bash
# Unit tests
./gradlew test

# Instrumented tests
./gradlew connectedAndroidTest

# With coverage
./gradlew testDebugUnitTestCoverage
```

### Coverage Requirements

- Minimum 70% code coverage for new code
- Critical paths must be tested

---

## Documentation

### What to Document

- New features
- API changes
- Architecture decisions
- Setup instructions
- Breaking changes

### Where to Document

- **README.md**: High-level overview
- **docs/**: Detailed documentation
- **Code comments**: Complex logic
- **CHANGELOG.md**: Version history

---

## Project Structure

When adding new files, follow the existing structure:

```
app/src/main/java/com/example/splitpay/
├── data/
│   ├── model/           # Data models
│   └── repository/      # Repositories
├── domain/
│   └── usecase/         # Business logic
├── ui/
│   ├── [feature]/       # Feature-specific UI
│   ├── common/          # Reusable components
│   └── theme/           # Theming
├── navigation/          # Navigation
└── logger/              # Utilities
```

---

## Questions or Help?

- **Issues**: Open an issue for bugs or features
- **Discussions**: Use GitHub Discussions for questions
- **Contact**: [Your contact information]

---

## License

By contributing, you agree that your contributions will be licensed under the same license as the project (MIT License).

---

**Thank you for contributing to SplitPay!** 🎉
