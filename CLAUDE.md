# Backend Coding Guidelines

## Stack
- Java 17, Spring Boot 4, Lombok, WebSocket (raw `TextWebSocketHandler`), Jackson, Maven

## Package Structure
```
com.indezah.guess_it
├── configs/       # Spring config + WebSocket handler
├── controllers/   # REST endpoints only — no business logic
├── dtos/          # Data transfer objects (records preferred)
├── exceptions/    # Custom exceptions
├── models/        # In-memory domain models
└── services/      # All business logic lives here
```

## Dependency Injection
Prefer constructor injection over `@Autowired` field injection. Field injection hides dependencies and makes unit testing harder.

```java
// Prefer
@Service
@RequiredArgsConstructor
public class GameService {
    private final SocketConnectionHandler socketConnectionHandler;
}

// Avoid
@Autowired
private SocketConnectionHandler socketConnectionHandler;
```

Use `@Lazy` only when there is a documented circular-dependency reason.

## DTOs
All DTOs must be Java **records** — they are immutable by default and need no Lombok.

```java
// Correct
public record CreateRoomDTO(String roomCode, String playerCode) {}

// Avoid @Data classes for DTOs — they are mutable
```

## Models
Domain models that are mutated during a game session use `@Data`. Keep them inside the `models` package and never expose them directly through REST or WebSocket responses — always map to a DTO first.

## Lombok Usage
| Annotation | Use for |
|---|---|
| `@Data` | Mutable domain models |
| `@Value` | Immutable classes that aren't records |
| `@RequiredArgsConstructor` | Services and components (pairs with final fields) |
| `@Builder` | Complex object construction |

Never use `@Data` on DTOs. Never use `@SneakyThrows` — declare or handle exceptions explicitly.

## Exception Handling
Throw `GameException` (unchecked) for all expected game-rule violations. Add a `@ControllerAdvice` / `@RestControllerAdvice` that maps `GameException` to a consistent error response shape — do not let controllers catch it individually.

```java
// Service throws
throw new GameException("Room not found: " + roomCode);

// ControllerAdvice handles uniformly
@ExceptionHandler(GameException.class)
public ResponseEntity<ErrorResponse> handleGameException(GameException ex) { ... }
```

## WebSocket Action Routing
Action strings (`"SET_SECRET"`, `"GUESS"`) must be defined as constants or an enum, not scattered as raw string literals.

```java
public enum WsAction {
    SET_SECRET, GUESS
}
```

`SocketConnectionHandler` should route to dedicated handler methods, not grow via if/else chains in `handleMessage`.

## Thread Safety
- `roomStore` in `GameService` uses `ConcurrentHashMap` — keep it that way.
- `webSocketSessions` uses `Collections.synchronizedList` — always synchronize on the list when iterating.
- Game state mutations in `GameService` are not yet synchronized per room. Add per-room locking if race conditions are observed.

## Controllers
Controllers route HTTP requests to services and return responses. No logic belongs here.

```java
// Correct
@PostMapping("/room")
public ResponseEntity<CreateRoomDTO> createRoom() {
    return ResponseEntity.ok(gameService.create());
}

// Avoid: wildcard generic response type
public ResponseEntity<?> createRoom()
```

Always use typed `ResponseEntity<T>`, not `ResponseEntity<?>`.

## Validation
Use `@Valid` + Jakarta Bean Validation annotations on request bodies. Add a `@NotBlank`, `@Pattern`, or `@Size` constraint on the DTO field rather than hand-rolling validation inside the service.

```java
public record JoinRoomDTO(@NotBlank @Size(min = 6, max = 6) String roomCode) {}
```

## Naming Conventions
| Element | Convention | Example |
|---|---|---|
| Classes | PascalCase | `GameService` |
| Methods | camelCase | `setSecretNumber` |
| Constants | UPPER_SNAKE_CASE | `RATE_LIMIT_MS` |
| Packages | lowercase | `com.indezah.guess_it` |

## Testing
- Unit-test `GameService` in isolation with mocked collaborators.
- Integration-test `GameController` with `@WebMvcTest`.
- Do not test `SocketConnectionHandler` via unit tests — test via a real WebSocket client in an integration test.
- Test file mirrors the production path: `GameServiceTest` lives in `src/test/.../services/`.

## General
- Avoid wildcard imports (`import java.util.*`). Import explicitly.
- Remove dead code rather than commenting it out.
- No `System.out.println` — use SLF4J: `private static final Logger log = LoggerFactory.getLogger(Foo.class);` or `@Slf4j` (Lombok).
- `application.properties` must not contain secrets in committed code. Use environment variables or Spring profiles.
