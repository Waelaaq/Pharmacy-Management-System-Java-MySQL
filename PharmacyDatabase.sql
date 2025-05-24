
create DATABASE Pharmacy;
USE Pharmacy;
-- DLL
-- Supplier
CREATE TABLE Supplier (
    ID INT PRIMARY KEY,
    Name VARCHAR(100),
    ContactInfo VARCHAR(100),
    Address VARCHAR(255)
);

-- Medicine
CREATE TABLE Medicine (
    ID INT PRIMARY KEY,
    Name VARCHAR(100),
    Type VARCHAR(50),
    Price DECIMAL(10,2),
    StockQuantity INT,
    ExpiryDate DATE,
    SupplierID INT,
    FOREIGN KEY (SupplierID) REFERENCES Supplier(ID)
);

-- Inventory
CREATE TABLE Inventory (
    MedicineID INT PRIMARY KEY,
    StockQuantity INT,
    LastRestockedDate DATE,
    FOREIGN KEY (MedicineID) REFERENCES Medicine(ID)
);

-- Customer
CREATE TABLE Customer (
    ID INT PRIMARY KEY,
    Name VARCHAR(100),
    ContactInfo VARCHAR(100),
    Address VARCHAR(255)
);

-- Staff
CREATE TABLE Staff (
    ID INT PRIMARY KEY,
    Name VARCHAR(100),
    Role VARCHAR(50),
    Salary DECIMAL(10,2),
    ContactInfo VARCHAR(100)
);

-- Pharmacist
CREATE TABLE Pharmacist (
    ID INT PRIMARY KEY,
    LicenseNumber VARCHAR(50),
    FOREIGN KEY (ID) REFERENCES Staff(ID)
);

-- Prescription
CREATE TABLE Prescription (
    ID INT PRIMARY KEY,
    CustomerID INT,
    PharmacistID INT,
    DateIssued DATE,
    FOREIGN KEY (CustomerID) REFERENCES Customer(ID),
    FOREIGN KEY (PharmacistID) REFERENCES Pharmacist(ID)
);

-- Prescription_Details
CREATE TABLE Prescription_Details (
    PrescriptionID INT,
    MedicineID INT,
    Quantity INT,
    Dosage VARCHAR(100),
    PRIMARY KEY (PrescriptionID, MedicineID),
    FOREIGN KEY (PrescriptionID) REFERENCES Prescription(ID),
    FOREIGN KEY (MedicineID) REFERENCES Medicine(ID)
);

-- Sales
CREATE TABLE Sales (
    ID INT PRIMARY KEY,
    CustomerID INT,
    MedicineID INT,
    Quantity INT,
    TotalPrice DECIMAL(10,2),
    Date DATE,
    FOREIGN KEY (CustomerID) REFERENCES Customer(ID),
    FOREIGN KEY (MedicineID) REFERENCES Medicine(ID)
);


select * from Medicine ;

-- DML Queries 
-- Update
SET SQL_SAFE_UPDATES = 0;
UPDATE Medicine SET Price = 6.00 WHERE Name = 'Panadol';
SET SQL_SAFE_UPDATES = 1;

-- Delete


-- Insert
INSERT INTO Supplier VALUES (2, 'Global Pharma', '99887766', 'Dammam');

-- Basic Queries
-- LIKE
SELECT * FROM Medicine WHERE Name LIKE '%adol';

-- BETWEEN
SELECT * FROM Medicine WHERE Price BETWEEN 3 AND 10;

-- IN
SELECT * FROM Customer WHERE ID IN (1, 2, 3);

-- ORDER BY
SELECT * FROM Staff ORDER BY Salary DESC;

-- IS NULL
SELECT * FROM Inventory WHERE LastRestockedDate IS NULL;

-- complex Queries

-- UNION
SELECT Name FROM Customer
UNION
SELECT Name FROM Staff;

-- INTERSECT (use INNER JOIN alternative in MySQL)
SELECT DISTINCT Customer.Name FROM Customer
JOIN Sales ON Customer.ID = Sales.CustomerID
JOIN Prescription ON Customer.ID = Prescription.CustomerID;

-- GROUP BY + HAVING
SELECT SupplierID, COUNT(*) AS Num_Medicines
FROM Medicine
GROUP BY SupplierID
HAVING COUNT(*) > 1;

-- NATURAL JOIN (simulated)
SELECT * FROM Prescription
JOIN Prescription_Details ON Prescription.ID = Prescription_Details.PrescriptionID;

-- LEFT OUTER JOIN
SELECT Customer.Name, Sales.TotalPrice
FROM Customer
LEFT JOIN Sales ON Customer.ID = Sales.CustomerID;

-- EXISTS
SELECT Name FROM Medicine WHERE EXISTS (
    SELECT * FROM Sales WHERE Sales.MedicineID = Medicine.ID
);

-- NESTED QUERY
SELECT Name FROM Customer WHERE ID IN (
    SELECT CustomerID FROM Prescription WHERE DateIssued > '2025-01-01'
);

-- FUNCTION
DELIMITER //
CREATE FUNCTION TotalStock()
RETURNS INT
DETERMINISTIC
BEGIN
  DECLARE total INT;
  SELECT SUM(StockQuantity) INTO total FROM Inventory;
  RETURN total;
END //
DELIMITER ;

-- STORED PROCEDURE
DELIMITER //
CREATE PROCEDURE AddCustomer(
    IN name VARCHAR(100), 
    IN contact VARCHAR(100), 
    IN address VARCHAR(255)
)
BEGIN
    DECLARE new_id INT;

    -- Safely get the next ID
    SELECT IFNULL(MAX(ID), 0) + 1 INTO new_id FROM Customer;

    -- Now insert using that ID
    INSERT INTO Customer (ID, Name, ContactInfo, Address)
    VALUES (new_id, name, contact, address);
END //
DELIMITER ;
-- TRIGGER
DELIMITER //
CREATE TRIGGER Before_Sales_Insert
BEFORE INSERT ON Sales
FOR EACH ROW
BEGIN
  IF NEW.Quantity > (SELECT StockQuantity FROM Medicine WHERE ID = NEW.MedicineID) THEN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'Not enough stock available.';
  END IF;
END //
DELIMITER ;

-- VIEW
CREATE VIEW ExpiringSoon AS
SELECT * FROM Medicine
WHERE ExpiryDate < DATE_ADD(CURDATE(), INTERVAL 3 MONTH);





-- CONSTRAINTS

ALTER TABLE Staff ADD CONSTRAINT chk_salary CHECK (Salary > 0);
ALTER TABLE Medicine ADD CONSTRAINT chk_price CHECK (Price >= 0);
ALTER TABLE Prescription_Details ADD CONSTRAINT chk_quantity CHECK (Quantity > 0);

ALTER TABLE Sales ADD CONSTRAINT check_total CHECK (TotalPrice >= 0);


-- VIEWS
-- View: Medicine Stock Summary
CREATE OR REPLACE VIEW View_Medicine_Stock AS
SELECT ID, Name, Price, StockQuantity FROM medicine LIMIT 0, 1000;


-- View: Prescription Details Expanded
CREATE OR REPLACE VIEW View_Prescription_Info AS
SELECT     
    p.ID AS Prescription_ID,
    c.Name AS Customer_Name,
    s.Name AS Staff_Name,
    m.Name AS Medicine_Name,
    pd.Quantity,
    pd.Dosage,
    p.DateIssued
FROM Prescription p
JOIN Customer c ON p.CustomerID = c.ID
JOIN Staff s ON p.PharmacistID = s.ID
JOIN Prescription_Details pd ON p.ID = pd.PrescriptionID
JOIN Medicine m ON pd.MedicineID = m.ID;



-- View: Sales Summary
CREATE OR REPLACE VIEW View_Sales_Summary AS
SELECT sa.ID, cu.Name AS CustomerName, st.Name AS StaffName, me.Name AS MedicineName,
       sa.Quantity, sa.TotalPrice, sa.Date
FROM Sales sa
JOIN Customer cu ON sa.ID = cu.ID
JOIN Staff st ON sa.ID = st.ID
JOIN Medicine me ON sa.ID = me.ID;

DROP PROCEDURE IF EXISTS A_Sale;

-- Create an improved version of the A_Sale procedure
DELIMITER //

CREATE PROCEDURE A_Sale (
    IN p_CustomerID INT,           -- Customer ID
    IN p_MedicineID INT,           -- Medicine ID
    IN p_Quantity INT,             -- Quantity to sell
    IN p_TotalPrice DECIMAL(10,2),-- Total price of the sale
    IN p_Date DATE                 -- Date of sale
)
BEGIN
    DECLARE current_stock INT;
    DECLARE new_id INT;

    -- Get current stock from Inventory (NOT Medicine table)
    
    SELECT StockQuantity INTO current_stock 
    FROM Inventory 
    WHERE MedicineID = p_MedicineID;

    -- Get next sale ID
    SELECT IFNULL(MAX(ID), 0) + 1 INTO new_id FROM Sales;

    IF current_stock >= p_Quantity THEN
        -- Insert the new sale record
        INSERT INTO Sales (ID, CustomerID, MedicineID, Quantity, TotalPrice, Date)
        VALUES (new_id, p_CustomerID, p_MedicineID, p_Quantity, p_TotalPrice, p_Date);

        -- Update stock in Inventory
        UPDATE medicine
        SET StockQuantity = StockQuantity - p_Quantity
        WHERE ID = p_MedicineID;
        UPDATE Inventory 
        SET StockQuantity = StockQuantity - p_Quantity
        WHERE MedicineID = p_MedicineID;

        -- Optional: Display confirmation
        SELECT CONCAT('Sale completed successfully. Medicine ID: ', p_MedicineID, 
                      ', New stock level: ', (current_stock - p_Quantity)) AS Result;
    ELSE
        -- Not enough stock message
        SELECT CONCAT('Error: Not enough stock. Requested: ', p_Quantity, 
                      ', Available: ', current_stock) AS Result;
    END IF;
END //

DELIMITER ;



-- Procedure: Get all medicines under a certain quantity (low stock alert)
DELIMITER //

CREATE PROCEDURE Get_Low_Stock_Mediciness(IN threshold INT)
BEGIN
    SELECT * FROM Medicine
    WHERE StockQuantity <= threshold;
END;
//
DELIMITER ;



/* sample insert statements (10 per table)
INSERT INTO Supplier VALUES (1, 'PharmaCo', '123456789', 'Riyadh');

INSERT INTO Supplier VALUES (3, 'MediCare Ltd', '987654321', 'Jeddah');
INSERT INTO Supplier VALUES (4, 'HealthPro', '11223344', 'Mecca');
INSERT INTO Supplier VALUES (5, 'BioPharm', '55667788', 'Khobar');
INSERT INTO Supplier VALUES (6, 'CureTech', '66778899', 'Riyadh');
INSERT INTO Supplier VALUES (7, 'LifeLine', '77889900', 'Dammam');
INSERT INTO Supplier VALUES (8, 'HealWell', '88990011', 'Jeddah');
INSERT INTO Supplier VALUES (9, 'MediZone', '99001122', 'Mecca');
INSERT INTO Supplier VALUES (10, 'Wellness Inc.', '10111213', 'Khobar');
INSERT INTO Medicine VALUES (1, 'Panadol', 'Tablet', 5.5, 100, '2025-12-31', 1);
INSERT INTO Medicine VALUES (2, 'Aspirin', 'Tablet', 3.0, 150, '2026-01-10', 2);
INSERT INTO Medicine VALUES (3, 'Cough Syrup', 'Liquid', 7.25, 80, '2025-09-15', 3);
INSERT INTO Medicine VALUES (4, 'Ibuprofen', 'Tablet', 6.0, 90, '2026-05-01', 4);
INSERT INTO Medicine VALUES (5, 'Paracetamol', 'Tablet', 4.75, 200, '2025-11-20', 5);
INSERT INTO Medicine VALUES (6, 'Antibiotic', 'Capsule', 8.0, 70, '2026-03-25', 6);
INSERT INTO Medicine VALUES (7, 'Vitamin C', 'Tablet', 3.5, 130, '2025-10-10', 7);
INSERT INTO Medicine VALUES (8, 'Eye Drops', 'Liquid', 9.0, 60, '2026-06-30', 8);
INSERT INTO Medicine VALUES (9, 'Insulin', 'Injection', 12.0, 50, '2026-02-14', 9);
INSERT INTO Medicine VALUES (10, 'Allergy Relief', 'Tablet', 5.25, 110, '2025-08-22', 10);
INSERT INTO Inventory VALUES (1, 100, '2025-04-01');
INSERT INTO Inventory VALUES (2, 150, '2025-04-02');
INSERT INTO Inventory VALUES (3, 80, '2025-04-03');
INSERT INTO Inventory VALUES (4, 90, '2025-04-04');
INSERT INTO Inventory VALUES (5, 200, '2025-04-05');
INSERT INTO Inventory VALUES (6, 70, '2025-04-06');
INSERT INTO Inventory VALUES (7, 130, '2025-04-07');
INSERT INTO Inventory VALUES (8, 60, '2025-04-08');
INSERT INTO Inventory VALUES (9, 50, '2025-04-09');
INSERT INTO Inventory VALUES (10, 110, '2025-04-10');
INSERT INTO Customer VALUES (1, 'Ahmed Ali', '987654321', 'Jeddah');
INSERT INTO Customer VALUES (2, 'Sara Khan', '123123123', 'Mecca');
INSERT INTO Customer VALUES (3, 'Faisal Noor', '321321321', 'Dammam');
INSERT INTO Customer VALUES (4, 'Laila Saeed', '456456456', 'Riyadh');
INSERT INTO Customer VALUES (5, 'Omar Zaid', '789789789', 'Khobar');
INSERT INTO Customer VALUES (6, 'Nora Ahmed', '741741741', 'Tabuk');
INSERT INTO Customer VALUES (7, 'Hassan Omar', '852852852', 'Yanbu');
INSERT INTO Customer VALUES (8, 'Maha Salem', '963963963', 'Abha');
INSERT INTO Customer VALUES (9, 'Talal Nasser', '159159159', 'Hail');
INSERT INTO Customer VALUES (10, 'Aisha Fadel', '357357357', 'Buraidah');
INSERT INTO Staff VALUES (1, 'Dr. Khalid', 'Pharmacist', 8000, '1122334455');
INSERT INTO Staff VALUES (2, 'Dr. Amani', 'Pharmacist', 8200, '2233445566');
INSERT INTO Staff VALUES (3, 'Samir', 'Cashier', 4000, '3344556677');
INSERT INTO Staff VALUES (4, 'Lina', 'Manager', 10000, '4455667788');
INSERT INTO Staff VALUES (5, 'Rami', 'Assistant', 3500, '5566778899');
INSERT INTO Staff VALUES (6, 'Nada', 'Pharmacist', 8100, '6677889900');
INSERT INTO Staff VALUES (7, 'Adel', 'Security', 3000, '7788990011');
INSERT INTO Staff VALUES (8, 'Rana', 'Pharmacist', 8300, '8899001122');
INSERT INTO Staff VALUES (9, 'Fahad', 'Cleaner', 2500, '9900112233');
INSERT INTO Staff VALUES (10, 'Waleed', 'Inventory Manager', 6000, '1011121314');
INSERT INTO Pharmacist VALUES (1, 'PH12345');
INSERT INTO Pharmacist VALUES (2, 'PH12346');
INSERT INTO Pharmacist VALUES (6, 'PH12347');
INSERT INTO Pharmacist VALUES (8, 'PH12348');
INSERT INTO Prescription VALUES (1, 1, 1, '2025-04-30');
INSERT INTO Prescription VALUES (2, 2, 2, '2025-05-01');
INSERT INTO Prescription VALUES (3, 3, 6, '2025-05-02');
INSERT INTO Prescription VALUES (4, 4, 8, '2025-05-03');
INSERT INTO Prescription VALUES (5, 5, 1, '2025-05-04');
INSERT INTO Prescription VALUES (6, 6, 2, '2025-05-05');
INSERT INTO Prescription VALUES (7, 7, 6, '2025-05-06');
INSERT INTO Prescription VALUES (8, 8, 8, '2025-05-07');
INSERT INTO Prescription VALUES (9, 9, 1, '2025-05-08');
INSERT INTO Prescription VALUES (10, 10, 2, '2025-05-09');
INSERT INTO Prescription_Details VALUES (1, 1, 2, '1 tablet after meal');
INSERT INTO Prescription_Details VALUES (2, 2, 1, '2 tablets daily');
INSERT INTO Prescription_Details VALUES (3, 3, 1, '10 ml thrice daily');
INSERT INTO Prescription_Details VALUES (4, 4, 1, '1 tablet morning & night');
INSERT INTO Prescription_Details VALUES (5, 5, 2, '1 capsule daily');
INSERT INTO Prescription_Details VALUES (6, 6, 1, '1 tablet before bed');
INSERT INTO Prescription_Details VALUES (7, 7, 1, '1 tablet per day');
INSERT INTO Prescription_Details VALUES (8, 8, 1, '2 drops twice daily');
INSERT INTO Prescription_Details VALUES (9, 9, 1, '1 shot per day');
INSERT INTO Prescription_Details VALUES (10, 10, 1, '1 tablet as needed');
INSERT INTO Sales VALUES (1, 1, 1, 2, 11.0, '2025-05-01');
INSERT INTO Sales VALUES (2, 2, 2, 1, 3.0, '2025-05-01');
INSERT INTO Sales VALUES (3, 3, 3, 1, 7.25, '2025-05-02');
INSERT INTO Sales VALUES (4, 4, 4, 2, 12.0, '2025-05-03');
INSERT INTO Sales VALUES (5, 5, 5, 3, 14.25, '2025-05-04');
INSERT INTO Sales VALUES (6, 6, 6, 1, 8.0, '2025-05-05');
INSERT INTO Sales VALUES (7, 7, 7, 2, 7.0, '2025-05-06');
INSERT INTO Sales VALUES (8, 8, 8, 1, 9.0, '2025-05-07');
INSERT INTO Sales VALUES (9, 9, 9, 1, 12.0, '2025-05-08');
INSERT INTO Sales VALUES (10, 10, 10, 2, 10.5, '2025-05-09');


