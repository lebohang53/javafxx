
-- VEHICLE RENTAL SYSTEM DATABASE SETUP

-- VEHICLES TABLE
CREATE TABLE IF NOT EXISTS vehicles (
    vehicle_id INTEGER PRIMARY KEY AUTOINCREMENT,
    brand TEXT NOT NULL,
    model TEXT NOT NULL,
    category TEXT NOT NULL,
    price_per_day REAL NOT NULL,
    availability TEXT NOT NULL CHECK(availability IN ('Available', 'Rented'))
);

-- CUSTOMERS TABLE
CREATE TABLE IF NOT EXISTS customers (
    customer_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    contact TEXT NOT NULL,
    license_number TEXT NOT NULL UNIQUE
);

-- BOOKINGS TABLE
CREATE TABLE IF NOT EXISTS bookings (
    booking_id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER NOT NULL,
    vehicle_id INTEGER NOT NULL,
    start_date TEXT NOT NULL,
    end_date TEXT NOT NULL,
    payment_method TEXT NOT NULL,
    additional_fee REAL DEFAULT 0,
    late_fee REAL DEFAULT 0,
    total_amount REAL NOT NULL,
    status TEXT DEFAULT 'Active',
    FOREIGN KEY(customer_id) REFERENCES customers(customer_id),
    FOREIGN KEY(vehicle_id) REFERENCES vehicles(vehicle_id)
);

-- SAMPLE VEHICLES DATA
INSERT INTO vehicles (brand, model, category, price_per_day, availability) VALUES 
('Mazda', 'CX-5', 'Car', 300, 'Available'),
('Honda', 'Civic', 'Car', 280, 'Available'),
('BMW', 'X5', 'SUV', 500, 'Available'),
('Isuzu', 'D-Max', 'Truck', 400, 'Rented');

-- SAMPLE CUSTOMERS DATA
INSERT INTO customers (name, contact, license_number) VALUES 
('Alice Moleko', '1234567890', 'LS12345'),
('Thabo Mokoena', '0987654321', 'LS98765');

-- SAMPLE BOOKINGS DATA
INSERT INTO bookings (customer_id, vehicle_id, start_date, end_date, payment_method, additional_fee, late_fee, total_amount) VALUES 
(1, 1, '2025-04-01', '2025-04-05', 'Cash', 50, 0, 1250),
(2, 2, '2025-04-03', '2025-04-10', 'Online', 0, 100, 2060);
