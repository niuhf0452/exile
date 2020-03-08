Features
========

## Overview

* [Programmatic binding](#programmatic-binding)
* [Auto wire and class scanning](#auto-wire-and-class-scanning)
* [ServiceLoader integration](#serviceloader-integration)
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

The exile-inject supports class scanning and 


## ServiceLoader integration

## Caching and scope

## Spring style factory

## Nullable injection

## Provider injection

## Method interception

## Next

[Understand the design of Injector.](design.md)

