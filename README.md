# My Android Proxy App

## Description
My Android Proxy App is a tool that acts as a local proxy server, allowing users to intercept, modify, and forward HTTP/2 requests and responses. The app is designed to help developers test and debug their network traffic. It features a user-friendly interface for viewing and modifying requests and responses, with support for advanced features like SSL interception, request/response manipulation, and much more.

## Features
- Intercept and modify HTTP/2 requests and responses
- Support for SSL interception
- Customizable proxy settings
- View raw and modified network traffic in real-time
- Send and resend custom requests (Repeater functionality)
- UI components to display requests in a structured format
- Support for saving and exporting intercepted data

## Installation

### Prerequisites
- **Android Studio**: Make sure you have Android Studio installed and set up on your machine.
- **Java**: This app requires **Java 8 or higher**.
- **Android Device/Emulator**: A physical Android device or an emulator with internet access.

### Clone the Repository
```bash
git clone https://github.com/yourusername/your-repository.git
```

### Import into Android Studio
1. Open Android Studio.
2. Click on `File` > `Open` and select the cloned repository directory.
3. Android Studio will automatically sync the project with Gradle and download any necessary dependencies.

## Running the App

### Running on an Android Device
1. Connect your Android device via USB or set up an Android emulator.
2. In Android Studio, select your device from the top toolbar.
3. Click on the **Run** button (green play icon) or use the following command:
   ```bash
   ./gradlew installDebug
   ```

## Usage
1. **Start Proxy Server**: Open the app and start the proxy server.
2. **Modify Requests**: Use the `EditText` field to modify intercepted HTTP/2 requests.
3. **View Requests**: View all intercepted requests in an `ExpandableListView`.
4. **Send Custom Requests**: Modify and resend requests.
5. **SSL Interception**: View encrypted traffic in plaintext.
6. **Repeater**: Resend previously intercepted requests.

## Testing

### Unit Tests
Run unit tests using:
```bash
./gradlew test
```

### UI Tests
Run UI tests with:
```bash
./gradlew connectedAndroidTest
```

## Contributing

1. **Fork the Repository**.
2. **Clone Your Fork**.
   ```bash
   git clone https://github.com/yourusername/your-forked-repository.git
   ```
3. **Create a Branch**.
   ```bash
   git checkout -b feature/your-feature-name
   ```
4. **Make Changes & Commit**.
   ```bash
   git commit -am "Add feature"
   ```
5. **Push Changes**.
   ```bash
   git push origin feature/your-feature-name
   ```
6. **Create a Pull Request** on GitHub.

## License
This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

## Contact
- **Email**: your.email@example.com
- **GitHub Issues**: Report bugs or suggest features.

