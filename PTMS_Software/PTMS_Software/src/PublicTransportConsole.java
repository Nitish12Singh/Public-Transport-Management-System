import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Public Transport Management System - CONSOLE version (Admin / Driver / Passenger)
 *
 * Demo logins : admin / admin123   |   driver1 / driver123   |   passenger / pass123
 * Storage     : transport_data_v2.dat  (same file/format as the Swing version, so both share data)
 * Compile     : javac PublicTransportConsole.java
 * Run         : java PublicTransportConsole
 */
public class PublicTransportConsole {

    // ============================================================
    //  DATA LAYER
    // ============================================================
    static final String FILE = "transport_data_v2.dat";
    static final String ROUTES = "routes", VEHICLES = "vehicles", DRIVERS = "drivers",
            SCHEDULES = "schedules", TICKETS = "tickets", USERS = "users", ISSUES = "issues";

    // routes    : id, name, source, destination, distanceKm, fare
    // vehicles  : id, regNo, type, capacity
    // drivers   : id, name, phone, license, vehicleId
    // schedules : id, routeId, vehicleId, departure, status
    // tickets   : id, passenger, scheduleId, seats, amount, bookedAt, userId
    // users     : id, username, password, role, fullName, driverId
    // issues    : id, driverId, message, time, status
    static Map<String, List<String[]>> db = new HashMap<>();
    static String[] currentUser;
    static final Scanner sc = new Scanner(System.in);

    static final Object[][] REFS = {
            {SCHEDULES, 1, ROUTES}, {SCHEDULES, 2, VEHICLES}, {DRIVERS, 4, VEHICLES},
            {TICKETS, 2, SCHEDULES}, {ISSUES, 1, DRIVERS}
    };

    @SuppressWarnings("unchecked")
    static void load() {
        File f = new File(FILE);
        if (f.exists()) {
            try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
                db = (Map<String, List<String[]>>) in.readObject();
            } catch (Exception e) { db = new HashMap<>(); }
        }
        for (String t : new String[]{ROUTES, VEHICLES, DRIVERS, SCHEDULES, TICKETS, USERS, ISSUES})
            db.computeIfAbsent(t, k -> new ArrayList<>());
        if (db.get(ROUTES).isEmpty()) seed();
        boolean hasAdmin = false;
        for (String[] u : db.get(USERS)) if (u[3].equals("Admin")) hasAdmin = true;
        if (!hasAdmin) {
            db.get(USERS).add(new String[]{nextId(USERS), "admin", "admin123", "Admin", "Administrator", ""});
            save();
        }
    }

    static void save() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(FILE))) {
            out.writeObject(db);
        } catch (IOException e) { System.out.println("  ! Could not save data: " + e.getMessage()); }
    }

    static void seed() {
        db.get(ROUTES).add(new String[]{"1", "R-101 City Express", "Central Station", "Airport", "24", "45"});
        db.get(ROUTES).add(new String[]{"2", "R-202 University Link", "Old Town", "Campus", "12", "20"});
        db.get(ROUTES).add(new String[]{"3", "R-303 Metro Ring", "North Gate", "South Gate", "30", "30"});
        db.get(VEHICLES).add(new String[]{"1", "UP16-AB-1001", "Bus", "40"});
        db.get(VEHICLES).add(new String[]{"2", "UP16-CD-2002", "Bus", "35"});
        db.get(VEHICLES).add(new String[]{"3", "DL-MET-3003", "Metro", "120"});
        db.get(DRIVERS).add(new String[]{"1", "Ramesh Kumar", "9876543210", "DL-0420110012345", "1"});
        db.get(DRIVERS).add(new String[]{"2", "Suresh Yadav", "9123456780", "DL-0520120067890", "2"});
        db.get(SCHEDULES).add(new String[]{"1", "1", "1", "08:30", "Scheduled"});
        db.get(SCHEDULES).add(new String[]{"2", "2", "2", "09:15", "Scheduled"});
        db.get(SCHEDULES).add(new String[]{"3", "3", "3", "10:00", "Scheduled"});
        db.get(USERS).add(new String[]{"1", "admin", "admin123", "Admin", "Administrator", ""});
        db.get(USERS).add(new String[]{"2", "driver1", "driver123", "Driver", "Ramesh Kumar", "1"});
        db.get(USERS).add(new String[]{"3", "driver2", "driver123", "Driver", "Suresh Yadav", "2"});
        db.get(USERS).add(new String[]{"4", "passenger", "pass123", "Passenger", "Demo Passenger", ""});
        save();
    }

    static String nextId(String table) {
        int max = 0;
        for (String[] r : db.get(table)) max = Math.max(max, Integer.parseInt(r[0]));
        return String.valueOf(max + 1);
    }

    static String[] find(String table, String id) {
        for (String[] r : db.get(table)) if (r[0].equals(id)) return r;
        return null;
    }

    static String name(String table, String id) {
        String[] r = find(table, id);
        return r == null ? "?" : r[1];
    }

    static String label(String table, String id) {
        String[] r = find(table, id);
        if (r == null) return id + " - (deleted)";
        if (table.equals(SCHEDULES))
            return r[0] + " - " + name(ROUTES, r[1]) + " | " + name(VEHICLES, r[2]) + " @ " + r[3];
        return r[0] + " - " + r[1];
    }

    static boolean isReferenced(String table, String id) {
        for (Object[] ref : REFS) {
            if (!ref[2].equals(table)) continue;
            for (String[] r : db.get((String) ref[0])) if (r[(Integer) ref[1]].equals(id)) return true;
        }
        return false;
    }

    static int bookedSeats(String sid) {
        int n = 0;
        for (String[] t : db.get(TICKETS)) if (t[2].equals(sid)) n += Integer.parseInt(t[3]);
        return n;
    }

    static int capacity(String vehicleId) {
        String[] v = find(VEHICLES, vehicleId);
        return v == null ? 0 : Integer.parseInt(v[3]);
    }

    static int availableSeats(String sid) {
        String[] s = find(SCHEDULES, sid);
        return s == null ? 0 : capacity(s[2]) - bookedSeats(sid);
    }

    static double fareFor(String sid, int seats) {
        String[] s = find(SCHEDULES, sid);
        String[] r = s == null ? null : find(ROUTES, s[1]);
        return r == null ? 0 : Double.parseDouble(r[5]) * seats;
    }

    static String tripStatus(String sid) {
        String[] s = find(SCHEDULES, sid);
        return s == null ? "?" : s[4];
    }

    static String[] myDriver() { return find(DRIVERS, currentUser[5]); }

    static boolean bookable(String[] s) { return s[4].equals("Scheduled") || s[4].equals("Delayed"); }

    static String now() { return new SimpleDateFormat("dd-MM-yyyy HH:mm").format(new Date()); }

    static String fmt(double d) { return String.format("%.2f", d); }

    // ============================================================
    //  CONSOLE HELPERS
    // ============================================================
    static String ask(String prompt) {
        System.out.print(prompt);
        if (!sc.hasNextLine()) { System.out.println(); System.exit(0); }
        return sc.nextLine().trim();
    }

    static int askInt(String prompt, int min, int max) {
        while (true) {
            try {
                int v = Integer.parseInt(ask(prompt));
                if (v >= min && v <= max) return v;
            } catch (NumberFormatException ignored) { }
            System.out.println("  ! Enter a number between " + min + " and " + max + ".");
        }
    }

    static boolean confirm(String q) { return ask(q + " (y/n): ").equalsIgnoreCase("y"); }

    static void pause() { ask("\n  Press Enter to continue..."); }

    static void banner(String t) {
        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println("  " + t);
        System.out.println("=".repeat(70));
    }

    static int menu(String title, String exitText, String... items) {
        banner(title);
        for (int i = 0; i < items.length; i++) System.out.printf("  %d. %s%n", i + 1, items[i]);
        System.out.println("  0. " + exitText);
        return askInt("\n  Choose option: ", 0, items.length);
    }

    static void table(String[] h, List<String[]> rows) {
        if (rows.isEmpty()) { System.out.println("  (no records)"); return; }
        int[] w = new int[h.length];
        for (int i = 0; i < h.length; i++) w[i] = h[i].length();
        for (String[] r : rows) for (int i = 0; i < h.length; i++) w[i] = Math.max(w[i], r[i].length());
        StringBuilder sep = new StringBuilder("  +");
        for (int x : w) sep.append("-".repeat(x + 2)).append("+");
        System.out.println(sep);
        printRow(h, w);
        System.out.println(sep);
        for (String[] r : rows) printRow(r, w);
        System.out.println(sep);
        System.out.println("  " + rows.size() + " record(s)");
    }

    static void printRow(String[] r, int[] w) {
        StringBuilder sb = new StringBuilder("  |");
        for (int i = 0; i < w.length; i++) sb.append(' ').append(String.format("%-" + w[i] + "s", r[i])).append(" |");
        System.out.println(sb);
    }

    // ---------- shared row builders ----------
    static List<String[]> ticketRows(java.util.function.Predicate<String[]> p) {
        List<String[]> rows = new ArrayList<>();
        for (String[] t : db.get(TICKETS))
            if (p.test(t)) rows.add(new String[]{t[0], label(SCHEDULES, t[2]), t[1], t[3], t[4], t[5], tripStatus(t[2])});
        return rows;
    }

    static final String[] TICKET_COLS = {"Ticket", "Trip", "Passenger", "Seats", "Amount", "Booked At", "Status"};
    static final String[] TIMETABLE_COLS = {"ID", "Route", "From", "To", "Dep.", "Vehicle", "Fare", "Seats Left", "Status"};

    static List<String[]> timetableRows(boolean onlyBookable, String keyword) {
        List<String[]> rows = new ArrayList<>();
        for (String[] s : db.get(SCHEDULES)) {
            if (onlyBookable && !bookable(s)) continue;
            String[] r = find(ROUTES, s[1]), v = find(VEHICLES, s[2]);
            if (r == null || v == null) continue;
            String hay = (r[1] + " " + r[2] + " " + r[3]).toLowerCase();
            if (!keyword.isEmpty() && !hay.contains(keyword.toLowerCase())) continue;
            rows.add(new String[]{s[0], r[1], r[2], r[3], s[3], v[1] + " (" + v[2] + ")", r[5],
                    String.valueOf(availableSeats(s[0])), s[4]});
        }
        return rows;
    }

    // ============================================================
    //  GENERIC CRUD (admin pages)
    // ============================================================
    // kinds: text | num | opt | time | pick:A|B|C | ref:table
    static String promptField(String col, String kind, String current) {
        while (true) {
            if (kind.startsWith("ref:")) {
                String t = kind.substring(4);
                System.out.println("  Available " + t + ":");
                for (String[] r : db.get(t)) System.out.println("      " + label(t, r[0]));
            }
            if (kind.startsWith("pick:")) System.out.println("  Options: " + kind.substring(5).replace("|", ", "));
            String v = ask("  " + col + (current == null ? "" : " [" + current + "]") + (kind.equals("opt") ? " (optional)" : "") + ": ");
            if (v.isEmpty() && current != null) return current;
            if (v.isEmpty() && kind.equals("opt")) return "";
            if (v.isEmpty()) { System.out.println("  ! Required."); continue; }
            if (kind.equals("num")) {
                try { if (Double.parseDouble(v) > 0) return v; } catch (NumberFormatException ignored) { }
                System.out.println("  ! Enter a positive number.");
            } else if (kind.equals("time")) {
                if (v.matches("([01]?\\d|2[0-3]):[0-5]\\d")) return v;
                System.out.println("  ! Use HH:mm format, e.g. 08:30");
            } else if (kind.startsWith("ref:")) {
                if (find(kind.substring(4), v) != null) return v;
                System.out.println("  ! No such ID.");
            } else if (kind.startsWith("pick:")) {
                for (String o : kind.substring(5).split("\\|")) if (o.equalsIgnoreCase(v)) return o;
                System.out.println("  ! Choose one of the listed options.");
            } else return v;
        }
    }

    static String validateRow(String table, String[] row) {
        if (table.equals(USERS)) {
            for (String[] u : db.get(USERS))
                if (u[1].equalsIgnoreCase(row[1]) && !u[0].equals(row[0])) return "Username already exists.";
            if (row[3].equals("Driver") && find(DRIVERS, row[5]) == null)
                return "Driver accounts need a valid Driver ID (see Drivers).";
        }
        return null;
    }

    static void crud(String title, String table, String[] cols, String[] kinds) {
        String[] headers = new String[cols.length + 1];
        headers[0] = "ID";
        System.arraycopy(cols, 0, headers, 1, cols.length);
        while (true) {
            int c = menu(title, "Back", "View all", "Add new", "Update", "Delete");
            if (c == 0) return;
            if (c == 1) {
                List<String[]> rows = new ArrayList<>();
                for (String[] r : db.get(table)) {
                    String[] o = new String[r.length];
                    o[0] = r[0];
                    for (int i = 0; i < cols.length; i++)
                        o[i + 1] = kinds[i].startsWith("ref:") ? label(kinds[i].substring(4), r[i + 1])
                                : cols[i].equals("Password") ? "******" : r[i + 1];
                    rows.add(o);
                }
                table(headers, rows);
                pause();
            } else if (c == 2) {
                String[] row = new String[cols.length + 1];
                row[0] = nextId(table);
                for (int i = 0; i < cols.length; i++) row[i + 1] = promptField(cols[i], kinds[i], null);
                String err = validateRow(table, row);
                if (err != null) { System.out.println("  ! " + err); continue; }
                db.get(table).add(row);
                save();
                System.out.println("  + Added with ID " + row[0]);
            } else {
                String id = ask("  Enter record ID (Enter to cancel): ");
                if (id.isEmpty()) continue;
                String[] r = find(table, id);
                if (r == null) { System.out.println("  ! No record with ID " + id); continue; }
                if (c == 3) {
                    String[] copy = r.clone();
                    for (int i = 0; i < cols.length; i++) copy[i + 1] = promptField(cols[i], kinds[i], r[i + 1]);
                    String err = validateRow(table, copy);
                    if (err != null) { System.out.println("  ! " + err); continue; }
                    System.arraycopy(copy, 0, r, 0, r.length);
                    save();
                    System.out.println("  + Updated.");
                } else {
                    if (table.equals(USERS) && currentUser[0].equals(id)) { System.out.println("  ! You cannot delete your own account."); continue; }
                    if (isReferenced(table, id)) { System.out.println("  ! This record is used by other data. Remove those first."); continue; }
                    if (confirm("  Delete record " + id + "?")) {
                        db.get(table).removeIf(x -> x[0].equals(id));
                        save();
                        System.out.println("  + Deleted.");
                    }
                }
            }
        }
    }

    // ============================================================
    //  ADMIN
    // ============================================================
    static void adminDashboard() {
        banner("ADMIN DASHBOARD");
        int pax = 0, open = 0;
        double revenue = 0;
        for (String[] u : db.get(USERS)) if (u[3].equals("Passenger")) pax++;
        for (String[] i : db.get(ISSUES)) if (i[4].equals("Open")) open++;
        for (String[] t : db.get(TICKETS)) revenue += Double.parseDouble(t[4]);
        System.out.printf("  Routes: %-4d Vehicles: %-4d Drivers: %-4d Schedules: %-4d%n",
                db.get(ROUTES).size(), db.get(VEHICLES).size(), db.get(DRIVERS).size(), db.get(SCHEDULES).size());
        System.out.printf("  Tickets: %-3d Revenue: Rs. %-10s Passengers: %-3d Open issues: %d%n%n",
                db.get(TICKETS).size(), fmt(revenue), pax, open);

        Map<String, double[]> stats = new LinkedHashMap<>();
        for (String[] r : db.get(ROUTES)) stats.put(r[0], new double[3]);
        for (String[] t : db.get(TICKETS)) {
            String[] s = find(SCHEDULES, t[2]);
            if (s != null && stats.containsKey(s[1])) {
                double[] a = stats.get(s[1]);
                a[0]++; a[1] += Integer.parseInt(t[3]); a[2] += Double.parseDouble(t[4]);
            }
        }
        List<String[]> rows = new ArrayList<>();
        double max = 1;
        for (double[] a : stats.values()) max = Math.max(max, a[2]);
        for (Map.Entry<String, double[]> e : stats.entrySet()) {
            double[] a = e.getValue();
            rows.add(new String[]{name(ROUTES, e.getKey()), String.valueOf((int) a[0]), String.valueOf((int) a[1]),
                    fmt(a[2]), "#".repeat((int) Math.round(20 * a[2] / max))});
        }
        table(new String[]{"Route", "Tickets", "Seats", "Revenue (Rs.)", "Revenue chart"}, rows);
        pause();
    }

    static void adminBookings() {
        banner("ALL BOOKINGS");
        table(TICKET_COLS, ticketRows(t -> true));
        String id = ask("\n  Ticket ID to cancel (Enter to go back): ");
        if (id.isEmpty()) return;
        if (find(TICKETS, id) == null) { System.out.println("  ! No such ticket."); pause(); return; }
        if (confirm("  Cancel ticket " + id + "?")) {
            db.get(TICKETS).removeIf(t -> t[0].equals(id));
            save();
            System.out.println("  + Ticket cancelled.");
        }
        pause();
    }

    static void adminIssues() {
        banner("DRIVER ISSUE REPORTS");
        List<String[]> rows = new ArrayList<>();
        for (String[] i : db.get(ISSUES)) rows.add(new String[]{i[0], name(DRIVERS, i[1]), i[2], i[3], i[4]});
        table(new String[]{"ID", "Driver", "Message", "Reported At", "Status"}, rows);
        String id = ask("\n  Issue ID to mark Resolved (Enter to go back): ");
        if (id.isEmpty()) return;
        String[] i = find(ISSUES, id);
        if (i == null) System.out.println("  ! No such issue.");
        else { i[4] = "Resolved"; save(); System.out.println("  + Marked as Resolved."); }
        pause();
    }

    static void adminPortal() {
        while (true) {
            switch (menu("ADMIN PORTAL  -  " + currentUser[4], "Logout", "Dashboard", "Manage Routes", "Manage Vehicles",
                    "Manage Drivers", "Manage Schedules", "Bookings", "Driver Issues", "Manage Users", "My Profile")) {
                case 1: adminDashboard(); break;
                case 2: crud("MANAGE ROUTES", ROUTES,
                        new String[]{"Route Name", "Source", "Destination", "Distance (km)", "Fare per seat (Rs.)"},
                        new String[]{"text", "text", "text", "num", "num"}); break;
                case 3: crud("MANAGE VEHICLES", VEHICLES, new String[]{"Reg. Number", "Type", "Capacity"},
                        new String[]{"text", "pick:Bus|Metro|Tram|Van", "num"}); break;
                case 4: crud("MANAGE DRIVERS", DRIVERS, new String[]{"Name", "Phone", "License No.", "Assigned Vehicle"},
                        new String[]{"text", "text", "text", "ref:" + VEHICLES}); break;
                case 5: crud("MANAGE SCHEDULES", SCHEDULES, new String[]{"Route", "Vehicle", "Departure (HH:mm)", "Status"},
                        new String[]{"ref:" + ROUTES, "ref:" + VEHICLES, "time", "pick:Scheduled|Running|Delayed|Completed"}); break;
                case 6: adminBookings(); break;
                case 7: adminIssues(); break;
                case 8: crud("MANAGE USERS", USERS, new String[]{"Username", "Password", "Role", "Full Name", "Driver ID"},
                        new String[]{"text", "text", "pick:Admin|Driver|Passenger", "text", "opt"}); break;
                case 9: profile(); break;
                default: return;
            }
        }
    }

    // ============================================================
    //  DRIVER
    // ============================================================
    static List<String[]> myTrips() {
        List<String[]> rows = new ArrayList<>();
        String[] d = myDriver();
        for (String[] s : db.get(SCHEDULES)) {
            if (d == null || !s[2].equals(d[4])) continue;
            String[] r = find(ROUTES, s[1]);
            rows.add(new String[]{s[0], r == null ? "?" : r[1], r == null ? "?" : r[2] + " -> " + r[3], s[3], s[4],
                    bookedSeats(s[0]) + " / " + capacity(s[2])});
        }
        return rows;
    }

    static void driverHome() {
        banner("DRIVER HOME");
        String[] d = myDriver(), v = find(VEHICLES, d[4]);
        System.out.println("  Name    : " + d[1]);
        System.out.println("  Phone   : " + d[2]);
        System.out.println("  License : " + d[3]);
        System.out.println("  Vehicle : " + (v == null ? "-" : v[1] + " (" + v[2] + ", capacity " + v[3] + ")"));
        int seats = 0, open = 0;
        List<String[]> trips = myTrips();
        for (String[] t : trips) seats += bookedSeats(t[0]);
        for (String[] i : db.get(ISSUES)) if (i[1].equals(d[0]) && i[4].equals("Open")) open++;
        System.out.println("  Trips: " + trips.size() + "   Seats booked: " + seats + "   Open issues: " + open);
        pause();
    }

    static void driverUpdateTrip() {
        banner("UPDATE TRIP STATUS");
        table(new String[]{"ID", "Route", "From -> To", "Departure", "Status", "Booked/Cap"}, myTrips());
        String id = ask("\n  Trip ID (Enter to go back): ");
        if (id.isEmpty()) return;
        String[] s = find(SCHEDULES, id), d = myDriver();
        if (s == null || !s[2].equals(d[4])) { System.out.println("  ! That is not one of your trips."); pause(); return; }
        if (s[4].equals("Completed")) { System.out.println("  ! Trip already completed."); pause(); return; }
        System.out.println("  1. Start Trip (Running)\n  2. Mark Delayed\n  3. Complete Trip");
        int c = askInt("  Choose: ", 1, 3);
        s[4] = c == 1 ? "Running" : c == 2 ? "Delayed" : "Completed";
        save();
        System.out.println("  + Trip " + id + " is now " + s[4] + ".");
        pause();
    }

    static void driverIssues() {
        banner("REPORT / VIEW ISSUES");
        String[] d = myDriver();
        List<String[]> rows = new ArrayList<>();
        for (String[] i : db.get(ISSUES)) if (i[1].equals(d[0])) rows.add(new String[]{i[0], i[2], i[3], i[4]});
        table(new String[]{"ID", "Message", "Reported At", "Status"}, rows);
        if (confirm("\n  Report a new issue?")) {
            String m = ask("  Describe the issue: ");
            if (!m.isEmpty()) {
                db.get(ISSUES).add(new String[]{nextId(ISSUES), d[0], m, now(), "Open"});
                save();
                System.out.println("  + Issue reported to admin.");
            }
        }
        pause();
    }

    static void driverPortal() {
        while (true) {
            switch (menu("DRIVER PORTAL  -  " + currentUser[4], "Logout", "My Home", "My Trips", "Update Trip Status",
                    "Passenger List", "Report / View Issues", "My Profile")) {
                case 1: driverHome(); break;
                case 2: banner("MY TRIPS");
                    table(new String[]{"ID", "Route", "From -> To", "Departure", "Status", "Booked/Cap"}, myTrips()); pause(); break;
                case 3: driverUpdateTrip(); break;
                case 4: banner("PASSENGER LIST");
                    String[] d = myDriver();
                    table(TICKET_COLS, ticketRows(t -> {
                        String[] s = find(SCHEDULES, t[2]);
                        return s != null && s[2].equals(d[4]);
                    })); pause(); break;
                case 5: driverIssues(); break;
                case 6: profile(); break;
                default: return;
            }
        }
    }

    // ============================================================
    //  PASSENGER
    // ============================================================
    static void passengerBook() {
        banner("BOOK A TICKET");
        String kw = ask("  Search route/city (Enter = show all): ");
        List<String[]> rows = timetableRows(true, kw);
        table(TIMETABLE_COLS, rows);
        if (rows.isEmpty()) { pause(); return; }
        String sid = ask("\n  Enter Trip ID to book (Enter to cancel): ");
        if (sid.isEmpty()) return;
        boolean listed = false;
        for (String[] r : rows) if (r[0].equals(sid)) listed = true;
        if (!listed) { System.out.println("  ! Choose an ID from the list above."); pause(); return; }
        int left = availableSeats(sid);
        if (left <= 0) { System.out.println("  ! Trip is full."); pause(); return; }
        String pname = ask("  Passenger name [" + currentUser[4] + "]: ");
        if (pname.isEmpty()) pname = currentUser[4];
        int seats = askInt("  Seats (1-" + Math.min(left, 20) + "): ", 1, Math.min(left, 20));
        double amount = fareFor(sid, seats);
        System.out.println("  Fare for " + seats + " seat(s): Rs. " + fmt(amount));
        if (!confirm("  Confirm booking?")) return;
        String id = nextId(TICKETS), when = now();
        db.get(TICKETS).add(new String[]{id, pname, sid, String.valueOf(seats), fmt(amount), when, currentUser[0]});
        save();
        System.out.println();
        System.out.println("  ======== TICKET #" + id + " ========");
        System.out.println("  Passenger : " + pname);
        System.out.println("  Trip      : " + label(SCHEDULES, sid));
        System.out.println("  Seats     : " + seats);
        System.out.println("  Amount    : Rs. " + fmt(amount));
        System.out.println("  Booked at : " + when);
        System.out.println("  ===============================");
        pause();
    }

    static void passengerTickets(boolean allowCancel) {
        banner(allowCancel ? "CANCEL A TICKET" : "MY TICKETS");
        table(TICKET_COLS, ticketRows(t -> t[6].equals(currentUser[0])));
        if (!allowCancel) { pause(); return; }
        String id = ask("\n  Ticket ID to cancel (Enter to go back): ");
        if (id.isEmpty()) return;
        String[] t = find(TICKETS, id);
        if (t == null || !t[6].equals(currentUser[0])) { System.out.println("  ! That is not your ticket."); pause(); return; }
        String st = tripStatus(t[2]);
        if (!st.equals("Scheduled") && !st.equals("Delayed")) { System.out.println("  ! Only upcoming trips can be cancelled."); pause(); return; }
        if (confirm("  Cancel ticket " + id + "?")) {
            db.get(TICKETS).removeIf(x -> x[0].equals(id));
            save();
            System.out.println("  + Ticket cancelled.");
        }
        pause();
    }

    static void passengerPortal() {
        while (true) {
            switch (menu("PASSENGER PORTAL  -  " + currentUser[4], "Logout", "Timetable", "Search & Book Ticket",
                    "My Tickets", "Cancel Ticket", "My Profile")) {
                case 1: banner("TIMETABLE"); table(TIMETABLE_COLS, timetableRows(false, "")); pause(); break;
                case 2: passengerBook(); break;
                case 3: passengerTickets(false); break;
                case 4: passengerTickets(true); break;
                case 5: profile(); break;
                default: return;
            }
        }
    }

    // ============================================================
    //  PROFILE, LOGIN, MAIN
    // ============================================================
    static void profile() {
        banner("MY PROFILE");
        System.out.println("  Username : " + currentUser[1]);
        System.out.println("  Name     : " + currentUser[4]);
        System.out.println("  Role     : " + currentUser[3]);
        String n = ask("\n  New full name (Enter to keep): ");
        if (!n.isEmpty()) currentUser[4] = n;
        String p = ask("  New password (Enter to keep): ");
        if (!p.isEmpty()) {
            if (p.length() < 4) System.out.println("  ! Password must be at least 4 characters (not changed).");
            else currentUser[2] = p;
        }
        save();
        System.out.println("  + Profile saved.");
        pause();
    }

    static boolean login() {
        banner("LOGIN");
        for (int attempt = 1; attempt <= 3; attempt++) {
            String u = ask("  Username: "), p = ask("  Password: ");
            for (String[] row : db.get(USERS)) {
                if (row[1].equals(u) && row[2].equals(p)) {
                    currentUser = row;
                    if (row[3].equals("Driver") && myDriver() == null) {
                        System.out.println("  ! Driver account is not linked to a driver profile.");
                        currentUser = null;
                        return false;
                    }
                    System.out.println("  Welcome, " + row[4] + " (" + row[3] + ")");
                    return true;
                }
            }
            System.out.println("  ! Invalid username or password (" + attempt + "/3)");
        }
        return false;
    }

    static void register() {
        banner("PASSENGER REGISTRATION");
        String name = ask("  Full name: "), user = ask("  Username: "), pw = ask("  Password (min 4 chars): ");
        if (name.isEmpty() || user.isEmpty() || pw.length() < 4) { System.out.println("  ! Fill all fields (password min 4 characters)."); return; }
        for (String[] x : db.get(USERS))
            if (x[1].equalsIgnoreCase(user)) { System.out.println("  ! Username already taken."); return; }
        db.get(USERS).add(new String[]{nextId(USERS), user, pw, "Passenger", name, ""});
        save();
        System.out.println("  + Account created. You can now log in.");
    }

    public static void main(String[] args) {
        load();
        while (true) {
            System.out.println();
            System.out.println();
            System.out.println("  ****************************************************");
            System.out.println("  *                      P T M S                     *");
            System.out.println("  *        Public Transport Management System        *");
            System.out.println("  ****************************************************");
            switch (menu("MAIN MENU", "Exit", "Login", "Register as Passenger")) {
                case 1:
                    if (login()) {
                        String role = currentUser[3];
                        if (role.equals("Admin")) adminPortal();
                        else if (role.equals("Driver")) driverPortal();
                        else passengerPortal();
                        currentUser = null;
                        System.out.println("\n  Logged out.");
                    }
                    break;
                case 2: register(); break;
                default: System.out.println("\n  Thank you for using PTMS. Goodbye!"); return;
            }
        }
    }
}
