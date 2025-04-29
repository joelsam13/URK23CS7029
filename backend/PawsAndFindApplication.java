package com.pawsandfind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.DriverManager;
import java.sql.Statement;

@SpringBootApplication
@RestController
@RequestMapping("/api")
public class PawsAndFindApplication {

    @Autowired
    private DataSource dataSource; // Spring-managed connection pool

    // Optional: If you want to have manual connection parameters as well
    private static final String DB_URL = "jdbc:mysql://localhost:3306/pawsandfind";
    private static final String DB_USERNAME = "root";
    private static final String DB_PASSWORD = "connect";

    public static void main(String[] args) {
        SpringApplication.run(PawsAndFindApplication.class, args);
    }

    // Method to demonstrate manual connection (similar to dbdb3)
    private void manualConnectionExample() {
        try {
            // Load the JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Create a manual connection
            try (Connection manualConn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
                 Statement st = manualConn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM users")) {
                
                System.out.println("Manual Connection Established successfully");
                
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    String email = rs.getString("email");
                    System.out.println("ID: " + id + ", Name: " + name + ", Email: " + email);
                }
                
                System.out.println("Manual Connection Closed...");
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Error in manual connection: " + e.getMessage());
        }
    }

    // Create tables during application startup (using Spring's DataSource)
    @PostConstruct
    public void createTables() {
        // Using Spring's DataSource (recommended)
        try (Connection conn = dataSource.getConnection()) {
            // Create pets table
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS pets (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    type VARCHAR(50) NOT NULL,
                    breed VARCHAR(100),
                    age INT,
                    description TEXT,
                    image_url VARCHAR(255),
                    user_id INT,
                    is_adopted BOOLEAN DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // Create users table if not exists
            conn.createStatement().execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    email VARCHAR(100) NOT NULL UNIQUE,
                    password VARCHAR(255) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            System.out.println("Tables created successfully using DataSource");
            
            // Optionally demonstrate the manual connection
            manualConnectionExample();
            
        } catch (SQLException e) {
            System.err.println("Error creating tables: " + e.getMessage());
        }
    }

    // Rest of your endpoints remain the same...
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody User user) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE email = ?");
            stmt.setString(1, user.getEmail());
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return ResponseEntity.badRequest().body("Email already registered");
            }

            stmt = conn.prepareStatement("INSERT INTO users (name, email, password) VALUES (?, ?, ?)");
            stmt.setString(1, user.getName());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getPassword());  // Ideally, hash the password
            stmt.executeUpdate();

            return ResponseEntity.ok("Registration successful!");
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest loginRequest) {
        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users WHERE email = ?");
            stmt.setString(1, loginRequest.getEmail());
            ResultSet rs = stmt.executeQuery();

            if (!rs.next() || !rs.getString("password").equals(loginRequest.getPassword())) {
                return ResponseEntity.status(401).body("Invalid email or password");
            }

            return ResponseEntity.ok("Login successful!");
        } catch (SQLException e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}