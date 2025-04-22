package com.example.mitchelltutorial;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Date;

public class VehicleRentalSystem extends Application {

    private Connection conn;
    private boolean offlineMode = false;
    private final List<Map<String, Object>> offlineVehicles = new ArrayList<>();
    private final List<Map<String, Object>> offlineCustomers = new ArrayList<>();
    private final List<Map<String, Object>> offlineBookings = new ArrayList<>();
    private final List<Map<String, Object>> offlinePayments = new ArrayList<>();
    private final List<Map<String, Object>> offlineUsers = new ArrayList<>();

    private String currentUser = "";
    private String currentUserRole = "";
    private final TabPane tabPane = new TabPane();

    // Tables for data display
    private final TableView<Vehicle> vehicleTable = new TableView<>();
    private final TableView<Customer> customerTable = new TableView<>();
    private final TableView<Booking> bookingTable = new TableView<>();
    private final TableView<Payment> paymentTable = new TableView<>();

    // Observable lists for data
    private final ObservableList<Vehicle> vehicleData = FXCollections.observableArrayList();
    private final ObservableList<Customer> customerData = FXCollections.observableArrayList();
    private final ObservableList<Booking> bookingData = FXCollections.observableArrayList();
    private final ObservableList<Payment> paymentData = FXCollections.observableArrayList();

    // Charts for reporting
    private final PieChart vehicleCategoryChart = new PieChart();
    private final BarChart<String, Number> revenueChart = new BarChart<>(new CategoryAxis(), new NumberAxis());
    private final LineChart<String, Number> bookingTrendChart = new LineChart<>(new CategoryAxis(), new NumberAxis());

    @Override
    public void start(Stage primaryStage) {
        connectToDatabase();
        initializeUI(primaryStage);
    }

    private void initializeUI(Stage primaryStage) {
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        // Create tabs in order
        tabPane.getTabs().addAll(
                new Tab("Welcome", createWelcomeTab()),
                new Tab("Login", createAuthTab()),
                new Tab("Dashboard", new Label("Please login to view dashboard")),
                new Tab("Vehicle Management", createVehicleTab()),
                new Tab("Customer Management", createCustomerTab()),
                new Tab("Booking System", createBookingTab()),
                new Tab("Payments & Billing", createBillingTab()),
                new Tab("Reports", createReportsTab()),
                new Tab("User Management", createUserManagementTab())
        );

        disableTabsAfterLogin();

        // Stylish root layout
        BorderPane root = new BorderPane(tabPane);
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #E3F2FD, #BBDEFB);");

        Scene scene = new Scene(root, 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Hycianth's Vehicle Rental System" + (offlineMode ? " (Offline Mode)" : ""));
        primaryStage.show();
    }

    private Node createWelcomeTab() {
        // Create a background image with a Tesla
        ImageView background = new ImageView(new Image("https://tesla-cdn.thron.com/delivery/public/image/tesla/03e533bf-8b1d-463f-9813-9a597aafb280/bvlatuR/std/4096x2560/M3-Homepage-Desktop-LHD"));
        background.setFitWidth(1200);
        background.setFitHeight(800);
        background.setPreserveRatio(false);
        background.setOpacity(0.7);

        // Create overlay content
        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(50));

        Text title = new Text("Welcome to Hycianth's Vehicle Rental System");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 46));
        title.setFill(Color.DARKBLUE);

        Text subtitle = new Text("Your Premium Vehicle Rental Experience");
        subtitle.setFont(Font.font("Arial", FontWeight.NORMAL, 30));
        subtitle.setFill(Color.DARKBLUE);

        Button proceedButton = new Button("Proceed to Login");
        proceedButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10 20;");
        proceedButton.setOnAction(e -> tabPane.getSelectionModel().select(1)); // Switch to login tab

        content.getChildren().addAll(title, subtitle, proceedButton);

        // StackPane to overlay text on image
        StackPane welcomePane = new StackPane();
        welcomePane.getChildren().addAll(background, content);

        return welcomePane;
    }

    private Node createAuthTab() {
        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(25, 25, 25, 25));
        grid.setStyle("-fx-background-color: rgba(255,255,255,0.9); -fx-background-radius: 10;");

        Label title = new Label("Hycianth's Vehicle Rental Login");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 30));
        title.setTextFill(Color.DARKBLUE);
        grid.add(title, 0, 0, 2, 1);

        Label userName = new Label("Username:");
        userName.setFont(Font.font(16));
        grid.add(userName, 0, 1);

        TextField userTextField = new TextField();
        userTextField.setPromptText("Enter your username");
        userTextField.setStyle("-fx-font-size: 14px; -fx-padding: 8;");
        grid.add(userTextField, 1, 1);

        Label pw = new Label("Password:");
        pw.setFont(Font.font(16));
        grid.add(pw, 0, 2);

        PasswordField pwBox = new PasswordField();
        pwBox.setPromptText("Enter your password");
        pwBox.setStyle("-fx-font-size: 14px; -fx-padding: 8;");
        grid.add(pwBox, 1, 2);

        Label roleLabel = new Label("Role:");
        roleLabel.setFont(Font.font(16));
        grid.add(roleLabel, 0, 3);

        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("Admin", "Employee");
        roleCombo.setStyle("-fx-font-size: 14px;");
        roleCombo.setPromptText("Select your role");
        grid.add(roleCombo, 1, 3);

        Button btn = new Button("Sign in");
        btn.setStyle("-fx-background-color: yellow; -fx-text-fill: white; -fx-font-size: 16px; -fx-padding: 10 20;");
        HBox hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(btn);
        grid.add(hbBtn, 1, 4);

        final Label actiontarget = new Label();
        actiontarget.setStyle("-fx-text-fill: #F44336; -fx-font-size: 14px;");
        grid.add(actiontarget, 1, 6);

        btn.setOnAction(e -> {
            String username = userTextField.getText();
            String password = pwBox.getText();
            String role = roleCombo.getValue();

            if (username.isEmpty() || password.isEmpty() || role == null) {
                actiontarget.setText("Please fill all fields!");
                return;
            }

            if (authenticate(username, password, role)) {
                currentUser = username;
                currentUserRole = role;
                actiontarget.setText("Login successful!");
                actiontarget.setStyle("-fx-text-fill: #4CAF50;");
                enableTabsAfterLogin();
                showDashboard();
                loadVehicleData();
                loadCustomerData();
                loadBookingData();
                loadPaymentData();
                if (role.equals("Employee")) {
                    tabPane.getTabs().get(8).setDisable(true); // Disable User Management for employees
                }
                tabPane.getSelectionModel().select(2); // Switch to dashboard
            } else {
                actiontarget.setText("Invalid credentials!");
                actiontarget.setStyle("-fx-text-fill: #F44336;");
            }
        });

        return grid;
    }

    private Node createVehicleTab() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: #ffffff; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Title with icon
        HBox titleBox = new HBox(10);
        Label title = new Label("Vehicle Management");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0D47A1;");
        titleBox.getChildren().add(title);

        // Search functionality
        HBox searchBox = new HBox(10);
        TextField searchField = new TextField();
        searchField.setPromptText("Search vehicles...");
        searchField.setStyle("-fx-prompt-text-fill: derive(-fx-control-inner-background, -30%);");
        Button searchButton = new Button("Search");
        searchButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        searchButton.setOnAction(e -> searchVehicles(searchField.getText()));
        searchBox.getChildren().addAll(searchField, searchButton);

        // Table setup
        setupVehicleTable();

        // Action buttons with modern styling
        HBox buttonBox = new HBox(15);
        Button addButton = createStyledButton("Add Vehicle", "#4CAF50");
        Button editButton = createStyledButton("Edit Vehicle", "#2196F3");
        Button deleteButton = createStyledButton("Delete Vehicle", "#F44336");
        Button exportButton = createStyledButton("Export to CSV", "#FF9800");

        addButton.setOnAction(e -> showAddVehicleDialog());
        editButton.setOnAction(e -> editSelectedVehicle());
        deleteButton.setOnAction(e -> deleteSelectedVehicle());
        exportButton.setOnAction(e -> exportVehiclesToCSV());

        buttonBox.getChildren().addAll(addButton, editButton, deleteButton, exportButton);

        vbox.getChildren().addAll(titleBox, searchBox, vehicleTable, buttonBox);
        return new ScrollPane(vbox);
    }

    private void deleteSelectedVehicle() {
        Vehicle selected = vehicleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a vehicle to delete");
            return;
        }

        try {
            if (offlineMode) {
                offlineVehicles.removeIf(v -> v.get("id").equals(selected.getId()));
            } else {
                PreparedStatement ps = conn.prepareStatement("DELETE FROM vehicles WHERE id = ?");
                ps.setString(1, selected.getId());
                ps.executeUpdate();
            }
            showAlert("Success", "Vehicle deleted successfully");
            loadVehicleData();
        } catch (SQLException e) {
            showAlert("Error", "Failed to delete vehicle: " + e.getMessage());
        }
    }

    private void showAddVehicleDialog() {
        Dialog<Vehicle> dialog = new Dialog<>();
        dialog.setTitle("Add New Vehicle");
        dialog.setHeaderText("Enter vehicle details");

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create the form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        TextField idField = new TextField();
        idField.setPromptText("Vehicle ID");
        TextField brandField = new TextField();
        brandField.setPromptText("Brand");
        TextField modelField = new TextField();
        modelField.setPromptText("Model");
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Car", "SUV", "Truck", "Van", "Bike");
        TextField priceField = new TextField();
        priceField.setPromptText("Daily Rate");
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("Available", "Rented", "Maintenance");

        grid.add(new Label("ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Brand:"), 0, 1);
        grid.add(brandField, 1, 1);
        grid.add(new Label("Model:"), 0, 2);
        grid.add(modelField, 1, 2);
        grid.add(new Label("Category:"), 0, 3);
        grid.add(categoryCombo, 1, 3);
        grid.add(new Label("Daily Rate:"), 0, 4);
        grid.add(priceField, 1, 4);
        grid.add(new Label("Status:"), 0, 5);
        grid.add(statusCombo, 1, 5);

        dialog.getDialogPane().setContent(grid);

        // Convert the result to a vehicle when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    return new Vehicle(
                            idField.getText(),
                            brandField.getText(),
                            modelField.getText(),
                            categoryCombo.getValue(),
                            Double.parseDouble(priceField.getText()),
                            statusCombo.getValue()
                    );
                } catch (NumberFormatException e) {
                    showAlert("Error", "Please enter a valid daily rate");
                    return null;
                }
            }
            return null;
        });

        Optional<Vehicle> result = dialog.showAndWait();

        result.ifPresent(vehicle -> {
            try {
                if (offlineMode) {
                    Map<String, Object> vehicleMap = new HashMap<>();
                    vehicleMap.put("id", vehicle.getId());
                    vehicleMap.put("brand", vehicle.getBrand());
                    vehicleMap.put("model", vehicle.getModel());
                    vehicleMap.put("category", vehicle.getCategory());
                    vehicleMap.put("dailyRate", vehicle.getDailyRate());
                    vehicleMap.put("status", vehicle.getStatus());
                    offlineVehicles.add(vehicleMap);
                } else {
                    PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO vehicles (id, brand, model, category, daily_rate, status) VALUES (?, ?, ?, ?, ?, ?)");
                    ps.setString(1, vehicle.getId());
                    ps.setString(2, vehicle.getBrand());
                    ps.setString(3, vehicle.getModel());
                    ps.setString(4, vehicle.getCategory());
                    ps.setDouble(5, vehicle.getDailyRate());
                    ps.setString(6, vehicle.getStatus());
                    ps.executeUpdate();
                }
                showAlert("Success", "Vehicle added successfully");
                loadVehicleData();
            } catch (SQLException e) {
                showAlert("Error", "Failed to add vehicle: " + e.getMessage());
            }
        });
    }

    private void editSelectedVehicle() {
        Vehicle selected = vehicleTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Error", "Please select a vehicle to edit");
            return;
        }

        Dialog<Vehicle> dialog = new Dialog<>();
        dialog.setTitle("Edit Vehicle");
        dialog.setHeaderText("Edit vehicle details");

        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Create the form
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));

        TextField idField = new TextField(selected.getId());
        idField.setDisable(true);
        TextField brandField = new TextField(selected.getBrand());
        TextField modelField = new TextField(selected.getModel());
        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll("Car", "SUV", "Truck", "Van", "Bike");
        categoryCombo.setValue(selected.getCategory());
        TextField priceField = new TextField(String.valueOf(selected.getDailyRate()));
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll("Available", "Rented", "Maintenance");
        statusCombo.setValue(selected.getStatus());

        grid.add(new Label("ID:"), 0, 0);
        grid.add(idField, 1, 0);
        grid.add(new Label("Brand:"), 0, 1);
        grid.add(brandField, 1, 1);
        grid.add(new Label("Model:"), 0, 2);
        grid.add(modelField, 1, 2);
        grid.add(new Label("Category:"), 0, 3);
        grid.add(categoryCombo, 1, 3);
        grid.add(new Label("Daily Rate:"), 0, 4);
        grid.add(priceField, 1, 4);
        grid.add(new Label("Status:"), 0, 5);
        grid.add(statusCombo, 1, 5);

        dialog.getDialogPane().setContent(grid);

        // Convert the result to a vehicle when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                try {
                    return new Vehicle(
                            idField.getText(),
                            brandField.getText(),
                            modelField.getText(),
                            categoryCombo.getValue(),
                            Double.parseDouble(priceField.getText()),
                            statusCombo.getValue()
                    );
                } catch (NumberFormatException e) {
                    showAlert("Error", "Please enter a valid daily rate");
                    return null;
                }
            }
            return null;
        });

        Optional<Vehicle> result = dialog.showAndWait();

        result.ifPresent(vehicle -> {
            try {
                if (offlineMode) {
                    for (Map<String, Object> v : offlineVehicles) {
                        if (v.get("id").equals(vehicle.getId())) {
                            v.put("brand", vehicle.getBrand());
                            v.put("model", vehicle.getModel());
                            v.put("category", vehicle.getCategory());
                            v.put("dailyRate", vehicle.getDailyRate());
                            v.put("status", vehicle.getStatus());
                            break;
                        }
                    }
                } else {
                    PreparedStatement ps = conn.prepareStatement(
                            "UPDATE vehicles SET brand = ?, model = ?, category = ?, daily_rate = ?, status = ? WHERE id = ?");
                    ps.setString(1, vehicle.getBrand());
                    ps.setString(2, vehicle.getModel());
                    ps.setString(3, vehicle.getCategory());
                    ps.setDouble(4, vehicle.getDailyRate());
                    ps.setString(5, vehicle.getStatus());
                    ps.setString(6, vehicle.getId());
                    ps.executeUpdate();
                }
                showAlert("Success", "Vehicle updated successfully");
                loadVehicleData();
            } catch (SQLException e) {
                showAlert("Error", "Failed to update vehicle: " + e.getMessage());
            }
        });
    }

    private void searchVehicles(String text) {
        if (text == null || text.isEmpty()) {
            vehicleTable.setItems(vehicleData);
            return;
        }

        String searchText = text.toLowerCase();
        ObservableList<Vehicle> filteredList = FXCollections.observableArrayList();

        for (Vehicle vehicle : vehicleData) {
            if (vehicle.getId().toLowerCase().contains(searchText) ||
                    vehicle.getBrand().toLowerCase().contains(searchText) ||
                    vehicle.getModel().toLowerCase().contains(searchText) ||
                    vehicle.getCategory().toLowerCase().contains(searchText) ||
                    vehicle.getStatus().toLowerCase().contains(searchText)) {
                filteredList.add(vehicle);
            }
        }

        vehicleTable.setItems(filteredList);
    }

    private void setupVehicleTable() {
        vehicleTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        vehicleTable.setStyle("-fx-background-color: white; -fx-border-color: #B0BEC5;");

        TableColumn<Vehicle, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Vehicle, String> brandCol = new TableColumn<>("Brand");
        brandCol.setCellValueFactory(new PropertyValueFactory<>("brand"));

        TableColumn<Vehicle, String> modelCol = new TableColumn<>("Model");
        modelCol.setCellValueFactory(new PropertyValueFactory<>("model"));

        TableColumn<Vehicle, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Vehicle, Double> priceCol = new TableColumn<>("Daily Rate");
        priceCol.setCellValueFactory(new PropertyValueFactory<>("dailyRate"));
        priceCol.setCellFactory(tc -> new TableCell<Vehicle, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("M%.2f", price));
                    setStyle("-fx-alignment: CENTER-RIGHT;");
                }
            }
        });

        TableColumn<Vehicle, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(tc -> new TableCell<Vehicle, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("Available".equals(status)) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                    }
                }
            }
        });

        vehicleTable.getColumns().addAll(idCol, brandCol, modelCol, categoryCol, priceCol, statusCol);
        loadVehicleData();
    }

    private Node createCustomerTab() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: #ffffff; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Title with icon
        HBox titleBox = new HBox(10);
        Label title = new Label("Customer Management");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0D47A1;");
        titleBox.getChildren().add(title);

        // Customer form
        GridPane form = new GridPane();
        form.setVgap(15);
        form.setHgap(15);
        form.setPadding(new Insets(20));

        // Form fields with modern styling
        TextField idField = createStyledTextField("Customer ID");
        TextField nameField = createStyledTextField("Full Name");
        TextField phoneField = createStyledTextField("Phone Number");
        TextField emailField = createStyledTextField("Email");
        TextField licenseField = createStyledTextField("Driving License");
        DatePicker dobPicker = new DatePicker();
        dobPicker.setStyle("-fx-font-size: 14px;");

        form.addRow(0, new Label("Customer ID:"), idField);
        form.addRow(1, new Label("Full Name:"), nameField);
        form.addRow(2, new Label("Phone:"), phoneField);
        form.addRow(3, new Label("Email:"), emailField);
        form.addRow(4, new Label("License:"), licenseField);
        form.addRow(5, new Label("Date of Birth:"), dobPicker);

        // Action buttons
        HBox buttonBox = new HBox(15);
        Button addButton = createStyledButton("Add Customer", "#4CAF50");
        Button updateButton = createStyledButton("Update", "#2196F3");
        Button clearButton = createStyledButton("Clear", "#9E9E9E");

        addButton.setOnAction(e -> addCustomer(
                idField.getText(),
                nameField.getText(),
                phoneField.getText(),
                emailField.getText(),
                licenseField.getText(),
                dobPicker.getValue()
        ));

        clearButton.setOnAction(e -> {
            idField.clear();
            nameField.clear();
            phoneField.clear();
            emailField.clear();
            licenseField.clear();
            dobPicker.setValue(null);
        });

        buttonBox.getChildren().addAll(addButton, updateButton, clearButton);

        // Customer table
        setupCustomerTable();

        vbox.getChildren().addAll(titleBox, form, buttonBox, customerTable);
        return new ScrollPane(vbox);
    }

    private void setupCustomerTable() {
        customerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        customerTable.setStyle("-fx-background-color: white; -fx-border-color: #B0BEC5;");

        TableColumn<Customer, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Customer, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Customer, String> phoneCol = new TableColumn<>("Phone");
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));

        TableColumn<Customer, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));

        TableColumn<Customer, String> licenseCol = new TableColumn<>("License");
        licenseCol.setCellValueFactory(new PropertyValueFactory<>("license"));

        TableColumn<Customer, LocalDate> dobCol = new TableColumn<>("Date of Birth");
        dobCol.setCellValueFactory(new PropertyValueFactory<>("dob"));
        dobCol.setCellFactory(tc -> new TableCell<Customer, LocalDate>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                }
            }
        });

        customerTable.getColumns().addAll(idCol, nameCol, phoneCol, emailCol, licenseCol, dobCol);
        loadCustomerData();
    }

    private Node createBookingTab() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: #ffffff; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Title with icon
        HBox titleBox = new HBox(10);
        Label title = new Label("Booking System");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0D47A1;");
        titleBox.getChildren().add(title);

        // Booking form
        GridPane form = new GridPane();
        form.setVgap(15);
        form.setHgap(15);
        form.setPadding(new Insets(20));

        // Form fields
        ComboBox<Customer> customerCombo = new ComboBox<>();
        customerCombo.setPromptText("Select Customer");
        customerCombo.setConverter(new StringConverter<Customer>() {
            @Override
            public String toString(Customer customer) {
                return customer == null ? "" : customer.getName() + " (" + customer.getId() + ")";
            }

            @Override
            public Customer fromString(String string) {
                return null; // Not needed
            }
        });

        ComboBox<Vehicle> vehicleCombo = new ComboBox<>();
        vehicleCombo.setPromptText("Select Vehicle");
        vehicleCombo.setConverter(new StringConverter<Vehicle>() {
            @Override
            public String toString(Vehicle vehicle) {
                return vehicle == null ? "" : vehicle.getBrand() + " " + vehicle.getModel() + " (" + vehicle.getId() + ")";
            }

            @Override
            public Vehicle fromString(String string) {
                return null; // Not needed
            }
        });

        DatePicker startDatePicker = new DatePicker(LocalDate.now());
        DatePicker endDatePicker = new DatePicker(LocalDate.now().plusDays(1));

        // Load data into combos
        customerCombo.setItems(customerData);
        vehicleCombo.setItems(vehicleData.filtered(v -> "Available".equals(v.getStatus())));

        form.addRow(0, new Label("Customer:"), customerCombo);
        form.addRow(1, new Label("Vehicle:"), vehicleCombo);
        form.addRow(2, new Label("Start Date:"), startDatePicker);
        form.addRow(3, new Label("End Date:"), endDatePicker);

        // Calculate price button
        Button calculateButton = createStyledButton("Calculate Price", "#2196F3");
        Label priceLabel = new Label("M0.00");
        priceLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");

        calculateButton.setOnAction(e -> {
            if (vehicleCombo.getValue() != null && startDatePicker.getValue() != null
                    && endDatePicker.getValue() != null) {
                long days = endDatePicker.getValue().toEpochDay() - startDatePicker.getValue().toEpochDay();
                double total = days * vehicleCombo.getValue().getDailyRate();
                priceLabel.setText(String.format("M%.2f", total));
            }
        });

        form.addRow(4, calculateButton, priceLabel);

        // Action buttons
        HBox buttonBox = new HBox(15);
        Button bookButton = createStyledButton("Create Booking", "#4CAF50");
        Button cancelButton = createStyledButton("Cancel Booking", "#F44336");

        bookButton.setOnAction(e -> {
            if (customerCombo.getValue() == null || vehicleCombo.getValue() == null ||
                    startDatePicker.getValue() == null || endDatePicker.getValue() == null) {
                showAlert("Error", "Please fill all fields");
                return;
            }

            createBooking(
                    customerCombo.getValue().getId(),
                    vehicleCombo.getValue().getId(),
                    startDatePicker.getValue(),
                    endDatePicker.getValue()
            );
        });

        buttonBox.getChildren().addAll(bookButton, cancelButton);

        // Booking table
        setupBookingTable();

        vbox.getChildren().addAll(titleBox, form, buttonBox, bookingTable);
        return new ScrollPane(vbox);
    }

    private void setupBookingTable() {
        bookingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        bookingTable.setStyle("-fx-background-color: lightbrown; -fx-border-color: #B0BEC5;");

        TableColumn<Booking, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Booking, String> customerCol = new TableColumn<>("Customer");
        customerCol.setCellValueFactory(new PropertyValueFactory<>("customerName"));

        TableColumn<Booking, String> vehicleCol = new TableColumn<>("Vehicle");
        vehicleCol.setCellValueFactory(new PropertyValueFactory<>("vehicleDetails"));

        TableColumn<Booking, LocalDate> startDateCol = new TableColumn<>("Start Date");
        startDateCol.setCellValueFactory(new PropertyValueFactory<>("startDate"));
        startDateCol.setCellFactory(tc -> new TableCell<Booking, LocalDate>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                }
            }
        });

        TableColumn<Booking, LocalDate> endDateCol = new TableColumn<>("End Date");
        endDateCol.setCellValueFactory(new PropertyValueFactory<>("endDate"));
        endDateCol.setCellFactory(tc -> new TableCell<Booking, LocalDate>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                }
            }
        });

        TableColumn<Booking, Double> rateCol = new TableColumn<>("Daily Rate");
        rateCol.setCellValueFactory(new PropertyValueFactory<>("dailyRate"));
        rateCol.setCellFactory(tc -> new TableCell<Booking, Double>() {
            @Override
            protected void updateItem(Double rate, boolean empty) {
                super.updateItem(rate, empty);
                if (empty || rate == null) {
                    setText(null);
                } else {
                    setText(String.format("M%.2f", rate));
                    setStyle("-fx-alignment: CENTER-RIGHT;");
                }
            }
        });

        TableColumn<Booking, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(tc -> new TableCell<Booking, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    if ("Active".equals(status)) {
                        setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;");
                    } else if ("Completed".equals(status)) {
                        setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #F44336; -fx-font-weight: bold;");
                    }
                }
            }
        });

        bookingTable.getColumns().addAll(idCol, customerCol, vehicleCol, startDateCol, endDateCol, rateCol, statusCol);
        loadBookingData();
    }

    private Node createBillingTab() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: Orange; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Title with icon
        HBox titleBox = new HBox(10);
        Label title = new Label("Payments & Billing");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0D47A1;");
        titleBox.getChildren().add(title);

        // Payment form
        GridPane form = new GridPane();
        form.setVgap(15);
        form.setHgap(15);
        form.setPadding(new Insets(20));

        // Form fields
        ComboBox<Booking> bookingCombo = new ComboBox<>();
        bookingCombo.setPromptText("Select Booking");
        bookingCombo.setConverter(new StringConverter<Booking>() {
            @Override
            public String toString(Booking booking) {
                return booking == null ? "" : booking.getCustomerName() + " - " + booking.getVehicleDetails();
            }

            @Override
            public Booking fromString(String string) {
                return null; // Not needed
            }
        });

        bookingCombo.setItems(bookingData.filtered(b -> "Active".equals(b.getStatus())));

        TextField amountField = createStyledTextField("Amount");
        amountField.setEditable(false);

        ComboBox<String> paymentMethod = new ComboBox<>();
        paymentMethod.getItems().addAll("Cash", "Credit Card", "Online Payment");
        paymentMethod.setPromptText("Select Payment Method");

        DatePicker paymentDate = new DatePicker(LocalDate.now());

        // Calculate amount when booking is selected
        bookingCombo.setOnAction(e -> {
            Booking selected = bookingCombo.getValue();
            if (selected != null) {
                long days = selected.getEndDate().toEpochDay() - selected.getStartDate().toEpochDay();
                double total = days * selected.getDailyRate();
                amountField.setText(String.format("M%.2f", total));
            }
        });

        form.addRow(0, new Label("Booking:"), bookingCombo);
        form.addRow(1, new Label("Amount:"), amountField);
        form.addRow(2, new Label("Payment Method:"), paymentMethod);
        form.addRow(3, new Label("Payment Date:"), paymentDate);

        // Action buttons
        HBox buttonBox = new HBox(15);
        Button payButton = createStyledButton("Process Payment", "#4CAF50");
        Button invoiceButton = createStyledButton("Generate Invoice", "#2196F3");

        payButton.setOnAction(e -> {
            if (bookingCombo.getValue() == null || paymentMethod.getValue() == null) {
                showAlert("Error", "Please select a booking and payment method");
                return;
            }

            try {
                double amount = Double.parseDouble(amountField.getText().replace("$", ""));
                processPayment(
                        bookingCombo.getValue().getId(),
                        amount,
                        paymentMethod.getValue(),
                        paymentDate.getValue()
                );
            } catch (NumberFormatException ex) {
                showAlert("Error", "Invalid amount format");
            }
        });

        invoiceButton.setOnAction(e -> {
            if (bookingCombo.getValue() == null) {
                showAlert("Error", "Please select a booking");
                return;
            }
            generateInvoice(bookingCombo.getValue().getId());
        });

        buttonBox.getChildren().addAll(payButton, invoiceButton);

        // Payment history table
        setupPaymentTable();

        vbox.getChildren().addAll(titleBox, form, buttonBox, paymentTable);
        return new ScrollPane(vbox);
    }

    private void setupPaymentTable() {
        paymentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        paymentTable.setStyle("-fx-background-color: Blue; -fx-border-color: #B0BEC5;");

        TableColumn<Payment, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Payment, String> bookingCol = new TableColumn<>("Booking ID");
        bookingCol.setCellValueFactory(new PropertyValueFactory<>("bookingId"));

        TableColumn<Payment, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountCol.setCellFactory(tc -> new TableCell<Payment, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(String.format("M%.2f", amount));
                    setStyle("-fx-alignment: CENTER-RIGHT;");
                }
            }
        });

        TableColumn<Payment, String> methodCol = new TableColumn<>("Method");
        methodCol.setCellValueFactory(new PropertyValueFactory<>("method"));

        TableColumn<Payment, LocalDate> dateCol = new TableColumn<>("Payment Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("paymentDate"));
        dateCol.setCellFactory(tc -> new TableCell<Payment, LocalDate>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                }
            }
        });

        paymentTable.getColumns().addAll(idCol, bookingCol, amountCol, methodCol, dateCol);
        loadPaymentData();
    }

    private Node createReportsTab() {
        VBox vbox = new VBox(15);
        vbox.setPadding(new Insets(20));
        vbox.setStyle("-fx-background-color: purple; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Title with icon
        HBox titleBox = new HBox(10);
        Label title = new Label("Reports & Analytics");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0D47A1;");
        titleBox.getChildren().add(title);

        // Tab pane for different reports
        TabPane reportTabs = new TabPane();

        // Vehicle category distribution (Pie chart)
        Tab categoryTab = new Tab("Vehicle Categories", vehicleCategoryChart);
        vehicleCategoryChart.setTitle("Vehicle Distribution by Category");

        // Revenue by month (Bar chart)
        Tab revenueTab = new Tab("Revenue", revenueChart);
        revenueChart.setTitle("Monthly Revenue");
        ((CategoryAxis) revenueChart.getXAxis()).setLabel("Month");
        ((NumberAxis) revenueChart.getYAxis()).setLabel("Revenue ($)");

        // Booking trends (Line chart)
        Tab trendsTab = new Tab("Booking Trends", bookingTrendChart);
        bookingTrendChart.setTitle("Monthly Booking Trends");
        ((CategoryAxis) bookingTrendChart.getXAxis()).setLabel("Month");
        ((NumberAxis) bookingTrendChart.getYAxis()).setLabel("Number of Bookings");

        reportTabs.getTabs().addAll(categoryTab, revenueTab, trendsTab);

        // Date range controls
        HBox dateRangeBox = new HBox(15);
        DatePicker startDate = new DatePicker(LocalDate.now().minusMonths(6));
        DatePicker endDate = new DatePicker(LocalDate.now());
        Button refreshButton = createStyledButton("Refresh Reports", "#2196F3");

        dateRangeBox.getChildren().addAll(
                new Label("From:"), startDate,
                new Label("To:"), endDate,
                refreshButton
        );

        refreshButton.setOnAction(e -> loadAllReports(startDate.getValue(), endDate.getValue()));

        // Summary report
        TextArea summaryArea = new TextArea();
        summaryArea.setEditable(false);
        summaryArea.setPrefRowCount(5);
        summaryArea.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 14px;");

        Button summaryButton = createStyledButton("Generate Summary Report", "#4CAF50");
        summaryButton.setOnAction(e -> generateSummaryReport(summaryArea));

        // Export buttons
        HBox exportBox = new HBox(15);
        Button exportCSVButton = createStyledButton("Export to CSV", "#FF9800");
        Button exportPDFButton = createStyledButton("Export to PDF", "#F44336");

        exportCSVButton.setOnAction(e -> exportReportsToCSV());
        exportPDFButton.setOnAction(e -> exportReportsToPDF());

        exportBox.getChildren().addAll(exportCSVButton, exportPDFButton);

        vbox.getChildren().addAll(
                titleBox,
                dateRangeBox,
                reportTabs,
                summaryButton,
                summaryArea,
                exportBox
        );

        // Initial load
        loadAllReports(startDate.getValue(), endDate.getValue());

        return new ScrollPane(vbox);
    }

    private Node createUserManagementTab() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));
        vbox.setStyle("-fx-background-color: PURPLE; -fx-border-radius: 10; -fx-background-radius: 10;");

        // Title with icon
        HBox titleBox = new HBox(10);
        Label title = new Label("User Management");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0D47A1;");
        titleBox.getChildren().add(title);

        // Form for adding new users
        GridPane form = new GridPane();
        form.setVgap(10);
        form.setHgap(10);
        form.setPadding(new Insets(20));

        TextField usernameField = createStyledTextField("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        passwordField.setStyle("-fx-font-size: 14px; -fx-padding: 5;");
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("Admin", "Employee");
        roleCombo.setPromptText("Select Role");

        form.addRow(0, new Label("Username:"), usernameField);
        form.addRow(1, new Label("Password:"), passwordField);
        form.addRow(2, new Label("Role:"), roleCombo);

        Button addButton = createStyledButton("Add User", "#4CAF50");
        addButton.setOnAction(e -> {
            String username = usernameField.getText();
            String password = passwordField.getText();
            String role = roleCombo.getValue();

            if (username.isEmpty() || password.isEmpty() || role == null) {
                showAlert("Error", "All fields are required!");
                return;
            }

            if (addUser(username, password, role)) {
                showAlert("Success", "User added successfully!");
                usernameField.clear();
                passwordField.clear();
                roleCombo.setValue(null);
                loadUserData();
            } else {
                showAlert("Error", "Failed to add user or user already exists!");
            }
        });

        form.add(addButton, 1, 3);

        // Table to display existing users
        TableView<User> userTable = new TableView<>();
        TableColumn<User, String> usernameCol = new TableColumn<>("Username");
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));

        userTable.getColumns().addAll(usernameCol, roleCol);

        // Load user data
        loadUserData();

        vbox.getChildren().addAll(titleBox, form, userTable);
        return new ScrollPane(vbox);
    }

    private void showDashboard() {
        VBox box = new VBox(20);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: orange; -fx-border-radius: 10; -fx-background-radius: 10;");

        Label welcome = new Label("Welcome, " + currentUserRole + " " + currentUser + "!");
        welcome.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #0D47A1;");

        // Dashboard statistics
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(20);
        statsGrid.setVgap(10);

        try {
            // Total vehicles
            int totalVehicles = 0;
            if (offlineMode) {
                totalVehicles = offlineVehicles.size();
            } else {
                PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM vehicles");
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    totalVehicles = rs.getInt(1);
                }
            }

            // Total customers
            int totalCustomers = 0;
            if (offlineMode) {
                totalCustomers = offlineCustomers.size();
            } else {
                PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM customers");
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    totalCustomers = rs.getInt(1);
                }
            }

            // Total revenue
            double totalRevenue = 0;
            if (offlineMode) {
                for (Map<String, Object> payment : offlinePayments) {
                    totalRevenue += (double) payment.get("amount");
                }
            } else {
                PreparedStatement ps = conn.prepareStatement("SELECT SUM(amount) FROM payments");
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    totalRevenue = rs.getDouble(1);
                }
            }

            statsGrid.addRow(0,
                    new Label("Total Vehicles:"), new Label(String.valueOf(totalVehicles)),
                    new Label("Total Customers:"), new Label(String.valueOf(totalCustomers))
            );
            statsGrid.addRow(1,
                    new Label("Total Revenue:"), new Label(String.format("$%.2f", totalRevenue)),
                    new Label("Active Bookings:"), new Label(String.valueOf(bookingData.filtered(b -> "Active".equals(b.getStatus())).size()))
            );

        } catch (SQLException ex) {
            showAlert("Error", "Failed to load dashboard statistics.");
        }

        // Quick actions based on role
        VBox quickActions = new VBox(10);
        quickActions.setPadding(new Insets(10));

        if (currentUserRole.equals("Admin")) {
            Button viewVehicles = createStyledButton("View All Vehicles", "#2196F3");
            viewVehicles.setOnAction(e -> tabPane.getSelectionModel().select(3));

            Button viewCustomers = createStyledButton("View All Customers", "#2196F3");
            viewCustomers.setOnAction(e -> tabPane.getSelectionModel().select(4));

            Button generateReport = createStyledButton("Generate Reports", "#4CAF50");
            generateReport.setOnAction(e -> tabPane.getSelectionModel().select(7));

            quickActions.getChildren().addAll(viewVehicles, viewCustomers, generateReport);
        } else {
            Button newBooking = createStyledButton("Create New Booking", "#4CAF50");
            newBooking.setOnAction(e -> tabPane.getSelectionModel().select(5));

            Button viewBookings = createStyledButton("View My Bookings", "#2196F3");
            viewBookings.setOnAction(e -> {
                // Filter bookings for current employee
                bookingTable.setItems(bookingData.filtered(b ->
                        b.getEmployeeId().equals(currentUser)));
                tabPane.getSelectionModel().select(5);
            });

            quickActions.getChildren().addAll(newBooking, viewBookings);
        }

        // Logout button
        Button logoutButton = createStyledButton("Logout", "#F44336");
        logoutButton.setOnAction(e -> {
            currentUser = "";
            currentUserRole = "";
            disableTabsAfterLogin();
            tabPane.getSelectionModel().select(1); // Switch to login tab
            vehicleData.clear();
            customerData.clear();
            bookingData.clear();
            paymentData.clear();
        });

        box.getChildren().addAll(welcome, statsGrid, quickActions, logoutButton);
        tabPane.getTabs().set(2, new Tab("Dashboard", box));
    }

    // Helper methods for creating styled UI components
    private Button createStyledButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 8 15; -fx-background-radius: 5;");
        button.setOnMouseEntered(e -> button.setStyle("-fx-background-color: derive(" + color + ", 20%); " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;"));
        button.setOnMouseExited(e -> button.setStyle("-fx-background-color: " + color + "; " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 15; -fx-background-radius: 5;"));
        return button;
    }

    private TextField createStyledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle("-fx-prompt-text-fill: derive(-fx-control-inner-background, -30%); " +
                "-fx-font-size: 14px; -fx-padding: 5; -fx-background-radius: 5;");
        return field;
    }

    private void disableTabsAfterLogin() {
        for (int i = 2; i < tabPane.getTabs().size(); i++) { // Skip Welcome and Login tabs
            tabPane.getTabs().get(i).setDisable(true);
        }
    }

    private void enableTabsAfterLogin() {
        for (int i = 2; i < tabPane.getTabs().size(); i++) { // Skip Welcome and Login tabs
            tabPane.getTabs().get(i).setDisable(false);
        }
    }

    // Database operations
    private void connectToDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/vehicle_rental", "root", "password");

            // Create tables if they don't exist
            Statement stmt = conn.createStatement();

            // Users table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "username VARCHAR(50) PRIMARY KEY, " +
                    "password VARCHAR(50) NOT NULL, " +
                    "role VARCHAR(20) NOT NULL)");

            // Vehicles table
            stmt.execute("CREATE TABLE IF NOT EXISTS vehicles (" +
                    "id VARCHAR(20) PRIMARY KEY, " +
                    "brand VARCHAR(50) NOT NULL, " +
                    "model VARCHAR(50) NOT NULL, " +
                    "category VARCHAR(30) NOT NULL, " +
                    "daily_rate DECIMAL(10,2) NOT NULL, " +
                    "status VARCHAR(20) NOT NULL)");

            // Customers table
            stmt.execute("CREATE TABLE IF NOT EXISTS customers (" +
                    "id VARCHAR(20) PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "phone VARCHAR(20), " +
                    "email VARCHAR(100), " +
                    "license VARCHAR(50), " +
                    "dob DATE)");

            // Bookings table
            stmt.execute("CREATE TABLE IF NOT EXISTS bookings (" +
                    "id VARCHAR(20) PRIMARY KEY, " +
                    "customer_id VARCHAR(20) NOT NULL, " +
                    "vehicle_id VARCHAR(20) NOT NULL, " +
                    "start_date DATE NOT NULL, " +
                    "end_date DATE NOT NULL, " +
                    "daily_rate DECIMAL(10,2) NOT NULL, " +
                    "status VARCHAR(20) NOT NULL, " +
                    "employee_id VARCHAR(50) NOT NULL, " +
                    "FOREIGN KEY (customer_id) REFERENCES customers(id), " +
                    "FOREIGN KEY (vehicle_id) REFERENCES vehicles(id))");

            // Payments table
            stmt.execute("CREATE TABLE IF NOT EXISTS payments (" +
                    "id VARCHAR(20) PRIMARY KEY, " +
                    "booking_id VARCHAR(20) NOT NULL, " +
                    "amount DECIMAL(10,2) NOT NULL, " +
                    "method VARCHAR(30) NOT NULL, " +
                    "payment_date DATE NOT NULL, " +
                    "FOREIGN KEY (booking_id) REFERENCES bookings(id))");

            // Add default admin user if none exists
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO users VALUES ('admin', 'admin123', 'Admin')");
                stmt.execute("INSERT INTO users VALUES ('employee', 'emp123', 'Employee')");
            }

            // Add sample vehicle data if none exists
            rs = stmt.executeQuery("SELECT COUNT(*) FROM vehicles");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO vehicles VALUES " +
                        "('V001', 'Tesla', 'Model S', 'Car', 150.00, 'Available'), " +
                        "('V002', 'BMW', 'X5', 'SUV', 120.00, 'Available'), " +
                        "('V003', 'Ford', 'Transit', 'Van', 100.00, 'Available'), " +
                        "('V004', 'Toyota', 'Hilux', 'Truck', 110.00, 'Rented'), " +
                        "('V005', 'Honda', 'CBR600RR', 'Bike', 80.00, 'Maintanance')");
            }

            // Add sample customer data if none exists
            rs = stmt.executeQuery("SELECT COUNT(*) FROM customers");
            if (rs.next() && rs.getInt(1) == 0) {
                stmt.execute("INSERT INTO customers VALUES " +
                        "('C001', 'John Apple', '55501234', 'john.com', 'DL12345', '1980-05-15'), " +
                        "('C002', 'Jane Williams', '55595678', 'williams.com', 'DL67890', '1985-10-20')");
            }

            offlineMode = false;
        } catch (Exception e) {
            offlineMode = true;
            showAlert("Offline Mode", "Database connection failed. Running in offline mode with sample data.");

            // Sample offline data
            Map<String, Object> user1 = new HashMap<>();
            user1.put("username", "admin");
            user1.put("password", "admin123");
            user1.put("role", "Admin");

            Map<String, Object> user2 = new HashMap<>();
            user2.put("username", "employee");
            user2.put("password", "emp123");
            user2.put("role", "Employee");

            offlineUsers.add(user1);
            offlineUsers.add(user2);

            // Sample vehicles
            Map<String, Object> vehicle1 = new HashMap<>();
            vehicle1.put("id", "V001");
            vehicle1.put("brand", "Tesla");
            vehicle1.put("model", "Model S");
            vehicle1.put("category", "Car");
            vehicle1.put("dailyRate", 150.00);
            vehicle1.put("status", "Available");

            Map<String, Object> vehicle2 = new HashMap<>();
            vehicle2.put("id", "V002");
            vehicle2.put("brand", "BMW");
            vehicle2.put("model", "X5");
            vehicle2.put("category", "SUV");
            vehicle2.put("dailyRate", 120.00);
            vehicle2.put("status", "Available");

            Map<String, Object> vehicle3 = new HashMap<>();
            vehicle3.put("id", "V003");
            vehicle3.put("brand", "Ford");
            vehicle3.put("model", "Transit");
            vehicle3.put("category", "Van");
            vehicle3.put("dailyRate", 100.00);
            vehicle3.put("status", "Available");

            Map<String, Object> vehicle4 = new HashMap<>();
            vehicle4.put("id", "V004");
            vehicle4.put("brand", "Toyota");
            vehicle4.put("model", "Hilux");
            vehicle4.put("category", "Truck");
            vehicle4.put("dailyRate", 110.00);
            vehicle4.put("status", "Rented");

            Map<String, Object> vehicle5 = new HashMap<>();
            vehicle5.put("id", "V005");
            vehicle5.put("brand", "Honda");
            vehicle5.put("model", "CBR600RR");
            vehicle5.put("category", "Bike");
            vehicle5.put("dailyRate", 80.00);
            vehicle5.put("status", "Maintainance");

            offlineVehicles.add(vehicle1);
            offlineVehicles.add(vehicle2);
            offlineVehicles.add(vehicle3);
            offlineVehicles.add(vehicle4);
            offlineVehicles.add(vehicle5);

            // Sample customers
            Map<String, Object> customer1 = new HashMap<>();
            customer1.put("id", "C001");
            customer1.put("name", "John Apple");
            customer1.put("phone", "55501234");
            customer1.put("email", "john.com");
            customer1.put("license", "DL12345");
            customer1.put("dob", LocalDate.of(1980, 5, 15));

            Map<String, Object> customer2 = new HashMap<>();
            customer2.put("id", "C002");
            customer2.put("name", "Jane Williams");
            customer2.put("phone", "55595678");
            customer2.put("email", "williams.com");
            customer2.put("license", "DL67890");
            customer2.put("dob", LocalDate.of(1985, 10, 20));

            offlineCustomers.add(customer1);
            offlineCustomers.add(customer2);
        }
    }

    private boolean authenticate(String username, String password, String role) {
        if (offlineMode) {
            for (Map<String, Object> user : offlineUsers) {
                if (user.get("username").equals(username) &&
                        user.get("password").equals(password) &&
                        user.get("role").equals(role)) {
                    return true;
                }
            }
            return false;
        }

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM users WHERE username = ? AND password = ? AND role = ?");
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, role);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            return false;
        }
    }

    private boolean addUser(String username, String password, String role) {
        try {
            if (offlineMode) {
                for (Map<String, Object> user : offlineUsers) {
                    if (user.get("username").equals(username)) {
                        return false; // User already exists
                    }
                }

                Map<String, Object> newUser = new HashMap<>();
                newUser.put("username", username);
                newUser.put("password", password);
                newUser.put("role", role);
                offlineUsers.add(newUser);
                return true;
            } else {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO users (username, password, role) VALUES (?, ?, ?)");
                ps.setString(1, username);
                ps.setString(2, password);
                ps.setString(3, role);
                ps.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private void loadUserData() {
        try {
            ObservableList<User> users = FXCollections.observableArrayList();
            if (offlineMode) {
                for (Map<String, Object> user : offlineUsers) {
                    users.add(new User(
                            user.get("username").toString(),
                            "", // Don't show passwords
                            user.get("role").toString()
                    ));
                }
            } else {
                PreparedStatement ps = conn.prepareStatement("SELECT username, role FROM users");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    users.add(new User(
                            rs.getString("username"),
                            "", // Don't show passwords
                            rs.getString("role")
                    ));
                }
            }
            TableView<User> userTable = new TableView<>();
            userTable.setItems(users);
        } catch (SQLException ex) {
            showAlert("Error", "Failed to load user data.");
        }
    }

    private void loadVehicleData() {
        vehicleData.clear();
        try {
            if (offlineMode) {
                for (Map<String, Object> vehicle : offlineVehicles) {
                    vehicleData.add(new Vehicle(
                            vehicle.get("id").toString(),
                            vehicle.get("brand").toString(),
                            vehicle.get("model").toString(),
                            vehicle.get("category").toString(),
                            (double) vehicle.get("dailyRate"),
                            vehicle.get("status").toString()
                    ));
                }
            } else {
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM vehicles");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    vehicleData.add(new Vehicle(
                            rs.getString("id"),
                            rs.getString("brand"),
                            rs.getString("model"),
                            rs.getString("category"),
                            rs.getDouble("daily_rate"),
                            rs.getString("status")
                    ));
                }
            }
            vehicleTable.setItems(vehicleData);
        } catch (SQLException ex) {
            showAlert("Error", "Failed to load vehicle data.");
        }
    }

    private void loadCustomerData() {
        customerData.clear();
        try {
            if (offlineMode) {
                for (Map<String, Object> customer : offlineCustomers) {
                    customerData.add(new Customer(
                            customer.get("id").toString(),
                            customer.get("name").toString(),
                            customer.get("phone").toString(),
                            customer.get("email").toString(),
                            customer.get("license").toString(),
                            (LocalDate) customer.get("dob")
                    ));
                }
            } else {
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM customers");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    customerData.add(new Customer(
                            rs.getString("id"),
                            rs.getString("name"),
                            rs.getString("phone"),
                            rs.getString("email"),
                            rs.getString("license"),
                            rs.getDate("dob").toLocalDate()
                    ));
                }
            }
            customerTable.setItems(customerData);
        } catch (SQLException ex) {
            showAlert("Error", "Failed to load customer data.");
        }
    }

    private void loadBookingData() {
        bookingData.clear();
        try {
            if (offlineMode) {
                // In a real system, we would have offline booking data
                // For demo purposes, we'll create some sample bookings
                if (offlineBookings.isEmpty()) {
                    Map<String, Object> booking1 = new HashMap<>();
                    booking1.put("id", "B001");
                    booking1.put("customer_id", "C001");
                    booking1.put("vehicle_id", "V001");
                    booking1.put("start_date", LocalDate.now().minusDays(5));
                    booking1.put("end_date", LocalDate.now().plusDays(2));
                    booking1.put("daily_rate", 150.00);
                    booking1.put("status", "Active");
                    booking1.put("employee_id", "employee");
                    offlineBookings.add(booking1);

                    Map<String, Object> booking2 = new HashMap<>();
                    booking2.put("id", "B002");
                    booking2.put("customer_id", "C002");
                    booking2.put("vehicle_id", "V002");
                    booking2.put("start_date", LocalDate.now().minusDays(10));
                    booking2.put("end_date", LocalDate.now().minusDays(2));
                    booking2.put("daily_rate", 120.00);
                    booking2.put("status", "Completed");
                    booking2.put("employee_id", "employee");
                    offlineBookings.add(booking2);
                }

                for (Map<String, Object> booking : offlineBookings) {
                    String customerId = booking.get("customer_id").toString();
                    String vehicleId = booking.get("vehicle_id").toString();

                    String customerName = "";
                    for (Map<String, Object> customer : offlineCustomers) {
                        if (customer.get("id").equals(customerId)) {
                            customerName = customer.get("name").toString();
                            break;
                        }
                    }

                    String vehicleDetails = "";
                    for (Map<String, Object> vehicle : offlineVehicles) {
                        if (vehicle.get("id").equals(vehicleId)) {
                            vehicleDetails = vehicle.get("brand") + " " + vehicle.get("model");
                            break;
                        }
                    }

                    bookingData.add(new Booking(
                            booking.get("id").toString(),
                            customerId,
                            customerName,
                            vehicleId,
                            vehicleDetails,
                            (LocalDate) booking.get("start_date"),
                            (LocalDate) booking.get("end_date"),
                            (double) booking.get("daily_rate"),
                            booking.get("status").toString(),
                            booking.get("employee_id").toString()
                    ));
                }
            } else {
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT b.*, v.brand, v.model, c.name as customer_name " +
                                "FROM bookings b " +
                                "JOIN vehicles v ON b.vehicle_id = v.id " +
                                "JOIN customers c ON b.customer_id = c.id");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    bookingData.add(new Booking(
                            rs.getString("id"),
                            rs.getString("customer_id"),
                            rs.getString("customer_name"),
                            rs.getString("vehicle_id"),
                            rs.getString("brand") + " " + rs.getString("model"),
                            rs.getDate("start_date").toLocalDate(),
                            rs.getDate("end_date").toLocalDate(),
                            rs.getDouble("daily_rate"),
                            rs.getString("status"),
                            rs.getString("employee_id")
                    ));
                }
            }
            bookingTable.setItems(bookingData);
        } catch (SQLException ex) {
            showAlert("Error", "Failed to load booking data.");
        }
    }

    private void loadPaymentData() {
        paymentData.clear();
        try {
            if (offlineMode) {
                // In a real system, we would have offline payment data
                // For demo purposes, we'll create some sample payments
                if (offlinePayments.isEmpty()) {
                    Map<String, Object> payment1 = new HashMap<>();
                    payment1.put("id", "P001");
                    payment1.put("booking_id", "B002");
                    payment1.put("amount", 960.00);
                    payment1.put("method", "Credit Card");
                    payment1.put("payment_date", LocalDate.now().minusDays(2));
                    offlinePayments.add(payment1);
                }

                for (Map<String, Object> payment : offlinePayments) {
                    paymentData.add(new Payment(
                            payment.get("id").toString(),
                            payment.get("booking_id").toString(),
                            (double) payment.get("amount"),
                            payment.get("method").toString(),
                            (LocalDate) payment.get("payment_date")
                    ));
                }
            } else {
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM payments");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    paymentData.add(new Payment(
                            rs.getString("id"),
                            rs.getString("booking_id"),
                            rs.getDouble("amount"),
                            rs.getString("method"),
                            rs.getDate("payment_date").toLocalDate()
                    ));
                }
            }
            paymentTable.setItems(paymentData);
        } catch (SQLException ex) {
            showAlert("Error", "Failed to load payment data.");
        }
    }

    private void addCustomer(String id, String name, String phone, String email, String license, LocalDate dob) {
        try {
            if (offlineMode) {
                Map<String, Object> customer = new HashMap<>();
                customer.put("id", id);
                customer.put("name", name);
                customer.put("phone", phone);
                customer.put("email", email);
                customer.put("license", license);
                customer.put("dob", dob);
                offlineCustomers.add(customer);
            } else {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO customers (id, name, phone, email, license, dob) VALUES (?, ?, ?, ?, ?, ?)");
                ps.setString(1, id);
                ps.setString(2, name);
                ps.setString(3, phone);
                ps.setString(4, email);
                ps.setString(5, license);
                ps.setDate(6, java.sql.Date.valueOf(dob));
                ps.executeUpdate();
            }

            showAlert("Success", "Customer added successfully!");
            loadCustomerData(); // Refresh the customer table
        } catch (SQLException e) {
            showAlert("Error", "Failed to add customer: " + e.getMessage());
        }
    }

    private void createBooking(String customerId, String vehicleId, LocalDate startDate, LocalDate endDate) {
        try {
            String bookingId = "B" + System.currentTimeMillis();
            double dailyRate = 0;

            // Get vehicle daily rate
            if (offlineMode) {
                for (Map<String, Object> vehicle : offlineVehicles) {
                    if (vehicle.get("id").equals(vehicleId)) {
                        dailyRate = (double) vehicle.get("dailyRate");
                        break;
                    }
                }
            } else {
                PreparedStatement ps = conn.prepareStatement("SELECT daily_rate FROM vehicles WHERE id = ?");
                ps.setString(1, vehicleId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    dailyRate = rs.getDouble("daily_rate");
                }
            }

            if (offlineMode) {
                Map<String, Object> booking = new HashMap<>();
                booking.put("id", bookingId);
                booking.put("customer_id", customerId);
                booking.put("vehicle_id", vehicleId);
                booking.put("start_date", startDate);
                booking.put("end_date", endDate);
                booking.put("daily_rate", dailyRate);
                booking.put("status", "Active");
                booking.put("employee_id", currentUser);
                offlineBookings.add(booking);

                // Update vehicle status
                for (Map<String, Object> vehicle : offlineVehicles) {
                    if (vehicle.get("id").equals(vehicleId)) {
                        vehicle.put("status", "Rented");
                        break;
                    }
                }
            } else {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO bookings (id, customer_id, vehicle_id, start_date, end_date, daily_rate, status, employee_id) " +
                                "VALUES (?, ?, ?, ?, ?, ?, 'Active', ?)");
                ps.setString(1, bookingId);
                ps.setString(2, customerId);
                ps.setString(3, vehicleId);
                ps.setDate(4, java.sql.Date.valueOf(startDate));
                ps.setDate(5, java.sql.Date.valueOf(endDate));
                ps.setDouble(6, dailyRate);
                ps.setString(7, currentUser);
                ps.executeUpdate();

                // Update vehicle status
                ps = conn.prepareStatement("UPDATE vehicles SET status = 'Rented' WHERE id = ?");
                ps.setString(1, vehicleId);
                ps.executeUpdate();
            }

            showAlert("Success", "Booking created successfully! Booking ID: " + bookingId);
            loadBookingData();
            loadVehicleData();
        } catch (SQLException e) {
            showAlert("Error", "Failed to create booking: " + e.getMessage());
        }
    }

    private void processPayment(String bookingId, double amount, String method, LocalDate paymentDate) {
        try {
            String paymentId = "P" + System.currentTimeMillis();

            if (offlineMode) {
                Map<String, Object> payment = new HashMap<>();
                payment.put("id", paymentId);
                payment.put("booking_id", bookingId);
                payment.put("amount", amount);
                payment.put("method", method);
                payment.put("payment_date", paymentDate);
                offlinePayments.add(payment);

                // Update booking status
                for (Map<String, Object> booking : offlineBookings) {
                    if (booking.get("id").equals(bookingId)) {
                        booking.put("status", "Completed");
                        break;
                    }
                }

                // Update vehicle status
                for (Map<String, Object> booking : offlineBookings) {
                    if (booking.get("id").equals(bookingId)) {
                        String vehicleId = booking.get("vehicle_id").toString();
                        for (Map<String, Object> vehicle : offlineVehicles) {
                            if (vehicle.get("id").equals(vehicleId)) {
                                vehicle.put("status", "Available");
                                break;
                            }
                        }
                        break;
                    }
                }
            } else {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO payments (id, booking_id, amount, method, payment_date) VALUES (?, ?, ?, ?, ?)");
                ps.setString(1, paymentId);
                ps.setString(2, bookingId);
                ps.setDouble(3, amount);
                ps.setString(4, method);
                ps.setDate(5, java.sql.Date.valueOf(paymentDate));
                ps.executeUpdate();

                // Update booking status
                ps = conn.prepareStatement("UPDATE bookings SET status = 'Completed' WHERE id = ?");
                ps.setString(1, bookingId);
                ps.executeUpdate();

                // Update vehicle status
                ps = conn.prepareStatement(
                        "UPDATE vehicles v JOIN bookings b ON v.id = b.vehicle_id " +
                                "SET v.status = 'Available' WHERE b.id = ?");
                ps.setString(1, bookingId);
                ps.executeUpdate();
            }

            showAlert("Success", "Payment processed successfully! Payment ID: " + paymentId);
            loadPaymentData();
            loadBookingData();
            loadVehicleData();
        } catch (SQLException e) {
            showAlert("Error", "Failed to process payment: " + e.getMessage());
        }
    }

    private void generateInvoice(String bookingId) {
        try {
            Booking booking = null;
            Customer customer = null;
            Vehicle vehicle = null;

            if (offlineMode) {
                for (Map<String, Object> b : offlineBookings) {
                    if (b.get("id").equals(bookingId)) {
                        String customerId = b.get("customer_id").toString();
                        String vehicleId = b.get("vehicle_id").toString();

                        String customerName = "";
                        for (Map<String, Object> c : offlineCustomers) {
                            if (c.get("id").equals(customerId)) {
                                customerName = c.get("name").toString();
                                break;
                            }
                        }

                        String vehicleDetails = "";
                        for (Map<String, Object> v : offlineVehicles) {
                            if (v.get("id").equals(vehicleId)) {
                                vehicleDetails = v.get("brand") + " " + v.get("model");
                                break;
                            }
                        }

                        booking = new Booking(
                                b.get("id").toString(),
                                customerId,
                                customerName,
                                vehicleId,
                                vehicleDetails,
                                (LocalDate) b.get("start_date"),
                                (LocalDate) b.get("end_date"),
                                (double) b.get("daily_rate"),
                                b.get("status").toString(),
                                b.get("employee_id").toString()
                        );
                        break;
                    }
                }
            } else {
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT b.*, v.brand, v.model, c.name as customer_name " +
                                "FROM bookings b " +
                                "JOIN vehicles v ON b.vehicle_id = v.id " +
                                "JOIN customers c ON b.customer_id = c.id " +
                                "WHERE b.id = ?");
                ps.setString(1, bookingId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    booking = new Booking(
                            rs.getString("id"),
                            rs.getString("customer_id"),
                            rs.getString("customer_name"),
                            rs.getString("vehicle_id"),
                            rs.getString("brand") + " " + rs.getString("model"),
                            rs.getDate("start_date").toLocalDate(),
                            rs.getDate("end_date").toLocalDate(),
                            rs.getDouble("daily_rate"),
                            rs.getString("status"),
                            rs.getString("employee_id")
                    );
                }
            }

            if (booking == null) {
                showAlert("Error", "Booking not found.");
                return;
            }

            // Calculate total
            long days = booking.getEndDate().toEpochDay() - booking.getStartDate().toEpochDay();
            double total = days * booking.getDailyRate();

            // Generate HTML invoice
            String invoice = "<html><head><title>Invoice</title><style>" +
                    "body { font-family: Arial, sans-serif; margin: 20px; }" +
                    "h1 { color: #0D47A1; }" +
                    "table { width: 100%; border-collapse: collapse; margin: 20px 0; }" +
                    "th { background-color: #0D47A1; color: white; text-align: left; padding: 8px; }" +
                    "td { padding: 8px; border-bottom: 1px solid #ddd; }" +
                    ".total { font-weight: bold; font-size: 1.2em; }" +
                    "</style></head><body>" +
                    "<h1>Hyacinth's Vehicle Rental - Invoice</h1>" +
                    "<p><strong>Invoice Date:</strong> " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) + "</p>" +
                    "<p><strong>Booking ID:</strong> " + booking.getId() + "</p>" +
                    "<p><strong>Customer:</strong> " + booking.getCustomerName() + "</p>" +
                    "<p><strong>Vehicle:</strong> " + booking.getVehicleDetails() + "</p>" +
                    "<table>" +
                    "<tr><th>Description</th><th>Amount</th></tr>" +
                    "<tr><td>Rental from " + booking.getStartDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) +
                    " to " + booking.getEndDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) + " (" + days + " days)</td>" +
                    "<td>$" + String.format("%.2f", booking.getDailyRate()) + "/day</td></tr>" +
                    "<tr><td><strong>Subtotal</strong></td><td>$" + String.format("%.2f", total) + "</td></tr>" +
                    "<tr><td><strong>Total</strong></td><td class='total'>$" + String.format("%.2f", total) + "</td></tr>" +
                    "</table>" +
                    "<p>Thank you for choosing Hyacinth's Vehicle Rental!</p>" +
                    "</body></html>";

            // Show in a dialog
            Alert invoiceAlert = new Alert(Alert.AlertType.INFORMATION);
            invoiceAlert.setTitle("Invoice for Booking #" + bookingId);
            invoiceAlert.setHeaderText(null);
            invoiceAlert.setContentText(invoice.replaceAll("<[^>]*>", "")); // Simple text display
            invoiceAlert.showAndWait();

        } catch (Exception e) {
            showAlert("Error", "Failed to generate invoice: " + e.getMessage());
        }
    }

    private void loadAllReports(LocalDate startDate, LocalDate endDate) {
        loadVehicleCategoryChart();
        loadRevenueChart(startDate, endDate);
        loadBookingTrendChart(startDate, endDate);
    }

    private void loadVehicleCategoryChart() {
        vehicleCategoryChart.getData().clear();

        try {
            if (offlineMode) {
                Map<String, Integer> categoryCounts = new HashMap<>();
                for (Map<String, Object> vehicle : offlineVehicles) {
                    String category = vehicle.get("category").toString();
                    categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
                }

                for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                    vehicleCategoryChart.getData().add(new PieChart.Data(
                            entry.getKey() + " (" + entry.getValue() + ")",
                            entry.getValue()
                    ));
                }
            } else {
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT category, COUNT(*) as count FROM vehicles GROUP BY category");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    vehicleCategoryChart.getData().add(new PieChart.Data(
                            rs.getString("category") + " (" + rs.getInt("count") + ")",
                            rs.getInt("count")
                    ));
                }
            }
        } catch (SQLException e) {
            showAlert("Error", "Failed to load vehicle category data.");
        }
    }

    private void loadRevenueChart(LocalDate startDate, LocalDate endDate) {
        revenueChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Monthly Revenue");

        try {
            if (offlineMode) {
                // Simulate data for offline mode
                LocalDate current = startDate.withDayOfMonth(1);
                Random random = new Random();

                while (!current.isAfter(endDate)) {
                    double revenue = 5000 + random.nextInt(10000);
                    series.getData().add(new XYChart.Data<>(
                            current.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                            revenue
                    ));
                    current = current.plusMonths(1);
                }
            } else {
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT DATE_FORMAT(payment_date, '%Y-%m') as month, SUM(amount) as total " +
                                "FROM payments WHERE payment_date BETWEEN ? AND ? " +
                                "GROUP BY DATE_FORMAT(payment_date, '%Y-%m') ORDER BY month");
                ps.setDate(1, java.sql.Date.valueOf(startDate));
                ps.setDate(2, java.sql.Date.valueOf(endDate));

                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    series.getData().add(new XYChart.Data<>(
                            rs.getString("month"),
                            rs.getDouble("total")
                    ));
                }
            }

            revenueChart.getData().add(series);
        } catch (SQLException e) {
            showAlert("Error", "Failed to load revenue data: " + e.getMessage());
        }
    }

    private void loadBookingTrendChart(LocalDate startDate, LocalDate endDate) {
        bookingTrendChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Bookings");

        try {
            if (offlineMode) {
                // Simulate data for offline mode
                LocalDate current = startDate.withDayOfMonth(1);
                Random random = new Random();

                while (!current.isAfter(endDate)) {
                    int bookings = 5 + random.nextInt(20);
                    series.getData().add(new XYChart.Data<>(
                            current.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                            bookings
                    ));
                    current = current.plusMonths(1);
                }
            } else {
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT DATE_FORMAT(start_date, '%Y-%m') as month, COUNT(*) as count " +
                                "FROM bookings WHERE start_date BETWEEN ? AND ? " +
                                "GROUP BY DATE_FORMAT(start_date, '%Y-%m') ORDER BY month");
                ps.setDate(1, java.sql.Date.valueOf(startDate));
                ps.setDate(2, java.sql.Date.valueOf(endDate));

                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    series.getData().add(new XYChart.Data<>(
                            rs.getString("month"),
                            rs.getInt("count")
                    ));
                }
            }

            bookingTrendChart.getData().add(series);
        } catch (SQLException e) {
            showAlert("Error", "Failed to load booking trend data: " + e.getMessage());
        }
    }

    private void generateSummaryReport(TextArea summaryArea) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        sb.append("Hyacinth's Vehicle Rental - Summary Report\n");
        sb.append("Generated on: ").append(sdf.format(new Date())).append("\n\n");

        try {
            // Total vehicles
            int totalVehicles = 0;
            if (offlineMode) {
                totalVehicles = offlineVehicles.size();
            } else {
                PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM vehicles");
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    totalVehicles = rs.getInt(1);
                }
            }

            // Total customers
            int totalCustomers = 0;
            if (offlineMode) {
                totalCustomers = offlineCustomers.size();
            } else {
                PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM customers");
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    totalCustomers = rs.getInt(1);
                }
            }

            // Total revenue
            double totalRevenue = 0;
            if (offlineMode) {
                for (Map<String, Object> payment : offlinePayments) {
                    totalRevenue += (double) payment.get("amount");
                }
            } else {
                PreparedStatement ps = conn.prepareStatement("SELECT SUM(amount) FROM payments");
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    totalRevenue = rs.getDouble(1);
                }
            }

            sb.append("Total Vehicles: ").append(totalVehicles).append("\n");
            sb.append("Total Customers: ").append(totalCustomers).append("\n");
            sb.append("Total Revenue: $").append(String.format("%.2f", totalRevenue)).append("\n\n");

            // Vehicle category breakdown
            sb.append("Vehicle Categories:\n");
            if (offlineMode) {
                Map<String, Integer> categoryCounts = new HashMap<>();
                for (Map<String, Object> vehicle : offlineVehicles) {
                    String category = vehicle.get("category").toString();
                    categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
                }

                for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                    sb.append(String.format("  %-15s: %2d vehicles\n", entry.getKey(), entry.getValue()));
                }
            } else {
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT category, COUNT(*) as count FROM vehicles GROUP BY category");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    sb.append(String.format("  %-15s: %2d vehicles\n",
                            rs.getString("category"), rs.getInt("count")));
                }
            }

            summaryArea.setText(sb.toString());
        } catch (SQLException ex) {
            showAlert("Error", "Failed to generate summary report.");
        }
    }

    private void exportVehiclesToCSV() {
        try (FileWriter writer = new FileWriter("vehicles.csv")) {
            writer.write("ID,Brand,Model,Category,Daily Rate,Status\n");

            for (Vehicle vehicle : vehicleData) {
                writer.write(String.format("%s,%s,%s,%s,%.2f,%s\n",
                        vehicle.getId(),
                        vehicle.getBrand(),
                        vehicle.getModel(),
                        vehicle.getCategory(),
                        vehicle.getDailyRate(),
                        vehicle.getStatus()
                ));
            }

            showAlert("Success", "Vehicle data exported to vehicles.csv");
        } catch (IOException e) {
            showAlert("Error", "Failed to export vehicle data: " + e.getMessage());
        }
    }

    private void exportReportsToCSV() {
        try (FileWriter writer = new FileWriter("rental_reports.csv")) {
            // Vehicle category summary
            writer.write("Vehicle Category Summary\n");
            writer.write("Category,Count\n");

            if (offlineMode) {
                Map<String, Integer> categoryCounts = new HashMap<>();
                for (Map<String, Object> vehicle : offlineVehicles) {
                    String category = vehicle.get("category").toString();
                    categoryCounts.put(category, categoryCounts.getOrDefault(category, 0) + 1);
                }

                for (Map.Entry<String, Integer> entry : categoryCounts.entrySet()) {
                    writer.write(String.format("%s,%d\n", entry.getKey(), entry.getValue()));
                }
            } else {
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT category, COUNT(*) as count FROM vehicles GROUP BY category");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    writer.write(String.format("%s,%d\n",
                            rs.getString("category"), rs.getInt("count")));
                }
            }

            // Revenue summary
            writer.write("\nRevenue Summary\n");
            writer.write("Month,Revenue\n");

            LocalDate startDate = LocalDate.now().minusMonths(6);
            LocalDate endDate = LocalDate.now();

            if (offlineMode) {
                // Simulate data
                LocalDate current = startDate.withDayOfMonth(1);
                Random random = new Random();

                while (!current.isAfter(endDate)) {
                    double revenue = 5000 + random.nextInt(10000);
                    writer.write(String.format("%s,%.2f\n",
                            current.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                            revenue
                    ));
                    current = current.plusMonths(1);
                }
            } else {
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT DATE_FORMAT(payment_date, '%Y-%m') as month, SUM(amount) as total " +
                                "FROM payments WHERE payment_date BETWEEN ? AND ? " +
                                "GROUP BY DATE_FORMAT(payment_date, '%Y-%m') ORDER BY month");
                ps.setDate(1, java.sql.Date.valueOf(startDate));
                ps.setDate(2, java.sql.Date.valueOf(endDate));

                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    writer.write(String.format("%s,%.2f\n",
                            rs.getString("month"),
                            rs.getDouble("total")
                    ));
                }
            }

            showAlert("Success", "Reports exported to rental_reports.csv");
        } catch (IOException | SQLException e) {
            showAlert("Error", "Failed to export reports: " + e.getMessage());
        }
    }

    private void exportReportsToPDF() {
        showAlert("Info", "PDF export would be implemented  in a real system.");
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    // Model classes
    public static class Vehicle {
        private final String id;
        private final String brand;
        private final String model;
        private final String category;
        private final double dailyRate;
        private final String status;

        public Vehicle(String id, String brand, String model, String category, double dailyRate, String status) {
            this.id = id;
            this.brand = brand;
            this.model = model;
            this.category = category;
            this.dailyRate = dailyRate;
            this.status = status;
        }

        public String getId() { return id; }
        public String getBrand() { return brand; }
        public String getModel() { return model; }
        public String getCategory() { return category; }
        public double getDailyRate() { return dailyRate; }
        public String getStatus() { return status; }
    }

    public static class Customer {
        private final String id;
        private final String name;
        private final String phone;
        private final String email;
        private final String license;
        private final LocalDate dob;

        public Customer(String id, String name, String phone, String email, String license, LocalDate dob) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.email = email;
            this.license = license;
            this.dob = dob;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getPhone() { return phone; }
        public String getEmail() { return email; }
        public String getLicense() { return license; }
        public LocalDate getDob() { return dob; }
    }

    public static class Booking {
        private final String id;
        private final String customerId;
        private final String customerName;
        private final String vehicleId;
        private final String vehicleDetails;
        private final LocalDate startDate;
        private final LocalDate endDate;
        private final double dailyRate;
        private final String status;
        private final String employeeId;

        public Booking(String id, String customerId, String customerName, String vehicleId,
                       String vehicleDetails, LocalDate startDate, LocalDate endDate,
                       double dailyRate, String status, String employeeId) {
            this.id = id;
            this.customerId = customerId;
            this.customerName = customerName;
            this.vehicleId = vehicleId;
            this.vehicleDetails = vehicleDetails;
            this.startDate = startDate;
            this.endDate = endDate;
            this.dailyRate = dailyRate;
            this.status = status;
            this.employeeId = employeeId;
        }

        public String getId() { return id; }
        public String getCustomerId() { return customerId; }
        public String getCustomerName() { return customerName; }
        public String getVehicleId() { return vehicleId; }
        public String getVehicleDetails() { return vehicleDetails; }
        public LocalDate getStartDate() { return startDate; }
        public LocalDate getEndDate() { return endDate; }
        public double getDailyRate() { return dailyRate; }
        public String getStatus() { return status; }
        public String getEmployeeId() { return employeeId; }
    }

    public static class Payment {
        private final String id;
        private final String bookingId;
        private final double amount;
        private final String method;
        private final LocalDate paymentDate;

        public Payment(String id, String bookingId, double amount, String method, LocalDate paymentDate) {
            this.id = id;
            this.bookingId = bookingId;
            this.amount = amount;
            this.method = method;
            this.paymentDate = paymentDate;
        }

        public String getId() { return id; }
        public String getBookingId() { return bookingId; }
        public double getAmount() { return amount; }
        public String getMethod() { return method; }
        public LocalDate getPaymentDate() { return paymentDate; }
    }

    public static class User {
        private final String username;
        private final String password;
        private final String role;

        public User(String username, String password, String role) {
            this.username = username;
            this.password = password;
            this.role = role;
        }

        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getRole() { return role; }
    }
}