# Database Setup Guide for Pulse Connect

## Prerequisites
- MySQL Server installed on your system
- MySQL running on localhost:3306

## Setup Steps

### 1. Install MySQL (if not already installed)
Download from: https://dev.mysql.com/downloads/mysql/

### 2. Start MySQL Service
```powershell
# Windows - Run as Administrator
net start MySQL80
# or
net start MySQL
```

### 3. Run the Database Setup Script
```powershell
# Option 1: Using MySQL Command Line
mysql -u root -p < database-setup.sql

# Option 2: Using MySQL Workbench
# - Open MySQL Workbench
# - Connect to your local MySQL server
# - File > Run SQL Script
# - Select database-setup.sql
# - Click "Run"

# Option 3: Using Command Line directly
mysql -u root -p
# Enter your root password (default is often 'root' or empty)
# Then run:
source C:/Users/chait/Downloads/Telegram Desktop/oops pbl/database-setup.sql
```

### 4. Verify Database Setup
```sql
USE pulse_connect_db;
SHOW TABLES;
SELECT COUNT(*) FROM users;
SELECT COUNT(*) FROM donors;
```

### 5. Update MySQL Password (if needed)
If your MySQL root password is not 'root', update it in:
`src/main/resources/application.properties`

```properties
spring.datasource.password=YOUR_MYSQL_ROOT_PASSWORD
```

## Database Schema

### Tables Created:
1. **users** - User account information
2. **donors** - Donor-specific information
3. **blood_banks** - Blood bank locations
4. **emergency_requests** - Emergency blood requests
5. **donation_history** - Donation records
6. **emergency_alerts** - Alert notifications

### Sample Data:
- 10 sample donors with different blood groups and cities
- Cities: Mumbai, Delhi, Bangalore, Hyderabad, Kolkata, Pune, Chennai, Ahmedabad, Jaipur, Lucknow
- Blood Groups: A+, B+, O+, AB+, A-, B-, O-, AB-

## Rebuild and Run Application

After setting up the database:

```powershell
# 1. Rebuild the application
mvn clean package -DskipTests

# 2. Run the application
java -jar target/blood-donor-directory-1.0.0.jar
```

## Access Application
- URL: http://localhost:8081
- Username: admin
- Password: admin123

## Features Now Enabled:
✅ Real donor data from MySQL database
✅ Filter donors by blood group, city, and distance
✅ Save and retrieve donor information
✅ Emergency request system (backend ready)

## Troubleshooting

### Issue: Cannot connect to MySQL
**Solution:** Ensure MySQL is running
```powershell
net start MySQL80
```

### Issue: Access denied for user 'root'
**Solution:** Update password in application.properties or reset MySQL root password

### Issue: Database 'pulse_connect_db' doesn't exist
**Solution:** Run the database-setup.sql script again

### Issue: Port 3306 already in use
**Solution:** Check if MySQL is already running or another service is using port 3306
```powershell
netstat -ano | findstr :3306
```
