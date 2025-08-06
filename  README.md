# ⏱️ Timesheet Microservice

> **A modern Spring Boot microservice for secure and validated employee timesheet management.**

## 🚀 Overview

This microservice manages **employee timesheets**—enforcing business rules that:
- Internal employees submit **daily timesheets**
- External employees submit **weekly timesheets** (starting Mondays)
- All user validation occurs through integration with the User Register microservice.

## 📦 Technology Stack

| Purpose          | Tech                          |
|------------------|------------------------------|
| Core Framework   | Spring Boot 3.5 + Java 20    |
| Database         | MySQL (or any JPA DB)        |
| HTTP/JSON        | Jackson, Spring Web          |
| HTTP Calls       | RestTemplate                 |

## 🛠️ Setup & Configuration

### Prerequisites

- JDK 20+
- Maven
- MySQL running locally (or edit the DB connection string accordingly)
- User Register microservice up on `http://localhost:8083`

### Configuration

```
Edit `src/main/resources/application.properties`:

Database settings
spring.datasource.url=jdbc:mysql://localhost:3306/timesheetdb?useSSL=false&serverTimezone=UTC
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password

JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

User register service endpoint
user.register.service.url=http://localhost:8083/users/employee

server.port=8082
```

### Build & Run
    
```
mvn clean install
mvn spring-boot:run
```


## 🌐 API Documentation

The interactive API documentation is available via **Swagger UI** at:
```
http://localhost:7071/swagger-ui/index.html
```

Use this page to explore, test, and learn about the API endpoints, request formats, and response structures.

## 📝 API Usage Summary

*(For detailed API schema and interactive testing, please use the Swagger UI above)*
```
- **Submit Timesheet** (POST `/timesheet/submit?userId={userId}`)  
  Submit daily or weekly timesheets depending on employee type.

- **Retrieve Timesheets** (GET `/timesheet/user?userId={userId}`)  
  Retrieve all timesheets submitted by a given user.
```
## 🗃️ Sample Submit Timesheet Request

### Internal Employee (Daily):

```json
{
  "type": "DAILY",
  "date": "2025-08-06",
  "entries": [
    {
      "project": "Finance System",
      "hours": 8,
      "description": "Processed invoices"
    }
  ]
}

```


### External Employee (Weekly):

```
{
  "type": "WEEKLY",
  "date": "2025-08-04",
  "entries": [
    {
      "day": "MON",
      "project": "Customer Portal",
      "hours": 6,
      "description": "Frontend work"
    },
    {
      "day": "TUE",
      "project": "Customer Portal",
      "hours": 7,
      "description": "Backend API"
    }
  ]
}
```
## 🗃️ Sample Retrieve Timesheets Request

### Retrieve Timesheets for User ID 123:

```http
{
    "success": true,
    "message": "Timesheet submitted successfully",
    "data": {
        "id": 3,
        "userId": "ADRTIN004",
        "employeeType": "EXTERNAL",
        "timesheetType": "WEEKLY",
        "entries": [
            {
                "project": "Customer Portal",
                "hours": 6.0,
                "description": "Frontend work"
            },
            {
                "project": "Customer Portal",
                "hours": 7.0,
                "description": "Backend API"
            }
        ],
        "timesheetDate": "2025-08-04",
        "percentageOfTarget": 32.5
    },
    "error": null,
    "timestamp": "2025-08-06T15:09:46.0227741"
}
```

## ⚠️ Error Handling

- Validation errors yield HTTP 400 with detailed messages including field-level errors.
- User-not-found error returns a validation failure response.
- Unexpected exceptions return HTTP 500 with a standardized error structure.
- Global centralized exception handling ensures consistent API error responses.

## 🗂️ Data Model Summary

- **Timesheet Entity Fields:**
    - `id`: Unique timesheet record identifier
    - `userId`: Links timesheet to user in User Register service
    - `timesheetType`: Enumeration `DAILY` or `WEEKLY`
    - `timesheetDate`: Date (for daily) or week start date (for weekly)
    - `entriesJson`: JSON string storing timesheet entries
    - `percentageOfTarget`: Calculated progress against expected working hours (8/day or 40/week)
    - `employeeType`: `"INTERNAL"` or `"EXTERNAL"` derived based on timesheet type
    - `createdAt`, `updatedAt`: Automatic timestamps for audit

- **Timesheet Entries:** Each entry includes project info, hours worked, description, and optionally day of week for weekly entries.

## 👩‍💻 Contributing

- Fork and clone the repository
- Set up the environment as documented
- Follow coding conventions and write tests
- Submit detailed pull requests for review

---

## 🐳 Dockerfile

Your repository contains a Dockerfile implementing a multi-stage build with SSL certificate import and configurable Spring profiles & ports. It creates a production-ready container image for the Timesheet microservice.

Highlights:

- Uses OpenJDK slim images (JDK 17) for build and runtime stages.
- Installs Maven in the builder stage for dependency management and packaging.
- Copies and imports a custom SSL certificate into the Java truststore for secure outgoing HTTPS connections.
- Supports dynamic Spring profile and server port configuration via build arguments and environment variables.

---

## ⚙️ CI/CD Pipeline (GitHub Actions)

Your project includes a GitHub Actions workflow (`docker-build.yml`) that automates:

- Building Docker images for dev (`develop` branch) and prod (`master` branch).
- Decoding and injecting your SSL certificate from GitHub Secrets during production builds.
- Tagging images with semantic versioning that automatically increments patches.
- Pushing images to Docker Hub.
- Cleaning up old Docker tags while preserving the latest 10 per environment.
---
## 📄 License

Specify your license information here.

---

*For questions, support, or contributions, please contact the maintainers.*

