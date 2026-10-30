import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/** MVC view: Swing widgets, forms, and rendering; no authentication or device logic. */
public final class SwingHomeView extends JFrame {
    private static final Color INK = new Color(24, 43, 57);
    private static final Color MUTED = new Color(93, 110, 122);
    private static final Color ACCENT = new Color(0, 113, 108);
    private static final Color BACKGROUND = new Color(242, 246, 247);
    private static final Color LINE = new Color(219, 228, 231);
    private static final Color ERROR = new Color(174, 45, 55);

    // Package-private controls are wired by the controller, not by the model.
    final JTextField loginUsername = field("Login username");
    final JPasswordField loginPassword = password("Login password");
    final JTextField registerUsername = field("Registration username");
    final JPasswordField registerPassword = password("Registration password");
    final JPasswordField confirmPassword = password("Confirm password");
    final JButton loginButton = button("Sign in", true);
    final JButton registerButton = button("Create account", true);
    final JButton logoutButton = button("Log out", false);
    final JButton addButton = button("+ Add device", true);
    final JButton cloneButton = button("Clone", false);
    final JButton onButton = button("Turn ON", false);
    final JButton offButton = button("Turn OFF", false);
    final JButton connectionButton = button("Connection", false);
    final JButton presetButton = button("Home preset", false);
    final JButton saveButton = button("Save snapshot", false);
    final JButton restoreButton = button("Restore", false);
    final JButton undoButton = button("Undo change", false);
    final JButton securityButton = button("Trigger event", true);
    final JComboBox<String> securityType = new JComboBox<>(new String[]{"Motion", "Door", "Alarm"});
    final JTextField securityLocation = field("Security event location");
    final JTable deviceTable;

    private final CardLayout cards = new CardLayout();
    private final JPanel screens = new JPanel(cards);
    private final JTabbedPane authTabs = new JTabbedPane();
    private final JLabel authStatus = new JLabel(" ");
    private final JLabel status = new JLabel("Ready.");
    private final JLabel welcome = new JLabel();
    private final JLabel totalValue = metricValue();
    private final JLabel onValue = metricValue();
    private final JLabel lightsValue = metricValue();
    private final JLabel fansValue = metricValue();
    private final JLabel emptyHint = new JLabel("No devices yet. Click + Add device to get started.");
    private final JTextArea activity = new JTextArea();
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Device name", "Type", "Room", "Connection", "Power"}, 0) {
        public boolean isCellEditable(int row, int column) { return false; }
    };
    private List<Device> displayedDevices = List.of();

    public SwingHomeView() {
        super("Smart Home System");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(980, 720));
        setSize(1160, 820);
        setLocationRelativeTo(null);
        deviceTable = new JTable(tableModel);
        screens.add(authenticationScreen(), "authentication");
        screens.add(homeScreen(), "home");
        setContentPane(screens);
        authTabs.addChangeListener(event -> {
            getRootPane().setDefaultButton(authTabs.getSelectedIndex() == 0 ? loginButton : registerButton);
            setAuthenticationStatus(" ", false);
        });
        deviceTable.getSelectionModel().addListSelectionListener(event -> updateSelectionButtons());
        showAuthentication();
    }

    private JPanel authenticationScreen() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(BACKGROUND);
        JPanel brand = new JPanel();
        brand.setLayout(new BoxLayout(brand, BoxLayout.Y_AXIS));
        brand.setBackground(INK);
        brand.setPreferredSize(new Dimension(360, 0));
        brand.setBorder(BorderFactory.createEmptyBorder(64, 38, 42, 30));
        JLabel eyebrow = label("SMART HOME", 14, Font.BOLD, new Color(116, 223, 204));
        brand.add(eyebrow);
        brand.add(Box.createVerticalStrut(36));
        brand.add(label("Your home.", 34, Font.BOLD, Color.WHITE));
        brand.add(label("In your control.", 30, Font.BOLD, Color.WHITE));
        brand.add(Box.createVerticalStrut(28));
        brand.add(label("Lights, fans, and security", 16, Font.PLAIN, new Color(199, 215, 223)));
        brand.add(label("together in one place.", 16, Font.PLAIN, new Color(199, 215, 223)));
        brand.add(Box.createVerticalGlue());
        brand.add(label("DESKTOP CONTROL CENTER", 12, Font.BOLD, new Color(116, 223, 204)));
        brand.add(Box.createVerticalStrut(10));
        brand.add(label("Local device simulation", 13, Font.PLAIN, new Color(199, 215, 223)));
        page.add(brand, BorderLayout.WEST);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        JPanel card = new JPanel(new BorderLayout(0, 20));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(30, 32, 26, 32)));
        card.setPreferredSize(new Dimension(490, 500));
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 10));
        heading.setOpaque(false);
        heading.add(label("Welcome home", 28, Font.BOLD, INK));
        heading.add(label("Sign in or create your local account.", 14, Font.PLAIN, MUTED));
        card.add(heading, BorderLayout.NORTH);

        JPanel login = form();
        formRow(login, "Username", loginUsername);
        formRow(login, "Password", loginPassword);
        login.add(Box.createVerticalStrut(8));
        alignButton(loginButton);
        login.add(loginButton);
        JPanel register = form();
        formRow(register, "Username", registerUsername);
        formRow(register, "Password (at least 4 characters)", registerPassword);
        formRow(register, "Confirm password", confirmPassword);
        alignButton(registerButton);
        register.add(registerButton);
        authTabs.addTab("Sign in", login);
        authTabs.addTab("Create account", register);
        card.add(authTabs, BorderLayout.CENTER);
        authStatus.setPreferredSize(new Dimension(420, 42));
        authStatus.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        card.add(authStatus, BorderLayout.SOUTH);
        center.add(card);
        page.add(center, BorderLayout.CENTER);
        return page;
    }

    private JPanel homeScreen() {
        JPanel page = new JPanel(new BorderLayout(0, 18));
        page.setBackground(BACKGROUND);
        page.setBorder(BorderFactory.createEmptyBorder(24, 28, 18, 28));
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JPanel heading = new JPanel(new GridLayout(2, 1, 0, 6));
        heading.setOpaque(false);
        heading.add(label("Home dashboard", 28, Font.BOLD, INK));
        welcome.setForeground(MUTED);
        heading.add(welcome);
        top.add(heading, BorderLayout.CENTER);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 4));
        right.setOpaque(false);
        right.add(label("LOCAL SIMULATION", 12, Font.BOLD, ACCENT));
        right.add(logoutButton);
        top.add(right, BorderLayout.EAST);
        page.add(top, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        JPanel metrics = new JPanel(new GridLayout(1, 4, 14, 0));
        metrics.setOpaque(false);
        metrics.add(metric("TOTAL DEVICES", totalValue));
        metrics.add(metric("POWERED ON", onValue));
        metrics.add(metric("LIGHTS", lightsValue));
        metrics.add(metric("FANS", fansValue));
        body.add(metrics, BorderLayout.NORTH);

        JPanel devices = surface(new BorderLayout(0, 10));
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tools.setOpaque(false);
        tools.add(addButton);
        tools.add(cloneButton);
        tools.add(onButton);
        tools.add(offButton);
        tools.add(connectionButton);
        tools.add(presetButton);
        devices.add(tools, BorderLayout.NORTH);
        deviceTable.setName("Devices");
        deviceTable.setRowHeight(38);
        deviceTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        deviceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        deviceTable.setAutoCreateRowSorter(true);
        deviceTable.setFillsViewportHeight(true);
        deviceTable.setShowGrid(false);
        deviceTable.setIntercellSpacing(new Dimension(0, 0));
        deviceTable.setSelectionBackground(new Color(221, 243, 237));
        deviceTable.setSelectionForeground(INK);
        deviceTable.getTableHeader().setReorderingAllowed(false);
        deviceTable.getTableHeader().setPreferredSize(new Dimension(0, 36));
        deviceTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        deviceTable.getColumnModel().getColumn(0).setPreferredWidth(230);
        deviceTable.getColumnModel().getColumn(2).setPreferredWidth(180);
        deviceTable.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean selected, boolean focus, int row, int column) {
                Component cell = super.getTableCellRendererComponent(table, value, selected, focus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                cell.setForeground("ON".equals(value) ? ACCENT : MUTED);
                cell.setFont(cell.getFont().deriveFont(Font.BOLD));
                return cell;
            }
        });
        JScrollPane deviceScroll = new JScrollPane(deviceTable);
        deviceScroll.setBorder(BorderFactory.createLineBorder(LINE));
        devices.add(deviceScroll, BorderLayout.CENTER);
        emptyHint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        emptyHint.setForeground(MUTED);
        devices.add(emptyHint, BorderLayout.SOUTH);

        JPanel lower = new JPanel(new BorderLayout(16, 0));
        lower.setOpaque(false);
        JPanel controls = new JPanel(new GridLayout(2, 1, 0, 12));
        controls.setOpaque(false);
        controls.setPreferredSize(new Dimension(385, 245));
        JPanel security = surface(new BorderLayout(0, 9));
        security.add(label("Security event", 16, Font.BOLD, INK), BorderLayout.NORTH);
        JPanel eventFields = new JPanel(new BorderLayout(8, 0));
        eventFields.setOpaque(false);
        securityType.setName("Security event type");
        eventFields.add(securityType, BorderLayout.WEST);
        securityLocation.setText("Entrance");
        eventFields.add(securityLocation, BorderLayout.CENTER);
        security.add(eventFields, BorderLayout.CENTER);
        security.add(securityButton, BorderLayout.SOUTH);
        controls.add(security);
        JPanel history = surface(new BorderLayout(0, 8));
        history.add(label("Home history", 16, Font.BOLD, INK), BorderLayout.NORTH);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        actions.setOpaque(false);
        actions.add(saveButton);
        actions.add(restoreButton);
        actions.add(undoButton);
        history.add(actions, BorderLayout.CENTER);
        history.add(label("Snapshots last for this application run.", 12, Font.PLAIN, MUTED), BorderLayout.SOUTH);
        controls.add(history);
        lower.add(controls, BorderLayout.WEST);

        JPanel events = surface(new BorderLayout(0, 8));
        events.add(label("Activity & notifications", 16, Font.BOLD, INK), BorderLayout.NORTH);
        activity.setEditable(false);
        activity.setLineWrap(true);
        activity.setWrapStyleWord(true);
        activity.setFont(new Font("Consolas", Font.PLAIN, 12));
        activity.setForeground(INK);
        activity.setMargin(new Insets(8, 8, 8, 8));
        JScrollPane activityScroll = new JScrollPane(activity);
        activityScroll.setBorder(BorderFactory.createLineBorder(LINE));
        events.add(activityScroll, BorderLayout.CENTER);
        lower.add(events, BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, devices, lower);
        split.setBorder(null);
        split.setOpaque(false);
        split.setResizeWeight(0.55);
        split.setDividerSize(12);
        devices.setPreferredSize(new Dimension(900, 280));
        lower.setPreferredSize(new Dimension(900, 245));
        devices.setMinimumSize(new Dimension(0, 180));
        lower.setMinimumSize(new Dimension(0, 245));
        body.add(split, BorderLayout.CENTER);
        page.add(body, BorderLayout.CENTER);
        status.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        status.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        page.add(status, BorderLayout.SOUTH);
        return page;
    }

    public void showAuthentication() {
        loginPassword.setText("");
        registerPassword.setText("");
        confirmPassword.setText("");
        displayedDevices = List.of();
        tableModel.setRowCount(0);
        activity.setText("");
        updateSelectionButtons();
        authTabs.setSelectedIndex(0);
        cards.show(screens, "authentication");
        getRootPane().setDefaultButton(loginButton);
        SwingUtilities.invokeLater(() -> loginUsername.requestFocusInWindow());
    }

    public void showHome(HomeModel model) {
        loginPassword.setText("");
        registerPassword.setText("");
        confirmPassword.setText("");
        welcome.setText("Welcome, " + model.getOwner().username() + ". Your devices at a glance.");
        cards.show(screens, "home");
        getRootPane().setDefaultButton(null);
        render(model);
        showStatus("Select a device to control it, or add your first device.", false);
    }

    public void render(HomeModel model) {
        UUID selected = selectedDevice();
        displayedDevices = model.getDevices();
        tableModel.setRowCount(0);
        int selectedIndex = -1;
        int on = 0;
        int lights = 0;
        for (int index = 0; index < displayedDevices.size(); index++) {
            Device device = displayedDevices.get(index);
            tableModel.addRow(new Object[]{device.getName(), device.getType().label(), device.getRoom(),
                    device.getCommunicationName(), device.isOn() ? "ON" : "OFF"});
            if (device.getId().equals(selected)) { selectedIndex = index; }
            if (device.isOn()) { on++; }
            if (device.getType() == DeviceType.LIGHT) { lights++; }
        }
        if (selectedIndex >= 0) {
            int row = deviceTable.convertRowIndexToView(selectedIndex);
            deviceTable.setRowSelectionInterval(row, row);
        } else if (!displayedDevices.isEmpty()) {
            deviceTable.setRowSelectionInterval(0, 0);
        }
        totalValue.setText(String.valueOf(displayedDevices.size()));
        onValue.setText(String.valueOf(on));
        lightsValue.setText(String.valueOf(lights));
        fansValue.setText(String.valueOf(displayedDevices.size() - lights));
        emptyHint.setText(displayedDevices.isEmpty() ? "No devices yet. Click + Add device to get started."
                : "Select a row to control a device. Devices are kept for this application run.");
        List<String> events = model.getActivity();
        activity.setText(events.isEmpty() ? "Your home activity will appear here." : String.join("\n", events));
        activity.setCaretPosition(activity.getDocument().getLength());
        updateSelectionButtons();
    }

    public UUID selectedDevice() {
        int row = deviceTable.getSelectedRow();
        if (row < 0) { return null; }
        int index = deviceTable.convertRowIndexToModel(row);
        return index < displayedDevices.size() ? displayedDevices.get(index).getId() : null;
    }

    private void updateSelectionButtons() {
        boolean selected = selectedDevice() != null;
        for (JButton button : List.of(cloneButton, onButton, offButton, connectionButton)) {
            button.setEnabled(selected);
        }
    }

    public void setAuthenticationBusy(boolean busy) {
        loginButton.setEnabled(!busy);
        registerButton.setEnabled(!busy);
        authTabs.setEnabled(!busy);
        for (JTextField field : List.of(loginUsername, loginPassword,
                registerUsername, registerPassword, confirmPassword)) { field.setEnabled(!busy); }
    }

    public void registrationComplete(String username) {
        loginUsername.setText(username);
        registerUsername.setText("");
        registerPassword.setText("");
        confirmPassword.setText("");
        authTabs.setSelectedIndex(0);
        setAuthenticationStatus("Account created. Sign in to continue.", false);
        loginPassword.requestFocusInWindow();
    }

    public void setAuthenticationStatus(String message, boolean error) {
        authStatus.setForeground(error ? ERROR : ACCENT);
        authStatus.setText("<html>" + escape(message) + "</html>");
    }

    public void showStatus(String message, boolean error) {
        status.setForeground(error ? ERROR : ACCENT);
        status.setText(message);
    }

    String authenticationMessage() { return authStatus.getText(); }
    String statusMessage() { return status.getText(); }

    public void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Smart Home", JOptionPane.WARNING_MESSAGE);
    }

    public DeviceForm requestDevice(boolean preset) {
        JComboBox<String> type = new JComboBox<>(preset
                ? new String[]{"Normal Home - Light", "Security Home - Fan"}
                : new String[]{"Light", "Fan"});
        JTextField name = field("Device name");
        JTextField room = field("Room");
        room.setText("Living Room");
        JComboBox<String> connection = new JComboBox<>(new String[]{"WiFi", "Bluetooth"});
        JPanel form = dialogForm();
        addDialogRow(form, "Type", type);
        addDialogRow(form, "Name", name);
        addDialogRow(form, "Room", room);
        addDialogRow(form, "Connection", connection);
        while (JOptionPane.showConfirmDialog(this, form, preset ? "Create home preset" : "Add device",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            if (name.getText().isBlank() || room.getText().isBlank()) {
                showError("Enter a device name and room.");
                continue;
            }
            return new DeviceForm(type.getSelectedIndex() == 0 ? DeviceType.LIGHT : DeviceType.FAN,
                    name.getText().trim(), room.getText().trim(), (String) connection.getSelectedItem());
        }
        return null;
    }

    public String requestCloneName(String currentName) {
        return (String) JOptionPane.showInputDialog(this, "Name for the cloned device:", "Clone device",
                JOptionPane.PLAIN_MESSAGE, null, null, currentName + " Copy");
    }

    public String requestConnection(String current) {
        return (String) JOptionPane.showInputDialog(this, "Communication method:", "Change connection",
                JOptionPane.PLAIN_MESSAGE, null, new String[]{"WiFi", "Bluetooth"}, current);
    }

    public boolean confirmRestore() {
        return JOptionPane.showConfirmDialog(this,
                "Restore the saved device list, settings, and power states?\nYou can undo this change.",
                "Restore home snapshot", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }

    public record DeviceForm(DeviceType type, String name, String room, String connection) { }

    private static JPanel form() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 4, 8, 4));
        return panel;
    }

    private static void formRow(JPanel panel, String title, JTextField field) {
        JLabel label = label(title, 12, Font.BOLD, MUTED);
        label.setLabelFor(field);
        panel.add(label);
        panel.add(Box.createVerticalStrut(5));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(field);
        panel.add(Box.createVerticalStrut(12));
    }

    private static JPanel dialogForm() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setPreferredSize(new Dimension(370, 175));
        return panel;
    }

    private static void addDialogRow(JPanel panel, String title, Component component) {
        int row = panel.getComponentCount() / 2;
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(5, 5, 5, 12);
        JLabel label = new JLabel(title);
        label.setLabelFor(component);
        panel.add(label, c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        panel.add(component, c);
    }

    private static JPanel surface(java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(LINE),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        return panel;
    }

    private static JPanel metric(String title, JLabel value) {
        JPanel panel = surface(new BorderLayout(0, 3));
        panel.add(label(title, 11, Font.BOLD, MUTED), BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    private static JLabel metricValue() { return label("0", 28, Font.BOLD, INK); }

    private static JLabel label(String text, int size, int style, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", style, size));
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    private static JTextField field(String name) {
        JTextField field = new JTextField(20);
        field.setName(name);
        field.getAccessibleContext().setAccessibleName(name);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        return field;
    }

    private static JPasswordField password(String name) {
        JPasswordField field = new JPasswordField(20);
        field.setName(name);
        field.getAccessibleContext().setAccessibleName(name);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        return field;
    }

    private static JButton button(String text, boolean primary) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setFocusPainted(false);
        button.setMargin(new Insets(7, 10, 7, 10));
        if (primary) {
            button.setBackground(ACCENT);
            button.setForeground(Color.WHITE);
        } else {
            button.setBackground(Color.WHITE);
            button.setForeground(INK);
        }
        return button;
    }

    private static void alignButton(JButton button) {
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
