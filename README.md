# LangChain4j AI Order Support Demo

A Spring Boot application that demonstrates how to integrate LangChain4j with an AI-powered customer support assistant for ecommerce order queries. The project exposes REST endpoints that route user questions to an AI agent, which can use tools to fetch real order information from a MySQL-backed database and answer or act on order requests.

This is a practical example of the "tool-using AI agent" pattern: the LLM is prompted with system instructions, and when a customer asks for order details, it invokes structured Java methods rather than making up answers.

## Project purpose

The application shows how to:

- integrate LangChain4j with Spring Boot
- expose AI capabilities through REST endpoints
- use structured tools for domain-specific actions
- connect an LLM to an application data layer via JPA and MySQL
- build a customer support assistant for order status and cancellation flows

## Technologies used

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA
- MySQL Connector/J
- LangChain4j 1.21.0-beta31
- LangChain4j OpenAI Spring Boot starter
- GLM-5.3-Flash model via the OpenCode Go API (OpenAI-compatible endpoint)
- Maven build tool
- JUnit 5 for test setup

## Architecture overview

The system is organized around the following layers:

1. Controller layer
   - `AiSupportController`
   - Exposes endpoints under `/api/ai-support`

2. AI service layer
   - `JavaTechieAIAgent`
   - `OrderSupportAIAgent`
   - Declared with `@AiService` and connected to LangChain4j

3. Domain/service layer
   - `OrderService`
   - Encapsulates business rules for orders

4. Tool layer
   - `OrderTools`
   - Exposes Java methods as LangChain4j tools for the LLM to call

5. Persistence layer
   - `OrderRepository`
   - Spring Data JPA repository for database access

6. Database model
   - `OrderEntity`
   - JPA entity mapped to the `orders` table

## Key application flow

When an HTTP request is sent to the AI support endpoint:

- the controller receives the user question
- the AI agent receives the prompt
- the model decides whether it needs to use business tools
- if the message is about an order, the tool layer calls `OrderService`
- `OrderService` reads data from MySQL using `OrderRepository`
- the LLM converts that data into a helpful response

Example flow for an order inquiry:

- Customer asks: "What is the status of order ORD-1001?"
- The model recognizes the request as order-related
- It invokes `getOrderStatus(orderId)` from the tool set
- The service queries the `orders` table
- The raw order record is returned to the LLM
- The final answer is generated in natural language

## Project structure

```text
langchain4j-agent/
├── .gitignore
├── compose.yml
├── pom.xml
├── README.md
├── request.http
├── langchain4j-agent.postman_collection.json
├── src/
│   ├── main/
│   │   ├── java/com/javatechie/
│   │   │   ├── Langchain4jExampleApplication.java
│   │   │   ├── config/
│   │   │   │   └── ToolErrorHandlingConfig.java
│   │   │   ├── controller/
│   │   │   │   └── AiSupportController.java
│   │   │   ├── exception/
│   │   │   │   ├── OrderCannotBeCancelledException.java
│   │   │   │   └── OrderNotFoundException.java
│   │   │   ├── model/
│   │   │   │   └── OrderEntity.java
│   │   │   ├── repository/
│   │   │   │   └── OrderRepository.java
│   │   │   ├── service/
│   │   │   │   ├── JavaTechieAIAgent.java
│   │   │   │   ├── OrderSupportAIAgent.java
│   │   │   │   └── OrderService.java
│   │   │   └── tool/
│   │   │       └── OrderTools.java
│   │   └── resources/
│   │       └── application.yaml
│   └── test/
│       └── java/com/javatechie/
│           └── Langchain4JExampleApplicationTests.java
```

The root `.gitignore` excludes generated Maven output (`target/`), IDE project metadata, logs, temporary files, and common operating-system files.

## Configuration

The project is configured in `src/main/resources/application.yaml`.

### Main settings

```yaml
spring:
  application:
    name: langchain4j-demo
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/javatechie
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:Password}

  jpa:
    show-sql: true
    hibernate:
      ddl-auto: update
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect

server.port: 9292

langchain4j:
  open-ai:
    chat-model:
      base-url: https://opencode.ai/inference/go/openai/v1
      api-key: ${OPENCODE_API_KEY}
      model-name: glm-5.3-flash
      custom-headers:
        User-Agent: langchain4j-agent/1.0
        x-opencode-session: langchain4j-order-support
```

### Important notes

- The project expects a MySQL database named `javatechie` on localhost.
- The app uses `ddl-auto: update`, so Hibernate can create or update tables based on the entity model.
- The LLM is served through the OpenAI-compatible OpenCode Go endpoint (`base-url` above). Set the `OPENCODE_API_KEY` environment variable before starting the application; the key is never stored in source control. Other Chat-Completions-native Go models (for example `glm-5.3`, `kimi-k3.1-pro`, `deepseek-v4.1-flash`) can be used by changing `model-name`.
- Database credentials default to local development values and can be overridden with the `DB_USERNAME` and `DB_PASSWORD` environment variables.
- The server listens on port `9292`.

## Data model

The application uses the `OrderEntity` class mapped to the `orders` table.

Entity fields:

- `id` — primary key
- `orderId` — unique order identifier
- `customerName`
- `productName`
- `status`
- `currentLocation`
- `expectedDelivery`
- `totalAmount`
- `paymentStatus`

The repository interface contains:

```java
Optional<OrderEntity> findByOrderId(String orderId);
```

This allows the AI tool to fetch real order records by a given `orderId`.

## Order logic

### `OrderService`

This service contains two important functions:

- `getOrderStatus(String orderId)`
  - fetches order details from the repository
  - throws `OrderNotFoundException` if the order is not found

- `cancelOrder(String orderId)`
  - loads the order
  - refuses cancellation if status is `Shipped` (throws `OrderCannotBeCancelledException`)
  - changes the status to `CANCELLED`
  - clears `expectedDelivery`
  - saves the updated record

Both exceptions implement LangChain4j's `ToolErrorVisibleToLlm`, so when they
escape a tool, only a curated, user-facing message is sent to the LLM instead of
raw internal error details. `ToolErrorHandlingConfig` wires the corresponding
tool error handlers into every `@AiService`.

### `OrderTools`

The tools are annotated with `@Tool`, which tells LangChain4j that these Java methods are callable by the AI model.

Available tools:

- `getOrderStatus(String orderId)`
- `cancelOrder(String orderId)`

These methods are intentionally simple and domain-specific so the LLM can safely operate on business data without hallucinating results.

## AI agent definitions

### `JavaTechieAIAgent`

```java
@AiService
public interface JavaTechieAIAgent {

    @SystemMessage("""
            You are JavaTechie Assistant, a helpful and concise AI assistant.

            You are powered by the GLM model (glm-5.3-flash, trained by Z.ai),
            served through the OpenCode Go API. If asked who you are or which
            model you run on, answer truthfully based on this information.
            """)
    String chat(String message);
}
```

This acts as a general-purpose chat agent. The system message establishes the
assistant's identity — without it, models cannot reliably self-identify.

### `OrderSupportAIAgent`

This is the domain-focused assistant. It uses a system message:

```java
@SystemMessage("""
    You are an ecommerce customer support assistant.

    When a customer asks about an order,
    use the available tools to get the real order information.

    Do not invent order status.
    Keep your response concise and helpful.
    """)
```

This makes the model tool-aware and prevents it from inventing order information.

## REST endpoints

### General chat endpoint

```http
GET /api/ai-support?message=hello
```

This sends a text prompt to the generic AI agent.

### Order support endpoint

```http
GET /api/ai-support/order?message=What is the status of order ORD-1001?
```

This routes the message through the order support assistant with access to order tools.

## Example usage

### Start the app

```bash
export OPENCODE_API_KEY=<your-opencode-go-api-key>
mvn spring-boot:run
```

### Example requests

```bash
curl "http://localhost:9292/api/ai-support?message=How%20are%20you?"
curl "http://localhost:9292/api/ai-support/order?message=What%20is%20the%20status%20of%20order%20ORD-1001?"
curl "http://localhost:9292/api/ai-support/order?message=Cancel%20order%20ORD-1001"
```

## Required MySQL setup

Docker Compose starts MySQL automatically when the Spring Boot application starts (Docker Compose and a running Docker daemon must be installed). The `compose.yml` service uses the same database name and credentials as `application.yaml`; the named `mysql_data` volume retains database files across container restarts and recreation.

You can also start the database manually:

```sql
CREATE DATABASE javatechie;
USE javatechie;
```

Hibernate will create the `orders` table automatically due to `ddl-auto: update`, but you can also define it yourself.

Example table definition:

```sql
CREATE TABLE orders (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id VARCHAR(255) NOT NULL UNIQUE,
    customer_name VARCHAR(255),
    product_name VARCHAR(255),
    status VARCHAR(255),
    current_location VARCHAR(255),
    expected_delivery DATE,
    total_amount DECIMAL(19, 2),
    payment_status VARCHAR(255),
    PRIMARY KEY (id)
);
```

Sample data:

```sql
INSERT INTO orders (
    order_id, customer_name, product_name, status,
    current_location, expected_delivery, total_amount, payment_status
) VALUES (
    'ORD-1001', 'John Doe', 'iPhone 15', 'Shipped',
    'Hyderabad', '2026-11-05', 999.00, 'PAID'
);
```

## Build and run commands

### Compile the project

```bash
mvn clean compile
```

### Run tests

```bash
mvn test
```

### Start locally

```bash
mvn spring-boot:run
```

Spring Boot's Docker Compose integration starts the MySQL service from `compose.yml` before the application and stops it when the application shuts down. To manage the database separately, use `docker compose up -d` and `docker compose down`; the named volume remains intact when containers are removed. To permanently remove stored database data, explicitly remove the volume with `docker compose down --volumes`.

## Testing

The repository contains a minimal Spring Boot test:

```java
@SpringBootTest
class Langchain4JExampleApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

This verifies that the Spring application context loads successfully.

## Notes and best practices

- Keep API keys out of source control.
- Use environment variables or secret management in production.
- Real-world systems should validate user input and enforce authorization.
- For production use, add logging, circuit breakers, retries, better error handling, and request validation.
- The project is a demo and intentionally keeps the order operations simple.

## Common issues

### 1. MySQL connection failure

Check:
- MySQL is running locally
- database name is `javatechie`
- username/password match your local configuration
- driver is available via Maven dependency

### 2. OpenCode Go API key missing

Set the `OPENCODE_API_KEY` environment variable (an OpenCode Go subscription key) before starting the app.

### 3. Port already in use

If port `9292` is occupied, change the value in `application.yaml`:

```yaml
server.port: 9293
```

## References and official documentation

1. LangChain4j official documentation: https://docs.langchain4j.dev/

2. LangChain4j Spring Boot integration: https://docs.langchain4j.dev/integrations/spring-boot/

3. LangChain4j AI Services: https://docs.langchain4j.dev/tutorials/ai-services

4. Spring Boot official docs: https://spring.io/projects/spring-boot

5. Spring Data JPA docs: https://spring.io/projects/spring-data-jpa

6. OpenCode Go (model API used by this project): https://opencode.ai/v2/docs/console/go/

7. OpenAI API documentation (for the fallback OpenAI configuration): https://platform.openai.com/docs/api-reference

8. MySQL official docs: https://dev.mysql.com/doc/

9. Maven official website: https://maven.apache.org/

10. Java 21 documentation: https://docs.oracle.com/en/java/javase/21/

11. JPA/Hibernate reference: https://hibernate.org/orm/

## Summary

This project is a compact but realistic demonstration of an AI-powered order support assistant using LangChain4j, Spring Boot, and MySQL. It shows how an LLM can safely work with business data through tool-based architecture, making it a useful starting point for building AI customer support, internal assistant, or workflow automation systems.
