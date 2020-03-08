Features
========

## Overview

* [Programmatic binding](#programmatic-binding)
* [Auto wire and class scanning](#auto-wire-and-class-scanning)
* [ServiceLoader integration](#serviceloader-integration)
* [Qualifiers](#qualifiers)
* [Caching and scope](#caching-and-scope)
* [Spring style factory](#spring-style-factory)
* [Nullable injection](#nullable-injection)
* [Provider injection](#provider-injection)
* [Method interception](#method-interception)

## Programmatic binding

Inspired by Guice, exile-inject provides a similar API to create bindings explicitly.

For example,

```kotlin
    val injector = Injector.builder()
            .addPackage("com.github.niuhf0452.exile.examples")
            .enableStatic { c ->
                c.bind(UserRepository::class).toInstance(FileUserRepository(),
                        qualifiers = listOf(Qualifiers.named("file")))
                c.bind(UserRepository::class).toType(TypeKey(InMemoryUserRepository::class),
                        qualifiers = listOf(Qualifiers.named("in-memory")))
            }
            .build()
```

Like Guice, the API is quite straight forward. In exile-inject, it's called static binding, because
it's lack of flexible. We can't extend the bindings into an existing code without changes. So it's recommended
to use Autowire and Factory bindings as first choice.

## Auto wire and class scanning

The exile-inject supports class scanning and the feature auto create bindings for Kotlin interfaces to their
implementation classes. This feature is called auto wire.

With auto wire, we don't need to create bindings with static API explicitly. The exile-inject defines implicit
rules to match the implementation class to the interface.

1. The injected class MUST be annotated with @Inject.
2. The injected class MUST have a public constructor.
3. The injected class MUST implement the requested interface, either directly or indirectly.
4. The injected class MUST be in class path, that means generated class won't be bound.

For example,

```kotlin
interface UserRepository {
    fun findUser(email: String): User?
}

@Serializable
data class User(val id: Long, val email: String, val name: String)

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

fun main() {
    val injector = Injector.builder()
            .addPackage("com.github.niuhf0452.exile.examples")
            .enableAutowire()
            .enableScope()
            .build()
    val fileRepository = injector.getInstance(UserRepository::class, listOf(Qualifiers.named("file")))
    val john = fileRepository.findUser("john.smith@example.com")
    println(john)
}
```

In the example above, the `Injector` creates binding from the class `FileUserRepository` to the interface
`UserRepository` implicitly, so that it can inject the instance of `UserRepository`.

Note that: `addPackage()` declares which package the `ClassScanner` should work on to find the classes.

Auto wire is quite powerful. The core benefit to use auto wire is removal of the centralized binding
configuration code, and also it's an easy to use API.

Sometimes a class implements an interface but don't want to be auto wired. We can do this by two ways:

* Remove the @Inject annotation. If a class is not annotated with @Inject, the Injector won't inject the class.
* Annotated the class with @Excludes. The @Excludes annotation is a hint to Injector that the class shouldn't
be injected for the request of certain interfaces. It's useful especially a class implements multiple interfaces.

For a interface in SPI style, it usually has multiple implementation classes. To differentiate the implementations,
we can annotated them with qualifiers. The qualifier is just a normal annotation, but itself is annotated with
@Qualifier, so that Injector will bring the annotations to bindings. It works like metadata.
A consumer who want to inject an instance of interface, can pick up the binding from all bindings by match
against the qualifiers. The coding looks like:

```kotlin
    val bindings = injector.getBindings(TypeKey(UserRepository::class))
    val binding = bindings.find { it.qualifiers.contains(Qualifiers.named("file")) }
        ?: throw IllegalStateException()
    val userRepository = binding.getInstance() as UserRepository
```

The qualifier type is extensible. See [qualifiers](#qualifiers).

## ServiceLoader integration

To integrate with libraries using ServiceLoader, the `Injector` supports the feature that inject instances
by calling to ServiceLoader.



## Caching and scope

## Spring style factory

## Nullable injection

## Provider injection

## Method interception

## Next

[Understand the design of Injector.](design.md)

