import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Path2D;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.function.*;
import java.util.regex.Pattern;

/**
 * Public Transport Management System - Java Swing, role based (Admin / Driver / Passenger)
 *
 * Demo logins : admin / admin123   |   driver1 / driver123   |   passenger / pass123
 * Storage     : transport_data_v2.dat (auto-saved on every change)
 * Alerts      : drivers and passengers can send alerts; the admin gets a live notification
 *               (bell + sidebar badge + pop-up) even when the alert comes from another running window.
 * Compile     : javac PublicTransportApp.java
 * Run         : java PublicTransportApp
 */
public class PublicTransportApp {

    // ============================================================
    //  THEME
    // ============================================================
    static final Color DARK = new Color(15, 23, 42), PRIMARY = new Color(37, 99, 235),
            ACCENT = new Color(16, 185, 129), WARN = new Color(245, 158, 11),
            DANGER = new Color(239, 68, 68), PURPLE = new Color(139, 92, 246),
            BG = new Color(241, 245, 249), MUTED = new Color(100, 116, 139),
            LINE = new Color(226, 232, 240);
    static final Color[] PALETTE = {PRIMARY, ACCENT, WARN, PURPLE, DANGER};

    static Border pad(int t, int l, int b, int r) { return BorderFactory.createEmptyBorder(t, l, b, r); }
    static Border pad(int a) { return pad(a, a, a, a); }
    static Border cardBorder() {
        return BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE), pad(14));
    }

    static JLabel pageTitle(String t) {
        JLabel l = new JLabel(t);
        l.setFont(l.getFont().deriveFont(Font.BOLD, 22f));
        l.setForeground(DARK);
        return l;
    }

    static JLabel lbl(String t) {
        JLabel l = new JLabel(t);
        l.setForeground(MUTED);
        return l;
    }

    static void style(JTextComponent c) {
        c.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225)), pad(6, 8, 6, 8)));
    }

    static JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(BorderFactory.createLineBorder(LINE));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    static void onChange(JTextComponent tf, Runnable r) {
        tf.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { r.run(); }
            public void removeUpdate(DocumentEvent e) { r.run(); }
            public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }

    static void error(Component c, String msg) {
        JOptionPane.showMessageDialog(c, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    static void info(Component c, String msg) {
        JOptionPane.showMessageDialog(c, msg, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    static String fmt(double d) { return String.format("%.2f", d); }

    // ---------- rounded button ----------
    static class RoundButton extends JButton {
        final Color base; boolean hover;
        RoundButton(String t, Color c) {
            super(t);
            base = c;
            setForeground(Color.WHITE);
            setFont(getFont().deriveFont(Font.BOLD));
            setContentAreaFilled(false); setFocusPainted(false); setBorderPainted(false); setOpaque(false);
            setBorder(pad(9, 20, 9, 20));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getModel().isPressed() ? base.darker().darker() : hover ? base.darker() : base);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ---------- sidebar button ----------
    static class NavButton extends JButton {
        boolean active, hover;
        int badge;
        NavButton(String t) {
            super(t);
            setHorizontalAlignment(SwingConstants.LEFT);
            setForeground(new Color(203, 213, 225));
            setFont(getFont().deriveFont(Font.BOLD, 14f));
            setContentAreaFilled(false); setFocusPainted(false); setBorderPainted(false); setOpaque(false);
            setBorder(pad(11, 16, 11, 16));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { hover = true; repaint(); }
                public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }
        void setActive(boolean a) { active = a; setForeground(a ? Color.WHITE : new Color(203, 213, 225)); repaint(); }
        @Override protected void paintComponent(Graphics g) {
            if (active || hover) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(active ? PRIMARY : new Color(30, 41, 59));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
            super.paintComponent(g);
            if (badge > 0) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                String txt = badge > 99 ? "99+" : String.valueOf(badge);
                g2.setFont(getFont().deriveFont(Font.BOLD, 11f));
                int w = Math.max(22, g2.getFontMetrics().stringWidth(txt) + 12), h = 20;
                int x = getWidth() - w - 10, y = (getHeight() - h) / 2;
                g2.setColor(DANGER);
                g2.fillRoundRect(x, y, w, h, h, h);
                g2.setColor(Color.WHITE);
                g2.drawString(txt, x + (w - g2.getFontMetrics().stringWidth(txt)) / 2,
                        y + (h + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2);
                g2.dispose();
            }
        }
        void setBadge(int n) { badge = n; repaint(); }
    }

    // ---------- bell icon with unread counter (admin header) ----------
    static class BellButton extends JButton {
        int count;
        BellButton() {
            setPreferredSize(new Dimension(44, 38));
            setContentAreaFilled(false); setFocusPainted(false); setBorderPainted(false); setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("No new alerts");
        }
        void setCount(int n) {
            count = n;
            setToolTipText(n == 0 ? "No new alerts" : n + " new alert(s) - click to open");
            repaint();
        }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cx = getWidth() / 2 - 2;
            g2.setColor(count > 0 ? WARN : MUTED);
            Path2D body = new Path2D.Double();
            body.moveTo(cx - 9, 24);
            body.curveTo(cx - 9, 16, cx - 8, 9, cx, 9);
            body.curveTo(cx + 8, 9, cx + 9, 16, cx + 9, 24);
            body.closePath();
            g2.fill(body);
            g2.fillRoundRect(cx - 12, 23, 24, 4, 4, 4);
            g2.fillOval(cx - 3, 28, 6, 6);
            g2.fillOval(cx - 2, 6, 4, 4);
            if (count > 0) {
                String txt = count > 99 ? "99+" : String.valueOf(count);
                g2.setFont(getFont().deriveFont(Font.BOLD, 10f));
                int w = Math.max(17, g2.getFontMetrics().stringWidth(txt) + 9), h = 17;
                int x = cx + 4, y = 1;
                g2.setColor(DANGER);
                g2.fillRoundRect(x, y, w, h, h, h);
                g2.setColor(Color.WHITE);
                g2.drawString(txt, x + (w - g2.getFontMetrics().stringWidth(txt)) / 2,
                        y + (h + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2);
            }
            g2.dispose();
        }
    }

    // ---------- pop-up notification (toast) in the bottom-right corner of the window ----------
    static int activeToasts = 0;

    static String esc(String t) {
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    static void toast(JFrame owner, String title, String text, Color accent) {
        if (owner == null || !owner.isShowing()) return;
        if (text.length() > 140) text = text.substring(0, 137) + "...";
        JWindow w = new JWindow(owner);
        w.setFocusableWindowState(false);
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setBackground(DARK);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 6, 0, 0, accent), pad(12, 14, 12, 16)));
        JLabel t = new JLabel(title);
        t.setForeground(Color.WHITE);
        t.setFont(t.getFont().deriveFont(Font.BOLD, 14f));
        JLabel m = new JLabel("<html><body style='width:270px'>" + esc(text) + "</body></html>");
        m.setForeground(new Color(203, 213, 225));
        p.add(t, BorderLayout.NORTH);
        p.add(m, BorderLayout.CENTER);
        w.setContentPane(p);
        w.pack();
        Point loc = owner.getLocationOnScreen();
        int slot = activeToasts++;
        w.setLocation(loc.x + owner.getWidth() - w.getWidth() - 24,
                loc.y + owner.getHeight() - w.getHeight() - 36 - slot * (w.getHeight() + 10));
        w.setAlwaysOnTop(true);
        w.setVisible(true);
        final boolean[] closed = {false};
        Runnable close = () -> { if (!closed[0]) { closed[0] = true; activeToasts = Math.max(0, activeToasts - 1); w.dispose(); } };
        p.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { close.run(); } });
        javax.swing.Timer tm = new javax.swing.Timer(7000, e -> close.run());
        tm.setRepeats(false);
        tm.start();
    }

    static Color priorityColor(String pr) {
        return pr.equals("Emergency") ? DANGER : pr.equals("High") ? WARN : PRIMARY;
    }

    // ---------- styled tables ----------
    static DefaultTableModel readOnlyModel(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    static JTable makeTable(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(30);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setSelectionBackground(new Color(219, 234, 254));
        t.setSelectionForeground(DARK);
        t.setAutoCreateRowSorter(true);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setPreferredSize(new Dimension(0, 34));
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(tb, v, s, f, r, c);
                setOpaque(true);
                setBackground(PRIMARY);
                setForeground(Color.WHITE);
                setFont(getFont().deriveFont(Font.BOLD));
                setBorder(pad(0, 10, 0, 10));
                return this;
            }
        });
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                super.getTableCellRendererComponent(tb, v, s, f, r, c);
                setBorder(pad(0, 10, 0, 10));
                if (!s) setBackground(r % 2 == 0 ? Color.WHITE : new Color(248, 250, 252));
                String str = String.valueOf(v);
                Color fg = DARK;
                switch (str) {
                    case "Running": case "Resolved": fg = ACCENT.darker(); break;
                    case "Delayed": case "Seen": case "High": fg = WARN.darker(); break;
                    case "Open": case "New": case "Emergency": fg = DANGER; break;
                    case "Completed": fg = MUTED; break;
                    default: break;
                }
                setForeground(fg);
                setFont(getFont().deriveFont(fg == DARK ? Font.PLAIN : Font.BOLD));
                return this;
            }
        });
        return t;
    }

    @SuppressWarnings("unchecked")
    static void attachSearch(JTable t, JTextField tf) {
        onChange(tf, () -> {
            TableRowSorter<TableModel> s = (TableRowSorter<TableModel>) t.getRowSorter();
            String q = tf.getText().trim();
            s.setRowFilter(q.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(q)));
        });
    }

    // ============================================================
    //  DATA LAYER
    // ============================================================
    static String FILE = "transport_data_v2.dat";
    static final String ROUTES = "routes", VEHICLES = "vehicles", DRIVERS = "drivers",
            SCHEDULES = "schedules", TICKETS = "tickets", USERS = "users", ISSUES = "issues", ALERTS = "alerts";
    static final String[] TABLES = {ROUTES, VEHICLES, DRIVERS, SCHEDULES, TICKETS, USERS, ISSUES, ALERTS};

    // routes    : id, name, source, destination, distanceKm, fare
    // vehicles  : id, regNo, type, capacity
    // drivers   : id, name, phone, license, vehicleId
    // schedules : id, routeId, vehicleId, departure, status
    // tickets   : id, passenger, scheduleId, seats, amount, bookedAt, userId
    // users     : id, username, password, role, fullName, driverId
    // issues    : (old format, converted into alerts automatically)
    // alerts    : id, senderUserId, senderRole, senderName, priority, message, time, status (New/Seen/Resolved)
    static Map<String, List<String[]>> db = new HashMap<>();
    static String[] currentUser;

    static final Object[][] REFS = {
            {SCHEDULES, 1, ROUTES}, {SCHEDULES, 2, VEHICLES}, {DRIVERS, 4, VEHICLES},
            {TICKETS, 2, SCHEDULES}, {ISSUES, 1, DRIVERS}
    };

    static String lastSig = "";

    static String fileSig() {
        File f = new File(FILE);
        return f.lastModified() + ":" + f.length();
    }

    @SuppressWarnings("unchecked")
    static boolean readFromDisk() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(FILE))) {
            db = (Map<String, List<String[]>>) in.readObject();
            lastSig = fileSig();
            return true;
        } catch (Exception e) { return false; }
    }

    static void load() {
        if (new File(FILE).exists() && !readFromDisk()) db = new HashMap<>();
        ensureTables();
    }

    /** Makes sure every table exists, sample data/admin are present and old issue reports become alerts. */
    static void ensureTables() {
        for (String t : TABLES) db.computeIfAbsent(t, k -> new ArrayList<>());
        boolean changed = false;
        if (db.get(ROUTES).isEmpty()) seed();
        boolean hasAdmin = false;
        for (String[] u : db.get(USERS)) if (u[3].equals("Admin")) hasAdmin = true;
        if (!hasAdmin) {
            db.get(USERS).add(new String[]{nextId(USERS), "admin", "admin123", "Admin", "Administrator", ""});
            changed = true;
        }
        for (String[] i : db.get(ISSUES)) {                     // i: id, driverId, message, time, status
            String uid = "0";
            for (String[] u : db.get(USERS)) if (u[3].equals("Driver") && u[5].equals(i[1])) { uid = u[0]; break; }
            db.get(ALERTS).add(new String[]{nextId(ALERTS), uid, "Driver", name(DRIVERS, i[1]), "Normal", i[2], i[3],
                    i[4].equals("Resolved") ? "Resolved" : "New"});
            changed = true;
        }
        db.get(ISSUES).clear();
        if (changed) save();
    }

    /** Pulls in changes saved by other running windows (e.g. a driver sending an alert). True if data changed. */
    static boolean syncFromDisk() {
        if (!new File(FILE).exists() || fileSig().equals(lastSig)) return false;
        if (!readFromDisk()) return false;
        ensureTables();
        if (currentUser != null) {
            String[] u = find(USERS, currentUser[0]);
            if (u != null) currentUser = u;
        }
        return true;
    }

    static boolean saveWarned = false;

    /**
     * Saves quietly and safely: the data is written to a private temp file and swapped in. If Windows (antivirus,
     * OneDrive, another open window) briefly locks a file, it retries; as a last resort it overwrites the file
     * directly. The user only sees a message if every attempt fails.
     */
    static void save() {
        byte[] data;
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            try (ObjectOutputStream out = new ObjectOutputStream(bos)) { out.writeObject(db); }
            data = bos.toByteArray();
        } catch (IOException e) { return; }
        java.nio.file.Path target = new File(FILE).getAbsoluteFile().toPath();
        java.nio.file.Path tmp = target.resolveSibling(target.getFileName() + "." + ProcessHandle.current().pid() + ".tmp");
        for (int attempt = 0; attempt < 10; attempt++) {
            try {
                java.nio.file.Files.write(tmp, data);
                try {
                    java.nio.file.Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                            java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException m) {
                    java.nio.file.Files.move(tmp, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                lastSig = fileSig();
                return;
            } catch (IOException e) {
                try { java.nio.file.Files.deleteIfExists(tmp); } catch (IOException ignored) { }
                try {                                              // fallback: overwrite the data file directly
                    java.nio.file.Files.write(target, data);
                    lastSig = fileSig();
                    return;
                } catch (IOException e2) { /* try again after a short pause */ }
            }
            try { Thread.sleep(120); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
        }
        if (!saveWarned) {
            saveWarned = true;
            error(null, "Could not save data to:\n" + target + "\n\nMove the PTMS folder to a normal folder "
                    + "(for example Documents or Desktop), not inside a zip file or Program Files.");
        }
    }

    /** Uses the current folder for the data file; if that folder is not writable, uses a PTMS folder in the user's home. */
    static void chooseDataFile() {
        File dir = new File(".").getAbsoluteFile();
        boolean writable;
        try {
            java.nio.file.Path probe = java.nio.file.Files.createTempFile(dir.toPath(), "ptms", ".probe");
            java.nio.file.Files.deleteIfExists(probe);
            writable = true;
        } catch (IOException | RuntimeException e) { writable = false; }
        if (writable) return;
        File home = new File(System.getProperty("user.home"), "PTMS");
        home.mkdirs();
        if (!home.isDirectory() || !home.canWrite()) {                  // no usable home folder: use the temp folder
            home = new File(System.getProperty("java.io.tmpdir"), "PTMS");
            home.mkdirs();
        }
        File target = new File(home, new File(FILE).getName());
        File old = new File(FILE);
        if (old.exists() && !target.exists()) {
            try { java.nio.file.Files.copy(old.toPath(), target.toPath()); } catch (IOException ignored) { }
        }
        FILE = target.getPath();
    }

    // ---------- alerts ----------
    static Runnable alertHook = () -> { };       // admin window sets this to refresh its bell / badge

    static int newAlertCount() {
        int n = 0;
        for (String[] a : db.get(ALERTS)) if (a[7].equals("New")) n++;
        return n;
    }

    static int maxAlertId() {
        int m = 0;
        for (String[] a : db.get(ALERTS)) m = Math.max(m, Integer.parseInt(a[0]));
        return m;
    }

    /** Sends an alert from the logged-in driver/passenger to the admin. */
    static void sendAlert(String priority, String message) {
        syncFromDisk();
        db.get(ALERTS).add(new String[]{nextId(ALERTS), currentUser[0], currentUser[3], currentUser[4], priority, message,
                new SimpleDateFormat("dd-MM-yyyy HH:mm").format(new Date()), "New"});
        save();
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

    static double totalRevenue() {
        double d = 0;
        for (String[] t : db.get(TICKETS)) d += Double.parseDouble(t[4]);
        return d;
    }

    static Map<String, double[]> routeStats() {
        Map<String, double[]> m = new LinkedHashMap<>();
        for (String[] r : db.get(ROUTES)) m.put(r[0], new double[3]);
        for (String[] t : db.get(TICKETS)) {
            String[] s = find(SCHEDULES, t[2]);
            if (s != null && m.containsKey(s[1])) {
                double[] a = m.get(s[1]);
                a[0]++; a[1] += Integer.parseInt(t[3]); a[2] += Double.parseDouble(t[4]);
            }
        }
        return m;
    }

    static List<Object[]> ticketRows(Predicate<String[]> p) {
        List<Object[]> rows = new ArrayList<>();
        for (String[] t : db.get(TICKETS))
            if (p.test(t)) rows.add(new Object[]{t[0], label(SCHEDULES, t[2]), t[1], t[3], t[4], t[5], tripStatus(t[2])});
        return rows;
    }

    static List<Object[]> timetableRows() {
        List<Object[]> rows = new ArrayList<>();
        for (String[] s : db.get(SCHEDULES)) {
            String[] r = find(ROUTES, s[1]), v = find(VEHICLES, s[2]);
            rows.add(new Object[]{s[0], r == null ? "?" : r[1], r == null ? "?" : r[2], r == null ? "?" : r[3],
                    s[3], v == null ? "?" : v[1] + " (" + v[2] + ")", r == null ? "?" : r[5],
                    availableSeats(s[0]), s[4]});
        }
        return rows;
    }

    static final String[] TICKET_COLS = {"Ticket", "Trip", "Passenger", "Seats", "Amount (Rs.)", "Booked At", "Trip Status"};
    static final String[] TIMETABLE_COLS = {"ID", "Route", "From", "To", "Departure", "Vehicle", "Fare (Rs.)", "Seats Left", "Status"};

    // ============================================================
    //  REUSABLE PANELS
    // ============================================================
    interface Refreshable { void refresh(); }

    // ---------- read-only table page with search + action buttons ----------
    static class ViewPanel extends JPanel implements Refreshable {
        final DefaultTableModel model;
        final JTable jt;
        final Supplier<List<Object[]>> src;
        final JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        final JTextField search = new JTextField(16);

        ViewPanel(String title, String[] cols, Supplier<List<Object[]>> src) {
            this.src = src;
            setLayout(new BorderLayout(0, 12));
            setBackground(BG);
            setBorder(pad(18));
            model = readOnlyModel(cols);
            jt = makeTable(model);
            style(search);
            attachSearch(jt, search);
            tools.setOpaque(false);
            tools.add(lbl("Search:"));
            tools.add(search);
            JPanel top = new JPanel(new BorderLayout(0, 10));
            top.setOpaque(false);
            top.add(pageTitle(title), BorderLayout.NORTH);
            top.add(tools, BorderLayout.CENTER);
            add(top, BorderLayout.NORTH);
            add(scroll(jt), BorderLayout.CENTER);
            refresh();
        }

        ViewPanel btn(String text, Color c, Consumer<ViewPanel> action) {
            RoundButton b = new RoundButton(text, c);
            b.addActionListener(e -> { syncFromDisk(); action.accept(this); refresh(); });
            tools.add(b);
            return this;
        }

        String selected(int col) {
            int r = jt.getSelectedRow();
            return r < 0 ? null : String.valueOf(model.getValueAt(jt.convertRowIndexToModel(r), col));
        }

        public void refresh() {
            model.setRowCount(0);
            for (Object[] r : src.get()) model.addRow(r);
        }
    }

    // ---------- stat cards + extra content ----------
    static JPanel statCard(String title, JLabel val, Color c) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createLineBorder(LINE));
        JPanel strip = new JPanel();
        strip.setBackground(c);
        strip.setPreferredSize(new Dimension(6, 0));
        val.setFont(val.getFont().deriveFont(Font.BOLD, 28f));
        val.setForeground(DARK);
        JPanel in = new JPanel(new BorderLayout());
        in.setOpaque(false);
        in.setBorder(pad(14, 16, 14, 16));
        in.add(val, BorderLayout.CENTER);
        in.add(lbl(title), BorderLayout.SOUTH);
        p.add(strip, BorderLayout.WEST);
        p.add(in, BorderLayout.CENTER);
        return p;
    }

    static class StatsPanel extends JPanel implements Refreshable {
        final JLabel[] vals;
        final Supplier<String[]> src;
        final Runnable extra;

        StatsPanel(String title, int cols, String[] titles, Color[] colors, Supplier<String[]> src,
                   JComponent center, Runnable extra) {
            this.src = src;
            this.extra = extra;
            setLayout(new BorderLayout(0, 14));
            setBackground(BG);
            setBorder(pad(18));
            vals = new JLabel[titles.length];
            JPanel grid = new JPanel(new GridLayout(0, cols, 12, 12));
            grid.setOpaque(false);
            for (int i = 0; i < titles.length; i++) {
                vals[i] = new JLabel("0");
                grid.add(statCard(titles[i], vals[i], colors[i % colors.length]));
            }
            JPanel top = new JPanel(new BorderLayout(0, 12));
            top.setOpaque(false);
            top.add(pageTitle(title), BorderLayout.NORTH);
            top.add(grid, BorderLayout.CENTER);
            add(top, BorderLayout.NORTH);
            add(center, BorderLayout.CENTER);
            refresh();
        }

        public void refresh() {
            String[] v = src.get();
            for (int i = 0; i < vals.length; i++) vals[i].setText(v[i]);
            extra.run();
        }
    }

    static class BarChart extends JPanel {
        final Map<String, Double> data = new LinkedHashMap<>();
        BarChart() {
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(LINE));
            setPreferredSize(new Dimension(380, 220));
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(DARK);
            g2.setFont(getFont().deriveFont(Font.BOLD, 14f));
            g2.drawString("Revenue by Route (Rs.)", 16, 26);
            if (data.isEmpty()) return;
            double max = 1;
            for (double v : data.values()) max = Math.max(max, v);
            int slot = (w - 40) / data.size(), i = 0;
            g2.setFont(getFont().deriveFont(11f));
            for (Map.Entry<String, Double> e : data.entrySet()) {
                int bh = (int) ((h - 100) * (e.getValue() / max));
                int x = 20 + i * slot + slot / 6, bw = slot * 2 / 3;
                g2.setColor(PALETTE[i % PALETTE.length]);
                g2.fillRoundRect(x, h - 40 - bh, bw, Math.max(bh, 2), 8, 8);
                g2.setColor(MUTED);
                g2.drawString(e.getKey().split(" ")[0], x, h - 22);
                g2.drawString(String.format("%.0f", e.getValue()), x, h - 44 - bh);
                i++;
            }
        }
    }

    // ---------- generic CRUD page (admin) ----------
    static final int TEXT = 0, NUM = 1, STATIC = 2, REF = 3, OPT = 4;

    static class Field {
        String name; int kind; String ref; String[] opts;
        Field(String n, int k, String r, String... o) { name = n; kind = k; ref = r; opts = o; }
        static Field text(String n) { return new Field(n, TEXT, null); }
        static Field opt(String n) { return new Field(n, OPT, null); }
        static Field num(String n) { return new Field(n, NUM, null); }
        static Field pick(String n, String... o) { return new Field(n, STATIC, null, o); }
        static Field ref(String n, String t) { return new Field(n, REF, t); }
        boolean isText() { return kind == TEXT || kind == NUM || kind == OPT; }
    }

    static class CrudPanel extends JPanel implements Refreshable {
        final String table; final Field[] f; final DefaultTableModel model;
        final JTable jt; final JComponent[] in;

        @SuppressWarnings("unchecked")
        CrudPanel(String title, String table, Field... f) {
            this.table = table; this.f = f;
            setLayout(new BorderLayout(0, 12));
            setBackground(BG);
            setBorder(pad(18));

            String[] cols = new String[f.length + 1];
            cols[0] = "ID";
            for (int i = 0; i < f.length; i++) cols[i + 1] = f[i].name;
            model = readOnlyModel(cols);
            jt = makeTable(model);

            JPanel form = new JPanel(new GridLayout(0, 4, 14, 10));
            form.setBackground(Color.WHITE);
            form.setBorder(cardBorder());
            in = new JComponent[f.length];
            for (int i = 0; i < f.length; i++) {
                if (f[i].isText()) { JTextField tf = new JTextField(); style(tf); in[i] = tf; }
                else {
                    JComboBox<String> cb = new JComboBox<>();
                    if (f[i].kind == STATIC) for (String o : f[i].opts) cb.addItem(o);
                    in[i] = cb;
                }
                form.add(lbl(f[i].name + (f[i].kind == OPT ? " (optional):" : ":")));
                form.add(in[i]);
            }
            if (f.length % 2 == 1) { form.add(new JLabel()); form.add(new JLabel()); }

            RoundButton add = new RoundButton("Add", ACCENT), upd = new RoundButton("Update", PRIMARY),
                    del = new RoundButton("Delete", DANGER), clr = new RoundButton("Clear", MUTED);
            add.addActionListener(e -> add());
            upd.addActionListener(e -> update());
            del.addActionListener(e -> delete());
            clr.addActionListener(e -> clearForm());
            JTextField search = new JTextField(14);
            style(search);
            attachSearch(jt, search);
            JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            btns.setOpaque(false);
            btns.add(add); btns.add(upd); btns.add(del); btns.add(clr);
            btns.add(Box.createHorizontalStrut(20)); btns.add(lbl("Search:")); btns.add(search);

            JPanel top = new JPanel(new BorderLayout(0, 12));
            top.setOpaque(false);
            top.add(pageTitle(title), BorderLayout.NORTH);
            top.add(form, BorderLayout.CENTER);
            top.add(btns, BorderLayout.SOUTH);
            add(top, BorderLayout.NORTH);
            add(scroll(jt), BorderLayout.CENTER);

            jt.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) fillForm(); });
            refresh();
        }

        @SuppressWarnings("unchecked")
        String getValue(int i) {
            if (f[i].isText()) return ((JTextField) in[i]).getText().trim();
            Object sel = ((JComboBox<String>) in[i]).getSelectedItem();
            if (sel == null) return "";
            return f[i].kind == REF ? sel.toString().split(" - ")[0] : sel.toString();
        }

        @SuppressWarnings("unchecked")
        void setValue(int i, String v) {
            if (f[i].isText()) { ((JTextField) in[i]).setText(v); return; }
            JComboBox<String> cb = (JComboBox<String>) in[i];
            for (int k = 0; k < cb.getItemCount(); k++) {
                String item = cb.getItemAt(k);
                if (f[i].kind == REF ? item.startsWith(v + " - ") : item.equals(v)) { cb.setSelectedIndex(k); return; }
            }
        }

        String[] readForm(String id) {
            String[] row = new String[f.length + 1];
            row[0] = id;
            for (int i = 0; i < f.length; i++) {
                String v = getValue(i);
                if (v.isEmpty() && f[i].kind != OPT) { error(this, f[i].name + " is required."); return null; }
                if (f[i].kind == NUM) {
                    try { if (Double.parseDouble(v) <= 0) throw new NumberFormatException(); }
                    catch (NumberFormatException ex) { error(this, f[i].name + " must be a positive number."); return null; }
                }
                row[i + 1] = v;
            }
            if (table.equals(USERS)) {
                for (String[] u : db.get(USERS))
                    if (u[1].equalsIgnoreCase(row[1]) && !u[0].equals(id)) { error(this, "Username already exists."); return null; }
                if (row[3].equals("Driver") && find(DRIVERS, row[5]) == null) {
                    error(this, "Driver accounts need a valid Driver ID (see Drivers page)."); return null;
                }
            }
            return row;
        }

        void add() {
            syncFromDisk();
            String[] row = readForm(nextId(table));
            if (row == null) return;
            db.get(table).add(row);
            save(); refresh(); clearForm();
        }

        void update() {
            syncFromDisk();
            String id = selectedId();
            if (id == null) { error(this, "Select a row to update."); return; }
            String[] row = readForm(id);
            if (row == null) return;
            List<String[]> list = db.get(table);
            for (int i = 0; i < list.size(); i++) if (list.get(i)[0].equals(id)) list.set(i, row);
            if (table.equals(USERS) && currentUser != null && currentUser[0].equals(id)) currentUser = row;
            save(); refresh();
        }

        void delete() {
            syncFromDisk();
            String id = selectedId();
            if (id == null) { error(this, "Select a row to delete."); return; }
            if (table.equals(USERS) && currentUser[0].equals(id)) { error(this, "You cannot delete your own account."); return; }
            if (isReferenced(table, id)) { error(this, "This record is used by other data. Remove those first."); return; }
            if (JOptionPane.showConfirmDialog(this, "Delete record " + id + "?", "Confirm",
                    JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            db.get(table).removeIf(r -> r[0].equals(id));
            save(); refresh(); clearForm();
        }

        String selectedId() {
            int r = jt.getSelectedRow();
            return r < 0 ? null : model.getValueAt(jt.convertRowIndexToModel(r), 0).toString();
        }

        void fillForm() {
            String id = selectedId();
            if (id == null) return;
            String[] r = find(table, id);
            if (r != null) for (int i = 0; i < f.length; i++) setValue(i, r[i + 1]);
        }

        void clearForm() {
            jt.clearSelection();
            for (int i = 0; i < f.length; i++) {
                if (in[i] instanceof JTextField) ((JTextField) in[i]).setText("");
                else if (((JComboBox<?>) in[i]).getItemCount() > 0) ((JComboBox<?>) in[i]).setSelectedIndex(0);
            }
        }

        @SuppressWarnings("unchecked")
        public void refresh() {
            for (int i = 0; i < f.length; i++) {
                if (f[i].kind != REF) continue;
                JComboBox<String> cb = (JComboBox<String>) in[i];
                Object keep = cb.getSelectedItem();
                cb.removeAllItems();
                for (String[] r : db.get(f[i].ref)) cb.addItem(label(f[i].ref, r[0]));
                if (keep != null) cb.setSelectedItem(keep);
            }
            model.setRowCount(0);
            for (String[] r : db.get(table)) {
                Object[] o = new Object[r.length];
                o[0] = r[0];
                for (int i = 0; i < f.length; i++) {
                    if (f[i].kind == REF) o[i + 1] = label(f[i].ref, r[i + 1]);
                    else o[i + 1] = f[i].name.equals("Password") ? "******" : r[i + 1];
                }
                model.addRow(o);
            }
        }
    }

    // ============================================================
    //  PASSENGER: BOOKING PAGE
    // ============================================================
    static class BookPanel extends JPanel implements Refreshable {
        final JTextField search = new JTextField(), tfName = new JTextField();
        final JComboBox<String> cb = new JComboBox<>();
        final JSpinner sp = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        final JLabel info = new JLabel(" ");
        final ViewPanel tt = new ViewPanel("Available Trips (click a row to select it)", TIMETABLE_COLS,
                PublicTransportApp::timetableRows);
        boolean loading;

        BookPanel() {
            setLayout(new BorderLayout(0, 14));
            setBackground(BG);
            setBorder(pad(18));
            style(search); style(tfName);
            info.setFont(info.getFont().deriveFont(Font.BOLD, 14f));
            info.setForeground(PRIMARY);

            JPanel form = new JPanel(new GridLayout(0, 4, 14, 10));
            form.setBackground(Color.WHITE);
            form.setBorder(cardBorder());
            form.add(lbl("Search route / city:")); form.add(search);
            form.add(lbl("Select trip:")); form.add(cb);
            form.add(lbl("Passenger name:")); form.add(tfName);
            form.add(lbl("Seats:")); form.add(sp);

            RoundButton book = new RoundButton("Confirm Booking", ACCENT);
            book.addActionListener(e -> book());
            JPanel btn = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
            btn.setOpaque(false);
            btn.add(book); btn.add(info);

            JPanel top = new JPanel(new BorderLayout(0, 12));
            top.setOpaque(false);
            top.add(pageTitle("Book a Ticket"), BorderLayout.NORTH);
            top.add(form, BorderLayout.CENTER);
            top.add(btn, BorderLayout.SOUTH);
            add(top, BorderLayout.NORTH);

            tt.setBorder(pad(0));
            add(tt, BorderLayout.CENTER);

            onChange(search, this::reload);
            cb.addActionListener(e -> updateInfo());
            sp.addChangeListener(e -> updateInfo());
            tt.jt.getSelectionModel().addListSelectionListener(e -> {
                String id = tt.selected(0);
                if (e.getValueIsAdjusting() || id == null) return;
                for (int k = 0; k < cb.getItemCount(); k++)
                    if (cb.getItemAt(k).startsWith(id + " - ")) { cb.setSelectedIndex(k); return; }
            });
        }

        String schedId() {
            Object s = cb.getSelectedItem();
            return s == null ? null : s.toString().split(" - ")[0];
        }

        void reload() {
            loading = true;
            Object keep = cb.getSelectedItem();
            cb.removeAllItems();
            String q = search.getText().trim().toLowerCase();
            for (String[] s : db.get(SCHEDULES)) {
                if (s[4].equals("Completed") || s[4].equals("Running")) continue;
                String[] r = find(ROUTES, s[1]);
                String hay = r == null ? "" : (r[1] + " " + r[2] + " " + r[3]).toLowerCase();
                if (q.isEmpty() || hay.contains(q)) cb.addItem(label(SCHEDULES, s[0]));
            }
            if (keep != null) cb.setSelectedItem(keep);
            loading = false;
            updateInfo();
        }

        void updateInfo() {
            if (loading) return;
            String sid = schedId();
            if (sid == null) { info.setText("No matching trips"); return; }
            int seats = (Integer) sp.getValue();
            info.setText("Seats left: " + availableSeats(sid) + "   |   Fare: Rs. " + fmt(fareFor(sid, seats)));
        }

        void book() {
            syncFromDisk();
            String sid = schedId(), pname = tfName.getText().trim();
            int seats = (Integer) sp.getValue();
            if (sid == null) { error(this, "Select a trip first."); return; }
            if (pname.isEmpty()) { error(this, "Enter passenger name."); return; }
            if (seats > availableSeats(sid)) { error(this, "Only " + availableSeats(sid) + " seat(s) left on this trip."); return; }
            double amount = fareFor(sid, seats);
            String when = new SimpleDateFormat("dd-MM-yyyy HH:mm").format(new Date());
            String id = nextId(TICKETS);
            db.get(TICKETS).add(new String[]{id, pname, sid, String.valueOf(seats), fmt(amount), when, currentUser[0]});
            save();
            info("Ticket #" + id + " confirmed!\n\nPassenger : " + pname + "\nTrip      : " + label(SCHEDULES, sid)
                    + "\nSeats     : " + seats + "\nAmount    : Rs. " + fmt(amount) + "\nBooked at : " + when);
            sp.setValue(1);
            refresh();
        }

        void info(String m) { PublicTransportApp.info(this, m); }

        public void refresh() {
            tt.refresh();
            reload();
            if (tfName.getText().isEmpty()) tfName.setText(currentUser[4]);
        }
    }

    // ============================================================
    //  PROFILE PAGE (all roles)
    // ============================================================
    static class ProfilePanel extends JPanel implements Refreshable {
        final JTextField tfName = new JTextField(), tfUser = new JTextField();
        final JPasswordField pw = new JPasswordField();

        ProfilePanel() {
            setLayout(new BorderLayout(0, 14));
            setBackground(BG);
            setBorder(pad(18));
            style(tfName); style(tfUser); style(pw);
            tfUser.setEditable(false);
            JPanel form = new JPanel(new GridLayout(0, 2, 14, 12));
            form.setBackground(Color.WHITE);
            form.setBorder(cardBorder());
            form.add(lbl("Username:")); form.add(tfUser);
            form.add(lbl("Full name:")); form.add(tfName);
            form.add(lbl("New password (blank = keep):")); form.add(pw);
            RoundButton save = new RoundButton("Save Changes", PRIMARY);
            save.addActionListener(e -> {
                if (tfName.getText().trim().isEmpty()) { error(this, "Name cannot be empty."); return; }
                currentUser[4] = tfName.getText().trim();
                String np = new String(pw.getPassword());
                if (!np.isEmpty()) {
                    if (np.length() < 4) { error(this, "Password must be at least 4 characters."); return; }
                    currentUser[2] = np;
                }
                PublicTransportApp.save();
                pw.setText("");
                info(this, "Profile updated.");
            });
            JPanel box = new JPanel(new BorderLayout(0, 12));
            box.setOpaque(false);
            box.setPreferredSize(new Dimension(520, 220));
            box.add(form, BorderLayout.CENTER);
            JPanel b = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            b.setOpaque(false);
            b.add(save);
            box.add(b, BorderLayout.SOUTH);
            JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            wrap.setOpaque(false);
            wrap.add(box);
            add(pageTitle("My Profile"), BorderLayout.NORTH);
            add(wrap, BorderLayout.CENTER);
        }

        public void refresh() { tfName.setText(currentUser[4]); tfUser.setText(currentUser[1]); pw.setText(""); }
    }

    // ============================================================
    //  PAGE FACTORIES PER ROLE
    // ============================================================
    static JLabel infoCard() {
        JLabel l = new JLabel();
        l.setVerticalAlignment(SwingConstants.TOP);
        l.setOpaque(true);
        l.setBackground(Color.WHITE);
        l.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE), pad(18)));
        return l;
    }

    static StatsPanel adminDashboard() {
        DefaultTableModel rm = readOnlyModel("Route", "Tickets", "Seats", "Revenue (Rs.)");
        JTable rt = makeTable(rm);
        BarChart chart = new BarChart();
        JPanel center = new JPanel(new GridLayout(1, 2, 12, 0));
        center.setOpaque(false);
        center.add(scroll(rt));
        center.add(chart);
        Runnable extra = () -> {
            rm.setRowCount(0);
            chart.data.clear();
            for (Map.Entry<String, double[]> e : routeStats().entrySet()) {
                double[] a = e.getValue();
                rm.addRow(new Object[]{name(ROUTES, e.getKey()), (int) a[0], (int) a[1], fmt(a[2])});
                chart.data.put(name(ROUTES, e.getKey()), a[2]);
            }
            chart.repaint();
        };
        return new StatsPanel("Admin Dashboard", 4,
                new String[]{"Routes", "Vehicles", "Drivers", "Schedules", "Tickets Sold", "Revenue (Rs.)", "Passengers", "New Alerts"},
                PALETTE, () -> {
                    int pax = 0, open = 0;
                    for (String[] u : db.get(USERS)) if (u[3].equals("Passenger")) pax++;
                    open = newAlertCount();
                    return new String[]{"" + db.get(ROUTES).size(), "" + db.get(VEHICLES).size(), "" + db.get(DRIVERS).size(),
                            "" + db.get(SCHEDULES).size(), "" + db.get(TICKETS).size(),
                            String.format("%.0f", totalRevenue()), "" + pax, "" + open};
                }, center, extra);
    }

    static ViewPanel adminBookings() {
        return new ViewPanel("All Bookings", TICKET_COLS, () -> ticketRows(t -> true))
                .btn("Cancel Selected Ticket", DANGER, p -> {
                    String id = p.selected(0);
                    if (id == null) { error(p, "Select a ticket first."); return; }
                    if (JOptionPane.showConfirmDialog(p, "Cancel ticket " + id + "?", "Confirm",
                            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        db.get(TICKETS).removeIf(t -> t[0].equals(id));
                        save();
                    }
                });
    }

    static void widen(JTable t, int col, int w) { t.getColumnModel().getColumn(col).setPreferredWidth(w); }

    static ViewPanel adminAlerts() {
        ViewPanel v = new ViewPanel("Alerts from Drivers & Passengers",
                new String[]{"ID", "From", "Role", "Priority", "Message", "Received", "Status"}, () -> {
            List<Object[]> rows = new ArrayList<>();
            List<String[]> list = new ArrayList<>(db.get(ALERTS));
            Collections.reverse(list);                                   // newest first
            for (String[] a : list) rows.add(new Object[]{a[0], a[3], a[2], a[4], a[5], a[6], a[7]});
            return rows;
        }).btn("Mark Seen", WARN, p -> {
            String id = p.selected(0);
            if (id == null) { error(p, "Select an alert first."); return; }
            String[] a = find(ALERTS, id);
            if (a != null && a[7].equals("New")) { a[7] = "Seen"; save(); }
            alertHook.run();
        }).btn("Mark Resolved", ACCENT, p -> {
            String id = p.selected(0);
            if (id == null) { error(p, "Select an alert first."); return; }
            String[] a = find(ALERTS, id);
            if (a != null) { a[7] = "Resolved"; save(); }
            alertHook.run();
        }).btn("Mark All Seen", PRIMARY, p -> {
            for (String[] a : db.get(ALERTS)) if (a[7].equals("New")) a[7] = "Seen";
            save();
            alertHook.run();
        }).btn("Delete", MUTED, p -> {
            String id = p.selected(0);
            if (id == null) { error(p, "Select an alert first."); return; }
            if (JOptionPane.showConfirmDialog(p, "Delete alert " + id + "?", "Confirm",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                db.get(ALERTS).removeIf(a -> a[0].equals(id));
                save();
            }
            alertHook.run();
        });
        widen(v.jt, 0, 40); widen(v.jt, 1, 130); widen(v.jt, 3, 110); widen(v.jt, 4, 320); widen(v.jt, 5, 140);
        return v;
    }

    /** Page for drivers and passengers: write an alert for the admin and track its status. */
    static class SendAlertPanel extends JPanel implements Refreshable {
        final JComboBox<String> priority = new JComboBox<>(new String[]{"Normal", "High", "Emergency"});
        final JTextArea msg = new JTextArea(4, 40);
        final DefaultTableModel model = readOnlyModel("ID", "Priority", "Message", "Sent At", "Status");
        final JTable jt = makeTable(model);

        SendAlertPanel(String title, String hint) {
            setLayout(new BorderLayout(0, 14));
            setBackground(BG);
            setBorder(pad(18));
            msg.setLineWrap(true);
            msg.setWrapStyleWord(true);
            style(msg);

            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            row.setOpaque(false);
            row.add(lbl("Priority:"));
            row.add(priority);
            JPanel head = new JPanel(new BorderLayout(0, 8));
            head.setOpaque(false);
            head.add(lbl(hint), BorderLayout.NORTH);
            head.add(row, BorderLayout.SOUTH);

            RoundButton send = new RoundButton("Send Alert to Admin", DANGER);
            send.addActionListener(e -> send());
            JPanel btn = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            btn.setOpaque(false);
            btn.add(send);

            JPanel form = new JPanel(new BorderLayout(0, 10));
            form.setBackground(Color.WHITE);
            form.setBorder(cardBorder());
            form.add(head, BorderLayout.NORTH);
            form.add(msg, BorderLayout.CENTER);
            form.add(btn, BorderLayout.SOUTH);

            JPanel top = new JPanel(new BorderLayout(0, 12));
            top.setOpaque(false);
            top.add(pageTitle(title), BorderLayout.NORTH);
            top.add(form, BorderLayout.CENTER);

            JPanel hist = new JPanel(new BorderLayout(0, 8));
            hist.setOpaque(false);
            hist.add(lbl("Your sent alerts and what the admin did with them:"), BorderLayout.NORTH);
            hist.add(scroll(jt), BorderLayout.CENTER);
            add(top, BorderLayout.NORTH);
            add(hist, BorderLayout.CENTER);
            widen(jt, 0, 40); widen(jt, 2, 420);
        }

        void send() {
            String m = msg.getText().trim();
            if (m.isEmpty()) { error(this, "Type a message first."); return; }
            sendAlert((String) priority.getSelectedItem(), m);
            msg.setText("");
            priority.setSelectedIndex(0);
            info(this, "Alert sent. The admin has been notified.");
            refresh();
        }

        public void refresh() {
            model.setRowCount(0);
            List<String[]> mine = new ArrayList<>();
            for (String[] a : db.get(ALERTS)) if (a[1].equals(currentUser[0])) mine.add(a);
            Collections.reverse(mine);
            for (String[] a : mine) model.addRow(new Object[]{a[0], a[4], a[5], a[6], a[7]});
        }
    }

    static StatsPanel driverHome() {
        JLabel card = infoCard();
        Supplier<List<String[]>> trips = () -> {
            List<String[]> l = new ArrayList<>();
            String[] d = myDriver();
            for (String[] s : db.get(SCHEDULES)) if (d != null && s[2].equals(d[4])) l.add(s);
            return l;
        };
        Runnable extra = () -> {
            String[] d = myDriver(), v = d == null ? null : find(VEHICLES, d[4]);
            card.setText("<html><h2 style='color:#0f172a'>Driver Profile</h2>"
                    + "<p><b>Name:</b> " + (d == null ? "-" : d[1]) + "</p>"
                    + "<p><b>Phone:</b> " + (d == null ? "-" : d[2]) + "</p>"
                    + "<p><b>License:</b> " + (d == null ? "-" : d[3]) + "</p>"
                    + "<p><b>Vehicle:</b> " + (v == null ? "-" : v[1] + " (" + v[2] + ", capacity " + v[3] + ")") + "</p>"
                    + "<br><p style='color:#64748b'>Use <b>My Trips</b> to start, delay or complete a trip, "
                    + "<b>Passenger List</b> to see who has booked, and <b>Send Alert</b> to tell the admin about breakdowns or emergencies.</p></html>");
        };
        return new StatsPanel("Driver Home", 4, new String[]{"Assigned Vehicle", "My Trips", "Passengers Booked", "My Open Alerts"},
                PALETTE, () -> {
                    String[] d = myDriver();
                    int seats = 0, open = 0;
                    List<String[]> mine = trips.get();
                    for (String[] s : mine) seats += bookedSeats(s[0]);
                    for (String[] a : db.get(ALERTS)) if (a[1].equals(currentUser[0]) && !a[7].equals("Resolved")) open++;
                    String[] v = d == null ? null : find(VEHICLES, d[4]);
                    return new String[]{v == null ? "-" : v[1], "" + mine.size(), "" + seats, "" + open};
                }, card, extra);
    }

    static void setStatus(ViewPanel p, String status) {
        String id = p.selected(0);
        if (id == null) { error(p, "Select a trip first."); return; }
        String[] s = find(SCHEDULES, id);
        if (s[4].equals("Completed")) { error(p, "This trip is already completed."); return; }
        s[4] = status;
        save();
    }

    static ViewPanel driverTrips() {
        return new ViewPanel("My Trips", new String[]{"ID", "Route", "From -> To", "Departure", "Status", "Booked / Capacity"}, () -> {
            List<Object[]> rows = new ArrayList<>();
            String[] d = myDriver();
            for (String[] s : db.get(SCHEDULES)) {
                if (d == null || !s[2].equals(d[4])) continue;
                String[] r = find(ROUTES, s[1]);
                rows.add(new Object[]{s[0], r == null ? "?" : r[1], r == null ? "?" : r[2] + " -> " + r[3], s[3], s[4],
                        bookedSeats(s[0]) + " / " + capacity(s[2])});
            }
            return rows;
        }).btn("Start Trip", ACCENT, p -> setStatus(p, "Running"))
          .btn("Mark Delayed", WARN, p -> setStatus(p, "Delayed"))
          .btn("Complete Trip", PRIMARY, p -> setStatus(p, "Completed"));
    }

    static ViewPanel driverPassengers() {
        return new ViewPanel("Passenger List", TICKET_COLS, () -> ticketRows(t -> {
            String[] d = myDriver(), s = find(SCHEDULES, t[2]);
            return d != null && s != null && s[2].equals(d[4]);
        }));
    }

    static StatsPanel passengerHome() {
        JLabel card = infoCard();
        Runnable extra = () -> card.setText("<html><h2 style='color:#0f172a'>Welcome, " + currentUser[4] + "</h2>"
                + "<p style='color:#64748b'>Plan your journey in three steps:</p><br>"
                + "<p><b>1.</b> Open <b>Timetable</b> to see routes, fares and seats left.</p>"
                + "<p><b>2.</b> Go to <b>Book Ticket</b>, search your route and confirm your seats.</p>"
                + "<p><b>3.</b> Track or cancel bookings from <b>My Tickets</b>.</p><p><b>4.</b> Lost item, complaint or safety problem? Use <b>Send Alert</b> to notify the admin.</p></html>");
        return new StatsPanel("Passenger Home", 4, new String[]{"My Bookings", "Seats Booked", "Total Spent (Rs.)", "Trips Available"},
                PALETTE, () -> {
                    int n = 0, seats = 0, avail = 0;
                    double spent = 0;
                    for (String[] t : db.get(TICKETS))
                        if (t[6].equals(currentUser[0])) { n++; seats += Integer.parseInt(t[3]); spent += Double.parseDouble(t[4]); }
                    for (String[] s : db.get(SCHEDULES)) if (s[4].equals("Scheduled") || s[4].equals("Delayed")) avail++;
                    return new String[]{"" + n, "" + seats, String.format("%.0f", spent), "" + avail};
                }, card, extra);
    }

    static ViewPanel passengerTickets() {
        return new ViewPanel("My Tickets", TICKET_COLS, () -> ticketRows(t -> t[6].equals(currentUser[0])))
                .btn("Cancel Selected Ticket", DANGER, p -> {
                    String id = p.selected(0);
                    if (id == null) { error(p, "Select a ticket first."); return; }
                    String st = p.selected(6);
                    if (!st.equals("Scheduled") && !st.equals("Delayed")) {
                        error(p, "Only tickets for upcoming trips can be cancelled."); return;
                    }
                    if (JOptionPane.showConfirmDialog(p, "Cancel ticket " + id + "?", "Confirm",
                            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        db.get(TICKETS).removeIf(t -> t[0].equals(id));
                        save();
                    }
                });
    }

    // ============================================================
    //  MAIN WINDOW (sidebar + header + card pages)
    // ============================================================
    static class MainFrame extends JFrame {
        final CardLayout cl = new CardLayout();
        final JPanel content = new JPanel(cl);
        final Map<String, Refreshable> pages = new LinkedHashMap<>();
        final Map<String, NavButton> navs = new LinkedHashMap<>();
        final JPanel side = new JPanel();
        final BellButton bell = new BellButton();
        javax.swing.Timer poll;
        String current = "";
        int lastNotifiedId = 0;
        final boolean isAdmin;

        MainFrame() {
            super("Public Transport Management System  |  " + currentUser[3] + " Portal");
            isAdmin = currentUser[3].equals("Admin");
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setSize(1200, 720);
            setMinimumSize(new Dimension(1000, 600));
            setLocationRelativeTo(null);
            setLayout(new BorderLayout());

            side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
            side.setBackground(DARK);
            side.setBorder(pad(22, 14, 14, 14));
            side.setPreferredSize(new Dimension(220, 0));
            JLabel logo = new JLabel("PTMS");
            logo.setFont(logo.getFont().deriveFont(Font.BOLD, 28f));
            logo.setForeground(Color.WHITE);
            JLabel role = new JLabel(currentUser[3] + " Portal");
            role.setForeground(ACCENT);
            role.setFont(role.getFont().deriveFont(Font.BOLD, 13f));
            logo.setAlignmentX(LEFT_ALIGNMENT); role.setAlignmentX(LEFT_ALIGNMENT);
            side.add(logo); side.add(role); side.add(Box.createVerticalStrut(24));

            content.setBackground(BG);
            switch (currentUser[3]) {
                case "Admin":
                    page("Dashboard", adminDashboard());
                    page("Routes", new CrudPanel("Manage Routes", ROUTES, Field.text("Route Name"), Field.text("Source"),
                            Field.text("Destination"), Field.num("Distance (km)"), Field.num("Fare per seat (Rs.)")));
                    page("Vehicles", new CrudPanel("Manage Vehicles", VEHICLES, Field.text("Reg. Number"),
                            Field.pick("Type", "Bus", "Metro", "Tram", "Van"), Field.num("Capacity")));
                    page("Drivers", new CrudPanel("Manage Drivers", DRIVERS, Field.text("Name"), Field.text("Phone"),
                            Field.text("License No."), Field.ref("Assigned Vehicle", VEHICLES)));
                    page("Schedules", new CrudPanel("Manage Schedules", SCHEDULES, Field.ref("Route", ROUTES),
                            Field.ref("Vehicle", VEHICLES), Field.text("Departure (HH:mm)"),
                            Field.pick("Status", "Scheduled", "Running", "Delayed", "Completed")));
                    page("Bookings", adminBookings());
                    page("Alerts", adminAlerts());
                    page("Users", new CrudPanel("Manage Users", USERS, Field.text("Username"), Field.text("Password"),
                            Field.pick("Role", "Admin", "Driver", "Passenger"), Field.text("Full Name"),
                            Field.opt("Driver ID")));
                    break;
                case "Driver":
                    page("My Home", driverHome());
                    page("My Trips", driverTrips());
                    page("Passenger List", driverPassengers());
                    page("Send Alert", new SendAlertPanel("Send Alert to Admin",
                            "Report a breakdown, accident, delay or any emergency. The admin is notified immediately."));
                    break;
                default:
                    page("Home", passengerHome());
                    page("Book Ticket", new BookPanel());
                    page("My Tickets", passengerTickets());
                    page("Send Alert", new SendAlertPanel("Send Alert to Admin",
                            "Lost item, complaint, safety problem or driver feedback? Tell the admin."));
                    page("Timetable", new ViewPanel("Timetable", TIMETABLE_COLS, PublicTransportApp::timetableRows));
                    break;
            }
            page("My Profile", new ProfilePanel());
            side.add(Box.createVerticalGlue());

            JLabel who = new JLabel(currentUser[4] + "  (" + currentUser[3] + ")");
            who.setFont(who.getFont().deriveFont(Font.BOLD, 14f));
            who.setForeground(DARK);
            RoundButton logout = new RoundButton("Logout", DANGER);
            logout.addActionListener(e -> { dispose(); currentUser = null; new LoginFrame().setVisible(true); });
            JPanel header = new JPanel(new BorderLayout());
            header.setBackground(Color.WHITE);
            header.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, LINE), pad(10, 20, 10, 20)));
            JLabel app = new JLabel("Public Transport Management System");
            app.setForeground(MUTED);
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 0));
            right.setOpaque(false);
            if (isAdmin) {
                bell.addActionListener(e -> show("Alerts"));
                right.add(bell);
            }
            right.add(who); right.add(logout);
            header.add(app, BorderLayout.WEST);
            header.add(right, BorderLayout.EAST);

            JPanel main = new JPanel(new BorderLayout());
            main.add(header, BorderLayout.NORTH);
            main.add(content, BorderLayout.CENTER);
            add(side, BorderLayout.WEST);
            add(main, BorderLayout.CENTER);
            show(pages.keySet().iterator().next());

            if (isAdmin) { alertHook = this::updateBadges; updateBadges(); }
            poll = new javax.swing.Timer(2000, e -> pollTick());
            poll.setInitialDelay(800);
            poll.start();
        }

        @Override public void dispose() {
            if (poll != null) poll.stop();
            if (isAdmin) alertHook = () -> { };
            super.dispose();
        }

        void updateBadges() {
            int n = newAlertCount();
            NavButton nav = navs.get("Alerts");
            if (nav != null) nav.setBadge(n);
            bell.setCount(n);
        }

        /** Runs every 2 seconds: picks up changes from other windows and notifies the admin of new alerts. */
        void pollTick() {
            if (currentUser == null) return;
            boolean changed = syncFromDisk();
            if (changed && !current.equals("My Profile") && pages.containsKey(current)) pages.get(current).refresh();
            if (!isAdmin) return;
            updateBadges();
            List<String[]> fresh = new ArrayList<>();
            for (String[] a : db.get(ALERTS))
                if (Integer.parseInt(a[0]) > lastNotifiedId && a[7].equals("New")) fresh.add(a);
            lastNotifiedId = Math.max(lastNotifiedId, maxAlertId());
            if (fresh.isEmpty()) return;
            java.awt.Toolkit.getDefaultToolkit().beep();
            String[] last = fresh.get(fresh.size() - 1);
            if (fresh.size() == 1)
                toast(this, "New alert from " + last[3] + " (" + last[2] + ")", "[" + last[4] + "] " + last[5], priorityColor(last[4]));
            else
                toast(this, fresh.size() + " unread alerts", "Latest from " + last[3] + ": " + last[5], WARN);
        }

        <T extends JPanel & Refreshable> void page(String title, T p) {
            content.add(p, title);
            pages.put(title, p);
            NavButton b = new NavButton(title);
            b.addActionListener(e -> show(title));
            navs.put(title, b);
            side.add(b);
            side.add(Box.createVerticalStrut(4));
        }

        void show(String t) {
            current = t;
            pages.get(t).refresh();
            cl.show(content, t);
            navs.forEach((k, b) -> b.setActive(k.equals(t)));
        }
    }

    // ============================================================
    //  LOGIN WINDOW
    // ============================================================
    static class LoginFrame extends JFrame {
        final JComboBox<String> role = new JComboBox<>(new String[]{"Admin", "Driver", "Passenger"});
        final JTextField user = new JTextField();
        final JPasswordField pass = new JPasswordField();
        final JLabel msg = new JLabel(" ");

        static <T extends JComponent> T left(T c, int h) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
            return c;
        }

        LoginFrame() {
            super("PTMS - Login");
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            setSize(880, 540);
            setLocationRelativeTo(null);
            setResizable(false);
            setLayout(new BorderLayout());

            JPanel hero = new JPanel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int w = getWidth(), h = getHeight();
                    g2.setPaint(new GradientPaint(0, 0, PRIMARY, 0, h, DARK));
                    g2.fillRect(0, 0, w, h);
                    g2.setColor(Color.WHITE);
                    g2.setFont(getFont().deriveFont(Font.BOLD, 46f));
                    g2.drawString("PTMS", 40, 100);
                    g2.setFont(getFont().deriveFont(15f));
                    g2.drawString("Public Transport Management System", 42, 130);
                    int bx = 50, by = 230, bw = 280, bh = 100;
                    g2.setColor(new Color(255, 255, 255, 40));
                    g2.fillRoundRect(0, by + bh + 8, w, 6, 6, 6);
                    g2.setColor(new Color(250, 204, 21));
                    g2.fillRoundRect(bx, by, bw, bh, 28, 28);
                    g2.setColor(new Color(186, 230, 253));
                    for (int i = 0; i < 4; i++) g2.fillRoundRect(bx + 22 + i * 58, by + 16, 44, 34, 8, 8);
                    g2.setColor(DARK);
                    g2.fillRect(bx, by + 62, bw, 5);
                    g2.fillOval(bx + 40, by + bh - 16, 38, 38);
                    g2.fillOval(bx + bw - 80, by + bh - 16, 38, 38);
                    g2.setColor(Color.LIGHT_GRAY);
                    g2.fillOval(bx + 50, by + bh - 6, 18, 18);
                    g2.fillOval(bx + bw - 70, by + bh - 6, 18, 18);
                    g2.setColor(new Color(203, 213, 225));
                    g2.setFont(getFont().deriveFont(13f));
                    g2.drawString("Admin  |  Driver  |  Passenger portals", 42, h - 40);
                }
            };
            hero.setPreferredSize(new Dimension(400, 0));

            JPanel form = new JPanel();
            form.setBackground(Color.WHITE);
            form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
            form.setBorder(pad(44, 50, 30, 50));
            JLabel h = pageTitle("Welcome back");
            h.setFont(h.getFont().deriveFont(Font.BOLD, 28f));
            style(user); style(pass);
            msg.setForeground(DANGER);
            RoundButton login = new RoundButton("Sign In", PRIMARY);
            RoundButton reg = new RoundButton("New passenger? Create account", ACCENT);
            login.addActionListener(e -> doLogin());
            reg.addActionListener(e -> register(this));
            JLabel hint = new JLabel("<html>Demo: admin/admin123 &nbsp;|&nbsp; driver1/driver123 &nbsp;|&nbsp; passenger/pass123</html>");
            hint.setForeground(MUTED);
            hint.setFont(hint.getFont().deriveFont(11f));

            form.add(left(h, 40));
            form.add(left(lbl("Sign in to your portal"), 24));
            form.add(Box.createVerticalStrut(24));
            form.add(left(lbl("Login as"), 22));
            form.add(left(role, 36));
            form.add(Box.createVerticalStrut(12));
            form.add(left(lbl("Username"), 22));
            form.add(left(user, 38));
            form.add(Box.createVerticalStrut(12));
            form.add(left(lbl("Password"), 22));
            form.add(left(pass, 38));
            form.add(left(msg, 28));
            form.add(Box.createVerticalStrut(6));
            form.add(left(login, 42));
            form.add(Box.createVerticalStrut(10));
            form.add(left(reg, 42));
            form.add(Box.createVerticalGlue());
            form.add(left(hint, 36));

            add(hero, BorderLayout.WEST);
            add(form, BorderLayout.CENTER);
            getRootPane().setDefaultButton(login);
        }

        void doLogin() {
            syncFromDisk();
            String u = user.getText().trim(), p = new String(pass.getPassword()), r = (String) role.getSelectedItem();
            for (String[] row : db.get(USERS)) {
                if (row[1].equals(u) && row[2].equals(p) && row[3].equals(r)) {
                    currentUser = row;
                    if (r.equals("Driver") && myDriver() == null) {
                        currentUser = null;
                        msg.setText("Driver account is not linked to a driver profile.");
                        return;
                    }
                    dispose();
                    new MainFrame().setVisible(true);
                    return;
                }
            }
            msg.setText("Invalid username, password or role.");
        }

        static void register(Component parent) {
            syncFromDisk();
            JTextField n = new JTextField(), u = new JTextField();
            JPasswordField p = new JPasswordField();
            Object[] f = {"Full name:", n, "Username:", u, "Password (min 4 chars):", p};
            if (JOptionPane.showConfirmDialog(parent, f, "Passenger Registration",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
            String name = n.getText().trim(), user = u.getText().trim(), pw = new String(p.getPassword());
            if (name.isEmpty() || user.isEmpty() || pw.length() < 4) { error(parent, "Fill all fields (password min 4 characters)."); return; }
            for (String[] x : db.get(USERS))
                if (x[1].equalsIgnoreCase(user)) { error(parent, "Username already taken."); return; }
            db.get(USERS).add(new String[]{nextId(USERS), user, pw, "Passenger", name, ""});
            save();
            info(parent, "Account created. You can now sign in as Passenger.");
        }
    }

    // ============================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Font base = new Font("SansSerif", Font.PLAIN, 13);
            for (Object k : Collections.list(UIManager.getDefaults().keys()))
                if (UIManager.get(k) instanceof FontUIResource) UIManager.put(k, new FontUIResource(base));
            chooseDataFile();
            load();
            new LoginFrame().setVisible(true);
        });
    }
}
