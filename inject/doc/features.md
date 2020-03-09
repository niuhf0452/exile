Features
========

## Overview

* [Programmatic binding](#programmatic-binding)
* [Auto wire and class scanning](#auto-wire-and-class-scanning)
* [Parameter injection](#parameter-injection)
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

## Parameter injection

When injecting using auto wire, the implementation class will be instantiated. If its constructor
has parameters, the parameter is automatically injected by Injector. The type of parameter is the
request type to be injected, and qualifiers on the parameter will be used for selecting binding.
If no binding match the qualifiers or multiple bindings are matched, an exception will be thrown.

## ServiceLoader integration

To integrate with libraries using ServiceLoader, the `Injector` supports the feature that inject instances
by calling to ServiceLoader.

```kotlin
@Named("jdbc")
class JdbcUserRepository : UserRepository {
    override fun findUser(email: String): User? {
        ...
    }
}
```

```
# META-INF/services/com.github.niuhf0452.exile.examples.UserRepository

com.github.niuhf0452.exile.examples.JdbcUserRepository
```

```kotlin
    val injector = Injector.builder()
            .addPackage("com.github.niuhf0452.exile.examples")
            .enableServiceLoader()
            .build()
    val userRepository = injector.getInstance(UserRepository::class,
            qualifiers = listOf(Qualifiers.named("jdbc")))
    val john = userRepository.findUser("john.smith@example.com")
```

## Caching and scope

Like many DI libraries, the exile-inject supports scope. In exile-inject, a scope is a container
used to caching instances. For example, `@Singleton` is a global container. Any class annotated
with `@Singleton`, it will be instantiated at most once, then the instance will be cached.

To use a scoped caching, just annotate the class with scope qualifier.

```kotlin
    abstract class Car

    @Inject
    @Singleton
    class TheUniqueCar : Car()

    val injector = Injector.builder()
            .addPackage(Car::class.java.packageName)
            .enableAutowire()
            .enableScope()
            .build()
    val a = injector.getInstance(Car::class)
    val b = injector.getInstance(Car::class)
    println(a == b)  // true
```

The exile-inject follows the lazy initializing principle, that means the class will not be
instantiated until it's injected. Even for `@Singleton` or other scoped instances, they will
not be created if no one requests them.

`@Singleton` is the only scope the exile-inject provides. But we can extend new scopes as needed.
See `Scope` interface for more information.

## Spring style factory

The exile-inject supports Spring style factory method. Annotate `@Factory` on the class and
the factory method. The qualifiers annotated on the factory method will be collected as the
qualifiers of binding.

```kotlin
    @Factory
    class SpringStyleConfiguration {
        @Factory
        @Named("factory-injected")
        fun createUserRepository(): UserRepository {
            return FileUserRepository()
        }
    }

    val injector = Injector.builder()
            .addPackage(SpringStyleConfiguration::class.java.packageName)
            .enableAutowire()
            .build()
    val userRepository = injector.getInstance(UserRepository::class,
            qualifiers = listOf(Qualifiers.named("factory-injected")))
```

## Nullable injection

When injecting a instance of class, if there is no binding for the parameters, an exception will be
thrown. But if the parameter is nullable, then a null value will be injected as parameter value.

```kotlin
    @Inject
    class Test(val value: String?)

    val injector = Injector.builder()
            .addPackage(Test::class.java.packageName)
            .build()
    val obj = injector.getInstance(Test::class)
    println(obj.value)
```

## Provider injection

Sometimes we need a provider of the injected type as a lazy dependency.

```kotlin
    @Inject
    class Test

    @Inject
    class Test2(
            @Named("in-memory")
            val provider: Provider<UserRepository>
    )

    val injector = Injector.builder()
            .addPackage(Test::class.java.packageName)
            .enableAutowire()
            .build()
    val provider = injector.getProvider(Test::class)
    println(provider.get())

    val test2 = injector.getInstance(Test2::class)
    println(test2.provider.get())
```

## Method interception

This feature is used to implement AOP programming.

```kotlin
    class LoggingInterceptor : ClassInterceptor {
        override fun intercept(method: KFunction<*>): MethodInterceptor<*, *>? {
            if (method.name != "findUser") {
                return null
            }
            return object : MethodInterceptor<Any, Unit> {
                override fun beforeCall(instance: Any, args: List<Any?>) {
                    println("before call")
                }

                override fun afterCall(instance: Any, args: List<Any?>, exception: Exception?, returnValue: Any?, state: Unit) {
                    println("after call")
                }
            }
        }
    }

    val injector = Injector.builder()
            .addPackage(UserRepository::class.java.packageName)
            .enableAutowire()
            .addInterceptor(LoggingInterceptor())
            .enhancer(ByteBuddyEnhancer())
            .build()
    val userRepository = injector.getInstance(UserRepository::class,
            qualifiers = listOf(Qualifiers.named("file")))
    userRepository.findUser("chris@example.com")
```

By default, the exile-inject uses ByteBuddy to intercept the methods. But it's not the only choice.
An new approach can be extended easily using the `ClassEnhancer` interface.

## Next

[Understand the design of Injector.](design.md)

