# 🔧 Inyección de Dependencias en Spring Boot - Guía Rápida

## Para desarrolladores que vienen de Quarkus

### TL;DR - Lo Más Importante

```java
// ✅ MEJOR PRÁCTICA - Constructor Injection con Lombok
@Service
@RequiredArgsConstructor  // Lombok genera el constructor
public class PlaceOrderCommandHandler {
    private final OrderRepository orderRepository;  // final = inmutable
    private final KafkaTemplate kafkaTemplate;

    // Spring inyecta automáticamente
    // NO necesitas @Autowired
    // Fácil de testear
}

// ❌ EVITAR - Field Injection
@Service
public class PlaceOrderCommandHandler {
    @Autowired
    private OrderRepository orderRepository;  // No es final
    @Autowired
    private KafkaTemplate kafkaTemplate;

    // Difícil de testear
    // Puede ser null si olvidas @Autowired
}
```

---

## ¿Qué es un Bean en Spring?

Un **bean** es un objeto gestionado por el **contenedor IoC** (Inversion of Control) de Spring.

```java
// Spring crea y gestiona estos objetos automáticamente
@Service        // Bean de servicio
@Repository     // Bean de persistencia
@Controller     // Bean de controlador
@Component      // Bean genérico
@Configuration  // Bean de configuración
```

### Ciclo de Vida de un Bean

```
1. Spring escanea @ComponentScan
2. Encuentra @Service, @Repository, etc.
3. Crea instancia (por defecto: singleton)
4. Inyecta dependencias
5. Llama @PostConstruct (si existe)
6. Bean listo para usar
7. Al apagar: llama @PreDestroy
```

---

## Formas de Inyección

### 1️⃣ Constructor Injection (✅ RECOMENDADO)

```java
@Service
public class OrderService {
    private final OrderRepository repository;
    private final EventPublisher publisher;

    // Constructor - Spring inyecta automáticamente
    public OrderService(OrderRepository repository, EventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }
}
```

**Con Lombok** (aún mejor):
```java
@Service
@RequiredArgsConstructor  // Genera constructor para campos final
public class OrderService {
    private final OrderRepository repository;
    private final EventPublisher publisher;

    // Constructor generado automáticamente por Lombok
}
```

**Ventajas**:
- ✅ Campos `final` (inmutabilidad garantizada)
- ✅ Dependencias explícitas (se ven en el constructor)
- ✅ Fácil testing (pasas mocks en el constructor)
- ✅ Falla rápido si falta una dependencia
- ✅ No necesitas `@Autowired` (desde Spring 4.3)

**Testing**:
```java
@Test
void testOrderService() {
    OrderRepository mockRepo = mock(OrderRepository.class);
    EventPublisher mockPublisher = mock(EventPublisher.class);

    // ✅ Simple y directo
    OrderService service = new OrderService(mockRepo, mockPublisher);

    service.createOrder(order);
    verify(mockRepo).save(any(Order.class));
}
```

### 2️⃣ Field Injection (❌ NO RECOMENDADO)

```java
@Service
public class OrderService {
    @Autowired
    private OrderRepository repository;  // No puede ser final

    @Autowired
    private EventPublisher publisher;
}
```

**Problemas**:
- ❌ No puedes usar `final` (mutabilidad)
- ❌ Dependencias ocultas (no se ven claramente)
- ❌ Difícil de testear (necesitas reflection)
- ❌ Puede ser null si olvidas `@Autowired`

**Testing** (complicado):
```java
@Test
void testOrderService() {
    OrderService service = new OrderService();
    // ❌ repository y publisher son null!

    // Necesitas reflection o Spring TestContext
    ReflectionTestUtils.setField(service, "repository", mockRepo);
    ReflectionTestUtils.setField(service, "publisher", mockPublisher);
}
```

### 3️⃣ Setter Injection (⚠️ Casos especiales)

```java
@Service
public class OrderService {
    private OrderRepository repository;

    @Autowired
    public void setRepository(OrderRepository repository) {
        this.repository = repository;
    }
}
```

**Cuándo usar**:
- Dependencias opcionales
- Re-configuración en runtime (muy raro)

**En general**: Usa constructor injection.

---

## Ejemplo Real del Proyecto

### PlaceOrderCommandHandler

```java
@Service
@RequiredArgsConstructor  // Lombok
@Slf4j
public class PlaceOrderCommandHandler {

    // ✅ Todos final = inmutables
    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    // Lombok genera este constructor:
    // public PlaceOrderCommandHandler(
    //     OrderRepository orderRepository,
    //     WebClient.Builder webClientBuilder,
    //     KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate
    // ) {
    //     this.orderRepository = orderRepository;
    //     this.webClientBuilder = webClientBuilder;
    //     this.kafkaTemplate = kafkaTemplate;
    // }

    @Transactional
    public String handle(PlaceOrderCommand command) {
        // Usa las dependencias inyectadas
        Order order = Order.create(lineItems);
        orderRepository.save(order);  // inyectado
        kafkaTemplate.send("order-placed", event);  // inyectado
    }
}
```

---

## @Bean vs @Component/@Service

### @Component / @Service / @Repository

**Uso**: Para **tus propias clases**

```java
@Service  // Spring detecta y registra automáticamente
public class OrderService {
    // Tu código
}

@Repository  // Especialización de @Component para DAOs
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Spring Data crea la implementación
}
```

**Cómo funciona**:
1. Spring escanea el paquete base
2. Encuentra la anotación `@Service`
3. Crea una instancia automáticamente
4. La registra como bean

### @Bean

**Uso**: Para **clases de terceros** (librerías externas)

```java
@Configuration
public class KafkaConfig {

    @Bean  // TÚ controlas cómo se crea
    public KafkaTemplate<String, OrderEvent> kafkaTemplate() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        ProducerFactory<String, OrderEvent> factory =
            new DefaultKafkaProducerFactory<>(config);

        return new KafkaTemplate<>(factory);
    }
}
```

**Cuándo usar cada uno**:

| Caso | Usar |
|------|------|
| Tu clase `OrderService` | `@Service` |
| Tu clase `OrderRepository` | `@Repository` |
| Clase de librería `KafkaTemplate` | `@Bean` |
| Clase de librería `RestTemplate` | `@Bean` |
| Clase de librería `ObjectMapper` | `@Bean` |

---

## Scopes de Beans

```java
// Singleton (por defecto) - UNA instancia para toda la app
@Service  // Implícitamente @Scope("singleton")
public class OrderService {
    // La misma instancia se inyecta siempre
}

// Request - Nueva instancia por cada HTTP request
@Controller
@Scope("request")
public class OrderController {
    // Útil si necesitas estado por request
}

// Prototype - Nueva instancia cada vez que se inyecta
@Component
@Scope("prototype")
public class OrderBuilder {
    // Cada vez que se inyecta, se crea una nueva
}
```

**En la práctica**:
- 99% de las veces: Singleton (por defecto)
- 0.9% de las veces: Request (para estado de request)
- 0.1% de las veces: Prototype (muy raro)

---

## Spring Boot vs Quarkus - Inyección de Dependencias

### Equivalencias

| Spring Boot | Quarkus (CDI) |
|-------------|---------------|
| `@Service` | `@ApplicationScoped` |
| `@Component` | `@Dependent` |
| `@Scope("request")` | `@RequestScoped` |
| `@Scope("singleton")` | `@ApplicationScoped` |
| Constructor (sin anotación) | Constructor con `@Inject` |
| `@Autowired` (field) | `@Inject` (field) |
| `@Bean` | `@Produces` |

### Ejemplo Comparativo

**Quarkus (CDI)**:
```java
@ApplicationScoped
public class OrderService {

    @Inject  // ⚠️ @Inject es obligatorio
    OrderRepository repository;

    // O con constructor
    @Inject
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}
```

**Spring Boot**:
```java
@Service
public class OrderService {

    private final OrderRepository repository;

    // ✅ @Autowired NO es necesario (automático)
    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }
}

// Con Lombok (aún mejor)
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository repository;
    // Constructor generado automáticamente
}
```

### Diferencias Clave

1. **Spring**: Constructor injection automático (no necesitas anotación)
2. **Quarkus**: `@Inject` es explícito

3. **Spring**: Usa Lombok para reducir boilerplate
4. **Quarkus**: No necesita Lombok (CDI es más conciso)

5. **Spring**: Más flexible, ecosistema más grande
6. **Quarkus**: Más rápido, menos memoria

---

## Resolución de Dependencias

### Por Tipo (mayoría de casos)

```java
@Service
public class OrderService {
    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        // Spring busca un bean de tipo OrderRepository
        this.repository = repository;
    }
}
```

### Por Nombre (múltiples beans del mismo tipo)

```java
// Dos implementaciones de la misma interfaz
@Component("restClient")
class RestInventoryClient implements InventoryClient { }

@Component("grpcClient")
class GrpcInventoryClient implements InventoryClient { }

// Especificar cuál inyectar
@Service
public class OrderService {
    private final InventoryClient client;

    public OrderService(@Qualifier("grpcClient") InventoryClient client) {
        this.client = client;  // Inyecta GrpcInventoryClient
    }
}
```

### Con @Primary (bean por defecto)

```java
@Component
@Primary  // Este se inyecta si no especificas
class RestInventoryClient implements InventoryClient { }

@Component
class GrpcInventoryClient implements InventoryClient { }

@Service
public class OrderService {
    private final InventoryClient client;

    public OrderService(InventoryClient client) {
        // Inyecta RestInventoryClient (porque es @Primary)
        this.client = client;
    }
}
```

---

## Preguntas Frecuentes de Entrevista

### P: "¿Por qué no usas @Autowired?"

**R**: "Uso constructor injection sin @Autowired porque:
1. Es más testeable (puedo crear instancias sin Spring)
2. Permite campos `final` para inmutabilidad
3. Hace las dependencias explícitas
4. Spring 4.3+ lo soporta sin anotación
5. Lombok con `@RequiredArgsConstructor` hace el código aún más limpio"

### P: "¿Qué diferencia hay entre @Service y @Component?"

**R**: "Semánticamente, `@Service` indica lógica de negocio y `@Component` es genérico. Funcionalmente son iguales - ambos registran un bean. También existe `@Repository` (capa de datos) y `@Controller` (capa web). Uso cada uno para claridad y porque frameworks como Spring Data JPA tratan `@Repository` especialmente (traduciendo excepciones de persistencia)."

### P: "¿Cuándo usas @Bean en lugar de @Service?"

**R**: "Uso `@Bean` cuando configuro clases de terceros que no puedo anotar. Por ejemplo:
- `KafkaTemplate` de Spring Kafka
- `RestTemplate` o `WebClient`
- `ObjectMapper` de Jackson

Uso `@Service` para mis propias clases de lógica de negocio."

### P: "¿Cómo funciona la inyección circular?"

**R**: "Spring puede resolver inyección circular con setter injection o `@Lazy`, pero es un code smell. Si tengo A → B → A, probablemente necesito refactorizar:
- Extraer lógica común a una clase C
- Usar eventos para desacoplar
- Revisar si viola el Dependency Inversion Principle

En general, evito la inyección circular mediante buen diseño."

---

## Resumen - Cheatsheet

```java
// ✅ USAR ESTO
@Service
@RequiredArgsConstructor  // Lombok
public class MyService {
    private final DependencyA depA;
    private final DependencyB depB;
}

// ❌ NO USAR ESTO
@Service
public class MyService {
    @Autowired
    private DependencyA depA;
    @Autowired
    private DependencyB depB;
}

// 🎯 CONFIGURACIÓN DE TERCEROS
@Configuration
public class MyConfig {
    @Bean
    public SomeExternalClass externalBean() {
        return new SomeExternalClass(config);
    }
}

// 🔍 MÚLTIPLES BEANS DEL MISMO TIPO
@Service
public class MyService {
    public MyService(@Qualifier("specificBean") MyInterface bean) {
        // ...
    }
}
```

---

**Regla de oro**: Constructor injection + `final` + `@RequiredArgsConstructor` = Código limpio, testeable e inmutable ✅
