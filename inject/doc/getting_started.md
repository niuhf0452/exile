Getting Started
===============

### Add exile-inject to your project

For maven:

```xml
<repositories>
    <repository>
        <id>sonatype-oss-snapshot</id>
        <name>Sonatype OSS snapshot</name>
        <urlhttps://oss.sonatype.org/content/repositories/snapshots</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.niuhf0452</groupId>
        <artifactId>exile-inject</artifactId>
        <version>0.1-SNAPSHOT</version>
    </dependency>
</dependencies>

```

For gradle (kotlin-dsl):

```kotlin
repositories {
    maven {
      url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
}

dependencies {
    implementation("com.github.niuhf0452:exile-inject:0.1-SNAPSHOT")
}
```

### Design your application

**Build an Injector**

```kotlin
    val injector = Injector.builder()
            .addPackage("com.github.niuhf0452.exile.examples")
            .enableAutowire()
            .enableScope()
            .build()
```

**Add interfaces**

```kotlin
interface UserRepository {
    fun findUser(email: String): User?
}

@Serializable
data class User(val id: Long, val email: String, val name: String)
```

**Implement interfaces**

```kotlin
@Inject
@Named("in-memory")
class InMemoryUserRepository : UserRepository {
    private val users = listOf<User>(
            User(101L, "kristin.graham@example.com", "Kristin Graham"),
            User(102L, "tanya.deckow@example.com", "Tanya Deckow")
    )

    override fun findUser(email: String): User? {
        return users.find { it.email == email }
    }
}
```

**Injector the interface**

```kotlin
    val userRepository = injector.getInstance(UserRepository::class)
    val john = userRepository.findUser("john.smith@example.com")
```

**Add another implementation**

```kotlin
@Inject
@Singleton
@Named("file")
class FileUserRepository : UserRepository {
    private val users by lazy {
        val jsonString = javaClass.getResourceAsStream("/users.json").use { input ->
            input.readAllBytes().toString(Charsets.UTF_8)
        }
        val json = Json(JsonConfiguration.Stable)
        json.parse(UserList.serializer(), jsonString).users
    }

    override fun findUser(email: String): User? {
        return users.find { it.email == email }
    }

    @Serializable
    data class UserList(val users: List<User>)
}
```

**Work with multiple implementations**

```kotlin
    val fileRepository = injector.getInstance(UserRepository::class, listOf(Qualifiers.named("file")))
    val memRepository = injector.getInstance(UserRepository::class, listOf(Qualifiers.named("in-memory")))
    val john = fileRepository.findUser("john.smith@example.com")
    val susan = memRepository.findUser("susan.zhang@example.com")
```

**Iterate implementations**

```kotlin
    val bindings = injector.getBindings(TypeKey(UserRepository::class))
    bindings.forEach { binding ->
        val repository = binding.getInstance() as UserRepository
        val user = repository.findUser("tanya.deckow@example.com")
        println(user)
    }
```

**Next**

[See all features.](features.md)