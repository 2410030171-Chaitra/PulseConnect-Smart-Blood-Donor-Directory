-- Create Database
CREATE DATABASE IF NOT EXISTS pulse_connect_db;
USE pulse_connect_db;

-- Create Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    blood_group VARCHAR(10),
    address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(10),
    latitude DOUBLE,
    longitude DOUBLE,
    is_donor BOOLEAN DEFAULT FALSE,
    is_verified BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create Donors Table
CREATE TABLE IF NOT EXISTS donors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    date_of_birth DATE,
    gender VARCHAR(10),
    weight DOUBLE,
    height DOUBLE,
    is_eligible BOOLEAN DEFAULT TRUE,
    last_donation_date DATE,
    next_eligible_date DATE,
    total_donations INT DEFAULT 0,
    impact_score INT DEFAULT 0,
    medical_history TEXT,
    has_diabetes BOOLEAN DEFAULT FALSE,
    has_hypertension BOOLEAN DEFAULT FALSE,
    has_heart_disease BOOLEAN DEFAULT FALSE,
    has_kidney_disease BOOLEAN DEFAULT FALSE,
    has_infectious_disease BOOLEAN DEFAULT FALSE,
    is_available BOOLEAN DEFAULT TRUE,
    willing_to_travel_far BOOLEAN DEFAULT FALSE,
    priority_score INT DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Create Blood Banks Table
CREATE TABLE IF NOT EXISTS blood_banks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    city VARCHAR(100),
    state VARCHAR(100),
    pincode VARCHAR(10),
    phone_number VARCHAR(20),
    email VARCHAR(255),
    latitude DOUBLE,
    longitude DOUBLE,
    operating_hours VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE
);

-- Create Emergency Requests Table
CREATE TABLE IF NOT EXISTS emergency_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    patient_name VARCHAR(255) NOT NULL,
    contact_number VARCHAR(20) NOT NULL,
    required_blood_group VARCHAR(10) NOT NULL,
    units_required INT DEFAULT 1,
    hospital_location VARCHAR(500) NOT NULL,
    urgency_level VARCHAR(20) DEFAULT 'CRITICAL',
    additional_details TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    requested_by_user_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (requested_by_user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Create Donation History Table
CREATE TABLE IF NOT EXISTS donation_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    donor_id BIGINT NOT NULL,
    blood_bank_id BIGINT,
    donation_date DATE NOT NULL,
    units_donated DOUBLE DEFAULT 1.0,
    donation_type VARCHAR(50),
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (donor_id) REFERENCES donors(id) ON DELETE CASCADE,
    FOREIGN KEY (blood_bank_id) REFERENCES blood_banks(id) ON DELETE SET NULL
);

-- Create Emergency Alerts Table
CREATE TABLE IF NOT EXISTS emergency_alerts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    donor_id BIGINT NOT NULL,
    emergency_request_id BIGINT NOT NULL,
    alert_sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_responded BOOLEAN DEFAULT FALSE,
    response_time TIMESTAMP,
    FOREIGN KEY (donor_id) REFERENCES donors(id) ON DELETE CASCADE,
    FOREIGN KEY (emergency_request_id) REFERENCES emergency_requests(id) ON DELETE CASCADE
);

-- Insert Sample Donors Data
INSERT INTO users (full_name, email, password, phone_number, blood_group, city, state, is_donor, is_verified, is_active, latitude, longitude) VALUES
('Rajesh Kumar', 'rajesh@example.com', '$2a$10$dummypassword', '9876543210', 'A+', 'Mumbai', 'Maharashtra', TRUE, TRUE, TRUE, 19.0760, 72.8777),
('Priya Sharma', 'priya@example.com', '$2a$10$dummypassword', '9123456780', 'B+', 'Delhi', 'Delhi', TRUE, TRUE, TRUE, 28.7041, 77.1025),
('Amit Patel', 'amit@example.com', '$2a$10$dummypassword', '9988776655', 'O+', 'Bangalore', 'Karnataka', TRUE, TRUE, TRUE, 12.9716, 77.5946),
('Sneha Reddy', 'sneha@example.com', '$2a$10$dummypassword', '8877665544', 'AB+', 'Hyderabad', 'Telangana', TRUE, TRUE, TRUE, 17.3850, 78.4867),
('Vikram Singh', 'vikram@example.com', '$2a$10$dummypassword', '7766554433', 'A-', 'Kolkata', 'West Bengal', TRUE, TRUE, TRUE, 22.5726, 88.3639),
('Neha Gupta', 'neha@example.com', '$2a$10$dummypassword', '6655443322', 'B-', 'Pune', 'Maharashtra', TRUE, TRUE, TRUE, 18.5204, 73.8567),
('Manish Verma', 'manish@example.com', '$2a$10$dummypassword', '5544332211', 'O-', 'Chennai', 'Tamil Nadu', TRUE, TRUE, TRUE, 13.0827, 80.2707),
('Ritu Joshi', 'ritu@example.com', '$2a$10$dummypassword', '4433221100', 'AB-', 'Ahmedabad', 'Gujarat', TRUE, TRUE, TRUE, 23.0225, 72.5714),
('Suresh Yadav', 'suresh@example.com', '$2a$10$dummypassword', '3322110099', 'A+', 'Jaipur', 'Rajasthan', TRUE, TRUE, TRUE, 26.9124, 75.7873),
('Kavita Mehta', 'kavita@example.com', '$2a$10$dummypassword', '2211009988', 'B+', 'Lucknow', 'Uttar Pradesh', TRUE, TRUE, TRUE, 26.8467, 80.9462);

INSERT INTO donors (user_id, date_of_birth, gender, weight, height, is_eligible, total_donations, is_available, willing_to_travel_far, priority_score) VALUES
(1, '1990-05-15', 'MALE', 75.0, 175.0, TRUE, 5, TRUE, FALSE, 85),
(2, '1992-08-22', 'FEMALE', 60.0, 165.0, TRUE, 3, TRUE, TRUE, 80),
(3, '1988-11-10', 'MALE', 80.0, 180.0, TRUE, 8, TRUE, FALSE, 90),
(4, '1995-03-18', 'FEMALE', 55.0, 160.0, TRUE, 2, TRUE, TRUE, 75),
(5, '1987-07-25', 'MALE', 70.0, 172.0, TRUE, 6, TRUE, FALSE, 82),
(6, '1993-12-05', 'FEMALE', 58.0, 162.0, TRUE, 4, TRUE, TRUE, 78),
(7, '1991-09-30', 'MALE', 78.0, 178.0, TRUE, 7, TRUE, FALSE, 88),
(8, '1994-01-14', 'FEMALE', 62.0, 168.0, TRUE, 3, TRUE, TRUE, 77),
(9, '1989-06-20', 'MALE', 72.0, 174.0, TRUE, 5, TRUE, FALSE, 84),
(10, '1996-04-08', 'FEMALE', 56.0, 158.0, TRUE, 2, TRUE, TRUE, 76);

-- Verify the data
SELECT u.full_name, u.blood_group, u.city, u.phone_number, d.total_donations, d.is_available
FROM users u
JOIN donors d ON u.id = d.user_id
WHERE u.is_donor = TRUE;
