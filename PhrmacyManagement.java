import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

public class PharmacyDB1 extends JFrame {
    //URL for database goes here 
    private static final String URL = "";
    //User for database here 
    private static final String USER = "";
    //password for database here
    private static final String PASS = "";
    private Connection conn;

    private JPanel contentPanel;
    // logo path goes here
    private static final String LOGO_PATH = "";
    private ImageIcon logoIcon;
    
    public enum UserRole {
        ADMIN, PHARMACIST, ASSISTANT
    }
    
    private UserRole currentRole;
    //username password role
    private static final Map<String, String[]> USERS = new HashMap<>();
    static {
        USERS.put("admin", new String[]{"admin", UserRole.ADMIN.name()});
        USERS.put("pharmacist", new String[]{"pharmacist", UserRole.PHARMACIST.name()});
        USERS.put("assistant", new String[]{"assistant", UserRole.ASSISTANT.name()});
    }
    
    private static final Map<String, UserRole[]> SECTION_ACCESS = new HashMap<>();
    static {
        SECTION_ACCESS.put("Dashboard", new UserRole[]{UserRole.ADMIN, UserRole.PHARMACIST, UserRole.ASSISTANT});
        SECTION_ACCESS.put("Medicines", new UserRole[]{UserRole.ADMIN, UserRole.PHARMACIST, UserRole.ASSISTANT});
        SECTION_ACCESS.put("Customers", new UserRole[]{UserRole.ADMIN, UserRole.PHARMACIST, UserRole.ASSISTANT});
        SECTION_ACCESS.put("Staff", new UserRole[]{UserRole.ADMIN});
        SECTION_ACCESS.put("Suppliers", new UserRole[]{UserRole.ADMIN, UserRole.PHARMACIST});
        SECTION_ACCESS.put("Sales", new UserRole[]{UserRole.ADMIN, UserRole.PHARMACIST, UserRole.ASSISTANT});
        SECTION_ACCESS.put("Prescriptions", new UserRole[]{UserRole.ADMIN, UserRole.PHARMACIST});
        SECTION_ACCESS.put("Inventory", new UserRole[]{UserRole.ADMIN, UserRole.PHARMACIST, UserRole.ASSISTANT});
        SECTION_ACCESS.put("Pharmacists", new UserRole[]{UserRole.ADMIN});
    }
    
    private static final Map<UserRole, String[]> ROLE_PERMISSIONS = new HashMap<>();
    static {
        ROLE_PERMISSIONS.put(UserRole.ADMIN, new String[]{"add", "update", "delete", "view"});
        ROLE_PERMISSIONS.put(UserRole.PHARMACIST, new String[]{"add", "update", "view"});
        ROLE_PERMISSIONS.put(UserRole.ASSISTANT, new String[]{"view"});
    }

    public PharmacyDB1() {
        setTitle("Pharmacy Management System");
        setSize(1000, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        connect();
        loadLogo();
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
        getContentPane().removeAll();
        
        JPanel loginPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(33, 150, 243), 2),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));
        
        GridBagConstraints formGbc = new GridBagConstraints();
        formGbc.fill = GridBagConstraints.HORIZONTAL;
        formGbc.insets = new Insets(5, 5, 5, 5);
        
        JLabel logoLabel = new JLabel(logoIcon);
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        formGbc.gridx = 0;
        formGbc.gridy = 0;
        formGbc.gridwidth = 2;
        formPanel.add(logoLabel, formGbc);
        
        JLabel titleLabel = new JLabel("Pharmacy Management System", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(33, 150, 243));
        formGbc.gridx = 0;
        formGbc.gridy = 1;
        formGbc.gridwidth = 2;
        formPanel.add(titleLabel, formGbc);
        
        JLabel subtitleLabel = new JLabel("Login to continue", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formGbc.gridy = 2;
        formGbc.gridwidth = 2;
        formGbc.insets = new Insets(5, 5, 15, 5);
        formPanel.add(subtitleLabel, formGbc);
        
        formGbc.gridwidth = 1;
        formGbc.insets = new Insets(5, 5, 5, 5);
        formGbc.gridy = 3;
        formGbc.gridx = 0;
        formPanel.add(new JLabel("Username:"), formGbc);
        
        JTextField usernameField = new JTextField(15);
        formGbc.gridx = 1;
        formPanel.add(usernameField, formGbc);
        
        formGbc.gridy = 4;
        formGbc.gridx = 0;
        formPanel.add(new JLabel("Password:"), formGbc);
        
        JPasswordField passwordField = new JPasswordField(15);
        formGbc.gridx = 1;
        formPanel.add(passwordField, formGbc);
      
        JButton loginButton = new JButton("Login");
        loginButton.setBackground(new Color(33, 150, 243));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        formGbc.gridy = 5;
        formGbc.gridx = 0;
        formGbc.gridwidth = 2;
        formGbc.insets = new Insets(15, 5, 5, 5);
        formPanel.add(loginButton, formGbc);
        
        JLabel errorLabel = new JLabel("", SwingConstants.CENTER);
        errorLabel.setForeground(Color.RED);
        formGbc.gridy = 6;
        formPanel.add(errorLabel, formGbc);
        
        loginPanel.add(formPanel);
        add(loginPanel);
        //Password For login
        loginButton.addActionListener(e -> {
            try {
                String username = usernameField.getText();
                String password = new String(passwordField.getPassword());
                
                if (username == null || username.trim().isEmpty()) {
                    throw new IllegalArgumentException("Username cannot be empty");
                }
                
                if (USERS.containsKey(username) && USERS.get(username)[0].equals(password)) {
                    currentRole = UserRole.valueOf(USERS.get(username)[1]);
                    initializeMainUI();
                } else {
                    throw new Exception("Invalid username or password");
                }
            } catch (Exception ex) {
                errorLabel.setText("Invalid username or password");
                System.err.println("Login error: " + ex.getMessage());
                passwordField.setText("");
            }
        });
        
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
        getContentPane().removeAll();
        
        contentPanel = new JPanel(new BorderLayout());

        JPanel sidebar = new JPanel(new GridLayout(9, 1, 0, 10));
        sidebar.setPreferredSize(new Dimension(200, 0));
        sidebar.setBackground(new Color(33, 150, 243));

        String[] buttons = {"Dashboard", "Medicines", "Customers", "Staff", "Suppliers", 
                           "Sales", "Prescriptions", "Inventory", "Pharmacists"};
        for (String name : buttons) {
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
        
        JPanel headerLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        headerLeft.setBackground(new Color(3, 169, 244));
        
        JLabel headerLogo = new JLabel(new ImageIcon(logoIcon.getImage().getScaledInstance(40, 40, Image.SCALE_SMOOTH)));
        headerLeft.add(headerLogo);
        
        JLabel lbl = new JLabel("Welcome to Pharmacy Dashboard - Logged in as: " + currentRole);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lbl.setForeground(Color.WHITE);
        headerLeft.add(lbl);
        
        header.add(headerLeft, BorderLayout.WEST);
        
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(new Color(25, 118, 210));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.addActionListener(e -> showLoginScreen());
        
        JPanel headerRight = new JPanel();
        headerRight.setBackground(new Color(3, 169, 244));
        headerRight.add(logoutBtn);
        header.add(headerRight, BorderLayout.EAST);

        add(sidebar, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        
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
            case "Prescription Details" -> buildCRUDPanel("prescription_details", 
                new String[]{"PrescriptionID", "MedicineID", "Quantity", "Dosage"});
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    private void showDashboard() {
        JPanel panel = new JPanel(new GridLayout(2, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        panel.add(card("Total Medicines", scalar("SELECT COUNT(*) FROM medicine")));
        panel.add(card("Total Sales (SAR)", scalar("SELECT COALESCE(SUM(TotalPrice),0) FROM sales")));
        panel.add(card("Low Stock Items (< 100)", scalar("SELECT COUNT(*) FROM medicine WHERE StockQuantity < 100")));
        panel.add(card("Total Customers", scalar("SELECT COUNT(*) FROM customer")));

        contentPanel.add(panel, BorderLayout.CENTER);
    }

    private JPanel card(String title, String value) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 2));
        p.setPreferredSize(new Dimension(200, 100));

        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        JLabel valLbl = new JLabel(value, SwingConstants.CENTER);
        valLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        valLbl.setForeground(new Color(33, 150, 243));

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
            } catch (SQLException e) {}
        }
    }

    private void buildCRUDPanel(String tableName, String[] fields) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        DefaultTableModel model = new DefaultTableModel();
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
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

            for (int i = 1; i <= cols; i++) {
                model.addColumn(meta.getColumnName(i));
            }

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= cols; i++) {
                    row.add(rs.getObject(i));
                }
                model.addRow(row);
            }
            
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
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        
        JButton addBtn = createStyledButton("Add");
        JButton updateBtn = createStyledButton("Update");
        JButton deleteBtn = createStyledButton("Delete");
        JButton clearBtn = createStyledButton("Clear");
        JButton exportBtn = createStyledButton("Export");

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
        buttonPanel.add(exportBtn);
        
        exportBtn.addActionListener(e -> {
            exportToTextFile(tableName, model);
        });
        
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
                clearInputs();
                switchPanel(tableName);
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
                clearInputs();
                switchPanel(tableName);
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
                    clearInputs();
                    switchPanel(tableName);
                } catch (SQLException ex) {
                    showError("Delete failed: " + ex.getMessage());
                }
            }
        });
        
        clearBtn.addActionListener(e -> clearInputs());
        
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
                model.setRowCount(0);
                
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
            switchPanel(tableName);
        });
        
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(searchPanel, BorderLayout.NORTH);
        
        if (hasPermission("add") || hasPermission("update")) {
            topPanel.add(inputPanel, BorderLayout.CENTER);
            topPanel.add(buttonPanel, BorderLayout.SOUTH);
        }
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        
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
    
    private void exportToTextFile(String tableName, DefaultTableModel model) {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No data to export", 
                "Export Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Export File");
        fileChooser.setSelectedFile(new File(tableName + "_export.txt"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                writer.println("Pharmacy Management System - " + tableName + " Export");
                writer.println("Role: " + currentRole);
                writer.println("----------------------------------------");
                writer.println();
                
                for (int i = 0; i < model.getColumnCount(); i++) {
                    writer.print(model.getColumnName(i));
                    if (i < model.getColumnCount() - 1) {
                        writer.print("\t");
                    }
                }
                writer.println();
                
                for (int i = 0; i < model.getColumnCount(); i++) {
                    writer.print("--------");
                    if (i < model.getColumnCount() - 1) {
                        writer.print("\t");
                    }
                }
                writer.println();
                
                for (int row = 0; row < model.getRowCount(); row++) {
                    for (int col = 0; col < model.getColumnCount(); col++) {
                        Object value = model.getValueAt(row, col);
                        writer.print(value != null ? value.toString() : "");
                        if (col < model.getColumnCount() - 1) {
                            writer.print("\t");
                        }
                    }
                    writer.println();
                }
            
            JOptionPane.showMessageDialog(this, "Data exported successfully to: " + file.getAbsolutePath(),
                "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            showError("Export failed: " + ex.getMessage());
        }
    }
}


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new PharmacyDB1().setVisible(true));
    }
}
