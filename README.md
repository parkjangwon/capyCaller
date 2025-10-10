# CapyCaller

<p align="left">
  <img src=".github/images/capyCallerLogo.png" alt="CapyCaller Logo" width="300"/>
</p>

CapyCaller is a simple yet powerful API client for Android that allows you to manage and execute API requests with ease, right from your device.

## Features

- **API Management**: Create, edit, copy, and organize your API requests with a clean, intuitive UI.
- **Detailed Request Configuration**:
    - Set API name, URL, and HTTP method (GET, POST, PUT, DELETE, PATCH).
    - Add query parameters, headers, and request bodies.
    - Supports `application/json`, `text/plain`, and `application/x-www-form-urlencoded` body types.
- **Execute and Inspect**: Execute API requests within the app and view the responses.
- **Home Screen Widgets**:
    - **Single API Widget**: Execute a specific API with a single tap from your home screen.
    - **Multi API Widget**: View and run a list of your APIs directly from a home screen widget.
- **App Shortcuts**: APIs marked as shortcuts will appear when you long-press the app icon, providing quick access.
- **Settings & Customization**:
    - **Customizable Theme**: Choose between System, Light, and Dark themes.
    - **Push Notification Control**: Enable or disable push notifications for background API executions (from widgets and shortcuts). Default is disabled.
    - **Backup & Restore**: Easily back up your API list to a file and restore it later.
- **Multi-Select Operations**: Long-press an item to enter multi-select mode to execute or delete multiple APIs at once.

## Getting Started

1. Clone the repository.
2. Open the project in Android Studio.
3. Build and run the app on your Android device or emulator.

## How to Use

- **Add an API**: Tap the floating action button on the main screen.
- **Edit an API**: Tap an API in the list.
- **Execute an API**: Tap the "Send" icon in the edit screen.
- **Multi-Select Mode**: Long-press any API in the list to enter selection mode. This allows you to execute, delete, or copy multiple items.
- **Add a Widget**:
    1. Long-press on your home screen.
    2. Select "Widgets".
    3. Find "CapyCaller" and choose either the Single or Multi API widget.
    4. For the Single API widget, you will be prompted to select which API to link.
- **Create a Shortcut**: In the API edit screen, enable the "Add to shortcuts" option. The API will now appear when you long-press the app icon.
- **Access Settings**: Tap the settings icon in the top-right corner of the main screen.
