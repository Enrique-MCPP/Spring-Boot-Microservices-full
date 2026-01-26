# 🎯 Guía de Entrevista - Spring Boot Microservices Project

## 📋 Resumen Ejecutivo del Proyecto

Este proyecto demuestra una arquitectura de microservicios completa con:
- **Spring Boot 4.0 + Java 21**
- **DDD (Domain-Driven Design)** con entidades ricas y Value Objects
- **CQRS (Command Query Responsibility Segregation)**
- **Arquitectura Hexagonal** (Ports & Adapters)
- **Resiliencia**: Circuit Breaker, Retry, Timeout
- **Comunicación**: REST, Kafka, gRPC (preparado)
- **Observabilidad**: Zipkin (tracing distribuido)
- **Seguridad**: OAuth2 con Keycloak
- **Testing**: 22 tests (21 unitarios puros + 1 contexto Spring)

---

## 🗣️ Preguntas Frecuentes de Entrevista

### 1. "¿Cómo funciona la inyección de dependencias en Spring Boot? ¿Por qué usas constructor injection?"

**Respuesta**:

"En Spring Boot, la inyección de dependencias se basa en el **IoC Container** (Inversion of Control) que gestiona los beans.

#### ¿Qué es un Bean?

Un bean es un objeto gestionado por Spring. Spring crea, configura y gestiona su ciclo de vida.

```java
// ✅ Esto marca la clase como un bean
@Service
public class PlaceOrderCommandHandler {
    // Spring crea UNA instancia (singleton por defecto)
}

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Spring crea la implementación automáticamente
}
```

#### Formas de Inyección de Dependencias

**❌ 1. Field Injection (NO recomendado)**:
```java
@Service
public class PlaceOrderCommandHandler {
    @Autowired
    private OrderRepository orderRepository;  // Field injection

    @Autowired
    private KafkaTemplate kafkaTemplate;
}
```

**Problemas**:
- No puedes hacer el campo `final` (inmutabilidad)
- Dificulta testing (necesitas reflection para inyectar mocks)
- Dependencias ocultas (no se ven en el constructor)
- NullPointerException si olvidas @Autowired

**✅ 2. Constructor Injection (RECOMENDADO)**:
```java
@Service
public class PlaceOrderCommandHandler {
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    // Constructor - Spring inyecta automáticamente
    public PlaceOrderCommandHandler(
            OrderRepository orderRepository,
            KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate) {
        this.orderRepository = orderRepository;
        this.kafkaTemplate = kafkaTemplate;
    }
}
```

**Ventajas**:
- ✅ Campos `final` (inmutabilidad)
- ✅ Fácil testing (pasas mocks en el constructor)
- ✅ Dependencias explícitas
- ✅ Falla rápido si falta alguna dependencia (compile-time)
- ✅ No necesitas @Autowired (desde Spring 4.3)

**🎯 3. Con Lombok (AÚN MEJOR)**:
```java
@Service
@RequiredArgsConstructor  // Genera constructor con campos final
public class PlaceOrderCommandHandler {
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    // No necesitas escribir el constructor manualmente
}
```

#### Testing: Constructor vs Field Injection

**❌ Con Field Injection**:
```java
@Test
void testWithFieldInjection() {
    PlaceOrderCommandHandler handler = new PlaceOrderCommandHandler();
    // ❌ orderRepository es null! No puedes inyectarlo sin Spring

    // Necesitas reflection o Spring TestContext
    ReflectionTestUtils.setField(handler, \"orderRepository\", mockRepo);
}
```

**✅ Con Constructor Injection**:
```java
@Test
void testWithConstructorInjection() {
    OrderRepository mockRepo = mock(OrderRepository.class);
    KafkaTemplate mockKafka = mock(KafkaTemplate.class);

    // ✅ Simple y claro
    PlaceOrderCommandHandler handler =
        new PlaceOrderCommandHandler(mockRepo, mockKafka);

    handler.handle(command);
    verify(mockRepo).save(any(Order.class));
}
```

#### Ciclo de Vida de los Beans

```java
@Service
public class MyService {

    public MyService(DependencyA depA, DependencyB depB) {
        // 1. Spring llama al constructor
        System.out.println(\"Constructor called\");
    }

    @PostConstruct
    public void init() {
        // 2. Después del constructor, antes de usar el bean
        System.out.println(\"Bean initialized\");
    }

    @PreDestroy
    public void cleanup() {
        // 3. Antes de destruir el bean (al cerrar la app)
        System.out.println(\"Bean destroyed\");
    }
}
```

#### Scopes de Beans

```java
@Service  // Por defecto: @Scope(\"singleton\")
public class OrderService {
    // UNA instancia para toda la aplicación
}

@RestController
@Scope(\"request\")  // Nueva instancia por cada request HTTP
public class MyController {
    // Útil si necesitas estado por request
}

@Component
@Scope(\"prototype\")  // Nueva instancia cada vez que se inyecta
public class PrototypeBean {
    // Raro en aplicaciones web
}
```

#### Diferencias con Quarkus (CDI)

Vienes de Quarkus, aquí están las diferencias clave:

| Aspecto | Spring Boot | Quarkus |
|---------|-------------|---------|
| **Estándar** | Spring DI (propio) | CDI (Java EE/Jakarta) |
| **Anotación Service** | `@Service` | `@ApplicationScoped` |
| **Inyección** | `@Autowired` (opcional) | `@Inject` |
| **Scope singleton** | `@Service` / `@Component` | `@ApplicationScoped` |
| **Scope request** | `@Scope(\"request\")` | `@RequestScoped` |
| **Tiempo de arranque** | ~3-5 seg | ~0.5 seg (nativo) |
| **Constructor injection** | Recomendado, automático | Recomendado, `@Inject` necesario |

**Ejemplo equivalente**:

```java
// Quarkus (CDI)
@ApplicationScoped
public class OrderService {
    @Inject
    OrderRepository repository;

    // O con constructor
    @Inject
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}

// Spring Boot
@Service
public class OrderService {
    private final OrderRepository repository;

    // @Autowired es opcional desde Spring 4.3
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```

#### ¿Cómo sabe Spring qué inyectar?

**Por Tipo**:
```java
@Service
public class OrderService {
    private final OrderRepository repository;  // Spring busca un bean de tipo OrderRepository

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```

**Por Nombre** (si hay múltiples beans del mismo tipo):
```java
@Service
public class OrderService {
    private final InventoryClient restClient;

    public OrderService(@Qualifier(\"restInventoryClient\") InventoryClient client) {
        this.restClient = client;
    }
}

@Component(\"restInventoryClient\")
class RestInventoryClient implements InventoryClient { }

@Component(\"grpcInventoryClient\")
class GrpcInventoryClient implements InventoryClient { }
```

#### @Bean vs @Component/@Service

```java
// @Component/@Service: Spring escanea y registra automáticamente
@Service
public class OrderService {
    // Spring crea la instancia automáticamente
}

// @Bean: TÚ controlas cómo se crea el bean
@Configuration
public class KafkaConfig {

    @Bean
    public KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate() {
        // Aquí defines exactamente cómo se crea
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, \"localhost:9092\");

        ProducerFactory<String, OrderPlacedEvent> factory =
            new DefaultKafkaProducerFactory<>(config);

        return new KafkaTemplate<>(factory);
    }
}
```

**¿Cuándo usar cada uno?**
- **@Service/@Component**: Tus propias clases
- **@Bean**: Clases de librerías externas (KafkaTemplate, RestTemplate, ObjectMapper)

#### En este Proyecto

```java
@Service
@RequiredArgsConstructor  // Lombok genera el constructor
public class PlaceOrderCommandHandler {

    // ✅ final = inmutable
    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;
    private final InventoryServiceGrpcClient inventoryClient;

    // Spring inyecta automáticamente en el constructor generado por Lombok

    @Transactional
    public String handle(PlaceOrderCommand command) {
        // Usa las dependencias inyectadas
        Order order = orderRepository.save(order);
        kafkaTemplate.send(\"order-placed\", event);
    }
}
```

**¿Por qué es mejor que Quarkus en Spring?**
- No necesitas `@Inject` explícitamente
- Lombok hace el código más limpio
- Más maduro y documentado (existe desde 2004)

**¿Por qué Quarkus es mejor en algunos casos?**
- Arranque súper rápido (~20x más rápido)
- Menor consumo de memoria
- Ideal para serverless/Kubernetes
"

---

### 2. "Explica la arquitectura del proyecto"

**Respuesta**:

"Tengo un sistema de microservicios con 4 servicios principales:

- **Product Service**: Catálogo de productos (MongoDB)
- **Order Service**: Gestión de pedidos (PostgreSQL)
- **Inventory Service**: Control de stock (PostgreSQL)
- **Notification Service**: Envío de notificaciones (consumen eventos Kafka)

La comunicación entre servicios usa tres patrones:
1. **REST síncrono** para llamadas críticas (Order → Inventory)
2. **Kafka asíncrono** para eventos (OrderPlacedEvent)
3. **gRPC** preparado para comunicación de alto rendimiento

Todo pasa por un **API Gateway** con autenticación OAuth2 usando Keycloak, y se registran en **Eureka** para service discovery. Usamos **Zipkin** para tracing distribuido."

### 2. "Explica la arquitectura del proyecto"

**Respuesta**:

"Tengo un sistema de microservicios con 4 servicios principales:

- **Product Service**: Catálogo de productos (MongoDB)
- **Order Service**: Gestión de pedidos (PostgreSQL)
- **Inventory Service**: Control de stock (PostgreSQL)
- **Notification Service**: Envío de notificaciones (consumen eventos Kafka)

La comunicación entre servicios usa tres patrones:
1. **REST síncrono** para llamadas críticas (Order → Inventory)
2. **Kafka asíncrono** para eventos (OrderPlacedEvent)
3. **gRPC** preparado para comunicación de alto rendimiento

Todo pasa por un **API Gateway** con autenticación OAuth2 usando Keycloak, y se registran en **Eureka** para service discovery. Usamos **Zipkin** para tracing distribuido."

### 3. "¿Qué es DDD y cómo lo implementas?"

**Respuesta**:

"DDD (Domain-Driven Design) pone la lógica de negocio en el dominio, no en servicios.

**Antes (Modelo Anémico)**:
```java
// ❌ Entity sin comportamiento
public class Order {
    private Long id;
    private List<OrderLineItem> items;
    // Solo getters/setters
}

// ❌ Lógica en el Service
public class OrderService {
    public void placeOrder(Order order) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderLineItem item : order.getItems()) {
            total = total.add(item.getPrice().multiply(item.getQuantity()));
        }
        order.setTotalPrice(total);
    }
}
```

**Después (Dominio Rico)**:
```java
// ✅ Entity con comportamiento
public class Order {
    public static Order create(List<OrderLineItem> items) {
        Order order = new Order();
        order.orderNumber = OrderNumber.generate();
        order.addItems(items);
        order.status = OrderStatus.PENDING;
        return order;
    }

    public Money calculateTotal() {
        return items.stream()
            .map(OrderLineItem::getSubtotal)
            .reduce(Money.zero(), Money::add);
    }

    public void confirm() {
        if (this.status != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be confirmed");
        }
        this.status = OrderStatus.CONFIRMED;
    }
}
```

**Beneficios**:
- La lógica de negocio está donde pertenece (el dominio)
- Fácil de testear sin Spring (tests unitarios puros)
- Menos bugs porque las reglas están encapsuladas
"

### 4. "¿Qué son los Value Objects?"

**Respuesta**:

"Los Value Objects son conceptos del dominio sin identidad, solo definidos por sus atributos. Son inmutables.

**Ejemplo con Money**:
```java
public class Money {
    private final BigDecimal amount;

    public Money add(Money other) {
        return new Money(this.amount.add(other.amount));  // Nuevo objeto
    }

    public Money multiply(int quantity) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)));
    }
}
```

**¿Por qué usar Money en lugar de BigDecimal?**
- Evita errores (no puedes sumar dinero con cantidad)
- Encapsula reglas (no permite valores negativos)
- Operaciones de negocio claras (`money.multiply(quantity)`)
- Inmutabilidad garantizada

Otros Value Objects en mi proyecto:
- `OrderNumber`: Genera y valida números de pedido
- `Quantity`: Asegura cantidades positivas
"

### 5. "¿Qué es CQRS y cómo lo usas?"

**Respuesta**:

"CQRS separa operaciones de lectura (Queries) de escritura (Commands).

**Command**:
```java
public record PlaceOrderCommand(
    List<OrderLineItemsDto> orderLineItems
) {
    public PlaceOrderCommand {
        if (orderLineItems == null || orderLineItems.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }
    }
}
```

**Command Handler**:
```java
@Service
public class PlaceOrderCommandHandler {
    @Transactional
    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackPlaceOrder")
    public String handle(PlaceOrderCommand command) {
        // 1. Crear orden (dominio)
        Order order = Order.create(lineItems);

        // 2. Verificar inventario
        boolean inStock = checkInventory(order.getSkuCodes());

        // 3. Confirmar (dominio)
        order.confirm();

        // 4. Persistir
        orderRepository.save(order);

        // 5. Publicar evento
        publishEvent(order);

        return \"Order placed successfully\";
    }
}
```

**Beneficios**:
- Separación clara de responsabilidades
- Validación en el Command
- Transacción en el Handler
- Escalabilidad independiente de lecturas/escrituras
"

### 6. "¿Qué es Arquitectura Hexagonal?"

**Respuesta**:

"La Arquitectura Hexagonal (Ports & Adapters) separa el dominio de la infraestructura.

**Estructura**:
```
ADAPTADORES PRIMARIOS (REST, Kafka Listener)
         ↓
    PUERTOS DE ENTRADA (Command Handlers)
         ↓
      DOMINIO (Order, Money, etc.)
         ↓
    PUERTOS DE SALIDA (Repository interfaces)
         ↓
ADAPTADORES SECUNDARIOS (JPA, Kafka Producer)
```

**Ejemplo práctico**:

El **dominio** no sabe que usa PostgreSQL:
```java
// Puerto (interface)
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
}

// El dominio solo depende de la interfaz, no de JPA
```

**Beneficio**: Puedo cambiar de PostgreSQL a MongoDB sin tocar el dominio:
```java
// Nueva implementación
public class MongoOrderRepository implements OrderRepository {
    // Usa Spring Data MongoDB
}
```

**Prueba de independencia**: Mis 21 tests del dominio NO usan Spring:
```java
@Test
void shouldCalculateTotalCorrectly() {
    Order order = Order.create(List.of(item1, item2));
    Money total = order.calculateTotal();
    assertEquals(expected, total);
}
```
"

### 7. "¿Cómo manejas fallos en llamadas entre servicios?"

**Respuesta**:

"Uso **Resilience4j** con tres patrones:

**1. Circuit Breaker**:
```java
@CircuitBreaker(name = "inventory", fallbackMethod = "fallbackPlaceOrder")
public String handle(PlaceOrderCommand command) {
    // Si inventory-service falla mucho, el circuito se abre
}

private String fallbackPlaceOrder(PlaceOrderCommand command, Exception e) {
    return \"Service temporarily unavailable\";
}
```

**2. Retry**:
```java
@Retry(name = "inventory")  // Reintenta automáticamente
```

**3. Timeout**:
```java
@TimeLimiter(name = "inventory")  // Límite de tiempo
```

**Configuración**:
```yaml
resilience4j:
  circuitbreaker:
    instances:
      inventory:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
```

**¿Por qué es importante?**
- Evita efecto dominó (un servicio caído tumba todo el sistema)
- Mejora UX (respuesta rápida aunque sea fallback)
- Protege recursos (no satura servicios caídos)
"

### 8. "¿Cómo testeas tu código?"

**Respuesta**:

"Tengo tres niveles de testing:

**1. Tests Unitarios del Dominio (21 tests)**:
```java
// ✅ SIN Spring - Rápidos (< 1 segundo todos)
class OrderTest {
    @Test
    void shouldNotCancelShippedOrder() {
        Order order = Order.create(List.of(item));
        order.confirm();
        order.markAsShipped();

        assertThrows(IllegalStateException.class, () -> order.cancel());
    }
}
```

**2. Tests de Value Objects (10 tests)**:
```java
class MoneyTest {
    @Test
    void shouldAddMoneyCorrectly() {
        Money m1 = Money.of(new BigDecimal(\"100.00\"));
        Money m2 = Money.of(new BigDecimal(\"50.50\"));

        Money result = m1.add(m2);

        assertEquals(new BigDecimal(\"150.50\"), result.getAmount());
    }
}
```

**3. Test de Contexto Spring (1 test)**:
```java
@SpringBootTest
class OrderServiceApplicationTests {
    @Test
    void contextLoads() {
        // Verifica que toda la configuración carga correctamente
    }
}
```

**Resultado**:
```
Tests run: 22, Failures: 0, Errors: 0 ✅
```

**¿Por qué son buenos tests?**
- Rápidos (los del dominio no cargan Spring)
- Aislados (prueban una cosa cada uno)
- Mantenibles (código simple y legible)
- TDD-friendly (puedo escribir el test primero)
"

### 9. "¿Cómo funciona la comunicación con Kafka?"

**Respuesta**:

"Uso Kafka para eventos asíncronos entre servicios.

**Producer (Order Service)**:
```java
@Configuration
public class KafkaProducerConfig {
    @Bean
    public KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    private ProducerFactory<String, OrderPlacedEvent> producerFactory() {
        return new DefaultKafkaProducerFactory<>(
            configProps,
            new StringSerializer(),
            new JacksonJsonSerializer<OrderPlacedEvent>().noTypeInfo()
        );
    }
}

// Publicar evento
kafkaTemplate.send(\"order-placed\", new OrderPlacedEvent(orderNumber));
```

**Consumer (Notification Service)**:
```java
@KafkaListener(topics = \"order-placed\")
public void handleOrderPlaced(OrderPlacedEvent event) {
    log.info(\"Sending notification for order: {}\", event.getOrderNumber());
    // Enviar email/SMS
}
```

**¿Por qué Kafka y no REST?**
- **Desacoplamiento**: Los servicios no se conocen
- **Escalabilidad**: Múltiples consumers del mismo evento
- **Resiliencia**: Si notification-service cae, el mensaje espera
- **Auditoría**: Los eventos quedan registrados

**Nota**: Uso Spring Kafka 4.1 con `JacksonJsonSerializer` (no el deprecated `JsonSerializer`)
"

### 10. "¿Cómo funciona el API Gateway?"

**Respuesta**:

"El API Gateway es el punto de entrada único al sistema.

**Responsabilidades**:
1. **Enrutamiento**: Redirige peticiones a servicios
2. **Autenticación**: Valida JWT tokens de Keycloak
3. **Load Balancing**: Distribuye carga entre instancias

**Configuración**:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: order-service
          uri: lb://order-service  # lb = load balanced via Eureka
          predicates:
            - Path=/api/order/**
          filters:
            - RemoveRequestHeader=Cookie

      default-filters:
        - TokenRelay  # Propaga el JWT a los servicios
```

**Seguridad**:
```java
@Bean
public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
    http.authorizeExchange(exchanges ->
        exchanges
            .pathMatchers(\"/eureka/**\").permitAll()
            .anyExchange().authenticated()
    )
    .oauth2ResourceServer(oauth2 -> oauth2.jwt());
}
```

**Flujo**:
1. Cliente → API Gateway (puerto 8080)
2. Gateway valida JWT con Keycloak
3. Gateway consulta Eureka: \"¿Dónde está order-service?\"
4. Eureka responde: \"En localhost:8081\"
5. Gateway reenvía request con JWT
"

### 11. "¿Qué mejorarías en el proyecto?"

**Respuesta**:

"Identifico tres áreas de mejora:

**1. Completar gRPC**:
Actualmente tengo el protobuf definido y los stubs generados, pero falta:
- Implementar el servidor gRPC en inventory-service
- Implementar el cliente gRPC en order-service
- Beneficio: Comunicación más rápida y tipada que REST

**2. Tests de Integración**:
Añadiría tests con Testcontainers:
```java
@SpringBootTest
@Testcontainers
class OrderServiceIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>(\"postgres:16-alpine\");

    @Test
    void shouldSaveAndRetrieveOrder() {
        // Test con base de datos real
    }
}
```

**3. Saga Pattern**:
Para transacciones distribuidas (ej: si inventory falla después de crear el order):
```java
// Compensating transaction
if (!inventoryReserved) {
    order.cancel();  // Rollback
    publishEvent(new OrderCancelledEvent());
}
```

**Nota**: Estas mejoras no están porque prioricé mostrar DDD, CQRS y testing sólido en el tiempo disponible.
"

---

## 📊 Métricas del Proyecto

| Aspecto | Valor |
|---------|-------|
| **Microservicios** | 4 servicios + Gateway + Discovery |
| **Lines of Code** | ~2,500 líneas (order-service) |
| **Tests** | 22 tests (100% pass rate) |
| **Cobertura de Dominio** | 21 tests unitarios puros |
| **Tecnologías** | 12+ (Spring Boot, Kafka, gRPC, OAuth2, etc.) |
| **Patrones** | DDD, CQRS, Hexagonal, Circuit Breaker, Saga (prep) |

---

## 🔄 Spring Boot vs Quarkus - Comparativa Detallada

Viniendo de **Quarkus**, aquí están las diferencias clave que debes conocer:

### Conceptos Equivalentes

| Concepto | Spring Boot | Quarkus |
|----------|-------------|---------|
| **Application Class** | `@SpringBootApplication` | `@QuarkusApplication` |
| **DI Container** | Spring IoC | CDI (ArC) |
| **Service Layer** | `@Service` | `@ApplicationScoped` |
| **REST Endpoint** | `@RestController` | `@Path` (JAX-RS) |
| **Inject Dependency** | Constructor (automático) | `@Inject` |
| **Configuration** | `application.properties` / `application.yml` | `application.properties` |
| **Health Check** | Spring Actuator `/actuator/health` | SmallRye Health `/q/health` |
| **Metrics** | Micrometer → Prometheus | MicroProfile Metrics |
| **Fault Tolerance** | Resilience4j | SmallRye Fault Tolerance |
| **Tracing** | Zipkin/Micrometer | OpenTelemetry |
| **Dev Mode** | Spring DevTools | `quarkus:dev` (hot reload) |

### Inyección de Dependencias

**Spring Boot**:
```java
@Service
@RequiredArgsConstructor  // Lombok
public class OrderService {
    private final OrderRepository repository;
    // Constructor generado automáticamente por Lombok
    // Spring inyecta sin necesidad de @Autowired
}
```

**Quarkus**:
```java
@ApplicationScoped
public class OrderService {
    @Inject
    OrderRepository repository;  // @Inject obligatorio

    // O con constructor
    @Inject
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```

### REST Endpoints

**Spring Boot**:
```java
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createOrder(@RequestBody OrderRequest request) {
        return orderService.create(request);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return orderService.findById(id);
    }
}
```

**Quarkus (JAX-RS)**:
```java
@Path("/api/orders")
@ApplicationScoped
public class OrderResource {

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrder(OrderRequest request) {
        OrderResponse response = orderService.create(request);
        return Response.status(201).entity(response).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public OrderResponse getOrder(@PathParam("id") Long id) {
        return orderService.findById(id);
    }
}
```

### Configuración de Propiedades

**Spring Boot** (`application.yml`):
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/orderdb
    username: user
    password: pass
  jpa:
    hibernate:
      ddl-auto: update

server:
  port: 8081
```

**Quarkus** (`application.properties`):
```properties
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/orderdb
quarkus.datasource.username=user
quarkus.datasource.password=pass
quarkus.hibernate-orm.database.generation=update

quarkus.http.port=8081
```

### Transacciones

**Spring Boot**:
```java
@Service
public class OrderService {

    @Transactional  // Spring Transaction Management
    public void placeOrder(Order order) {
        orderRepository.save(order);
        // Si falla aquí, rollback automático
        inventoryService.reserve(order.getItems());
    }
}
```

**Quarkus**:
```java
@ApplicationScoped
public class OrderService {

    @Transactional  // Mismo nombre, diferente implementación (JTA)
    public void placeOrder(Order order) {
        orderRepository.persist(order);
        inventoryService.reserve(order.getItems());
    }
}
```

### Repositorios / Persistencia

**Spring Boot (Spring Data JPA)**:
```java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    List<Order> findByStatus(OrderStatus status);
}
```

**Quarkus (Panache)**:
```java
@ApplicationScoped
public class OrderRepository implements PanacheRepository<Order> {
    public Optional<Order> findByOrderNumber(String orderNumber) {
        return find("orderNumber", orderNumber).firstResultOptional();
    }

    public List<Order> findByStatus(OrderStatus status) {
        return find("status", status).list();
    }
}

// O con Active Record:
@Entity
public class Order extends PanacheEntity {
    public static Optional<Order> findByOrderNumber(String orderNumber) {
        return find("orderNumber", orderNumber).firstResultOptional();
    }
}
```

### Kafka

**Spring Boot**:
```java
@Configuration
public class KafkaConfig {
    @Bean
    public KafkaTemplate<String, OrderEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}

@Service
public class OrderEventPublisher {
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public void publish(OrderEvent event) {
        kafkaTemplate.send("orders", event);
    }
}

@Component
public class OrderEventConsumer {
    @KafkaListener(topics = "orders")
    public void consume(OrderEvent event) {
        // Process event
    }
}
```

**Quarkus (SmallRye Reactive Messaging)**:
```java
@ApplicationScoped
public class OrderEventPublisher {

    @Channel("orders-out")
    Emitter<OrderEvent> emitter;

    public void publish(OrderEvent event) {
        emitter.send(event);
    }
}

@ApplicationScoped
public class OrderEventConsumer {

    @Incoming("orders-in")
    public void consume(OrderEvent event) {
        // Process event
    }
}

# application.properties
mp.messaging.outgoing.orders-out.connector=smallrye-kafka
mp.messaging.outgoing.orders-out.topic=orders
mp.messaging.incoming.orders-in.connector=smallrye-kafka
mp.messaging.incoming.orders-in.topic=orders
```

### Circuit Breaker / Fault Tolerance

**Spring Boot (Resilience4j)**:
```java
@Service
public class InventoryClient {

    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackInventory")
    @Retry(name = "inventory")
    public boolean checkStock(String sku) {
        return restTemplate.getForObject("/inventory/" + sku, Boolean.class);
    }

    private boolean fallbackInventory(String sku, Exception e) {
        return false;  // Assume out of stock
    }
}
```

**Quarkus (SmallRye Fault Tolerance)**:
```java
@ApplicationScoped
public class InventoryClient {

    @CircuitBreaker(requestVolumeThreshold = 4)
    @Retry(maxRetries = 3)
    @Fallback(fallbackMethod = "fallbackInventory")
    public boolean checkStock(String sku) {
        return restClient.checkStock(sku);
    }

    private boolean fallbackInventory(String sku) {
        return false;
    }
}
```

### Diferencias de Performance

| Métrica | Spring Boot | Quarkus JVM | Quarkus Native |
|---------|-------------|-------------|----------------|
| **Startup Time** | 3-5 segundos | 1-2 segundos | 0.05 segundos |
| **Memory (RSS)** | 200-300 MB | 150-200 MB | 30-50 MB |
| **First Response** | ~5 segundos | ~2 segundos | 0.1 segundos |
| **Build Time** | 10-20 seg | 10-20 seg | 2-5 minutos |
| **Throughput** | Alto | Alto | Muy Alto |

### ¿Cuándo usar cada uno?

**Spring Boot**:
- ✅ Ecosistema maduro (2004)
- ✅ Documentación extensa
- ✅ Integración con todo
- ✅ Equipo grande con experiencia en Spring
- ✅ Aplicaciones tradicionales (monolitos, microservicios estándar)
- ❌ Más pesado en memoria
- ❌ Startup más lento

**Quarkus**:
- ✅ Ultra-rápido (especialmente nativo)
- ✅ Bajo consumo de memoria
- ✅ Ideal para Kubernetes/serverless
- ✅ Developer experience (hot reload)
- ✅ Reactive por defecto
- ❌ Ecosistema más joven (2019)
- ❌ Menos librerías disponibles

### Migración Spring → Quarkus

**¿Qué cambia?**
1. Anotaciones: `@Service` → `@ApplicationScoped`
2. REST: `@RestController` → `@Path` (JAX-RS)
3. Inyección: Automática → `@Inject` explícito
4. JPA: Spring Data → Panache
5. Properties: YAML → Properties (o YAML con extensión)

**¿Qué NO cambia?**
1. Lógica de negocio (Java puro)
2. JPA entities (mismo estándar)
3. Transacciones (`@Transactional`)
4. Testing (JUnit 5)
5. Arquitectura (DDD, CQRS, Hexagonal)

### En una Entrevista

**Si preguntan "¿Conoces Quarkus?"**:

"Sí, vengo de trabajar con Quarkus. Las principales diferencias con Spring Boot son:

1. **DI**: Spring usa su propio container, Quarkus usa CDI estándar
2. **REST**: Spring usa `@RestController`, Quarkus usa JAX-RS `@Path`
3. **Performance**: Quarkus es más rápido y consume menos memoria
4. **Ecosistema**: Spring es más maduro, Quarkus más moderno

Pero los conceptos son los mismos: inyección de dependencias, REST APIs, JPA, transacciones. Mi código de dominio (DDD) funcionaría igual en ambos frameworks porque es Java puro sin dependencias del framework."

---

## 🎤 Frases Clave para la Entrevista

1. **Inyección de Dependencias**: "Uso constructor injection con campos final para inmutabilidad y testing fácil"
2. **DDD**: "Lógica de negocio en el dominio, no en services anémicos"
3. **CQRS**: "Separación clara entre Commands que cambian estado y Queries que leen"
4. **Hexagonal**: "El dominio no depende de infraestructura, solo de abstracciones"
5. **Testing**: "22 tests (21 del dominio sin Spring), ejecutan en milisegundos"
6. **Resiliencia**: "Circuit Breaker previene efecto dominó, Retry maneja fallos transitorios"
7. **Eventos**: "Kafka para desacoplamiento asíncrono entre servicios"
8. **Seguridad**: "OAuth2 con Keycloak, JWT propagado por el Gateway"
9. **Observabilidad**: "Zipkin con tracing distribuido para debugging"
10. **Spring vs Quarkus**: "Conceptos iguales (DI, JPA, REST), diferente sintaxis - vengo de Quarkus CDI"

---

## 🛠️ Comandos Útiles para Demo en Vivo

### Ejecutar Tests
```bash
cd order-service
mvn test

# Output:
# Tests run: 22, Failures: 0, Errors: 0 ✅
```

### Compilar con gRPC
```bash
mvn clean compile

# Genera stubs en:
# target/generated-sources/protobuf/grpc-java/
```

### Crear Orden (Postman)
```bash
POST http://localhost:8080/api/order
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json

{
  "orderLineItemsDtoList": [
    {
      "skuCode": "iphone_15",
      "price": 1200.00,
      "quantity": 2
    }
  ]
}
```

### Ver Trazas en Zipkin
```
http://localhost:9411

# Buscar: "order-service" en el dropdown
# Muestra: Order Service → Inventory Service → PostgreSQL
```

---

## 📚 Documentos de Apoyo

1. **ARQUITECTURA.md**: Diagrama completo del sistema
2. **MEJORAS-DDD-CQRS.md**: Comparativa antes/después con ejemplos
3. **HEXAGONAL-ARCHITECTURE.md**: Explicación detallada de Ports & Adapters
4. **Este archivo**: Guía de entrevista con preguntas frecuentes

---

## ✅ Checklist Pre-Entrevista

### Fundamentos Spring Boot
- [ ] **Inyección de Dependencias**: Repasar la pregunta #1 sobre constructor injection vs @Autowired
- [ ] **Beans y Scopes**: Entender singleton, prototype, request scopes
- [ ] **@Service vs @Bean**: Cuándo usar cada uno

### Arquitectura y Patrones
- [ ] **DDD**: Revisar [Order.java](order-service/src/main/java/com/programmingtechie/orderservice/domain/entity/Order.java) - Entidad rica
- [ ] **CQRS**: Entender [PlaceOrderCommandHandler.java](order-service/src/main/java/com/programmingtechie/orderservice/application/command/PlaceOrderCommandHandler.java)
- [ ] **Value Objects**: Ver [Money.java](order-service/src/main/java/com/programmingtechie/orderservice/domain/valueobject/Money.java)
- [ ] **Hexagonal Architecture**: Leer [HEXAGONAL-ARCHITECTURE.md](HEXAGONAL-ARCHITECTURE.md)

### Testing
- [ ] **Tests Unitarios**: Leer [OrderTest.java](order-service/src/test/java/com/programmingtechie/orderservice/domain/entity/OrderTest.java)
- [ ] **Ejecutar tests**: `cd order-service && mvn test` (debe pasar 22 tests)
- [ ] Entender por qué los tests del dominio NO usan Spring

### Infraestructura
- [ ] **Kafka**: Revisar [KafkaProducerConfig.java](order-service/src/main/java/com/programmingtechie/orderservice/config/KafkaProducerConfig.java)
- [ ] **Circuit Breaker**: Ver anotaciones en PlaceOrderCommandHandler
- [ ] **API Gateway**: Entender el flujo completo

### Diferencias Spring vs Quarkus
- [ ] Leer la sección completa de comparación Spring Boot vs Quarkus
- [ ] Preparar respuesta: "Vengo de Quarkus, estas son las diferencias..."

### Demo en Vivo
- [ ] Tener Docker corriendo con todos los servicios
- [ ] Preparar request de Postman para crear orden
- [ ] Saber navegar a Zipkin para mostrar trazas

---

**¡Buena suerte en tu entrevista! 🚀**

Este proyecto demuestra conocimientos sólidos en:
- ✅ **Spring Boot 4.0 + Java 21** - Última versión con features modernos
- ✅ **Inyección de Dependencias** - Constructor injection con immutability
- ✅ **Arquitectura de Microservicios** - 4 servicios + Gateway + Discovery
- ✅ **Domain-Driven Design (DDD)** - Entidades ricas, Value Objects
- ✅ **CQRS** - Commands y CommandHandlers separados
- ✅ **Arquitectura Hexagonal** - Ports & Adapters, dominio independiente
- ✅ **Resiliencia** - Circuit Breaker, Retry, Timeout con Resilience4j
- ✅ **Comunicación asíncrona** - Kafka con eventos (OrderPlacedEvent)
- ✅ **Seguridad OAuth2** - Keycloak con JWT
- ✅ **Testing efectivo** - 22 tests (21 unitarios puros sin Spring)
- ✅ **Observabilidad** - Zipkin para tracing distribuido
- ✅ **Experiencia Quarkus** - Transición CDI → Spring DI
