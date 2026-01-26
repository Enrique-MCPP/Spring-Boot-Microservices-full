# Arquitectura Hexagonal en Order Service

## Resumen Ejecutivo

El proyecto **order-service** implementa **Arquitectura Hexagonal** (también conocida como Ports & Adapters) de forma implícita pero clara. Este documento explica cómo se aplican los principios hexagonales en la estructura actual del proyecto.

## ¿Qué es Arquitectura Hexagonal?

La Arquitectura Hexagonal separa la lógica de negocio (dominio) de los detalles técnicos (infraestructura) mediante:

1. **Dominio (Core)**: Lógica de negocio pura, sin dependencias externas
2. **Puertos (Ports)**: Interfaces que definen cómo interactuar con el dominio
3. **Adaptadores (Adapters)**: Implementaciones concretas de los puertos

```
┌─────────────────────────────────────────────────────────┐
│                    ADAPTADORES PRIMARIOS                 │
│        (Driving Adapters - Inician acciones)            │
│                                                           │
│   ┌─────────────┐  ┌─────────────┐  ┌──────────────┐   │
│   │ REST API    │  │ Kafka       │  │ gRPC Client  │   │
│   │ Controller  │  │ Listener    │  │              │   │
│   └──────┬──────┘  └──────┬──────┘  └──────┬───────┘   │
│          │                │                 │            │
│          └────────────────┴─────────────────┘            │
│                           │                              │
└───────────────────────────┼──────────────────────────────┘
                            │
        ┌───────────────────▼───────────────────┐
        │        PUERTOS DE ENTRADA             │
        │   (Input Ports - Interfaces)          │
        │                                        │
        │   • PlaceOrderCommandHandler          │
        │   • QueryHandlers (futuros)           │
        └───────────────────┬───────────────────┘
                            │
        ┌───────────────────▼───────────────────┐
        │           DOMINIO (CORE)               │
        │      Lógica de Negocio Pura           │
        │                                        │
        │   ┌────────────────────────────────┐  │
        │   │  Entities                      │  │
        │   │  • Order                       │  │
        │   │  • OrderLineItem               │  │
        │   │  • OrderStatus                 │  │
        │   └────────────────────────────────┘  │
        │                                        │
        │   ┌────────────────────────────────┐  │
        │   │  Value Objects                 │  │
        │   │  • Money                       │  │
        │   │  • OrderNumber                 │  │
        │   │  • Quantity                    │  │
        │   └────────────────────────────────┘  │
        │                                        │
        │   ┌────────────────────────────────┐  │
        │   │  Commands                      │  │
        │   │  • PlaceOrderCommand           │  │
        │   └────────────────────────────────┘  │
        └───────────────────┬───────────────────┘
                            │
        ┌───────────────────▼───────────────────┐
        │        PUERTOS DE SALIDA              │
        │   (Output Ports - Interfaces)         │
        │                                        │
        │   • OrderRepository (JPA Interface)   │
        │   • InventoryService (future)         │
        │   • EventPublisher (implicit)         │
        └───────────────────┬───────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────┐
│                 ADAPTADORES SECUNDARIOS                   │
│       (Driven Adapters - Son invocados)                  │
│                                                            │
│   ┌──────────────┐  ┌────────────┐  ┌────────────────┐  │
│   │ PostgreSQL   │  │ Kafka      │  │ gRPC Server    │  │
│   │ (JPA)        │  │ Producer   │  │ (Inventory)    │  │
│   └──────────────┘  └────────────┘  └────────────────┘  │
└────────────────────────────────────────────────────────────┘
```

## Mapeo de la Estructura Actual

### 1. DOMINIO (Core Business Logic)

**Ubicación**: `domain/`

#### Entidades (`domain/entity/`)
```java
// Order.java - Agregado raíz con lógica de negocio
public class Order {
    // Factory Method
    public static Order create(List<OrderLineItem> items) { ... }

    // Business Logic
    public void confirm() { ... }
    public void cancel() { ... }
    public Money calculateTotal() { ... }
}
```

#### Value Objects (`domain/valueobject/`)
```java
// Money.java - Objeto inmutable con operaciones
public class Money {
    public Money add(Money other) { ... }
    public Money multiply(int quantity) { ... }
    public boolean isGreaterThan(Money other) { ... }
}
```

**Características**:
- ✅ Sin dependencias de frameworks (solo JPA annotations necesarias)
- ✅ Lógica de negocio pura
- ✅ Testeable sin Spring
- ✅ Inmutabilidad en Value Objects

### 2. APPLICATION LAYER (Use Cases)

**Ubicación**: `application/command/`

#### Command Handlers (Input Ports)
```java
// PlaceOrderCommandHandler.java
@Service
public class PlaceOrderCommandHandler {
    // Orquesta el caso de uso, NO contiene lógica de negocio
    public String handle(PlaceOrderCommand command) {
        Order order = Order.create(lineItems);  // Dominio
        order.confirm();                         // Dominio
        orderRepository.save(order);             // Puerto de salida
        publishEvent(order);                     // Puerto de salida
    }
}
```

**Características**:
- ✅ Orquesta casos de uso
- ✅ No contiene lógica de negocio
- ✅ Depende de abstracciones (Repository interface)
- ✅ Transaccional (@Transactional)

### 3. ADAPTADORES PRIMARIOS (Driving Adapters)

#### REST Adapter
**Ubicación**: `controller/OrderController.java`

```java
@RestController
@RequestMapping("/api/order")
public class OrderController {
    private final PlaceOrderCommandHandler handler;

    @PostMapping()
    public String placeOrder(@RequestBody OrderRequest request) {
        PlaceOrderCommand command = new PlaceOrderCommand(...);
        return handler.handle(command);  // Delega al puerto
    }
}
```

**Características**:
- ✅ Traduce HTTP requests a Commands
- ✅ No contiene lógica de negocio
- ✅ Delega todo al Command Handler

#### Kafka Event Listener (futuro)
Recibiría eventos y los traduciría a Commands.

### 4. ADAPTADORES SECUNDARIOS (Driven Adapters)

#### Persistence Adapter
**Ubicación**: `repository/OrderRepository.java`

```java
// Puerto (Interface)
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
}
```

**Implementación**: Proporcionada por Spring Data JPA

#### Kafka Event Publisher
**Ubicación**: `KafkaProducerConfig.java` + uso en CommandHandler

```java
// Configuración del adaptador
@Configuration
public class KafkaProducerConfig {
    @Bean
    public KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate() { ... }
}

// Uso en el Command Handler
private void publishOrderPlacedEvent(Order order) {
    OrderPlacedEvent event = new OrderPlacedEvent(...);
    kafkaTemplate.send("order-placed", event);
}
```

#### gRPC Client Adapter
**Ubicación**: Preparado con `inventory.proto`

Invoca al inventory-service mediante gRPC (puerto de salida).

## Beneficios Implementados

### 1. Independencia de Frameworks
```java
// ✅ Los tests del dominio NO usan Spring
@Test
void shouldCalculateTotalCorrectly() {
    // Pure domain logic - NO @SpringBootTest
    Order order = Order.create(List.of(item1, item2));
    Money total = order.calculateTotal();
    assertEquals(expectedTotal, total);
}
```

### 2. Testabilidad
- **Unit Tests**: 21 tests del dominio sin Spring (< 1 segundo)
- **Isolation**: Cada capa puede probarse independientemente
- **Mocks**: Los puertos pueden mockearse fácilmente

### 3. Flexibilidad de Cambios

#### Cambiar Base de Datos
```java
// Antes: PostgreSQL con JPA
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> { }

// Después: MongoDB (solo cambia implementación)
@Repository
public class MongoOrderRepository implements OrderRepository {
    // Nueva implementación, mismo puerto
}
```

#### Cambiar API
```java
// Antes: REST Controller
@RestController
public class OrderController { }

// Agregar: GraphQL Controller
@Controller
public class OrderGraphQLController {
    // Usa el MISMO PlaceOrderCommandHandler
}
```

## Estructura de Carpetas Recomendada

### Opción A: Estructura Actual (Implícita pero Clara)
```
order-service/
├── domain/                    # DOMINIO (CORE)
│   ├── entity/
│   │   ├── Order.java
│   │   ├── OrderLineItem.java
│   │   └── OrderStatus.java
│   └── valueobject/
│       ├── Money.java
│       ├── OrderNumber.java
│       └── Quantity.java
├── application/               # APPLICATION LAYER
│   └── command/
│       ├── PlaceOrderCommand.java
│       └── PlaceOrderCommandHandler.java  # Input Port
├── repository/                # Output Port
│   └── OrderRepository.java
├── controller/                # Input Adapter (REST)
│   └── OrderController.java
├── event/                     # Output Adapter (Events)
│   └── OrderPlacedEvent.java
└── config/                    # Infrastructure
    ├── KafkaProducerConfig.java
    └── SecurityConfig.java
```

### Opción B: Estructura Explícita con adapter/port/
```
order-service/
├── domain/                    # DOMINIO (CORE)
│   ├── entity/
│   ├── valueobject/
│   └── service/              # Domain Services
├── application/              # APPLICATION LAYER
│   ├── port/
│   │   ├── input/           # Input Ports
│   │   │   └── PlaceOrderUseCase.java (interface)
│   │   └── output/          # Output Ports
│   │       ├── OrderRepositoryPort.java
│   │       └── EventPublisherPort.java
│   └── usecase/
│       └── PlaceOrderUseCaseImpl.java
└── adapter/                 # ADAPTERS
    ├── input/               # Driving Adapters
    │   ├── rest/
    │   │   └── OrderController.java
    │   └── messaging/
    │       └── OrderKafkaListener.java
    └── output/              # Driven Adapters
        ├── persistence/
        │   └── OrderJpaAdapter.java
        └── messaging/
            └── KafkaEventPublisher.java
```

## Comparación de Opciones

| Aspecto | Opción A (Actual) | Opción B (Explícita) |
|---------|-------------------|----------------------|
| **Claridad** | Implícita | Muy explícita |
| **Líneas de código** | Menos código | Más interfaces |
| **Para entrevistas** | Requiere explicación | Auto-explicativa |
| **Complejidad** | Menor | Mayor |
| **Spring Data JPA** | Compatible | Requiere wrapper |

## Recomendación para Entrevistas

Para el contexto de entrevistas, **la estructura actual (Opción A) es suficiente** si puedes explicar:

### Preguntas Típicas y Respuestas

**P: "¿Cómo implementas Arquitectura Hexagonal?"**

R: "En mi proyecto order-service, el dominio está completamente aislado en el package `domain/`, con entidades ricas como `Order` que contienen lógica de negocio. Los casos de uso están en `application/command/` donde `PlaceOrderCommandHandler` actúa como puerto de entrada. Los adaptadores incluyen `OrderController` para REST y `OrderRepository` para persistencia. Puedes ver que los 21 unit tests del dominio no dependen de Spring, lo que demuestra la independencia del core."

**P: "¿Dónde están los puertos?"**

R: "Los puertos de entrada son las interfaces implícitas en los Command Handlers - en una versión más explícita serían interfaces, pero aquí la separación está clara por responsabilidades. Los puertos de salida son las interfaces de repositorio como `OrderRepository extends JpaRepository`. El dominio nunca depende de implementaciones concretas."

**P: "¿Cómo cambiarías de PostgreSQL a MongoDB?"**

R: "Solo necesitaría crear una nueva implementación de `OrderRepository` usando Spring Data MongoDB. El dominio y los Command Handlers no cambiarían en absoluto porque dependen de la interfaz, no de JPA específicamente."

## Tests que Demuestran la Arquitectura

```java
// ✅ Test de Dominio - Sin dependencias externas
class OrderTest {
    @Test
    void shouldCalculateTotalCorrectly() {
        Order order = Order.create(List.of(item1, item2));
        Money total = order.calculateTotal();
        assertEquals(new BigDecimal("4900.00"), total.getAmount());
    }
}

// ✅ Test de Use Case - Con mocks de puertos
class PlaceOrderCommandHandlerTest {
    @Mock
    private OrderRepository orderRepository;

    @Test
    void shouldSaveOrderWhenItemsInStock() {
        // Arrange
        PlaceOrderCommand command = new PlaceOrderCommand(...);

        // Act
        handler.handle(command);

        // Assert
        verify(orderRepository).save(any(Order.class));
    }
}

// ✅ Test de Adapter REST - Con MockMvc
class OrderControllerTest {
    @Test
    @WithMockUser
    void shouldPlaceOrderSuccessfully() {
        mockMvc.perform(post("/api/order")
                .with(csrf())
                .contentType(APPLICATION_JSON)
                .content(orderJson))
            .andExpect(status().isCreated());
    }
}
```

## Conclusión

El proyecto **YA implementa Arquitectura Hexagonal**:

- ✅ **Dominio aislado**: Entidades ricas, Value Objects inmutables
- ✅ **Puertos de entrada**: Command Handlers
- ✅ **Puertos de salida**: Interfaces de Repository
- ✅ **Adaptadores primarios**: REST Controller
- ✅ **Adaptadores secundarios**: JPA, Kafka
- ✅ **Testabilidad**: 21 tests unitarios sin Spring
- ✅ **Independencia de frameworks**: El dominio es POJO

La estructura puede hacerse más explícita con packages `adapter/` y `port/`, pero los principios ya están aplicados correctamente.

## Referencias para la Entrevista

Menciona estos conceptos:

1. **Dependency Inversion**: El dominio no depende de infraestructura
2. **Separation of Concerns**: Cada capa tiene responsabilidad clara
3. **Port & Adapter Pattern**: Interfaces definen contratos
4. **Domain-Driven Design**: Entidades ricas, Value Objects
5. **CQRS**: Commands separados de Queries
6. **Testabilidad**: Unit tests rápidos del dominio

---

**Archivo**: `HEXAGONAL-ARCHITECTURE.md`
**Proyecto**: Spring Boot Microservices - Order Service
**Autor**: Interview Preparation Material
**Fecha**: 2026-01-26
