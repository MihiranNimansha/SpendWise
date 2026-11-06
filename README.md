# SpendWise

A personal expense tracker for Android. Log what you spend, set monthly budgets per category and
see where the money goes. Everything is stored **on the device** (Room database); there is no
server and no account in the cloud.

## Features

- **Onboarding, register and sign in** with an email and password stored locally (hashed with BCrypt)
- **Dashboard** with this month's spending, a budget progress bar, spending per category and the
  latest expenses
- **Budget alert** when you have used 90% of the month's budget
- **Expenses**: add, edit, delete (swipe to delete in the history list), 7 built-in categories
- **History** with search and category filter
- **Budget goals** per category and a monthly total
- **Analytics**: pie chart of this month's spending by category
- **Settings and profile**: currency (LKR, USD, EUR, GBP), dark mode, notifications switch,
  change password, delete account
- **Export to CSV** and share it from the app

> Tip: tapping the greeting on the dashboard five times adds a few demo expenses, which is handy
> for screenshots.

## Tech stack

| Area | Choice |
| --- | --- |
| Language | Kotlin 2.1 |
| UI | Views + ViewBinding, Material 3, Navigation component (single activity) |
| Architecture | MVVM: `AndroidViewModel` + `LiveData`, Room `Flow`s exposed as LiveData |
| Storage | Room 2.6 (KSP), SharedPreferences for settings |
| Charts / animation | MPAndroidChart, Lottie |
| Security | jBCrypt for password hashing |
| Build | Gradle (Groovy) with AGP 8.13, compile/target SDK 34, min SDK 26 |

## Getting started

1. Clone the repository and open it in a recent Android Studio (one that supports AGP 8.13).
2. Let Gradle sync. The project asks Gradle for a JDK 21 toolchain and downloads it if needed.
3. Run the `app` configuration on an emulator or device, or from the terminal:

```bash
./gradlew assembleDebug        # build the debug APK
./gradlew testDebugUnitTest    # run the unit tests
```

### Release builds

Release signing secrets are **never** stored in the repository. To build a signed release, put
these keys in `local.properties` (which is git-ignored) or in environment variables:

```properties
SPENDWISE_STORE_FILE=/path/to/your.jks
SPENDWISE_STORE_PASSWORD=...
SPENDWISE_KEY_ALIAS=...
SPENDWISE_KEY_PASSWORD=...
```

## Continuous integration

`.github/workflows/android.yml` builds the debug APK and runs the unit tests on every push and
pull request, and uploads the APK and test reports as workflow artifacts.

## Project structure

```
app/src/main/java/com/spendwise/app/
├── data/
│   ├── local/        Room database, DAOs and entities
│   ├── preferences/  SharedPreferences wrapper (UserPreferences)
│   ├── repository/   Repository classes over the DAOs
│   └── model/        Plain UI models
├── viewmodel/        AuthViewModel, ExpenseViewModel, BudgetViewModel
├── ui/               One package per screen (auth, dashboard, expense, goals, analytics, ...)
├── utils/            Formatting, date helpers, CSV export, category styles, Event
└── MainActivity.kt   Navigation host, drawer and session reset
app/src/test/         JVM unit tests for the helpers in utils/
```

## Notes and known limitations

- Accounts live only in the local database on the device. Uninstalling the app (or clearing its
  data) removes them.
- Dark mode currently themes the system bars, toolbar and dialogs; most screen backgrounds still
  use the light palette.
- Notifications are a settings switch only; the app does not schedule notifications yet.
