import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Date;

public class PharmacyDB_Enhanced extends JFrame {
    //URL for database goes here 
    private static final String URL = "";
    //database User goes here
    private static final String USER = "";
    //password goes here
    private static final String PASS = "";
    private Connection conn;

    private JPanel contentPanel;
    
    // Logo image path
    private static final String LOGO_PATH = "src\\logo.png";
    private ImageIcon logoIcon;
    
    // User roles and permissions
    public enum UserRole {
        ADMIN, PHARMACIST, ASSISTANT
    }
    
    // Current user role
    private UserRole currentRole;
    private String currentUsername;
    
    // Login credentials map (username -> [password, role])
    private static final Map<String, String[]> USERS = new HashMap<>();
    static {
        // Format: username -> [password, role]
        USERS.put("admin", new String[]{"admin", UserRole.ADMIN.name()});
        USERS.put("pharmacist", new String[]{"pharmacist", UserRole.PHARMACIST.name()});
        USERS.put("assistant", new String[]{"assistant", UserRole.ASSISTANT.name()});
    }
    
    // Access control for different sections based on role
    private static final Map<String, UserRole[]> SECTION_ACCESS = new HashMap<>();
    static {
        // Format: section -> roles that can access it
        SECTION_ACCESS.put("Dashboard", new UserRole[]{UserRole.ADMIN, UserRole.PHARMACIST, UserRole.ASSISTANT});
        SECTION_ACCESS.put("Medicines", new UserRole[]{UserRole.ADMIN, UserRole.PHARMACIST, UserRole.ASSISTANT});
        SECTION_ACCESS.put("Customers", new UserRole[]{UserRole.ADMIN, UserRole.PHARMACIST, UserRole.ASSISTANT});
        SECTION_ACCESS.put("Staff", new UserRole[]{UserRole.ADMIN});
        SECTION_ACCESS.put("Suppliers", new UserRole[]{UserRole.ADMIN, UserRole.PHARMACIST});
        SECTION_ACCESS.put("Sales", new UserRole[]{UserRole.ADMIN, UserRole.PHARMACIST, UserRole.ASSISTANT});
        SECTION_ACCESS.put("Prescriptions", new UserRole[]{UserRole.ADMIN, UserRole.PHARMACIST});
        SECTION_ACCESS.put("Inventory", new UserRole[]{UserRole.ADMIN, UserRole.PHARMACIST, UserRole.ASSISTANT});
        SECTION_ACCESS.put("Pharmacists", new UserRole[]{UserRole.ADMIN});
        SECTION_ACCESS.put("Reports", new UserRole[]{UserRole.ADMIN, UserRole.PHARMACIST});
        SECTION_ACCESS.put("Alerts", new UserRole[]{UserRole.ADMIN, UserRole.PHARMACIST, UserRole.ASSISTANT});
    }
    
    // CRUD operation permissions based on role
    private static final Map<UserRole, String[]> ROLE_PERMISSIONS = new HashMap<>();
    static {
        // Format: role -> [operations]
        ROLE_PERMISSIONS.put(UserRole.ADMIN, new String[]{"add", "update", "delete", "view"});
        ROLE_PERMISSIONS.put(UserRole.PHARMACIST, new String[]{"add", "update", "view"});
        ROLE_PERMISSIONS.put(UserRole.ASSISTANT, new String[]{"view"});
    }

    public PharmacyDB_Enhanced() {
        setTitle("Pharmacy Management System - Enhanced");
        setSize(1200, 700);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        connect();
    
        // Load logo
        loadLogo();
        
        // Show login screen first
        showLoginScreen();
    }
    
    private void loadLogo() {
        try {
            BufferedImage originalImg = ImageIO.read(new File(LOGO_PATH));
            Image scaledImg = originalImg.getScaledInstance(200, 200, Image.SCALE_SMOOTH);
            logoIcon = new ImageIcon(scaledImg);
            setIconImage(originalImg);
        } catch (IOException e) {
            System.err.println("Could not load logo: " + e.getMessage());
            logoIcon = new ImageIcon(new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB));
        }
    }
    
    private void showLoginScreen() {
        // Clear any existing content
        getContentPane().removeAll();
        
        // Create login panel
        JPanel loginPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        
        // Create a sub-panel with border for login components
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(33, 150, 243), 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        
        GridBagConstraints formGbc = new GridBagConstraints();
        formGbc.fill = GridBagConstraints.HORIZONTAL;
        formGbc.insets = new Insets(5, 5, 5, 5);
        
        // Logo
        JLabel logoLabel = new JLabel(logoIcon);
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        formGbc.gridx = 0;
        formGbc.gridy = 0;
        formGbc.gridwidth = 2;
        formPanel.add(logoLabel, formGbc);
        
        // Title
        JLabel titleLabel = new JLabel("Pharmacy Management System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(33, 150, 243));
        formGbc.gridx = 0;
        formGbc.gridy = 1;
        formGbc.gridwidth = 2;
        formPanel.add(titleLabel, formGbc);
        
        // Subtitle
        JLabel subtitleLabel = new JLabel("Login to continue", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formGbc.gridy = 2;
        formGbc.gridwidth = 2;
        formGbc.insets = new Insets(5, 5, 15, 5);
        formPanel.add(subtitleLabel, formGbc);
        
        // Username
        formGbc.gridwidth = 1;
        formGbc.insets = new Insets(5, 5, 5, 5);
        formGbc.gridy = 3;
        formGbc.gridx = 0;
        formPanel.add(new JLabel("Username:"), formGbc);
        
        JTextField usernameField = new JTextField(15);
        formGbc.gridx = 1;
        formPanel.add(usernameField, formGbc);
        
        // Password
        formGbc.gridy = 4;
        formGbc.gridx = 0;
        formPanel.add(new JLabel("Password:"), formGbc);
        
        JPasswordField passwordField = new JPasswordField(15);
        formGbc.gridx = 1;
        formPanel.add(passwordField, formGbc);
        
        // Login button
        JButton loginButton = new JButton("Login");
        loginButton.setBackground(new Color(33, 150, 243));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        formGbc.gridy = 5;
        formGbc.gridx = 0;
        formGbc.gridwidth = 2;
        formGbc.insets = new Insets(15, 5, 5, 5);
        formPanel.add(loginButton, formGbc);
        
        // Error message label (initially hidden)
        JLabel errorLabel = new JLabel("", SwingConstants.CENTER);
        errorLabel.setForeground(Color.RED);
        formGbc.gridy = 6;
        formPanel.add(errorLabel, formGbc);
        
        // Add form panel to login panel
        loginPanel.add(formPanel);
        
        // Add login panel to frame
        add(loginPanel);
        
        // Login button action
        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            
            if (USERS.containsKey(username) && USERS.get(username)[0].equals(password)) {
                // Set current user role
                currentRole = UserRole.valueOf(USERS.get(username)[1]);
                currentUsername = username;
                logActivity("LOGIN", "User logged in");
                initializeMainUI();
            } else {
                errorLabel.setText("Invalid username or password");
                passwordField.setText("");
            }
        });
        
        // Also allow pressing Enter to login
        passwordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    loginButton.doClick();
                }
            }
        });
        
        revalidate();
        repaint();
    }
    
    private void initializeMainUI() {
        // Clear login screen
        getContentPane().removeAll();
        
        contentPanel = new JPanel(new BorderLayout());

        JPanel sidebar = new JPanel(new GridLayout(11, 1, 0, 10));
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBackground(new Color(33, 150, 243));

        String[] buttons = {"Dashboard", "Medicines", "Customers", "Staff", "Suppliers", 
                           "Sales", "Prescriptions", "Inventory", "Pharmacists", "Reports", "Alerts"};
        for (String name : buttons) {
            // Check if the current role has access to this section
            if (hasAccessToSection(name)) {
                JButton btn = new JButton(name);
                btn.setForeground(Color.WHITE);
                btn.setBackground(new Color(25, 118, 210));
                btn.setFocusPainted(false);
                btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                btn.addActionListener(e -> switchPanel(name));
                sidebar.add(btn);
            }
        }

        JPanel header = new JPanel(new BorderLayout());
        header.setPreferredSize(new Dimension(0, 60));
        header.setBackground(new Color(3, 169, 244));
        
        // Create a panel for logo and text in header
        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        headerLeft.setBackground(new Color(3, 169, 244));
        
        // Add logo to header
        JLabel headerLogo = new JLabel(new ImageIcon(logoIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH)));
        headerLeft.add(headerLogo);
        
        // Add text to header
        JLabel lbl = new JLabel("Pharmacy Management System - Logged in as: " + currentRole + " (" + currentUsername + ")");
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lbl.setForeground(Color.WHITE);
        headerLeft.add(lbl);
        
        header.add(headerLeft, BorderLayout.WEST);
        
        // Add logout button to header
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(new Color(25, 118, 210));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.addActionListener(e -> {
            logActivity("LOGOUT", "User logged out");
            showLoginScreen();
        });
        
        JPanel headerRight = new JPanel();
        headerRight.setBackground(new Color(3, 169, 244));
        headerRight.add(logoutBtn);
        header.add(headerRight, BorderLayout.EAST);

        add(sidebar, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        
        // Show dashboard by default
        showDashboard();
        
        revalidate();
        repaint();
    }
    
    private boolean hasAccessToSection(String section) {
        if (!SECTION_ACCESS.containsKey(section)) {
            return false;
        }
        
        UserRole[] allowedRoles = SECTION_ACCESS.get(section);
        for (UserRole role : allowedRoles) {
            if (role == currentRole) {
                return true;
            }
        }
        return false;
    }
    
    private boolean hasPermission(String operation) {
        String[] permissions = ROLE_PERMISSIONS.get(currentRole);
        for (String permission : permissions) {
            if (permission.equals(operation)) {
                return true;
            }
        }
        return false;
    }

    private void connect() {
        try {
            conn = DriverManager.getConnection(URL, USER, PASS);
        } catch (SQLException e) {
            showError("DB Connection Failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private void switchPanel(String name) {
        contentPanel.removeAll();
        switch (name) {
            case "Dashboard" -> showDashboard();
            case "Medicines" -> buildCRUDPanel("medicine", 
                new String[]{"ID", "Name", "Type", "Price", "StockQuantity", "ExpiryDate", "SupplierID"});
            case "Customers" -> buildCRUDPanel("customer", 
                new String[]{"ID", "Name", "ContactInfo", "Address"});
            case "Staff" -> buildCRUDPanel("staff", 
                new String[]{"ID", "Name", "Role", "Salary", "ContactInfo"});
            case "Suppliers" -> buildCRUDPanel("supplier", 
                new String[]{"ID", "Name", "ContactInfo", "Address"});
            case "Sales" -> buildCRUDPanel("sales", 
                new String[]{"ID", "CustomerID", "MedicineID", "Quantity", "TotalPrice", "Date"});
            case "Prescriptions" -> buildCRUDPanel("prescription", 
                new String[]{"ID", "CustomerID", "PharmacistID", "DateIssued"});
            case "Inventory" -> buildCRUDPanel("inventory", 
                new String[]{"MedicineID", "StockQuantity", "LastRestockedDate"});
            case "Pharmacists" -> buildCRUDPanel("pharmacist", 
                new String[]{"ID", "LicenseNumber"});
            case "Reports" -> showReportsPanel();
            case "Alerts" -> showAlertsPanel();
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showDashboard() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Top cards panel
        JPanel cardsPanel = new JPanel(new GridLayout(1, 4, 15, 15));
        cardsPanel.add(card("Total Medicines", scalar("SELECT COUNT(*) FROM medicine"), new Color(33, 150, 243)));
        cardsPanel.add(card("Total Sales (SAR)", scalar("SELECT COALESCE(SUM(TotalPrice),0) FROM sales"), new Color(76, 175, 80)));
        cardsPanel.add(card("Total Customers", scalar("SELECT COUNT(*) FROM customer"), new Color(255, 152, 0)));
        cardsPanel.add(card("Expiring Soon (<30 days)", scalar("SELECT COUNT(*) FROM medicine WHERE ExpiryDate BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY)"), new Color(244, 67, 54)));
        
        // Low stock medicines panel
        JPanel lowStockPanel = new JPanel(new BorderLayout(10, 10));
        lowStockPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(244, 67, 54), 2),
            "⚠ Low Stock Medicines (Quantity < 50)",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new Font("Segoe UI", Font.BOLD, 14),
            new Color(244, 67, 54)
        ));
        
        // Create table for low stock medicines
        DefaultTableModel lowStockModel = new DefaultTableModel();
        JTable lowStockTable = new JTable(lowStockModel);
        lowStockTable.setRowHeight(25);
        lowStockTable.setGridColor(Color.LIGHT_GRAY);
        
        // Add columns
        lowStockModel.addColumn("ID");
        lowStockModel.addColumn("Medicine Name");
        lowStockModel.addColumn("Type");
        lowStockModel.addColumn("Current Stock");
        lowStockModel.addColumn("Price (SAR)");
        lowStockModel.addColumn("Expiry Date");
        lowStockModel.addColumn("Status");
        
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT ID, Name, Type, StockQuantity, Price, ExpiryDate " +
                "FROM medicine " +
                "WHERE StockQuantity < 50 " +
                "ORDER BY StockQuantity ASC"
            );
            
            while (rs.next()) {
                int stock = rs.getInt("StockQuantity");
                String status;
                if (stock == 0) {
                    status = "OUT OF STOCK";
                } else if (stock < 10) {
                    status = "CRITICAL";
                } else if (stock < 30) {
                    status = "LOW";
                } else {
                    status = "REORDER SOON";
                }
                
                lowStockModel.addRow(new Object[]{
                    rs.getInt("ID"),
                    rs.getString("Name"),
                    rs.getString("Type"),
                    stock,
                    String.format("%.2f", rs.getDouble("Price")),
                    rs.getDate("ExpiryDate"),
                    status
                });
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            showError("Error loading low stock medicines: " + e.getMessage());
        }
        
        // Color coding for status column
        lowStockTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (column == 6) { // Status column
                    String status = value.toString();
                    if (status.equals("OUT OF STOCK")) {
                        c.setBackground(new Color(244, 67, 54));
                        c.setForeground(Color.WHITE);
                    } else if (status.equals("CRITICAL")) {
                        c.setBackground(new Color(255, 152, 0));
                        c.setForeground(Color.WHITE);
                    } else if (status.equals("LOW")) {
                        c.setBackground(new Color(255, 235, 59));
                        c.setForeground(Color.BLACK);
                    } else {
                        c.setBackground(new Color(255, 249, 196));
                        c.setForeground(Color.BLACK);
                    }
                } else if (!isSelected) {
                    c.setBackground(Color.WHITE);
                    c.setForeground(Color.BLACK);
                }
                
                return c;
            }
        });
        
        JScrollPane lowStockScroll = new JScrollPane(lowStockTable);
        lowStockScroll.setPreferredSize(new Dimension(0, 250));
        lowStockPanel.add(lowStockScroll, BorderLayout.CENTER);
        
        // Add export button for low stock report
        if (hasPermission("view")) {
            JButton exportBtn = createStyledButton("Export Low Stock Report");
            exportBtn.addActionListener(e -> exportLowStockReport());
            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            buttonPanel.add(exportBtn);
            lowStockPanel.add(buttonPanel, BorderLayout.SOUTH);
        }
        
        // Assemble main panel
        mainPanel.add(cardsPanel, BorderLayout.NORTH);
        mainPanel.add(lowStockPanel, BorderLayout.CENTER);
        
        contentPanel.add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel card(String title, String value, Color color) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        titleLbl.setForeground(Color.DARK_GRAY);
        
        JLabel valLbl = new JLabel(value, SwingConstants.CENTER);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 28));
        valLbl.setForeground(color);

        p.add(titleLbl, BorderLayout.NORTH);
        p.add(valLbl, BorderLayout.CENTER);
        return p;
    }

    private String scalar(String sql) {
        Statement s = null;
        ResultSet rs = null;
        try {
            s = conn.createStatement();
            rs = s.executeQuery(sql);
            String result = rs.next() ? rs.getString(1) : "0";
            rs.close();
            s.close();
            return result;
        } catch (SQLException e) {
            return "ERR";
        } finally {
            try {
                if (rs != null) rs.close();
                if (s != null) s.close();
            } catch (SQLException e) {
                // Ignore close errors
            }
        }
    }
    
    // NEW: Alerts Panel
    private void showAlertsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // Expiring medicines tab
        tabbedPane.addTab("Expiring Soon", createExpiryAlertsPanel());
        
        // Low stock tab (detailed view)
        tabbedPane.addTab("Low Stock", createLowStockAlertsPanel());
        
        panel.add(tabbedPane, BorderLayout.CENTER);
        contentPanel.add(panel, BorderLayout.CENTER);
    }
    
    private JPanel createExpiryAlertsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);
        table.setRowHeight(25);
        
        model.addColumn("Medicine ID");
        model.addColumn("Name");
        model.addColumn("Type");
        model.addColumn("Expiry Date");
        model.addColumn("Days Until Expiry");
        model.addColumn("Stock Quantity");
        model.addColumn("Alert Level");
        
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT ID, Name, Type, ExpiryDate, StockQuantity, " +
                "DATEDIFF(ExpiryDate, CURDATE()) as DaysUntilExpiry " +
                "FROM medicine " +
                "WHERE ExpiryDate <= DATE_ADD(CURDATE(), INTERVAL 90 DAY) " +
                "ORDER BY ExpiryDate ASC"
            );
            
            while (rs.next()) {
                int daysUntilExpiry = rs.getInt("DaysUntilExpiry");
                String alertLevel;
                if (daysUntilExpiry < 0) {
                    alertLevel = "EXPIRED";
                } else if (daysUntilExpiry <= 7) {
                    alertLevel = "CRITICAL";
                } else if (daysUntilExpiry <= 30) {
                    alertLevel = "WARNING";
                } else {
                    alertLevel = "NOTICE";
                }
                
                model.addRow(new Object[]{
                    rs.getInt("ID"),
                    rs.getString("Name"),
                    rs.getString("Type"),
                    rs.getDate("ExpiryDate"),
                    daysUntilExpiry,
                    rs.getInt("StockQuantity"),
                    alertLevel
                });
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            showError("Error loading expiry alerts: " + e.getMessage());
        }
        
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createLowStockAlertsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);
        table.setRowHeight(25);
        
        model.addColumn("Medicine ID");
        model.addColumn("Name");
        model.addColumn("Type");
        model.addColumn("Current Stock");
        model.addColumn("Reorder Level");
        model.addColumn("Supplier ID");
        model.addColumn("Status");
        
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT m.ID, m.Name, m.Type, m.StockQuantity, m.SupplierID " +
                "FROM medicine m " +
                "WHERE m.StockQuantity < 50 " +
                "ORDER BY m.StockQuantity ASC"
            );
            
            while (rs.next()) {
                int stock = rs.getInt("StockQuantity");
                int reorderLevel = 50;
                String status;
                if (stock == 0) {
                    status = "OUT OF STOCK";
                } else if (stock < 10) {
                    status = "CRITICAL";
                } else if (stock < 30) {
                    status = "LOW";
                } else {
                    status = "REORDER SOON";
                }
                
                model.addRow(new Object[]{
                    rs.getInt("ID"),
                    rs.getString("Name"),
                    rs.getString("Type"),
                    stock,
                    reorderLevel,
                    rs.getObject("SupplierID"),
                    status
                });
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            showError("Error loading stock alerts: " + e.getMessage());
        }
        
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    // NEW: Reports Panel
    private void showReportsPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 20, 20));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        
        // Sales Report
        JButton salesReportBtn = createReportButton("Sales Report", "Generate sales summary report");
        salesReportBtn.addActionListener(e -> generateSalesReport());
        panel.add(salesReportBtn);
        
        // Inventory Report
        JButton inventoryReportBtn = createReportButton("Inventory Report", "Current stock levels");
        inventoryReportBtn.addActionListener(e -> generateInventoryReport());
        panel.add(inventoryReportBtn);
        
        // Expiry Report
        JButton expiryReportBtn = createReportButton("Expiry Report", "Medicines expiring soon");
        expiryReportBtn.addActionListener(e -> generateExpiryReport());
        panel.add(expiryReportBtn);
        
        // Customer Report
        JButton customerReportBtn = createReportButton("Customer Report", "Top customers by purchases");
        customerReportBtn.addActionListener(e -> generateCustomerReport());
        panel.add(customerReportBtn);
        
        // Supplier Report
        JButton supplierReportBtn = createReportButton("Supplier Report", "Medicine suppliers overview");
        supplierReportBtn.addActionListener(e -> generateSupplierReport());
        panel.add(supplierReportBtn);
        
        // Activity Log
        JButton activityLogBtn = createReportButton("Activity Log", "User activity tracking");
        activityLogBtn.addActionListener(e -> showActivityLog());
        panel.add(activityLogBtn);
        
        contentPanel.add(panel, BorderLayout.CENTER);
    }
    
    private JButton createReportButton(String title, String description) {
        JButton btn = new JButton("<html><center><b>" + title + "</b><br>" + 
                                  "<small>" + description + "</small></center></html>");
        btn.setBackground(new Color(33, 150, 243));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        btn.setPreferredSize(new Dimension(250, 80));
        return btn;
    }
    
    private void generateSalesReport() {
        JDialog dialog = new JDialog(this, "Sales Report", true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);
        
        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);
        table.setRowHeight(25);
        
        model.addColumn("Sale ID");
        model.addColumn("Customer");
        model.addColumn("Medicine");
        model.addColumn("Quantity");
        model.addColumn("Total Price (SAR)");
        model.addColumn("Date");
        
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT s.ID, c.Name as CustomerName, m.Name as MedicineName, " +
                "s.Quantity, s.TotalPrice, s.Date " +
                "FROM sales s " +
                "LEFT JOIN customer c ON s.CustomerID = c.ID " +
                "LEFT JOIN medicine m ON s.MedicineID = m.ID " +
                "ORDER BY s.Date DESC"
            );
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("ID"),
                    rs.getString("CustomerName"),
                    rs.getString("MedicineName"),
                    rs.getInt("Quantity"),
                    String.format("%.2f", rs.getDouble("TotalPrice")),
                    rs.getDate("Date")
                });
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            showError("Error generating sales report: " + e.getMessage());
        }
        
        dialog.add(new JScrollPane(table));
        dialog.setVisible(true);
    }
    
    private void generateInventoryReport() {
        JDialog dialog = new JDialog(this, "Inventory Report", true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);
        
        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);
        table.setRowHeight(25);
        
        model.addColumn("Medicine ID");
        model.addColumn("Name");
        model.addColumn("Type");
        model.addColumn("Stock");
        model.addColumn("Price");
        model.addColumn("Total Value");
        model.addColumn("Last Restocked");
        
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT m.ID, m.Name, m.Type, m.StockQuantity, m.Price, " +
                "(m.StockQuantity * m.Price) as TotalValue, i.LastRestockedDate " +
                "FROM medicine m " +
                "LEFT JOIN inventory i ON m.ID = i.MedicineID " +
                "ORDER BY m.Name"
            );
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("ID"),
                    rs.getString("Name"),
                    rs.getString("Type"),
                    rs.getInt("StockQuantity"),
                    String.format("%.2f", rs.getDouble("Price")),
                    String.format("%.2f", rs.getDouble("TotalValue")),
                    rs.getDate("LastRestockedDate")
                });
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            showError("Error generating inventory report: " + e.getMessage());
        }
        
        dialog.add(new JScrollPane(table));
        dialog.setVisible(true);
    }
    
    private void generateExpiryReport() {
        JDialog dialog = new JDialog(this, "Expiry Report", true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);
        
        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);
        table.setRowHeight(25);
        
        model.addColumn("Medicine ID");
        model.addColumn("Name");
        model.addColumn("Type");
        model.addColumn("Expiry Date");
        model.addColumn("Days Until Expiry");
        model.addColumn("Stock");
        
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT ID, Name, Type, ExpiryDate, StockQuantity, " +
                "DATEDIFF(ExpiryDate, CURDATE()) as DaysUntilExpiry " +
                "FROM medicine " +
                "WHERE ExpiryDate <= DATE_ADD(CURDATE(), INTERVAL 180 DAY) " +
                "ORDER BY ExpiryDate ASC"
            );
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("ID"),
                    rs.getString("Name"),
                    rs.getString("Type"),
                    rs.getDate("ExpiryDate"),
                    rs.getInt("DaysUntilExpiry"),
                    rs.getInt("StockQuantity")
                });
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            showError("Error generating expiry report: " + e.getMessage());
        }
        
        dialog.add(new JScrollPane(table));
        dialog.setVisible(true);
    }
    
    private void generateCustomerReport() {
        JDialog dialog = new JDialog(this, "Customer Report", true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);
        
        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);
        table.setRowHeight(25);
        
        model.addColumn("Customer ID");
        model.addColumn("Name");
        model.addColumn("Contact");
        model.addColumn("Total Purchases");
        model.addColumn("Total Spent (SAR)");
        
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT c.ID, c.Name, c.ContactInfo, " +
                "COUNT(s.ID) as TotalPurchases, " +
                "COALESCE(SUM(s.TotalPrice), 0) as TotalSpent " +
                "FROM customer c " +
                "LEFT JOIN sales s ON c.ID = s.CustomerID " +
                "GROUP BY c.ID, c.Name, c.ContactInfo " +
                "ORDER BY TotalSpent DESC"
            );
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("ID"),
                    rs.getString("Name"),
                    rs.getString("ContactInfo"),
                    rs.getInt("TotalPurchases"),
                    String.format("%.2f", rs.getDouble("TotalSpent"))
                });
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            showError("Error generating customer report: " + e.getMessage());
        }
        
        dialog.add(new JScrollPane(table));
        dialog.setVisible(true);
    }
    
    private void generateSupplierReport() {
        JDialog dialog = new JDialog(this, "Supplier Report", true);
        dialog.setSize(800, 600);
        dialog.setLocationRelativeTo(this);
        
        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);
        table.setRowHeight(25);
        
        model.addColumn("Supplier ID");
        model.addColumn("Name");
        model.addColumn("Contact");
        model.addColumn("Total Medicines");
        model.addColumn("Total Stock Value");
        
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT s.ID, s.Name, s.ContactInfo, " +
                "COUNT(m.ID) as TotalMedicines, " +
                "SUM(m.StockQuantity * m.Price) as TotalValue " +
                "FROM supplier s " +
                "LEFT JOIN medicine m ON s.ID = m.SupplierID " +
                "GROUP BY s.ID, s.Name, s.ContactInfo " +
                "ORDER BY TotalValue DESC"
            );
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("ID"),
                    rs.getString("Name"),
                    rs.getString("ContactInfo"),
                    rs.getInt("TotalMedicines"),
                    String.format("%.2f", rs.getDouble("TotalValue"))
                });
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            showError("Error generating supplier report: " + e.getMessage());
        }
        
        dialog.add(new JScrollPane(table));
        dialog.setVisible(true);
    }
    
    private void exportLowStockReport() {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
            String filename = "LowStock_Report_" + dateFormat.format(new Date()) + ".csv";
            
            java.io.PrintWriter writer = new java.io.PrintWriter(filename);
            writer.println("ID,Medicine Name,Type,Current Stock,Price (SAR),Expiry Date,Status");
            
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT ID, Name, Type, StockQuantity, Price, ExpiryDate " +
                "FROM medicine " +
                "WHERE StockQuantity < 50 " +
                "ORDER BY StockQuantity ASC"
            );
            
            while (rs.next()) {
                int stock = rs.getInt("StockQuantity");
                String status;
                if (stock == 0) status = "OUT OF STOCK";
                else if (stock < 10) status = "CRITICAL";
                else if (stock < 30) status = "LOW";
                else status = "REORDER SOON";
                
                writer.printf("%d,%s,%s,%d,%.2f,%s,%s%n",
                    rs.getInt("ID"),
                    rs.getString("Name"),
                    rs.getString("Type"),
                    stock,
                    rs.getDouble("Price"),
                    rs.getDate("ExpiryDate"),
                    status
                );
            }
            
            writer.close();
            rs.close();
            stmt.close();
            
            JOptionPane.showMessageDialog(this, 
                "Report exported successfully to: " + filename,
                "Export Successful", 
                JOptionPane.INFORMATION_MESSAGE);
            
            logActivity("EXPORT", "Exported low stock report");
            
        } catch (Exception e) {
            showError("Export failed: " + e.getMessage());
        }
    }
    
    // Activity logging
    private void logActivity(String action, String description) {
        try {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO activity_log (Username, Role, Action, Description, Timestamp) VALUES (?, ?, ?, ?, NOW())"
            );
            ps.setString(1, currentUsername);
            ps.setString(2, currentRole.name());
            ps.setString(3, action);
            ps.setString(4, description);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            // Silently fail if activity_log table doesn't exist
            System.err.println("Activity logging failed: " + e.getMessage());
        }
    }
    
    private void showActivityLog() {
        JDialog dialog = new JDialog(this, "Activity Log", true);
        dialog.setSize(900, 600);
        dialog.setLocationRelativeTo(this);
        
        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);
        table.setRowHeight(25);
        
        model.addColumn("ID");
        model.addColumn("Username");
        model.addColumn("Role");
        model.addColumn("Action");
        model.addColumn("Description");
        model.addColumn("Timestamp");
        
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(
                "SELECT * FROM activity_log ORDER BY Timestamp DESC LIMIT 100"
            );
            
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("ID"),
                    rs.getString("Username"),
                    rs.getString("Role"),
                    rs.getString("Action"),
                    rs.getString("Description"),
                    rs.getTimestamp("Timestamp")
                });
            }
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            showError("Error loading activity log: " + e.getMessage());
        }
        
        dialog.add(new JScrollPane(table));
        dialog.setVisible(true);
    }

    private void refreshTable(String tableName, DefaultTableModel model) {
        try {
            // Clear existing rows
            model.setRowCount(0);
            
            // Reload data
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName);
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            
            // Add data rows
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= cols; i++) {
                    row.add(rs.getObject(i));
                }
                model.addRow(row);
            }
            
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            showError("Error refreshing table: " + e.getMessage());
        }
    }

    private void buildCRUDPanel(String tableName, String[] fields) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Table properties for better readability
        table.setRowHeight(25);
        table.setIntercellSpacing(new Dimension(10, 5));
        table.setGridColor(Color.LIGHT_GRAY);
        
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

        try {
            Statement stmt = conn.createStatement(); 
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName);
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();

            // Add column headers
            for (int i = 1; i <= cols; i++) {
                model.addColumn(meta.getColumnName(i));
            }

            // Add data rows
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= cols; i++) {
                    row.add(rs.getObject(i));
                }
                model.addRow(row);
            }
            
            // Add selection listener for table
            table.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting() && table.getSelectedRow() != -1) {
                    int row = table.getSelectedRow();
                    for (int i = 0; i < fields.length && i < model.getColumnCount(); i++) {
                        if (i < inputs.length) {
                            Object value = model.getValueAt(row, i);
                            inputs[i].setText(value != null ? value.toString() : "");
                        }
                    }
                }
            });
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            showError("Load error: " + e.getMessage());
        }

        // Create input panel with fields
        JPanel inputPanel = new JPanel(new GridLayout(0, 2, 10, 5));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Add/Edit " + tableName));
        
        inputs = new JTextField[fields.length];
        
        for (int i = 0; i < fields.length; i++) {
            JLabel label = new JLabel(fields[i] + ":");
            label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            inputPanel.add(label);
            
            inputs[i] = new JTextField(20);
            inputs[i].setEnabled(hasPermission("update") || hasPermission("add")); 
            inputPanel.add(inputs[i]);
        }
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        
        JButton addBtn = createStyledButton("Add");
        JButton updateBtn = createStyledButton("Update");
        JButton deleteBtn = createStyledButton("Delete");
        JButton clearBtn = createStyledButton("Clear");
        
        // Only add buttons if user has permission
        if (hasPermission("add")) {
            buttonPanel.add(addBtn);
        }
        if (hasPermission("update")) {
            buttonPanel.add(updateBtn);
        }
        if (hasPermission("delete")) {
            buttonPanel.add(deleteBtn);
        }
        buttonPanel.add(clearBtn);
        
        // Action listeners
        addBtn.addActionListener(e -> {
            if (!hasPermission("add")) {
                JOptionPane.showMessageDialog(this, "You don't have permission to add records.", 
                    "Permission Denied", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            StringBuilder fields_str = new StringBuilder();
            StringBuilder placeholders = new StringBuilder();
            
            for (String field : fields) {
                fields_str.append(field).append(",");
                placeholders.append("?,");
            }
            
            // Remove trailing comma
            if (fields.length > 0) {
                fields_str.deleteCharAt(fields_str.length() - 1);
                placeholders.deleteCharAt(placeholders.length() - 1);
            }
            
            String sql = "INSERT INTO " + tableName + " (" + fields_str + ") VALUES (" + placeholders + ")";
            
            try {
                PreparedStatement ps = conn.prepareStatement(sql);
                for (int i = 0; i < fields.length; i++) {
                    String value = inputs[i].getText().trim();
                    if (value.isEmpty()) {
                        ps.setNull(i + 1, Types.NULL);
                    } else {
                        ps.setString(i + 1, value);
                    }
                }
                ps.executeUpdate();
                ps.close();
                JOptionPane.showMessageDialog(this, "Record added successfully!");
                logActivity("ADD", "Added record to " + tableName);
                clearInputs();
                refreshTable(tableName, model);
            } catch (SQLException ex) {
                showError("Insert failed: " + ex.getMessage());
            }
        });
        
        updateBtn.addActionListener(e -> {
            if (!hasPermission("update")) {
                JOptionPane.showMessageDialog(this, "You don't have permission to update records.", 
                    "Permission Denied", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a record to update", 
                    "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Build SET clause
            StringBuilder setClause = new StringBuilder();
            for (String field : fields) {
                setClause.append(field).append("=?,");
            }
            setClause.deleteCharAt(setClause.length() - 1);
            
            Object id = model.getValueAt(row, 0);
            String pk = model.getColumnName(0);
            
            String sql = "UPDATE " + tableName + " SET " + setClause + " WHERE " + pk + "=?";
            
            try {
                PreparedStatement ps = conn.prepareStatement(sql);
                for (int i = 0; i < fields.length; i++) {
                    String value = inputs[i].getText().trim();
                    if (value.isEmpty()) {
                        ps.setNull(i + 1, Types.NULL);
                    } else {
                        ps.setString(i + 1, value);
                    }
                }
                ps.setObject(fields.length + 1, id);
                ps.executeUpdate();
                ps.close();
                JOptionPane.showMessageDialog(this, "Record updated successfully!");
                logActivity("UPDATE", "Updated record in " + tableName);
                clearInputs();
                refreshTable(tableName, model);
            } catch (SQLException ex) {
                showError("Update failed: " + ex.getMessage());
            }
        });
        
        deleteBtn.addActionListener(e -> {
            if (!hasPermission("delete")) {
                JOptionPane.showMessageDialog(this, "You don't have permission to delete records.", 
                    "Permission Denied", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Please select a record to delete", 
                    "No Selection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            Object id = model.getValueAt(row, 0);
            String pk = model.getColumnName(0);
            
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete this record?", 
                "Confirm Delete", JOptionPane.YES_NO_OPTION);
                
            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    PreparedStatement ps = conn.prepareStatement("DELETE FROM " + tableName + " WHERE " + pk + "=?");
                    ps.setObject(1, id);
                    ps.executeUpdate();
                    ps.close();
                    JOptionPane.showMessageDialog(this, "Record deleted successfully!");
                    logActivity("DELETE", "Deleted record from " + tableName);
                    clearInputs();
                    refreshTable(tableName, model);
                } catch (SQLException ex) {
                    showError("Delete failed: " + ex.getMessage());
                }
            }
        });
        
        clearBtn.addActionListener(e -> clearInputs());
        
        // Search panel
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        JTextField searchField = new JTextField(20);
        JButton searchBtn = createStyledButton("Search");
        JButton resetBtn = createStyledButton("Reset");
        
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchBtn);
        searchPanel.add(resetBtn);
        
        searchBtn.addActionListener(e -> {
            String searchTerm = searchField.getText().trim();
            if (searchTerm.isEmpty()) {
                switchPanel(tableName);
                return;
            }
            
            StringBuilder whereClause = new StringBuilder();
            
            for (int i = 0; i < fields.length; i++) {
                whereClause.append(fields[i]).append(" LIKE ?");
                if (i < fields.length - 1) {
                    whereClause.append(" OR ");
                }
            }
            
            String sql = "SELECT * FROM " + tableName + " WHERE " + whereClause;
            
            try {
                PreparedStatement ps = conn.prepareStatement(sql);
                for (int i = 0; i < fields.length; i++) {
                    ps.setString(i + 1, "%" + searchTerm + "%");
                }
                
                ResultSet rs = ps.executeQuery();
                
                // Clear current table data
                model.setRowCount(0);
                
                // Add search results
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    for (int i = 1; i <= cols; i++) {
                        row.add(rs.getObject(i));
                    }
                    model.addRow(row);
                }
                
                rs.close();
                ps.close();
            } catch (SQLException ex) {
                showError("Search failed: " + ex.getMessage());
            }
        });
        
        resetBtn.addActionListener(e -> {
            searchField.setText("");
            refreshTable(tableName, model);
        });
        
        // Main panel layout
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(searchPanel, BorderLayout.NORTH);
        
        // Only show input section if user has add/update permissions
        if (hasPermission("add") || hasPermission("update")) {
            topPanel.add(inputPanel, BorderLayout.CENTER);
            topPanel.add(buttonPanel, BorderLayout.SOUTH);
        }
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Display role-based access information
        JPanel roleInfoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        roleInfoPanel.setBackground(new Color(240, 240, 240));
        JLabel roleLabel = new JLabel("Current Role: " + currentRole + " - " + 
                                      "Permissions: " + String.join(", ", ROLE_PERMISSIONS.get(currentRole)));
        roleLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        roleInfoPanel.add(roleLabel);
        panel.add(roleInfoPanel, BorderLayout.SOUTH);
        
        contentPanel.add(panel, BorderLayout.CENTER);
    }
    
    private JTextField[] inputs;
    
    private void clearInputs() {
        for (JTextField field : inputs) {
            field.setText("");
        }
    }
    
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setBackground(new Color(33, 150, 243));
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        return button;
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
        System.err.println("Error: " + msg);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PharmacyDB_Enhanced().setVisible(true));
    }
}
