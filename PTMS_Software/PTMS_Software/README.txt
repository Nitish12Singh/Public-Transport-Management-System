PUBLIC TRANSPORT MANAGEMENT SYSTEM (PTMS)
=========================================
Requires: Java 11 or newer (JRE or JDK).

HOW TO RUN
  Windows : double-click Run_PTMS_GUI.bat      (window version)
            double-click Run_PTMS_Console.bat  (console version)
  Any OS  : java -jar PTMS.jar                 (window version)
            java -jar PTMS.jar --console       (console version)
  Linux/Mac: ./run_ptms.sh   or   ./run_ptms.sh --console

LOGINS (pick the matching role on the login screen)
  Admin     admin      / admin123
  Driver    driver1    / driver123
  Passenger passenger  / pass123   (or register a new passenger)

DATA
  Saved automatically in transport_data_v2.dat next to the jar.
  GUI and console versions share this file. Delete it to reset to sample data.

REBUILD FROM SOURCE
  cd src
  javac -d ../build *.java
  cd ../build && jar cfe ../PTMS.jar PTMS *.class

MAKE A WINDOWS INSTALLER (.exe) - optional, run on Windows with JDK 17+ and WiX
  jpackage --input . --name PTMS --main-jar PTMS.jar --main-class PTMS --type exe --win-menu --win-shortcut

ALERTS (new)
  Drivers and passengers: "Send Alert" page -> message + priority (Normal / High / Emergency).
  Admin: bell icon + red badge on "Alerts" + pop-up notification as soon as an alert arrives,
  even when the driver/passenger is using a different running window. Admin can mark alerts
  Seen / Resolved; the sender sees the status on their Send Alert page.

TROUBLESHOOTING
  "Could not save data ... Access is denied": extract the zip first (do not run from inside the zip),
  and keep the folder somewhere normal such as Documents or Desktop (not Program Files).
  If the folder is read-only, PTMS automatically stores its data in  <your user folder>\PTMS\  instead.
