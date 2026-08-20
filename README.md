# NeoBank

End-to-end **Digital Banking System** built with **Spring Boot, Microservices, Apache Kafka, Redis, SAGA Pattern, and Fraud Detection**.

## 🚀 Features

- Customer & Account Management
- Banking Transactions
- Money Transfer
- Fraud Detection
- Event-Driven Architecture
- Asynchronous Communication using Apache Kafka
- Distributed Transactions using SAGA Pattern
- Redis Caching
- RESTful APIs
- Microservices Architecture
- Docker & Docker Compose
- Exception Handling & Validation

## 🛠️ Tech Stack

- Java
- Spring Boot
- Spring Cloud
- Spring Data JPA
- Spring Security
- Apache Kafka
- Redis
- PostgreSQL
- Docker
- Maven
- Git & GitHub

## 🏗️ Architecture

```text
                         ┌──────────────────┐
                         │    API Gateway   │
                         └────────┬─────────┘
                                  │
             ┌────────────────────┼────────────────────┐
             ↓                    ↓                    ↓
      ┌─────────────┐      ┌─────────────┐      ┌─────────────┐
      │   Account   │      │ Transaction │      │   Payment   │
      │   Service   │      │   Service   │      │   Service   │
      └──────┬──────┘      └──────┬──────┘      └──────┬──────┘
             │                    │                    │
             └────────────────────┼────────────────────┘
                                  ↓
                         ┌──────────────────┐
                         │      Kafka       │
                         │ Event Streaming  │
                         └────────┬─────────┘
                                  │
                    ┌─────────────┴─────────────┐
                    ↓                           ↓
             ┌─────────────┐             ┌─────────────┐
             │    Redis    │             │    Fraud    │
             │    Cache    │             │  Detection  │
             └─────────────┘             └─────────────┘
             └─────────────┘             └─────────────┘


# 🔄 SAGA Pattern

The project uses the **SAGA Pattern** to manage distributed transactions across multiple microservices.

```text
Transaction
     ↓
Account Service
     ↓
Payment Service
     ↓
Fraud Detection
     ↓
Success → Commit
     │
     └── Failure → Compensation

# 📂 Project Structure

NeoBank/
│
├── api-gateway/
├── account-service/
├── transaction-service/
├── payment-service/
├── fraud-detection-service/
├── notification-service/
│
├── docker-compose.yml
└── README.md

# ⚙️ Getting Started

## Clone the Repository

```bash
git clone https://github.com/i-Abhisek/NeoBank.git
cd NeoBank

# Start Infrastructure
docker-compose up -d

# Build the Project
mvn clean install

# Run the Application

Start each Spring Boot microservice individually or using Docker Compose.

🔐 Security
Authentication & Authorization
Secure REST APIs
Input Validation
Transaction Validation
Fraud Detection
Distributed Transaction Management

# 🎯 Project Objective

The goal of this project is to demonstrate the design and implementation of a real-world digital banking platform using modern backend technologies and distributed-system patterns.

The project focuses on Microservices, Event-Driven Architecture, Apache Kafka, Redis, SAGA Pattern, and Fraud Detection.

 # 👨‍💻 Author

Abhisek Sahoo

Java Developer | Spring Boot | Microservices | Kafka
