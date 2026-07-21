# CSCI 4801 heliostat project

Welcome to the **Heliostat Engine** backend project! Heliostat is a lightweight, Java-based household chore, task-scheduling, and rewards management system.

This project uses a zero-external-database design—all persistent data is saved as serialized state files directly to your local file system under the `storage/` directory.

---

## 🛠️ Prerequisites

Before you begin, ensure you have the following installed on your machine:

- **Java Development Kit (JDK):** Version 17 or higher
- **Git:** For cloning and version control
- **Terminal / Shell:**
  - **macOS / Linux:** Terminal or Zsh
  - **Windows:** PowerShell or Command Prompt
- **API Client (recommended):** Postman, Bruno, or `curl` via terminal

> **Note:** You do **not** need to install Gradle manually. This repository includes the **Gradle Wrapper** (`gradlew`), which automatically manages the build system for you.

---

## 🚀 Getting Started

### 1. Clone the Repository
Open your terminal and clone the workspace repository:
```bash
git clone [https://github.com/sahu0037/CSCI-4801-Summer26-heliostat]
cd CSCI-4801-Summer26-heliostat

```

### 2. Verify Your Environment

Run the Gradle wrapper check to ensure Java is configured correctly:

* **macOS / Linux:**
```bash
./gradlew --version

```


* **Windows (PowerShell):**
```powershell
.\gradlew.bat --version

```



---

## 🏃 Running the Application

To compile the codebase and launch the local HTTP server, run:

```bash
# macOS / Linux
./gradlew clean run

# Windows
.\gradlew.bat clean run

```

When successfully launched, you will see output confirming that the HTTP server is listening:

```text
Heliostat Server listening on http://localhost:8080/

```

---

## 📂 Project Architecture & Storage Structure

When you create profiles, tasks, or rewards, the application writes serialized `.dat` files into an isolated directory structure created automatically in your project root:

```text
heliostat-engine/
├── storage/
│   ├── profiles/    # UserProfile binary serialized files (*.dat)
│   ├── tasks/       # Task binary serialized files (*.dat)
│   └── rewards/     # Reward binary serialized files (*.dat)
├── src/
│   └── main/java/com/heliostat/engine/
│       ├── model/       # Java Domain Models (UserProfile, Task, Reward)
│       ├── repository/  # File I/O Persistence Layers
│       ├── util/        # Cryptography and Security Helpers
│       └── ProfileApiServer.java  # HTTP Endpoints & Router
└── build.gradle

```

---

## 🧪 Quick Test Guide (API Lifecycle)

You can test the entire Task & Reward lifecycle using `curl` commands in a second terminal window. or use API Client Postman

### Step 1: Create Profile 

```bash
curl -X POST http://localhost:8080/api/login \
     -H "Content-Type: application/json" \
     -d '{
        "id": "parent-alex",
        "name": "alex Smith",
        "pinHash": "$2a$12$ScrambledTextHere",
        "balance": 50,
        "active": true,
        "roles": ["MANAGER"]
     }'

```

---

### Step 2: Create a Scheduled Task (Manager)

```bash
curl -X POST http://localhost:8080/api/tasks \
     -H "Content-Type: application/json" \
     -d '{
       "id": "task-lawn",
       "title": "Mow the Lawn",
       "description": "Mow front and back yards.",
       "rewardPoints": 100,
       "managerId": "parent-alex",
       "requiresReview": true,
       "recurrence": "WEEKLY"
     }'

```

---

### Step 3: Claim & Submit Task (Performer)

```bash
# Claim the task
curl -X POST "http://localhost:8080/api/tasks?action=claim&id=task-lawn&userId=child-timmy"

# Submit completed work for review
curl -X POST "http://localhost:8080/api/tasks?action=submit&id=task-lawn&userId=child-timmy"

```

---

### Step 4: Verify Task & Release Points (Manager)

```bash
curl -X POST "http://localhost:8080/api/tasks?action=verify&id=task-lawn&userId=parent-alex"

```

---

### Step 5: Purchase a Reward from Storefront (Performer)

```bash
curl -X POST "http://localhost:8080/api/rewards?action=purchase&id=reward-screentime&userId=child-timmy"

```

---

## ❓ Troubleshooting

### Error: `Address already in use` or `Port 8080 locked`

This means a previous instance of the server is still running in the background.

* **macOS / Linux:**
```bash
lsof -i :8080
kill -9 <PID>

```


* **Windows:**
```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F

```



### Error: `Lock file hash cache is currently in use`

Clear any locked Gradle background daemons:

```bash
./gradlew --stop

```

---
