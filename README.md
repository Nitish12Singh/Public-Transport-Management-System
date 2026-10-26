# Public Transport Management System (PTMS)

A Java-based Public Transport Management System with a graphical interface and a console mode. The application supports role-based access for administrators, drivers, and passengers, and includes an alert system for reporting important issues.

## Features

- **Role-based login** for Admin, Driver, and Passenger users
- **GUI and console modes** so the application can be run with a windowed interface or from the terminal
- **Transport management interface** with management, viewing, profile, booking, and statistics panels
- **Alert system** with Normal, High, and Emergency priorities
- **Alert status tracking**: administrators can mark alerts as Seen or Resolved, and senders can check the status
- **Local data storage** shared by the GUI and console versions
- **Passenger registration** is available from the login screen

## Technologies

- Java 11 or newer
- Java Swing for the graphical user interface
- Java Runtime Environment (JRE) or Java Development Kit (JDK)

## Requirements

Install **Java 11 or newer** and make sure the `java` command is available in your terminal.

Check your Java installation:

```bash
java -version
```

## Run the Application

Keep `PTMS.jar` and the launch scripts in the same folder.

### GUI mode

**Windows:** Double-click `Run_PTMS_GUI.bat` if that file is included in your project folder, or run:

```bash
java -jar PTMS.jar
```

**Linux/macOS:**

```bash
java -jar PTMS.jar
```

### Console mode

```bash
java -jar PTMS.jar --console
```

On Linux/macOS, if `run_ptms.sh` is present, you can also use:

```bash
chmod +x run_ptms.sh
./run_ptms.sh
./run_ptms.sh --console
```

## Demo Login Credentials

Choose the matching role on the login screen.

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Driver | `driver1` | `driver123` |
| Passenger | `passenger` | `pass123` |

These are demo credentials. Change or replace them before using the application with real users.

## Alerts

Drivers and passengers can open **Send Alert**, enter a message, and choose a priority:

- Normal
- High
- Emergency

Administrators can view incoming alerts, receive an in-app notification, and mark alerts as **Seen** or **Resolved**. The sender can check the alert status from the Send Alert page.

## Data Storage

The application saves data automatically in `transport_data_v2.dat` next to `PTMS.jar`. The GUI and console versions use the same data file.

To reset the application to its sample data, close the application and delete `transport_data_v2.dat`. **This deletes the locally saved application data**, so back up the file first if you need it.

If the application cannot write to the current folder, it may store data in a `PTMS` folder under your user directory.

## Build from Source

If the project source files are available in a `src` folder, compile and package them from the project directory:

```bash
cd src
javac -d ../build *.java
cd ../build
jar cfe ../PTMS.jar PTMS *.class
```

## Troubleshooting

- **Java command not found:** Install Java 11 or newer and configure your system `PATH`.
- **Could not save data / Access is denied:** Extract the project to a writable folder such as Documents or Desktop. Avoid running it from inside a ZIP archive or a protected folder such as `Program Files`.
- **Linux/macOS script does not run:** Make it executable with `chmod +x run_ptms.sh`, then try again.
- **Login issue:** Select the correct role and use the corresponding demo credentials listed above.

## Project Structure

Your repository may contain files similar to these:

```text
PTMS/
├── PTMS.jar
├── run_ptms.sh
├── Run_PTMS_GUI.bat        # if included
├── Run_PTMS_Console.bat    # if included
├── README.md
└── transport_data_v2.dat   # generated automatically when the app runs
```

The source directory and Windows batch files are optional in this example; include them in your repository only if they are actually present.

## Notes

- This repository includes a runnable JAR, but the source code must also be included if you want others to rebuild or inspect the implementation.
- Do not commit real user data or private credentials to a public repository.

---

**Public Transport Management System (PTMS)** — Java desktop application.

