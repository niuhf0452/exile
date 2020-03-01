# A full featured Dependency Injection library

[![Sonarcloud Status](https://sonarcloud.io/api/project_badges/measure?project=io.github.niuhf0452.exile&metric=alert_status)](https://sonarcloud.io/dashboard?id=io.github.niuhf0452.exile)
[![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=io.github.niuhf0452.exile&metric=coverage)](https://sonarcloud.io/component_measures/metric/coverage/list?id=io.github.niuhf0452.exile)
[![SonarCloud Bugs](https://sonarcloud.io/api/project_badges/measure?project=io.github.niuhf0452.exile&metric=bugs)](https://sonarcloud.io/component_measures/metric/reliability_rating/list?id=io.github.niuhf0452.exile)
[![SonarCloud Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=io.github.niuhf0452.exile&metric=vulnerabilities)](https://sonarcloud.io/component_measures/metric/security_rating/list?id=io.github.niuhf0452.exile)

## A brief introduction

The exile-inject is a full featured Dependency Injection library, designed for enterprise software, written in Kotlin.

### What is DI?

As we know, there are a lot of IoC and DI implementations currently. The popular ones are Spring, Guice, Dagger, etc.

But what is DI really stand for?

As an ERP software developer, from my opinion, DI should take 3 responsibilities:

1. Integrate an application by providing an interface based service locating mechanism.

2. Decouple classes by providing a unified instance creation method instead of 'new' keyword which directly reference
   to implementation class.

3. Extend technical features by providing general SPI patterns.

Are these important?

Yes, but not always. I think it depends on what kind of application we are working on.

For developing a simple personal usage desktop software, I think it doesn't need a DI system.
But for a enterprise-grade software, yes, it should use DI to manage the complexity of code structure.

### Why another DI?

**For the existing DI solutions, most of them can only take part of the 3 responsibilities.**

Let's talk about Guice a bit deeper to show this problem.

Guice is a great DI library powered by Google. It provides a simple and clear API, and easy to use.
However, it doesn't support auto wire, a.k.a it doesn't find the implement class for interface automatically.
So that when we use it in a large application, we have to write code to prepare bindings for every interface.
It means that at the topmost layer there is a piece of centralized integration code for gluing up other part
of services. Well, at most time, it doesn't matter. But if we break down the services into different libraries,
and we want to reuse the libraries, how we integrate them in the topmost application? Maybe you already know
Spring Boot Auto-configuration. It is exactly the answer to our question.

The truth is auth-configuration, auto wire, auto binding, or zero configure, no matter what you call it,
it's really important for a large application.

**How about Spring Boot?**

Spring Boot goes further than Guice, it's what all enterprise software need. And the best part is Spring
has a clean code.

However, it's so complex, most of people may not deeply understand how it works.

So that I want to write a DI implementation by myself to show how to construct the core of DI with simple code.
Go further, I want to make it easy to use library, not tight to a large framework.

It's always good if we have an alternative choice, right?

### Is it a demo?

Yes, it's a demo, because I haven't use it in a real application. But I design it suitable for an enterprise software.
Well, in my previous company, I already use the DI framework I designed in production for years. Although this project
is a new open source project from scratch, I put all my experience and effort to make it better than ever.
I very appreciate to anyone trying it.

### Does it work with Java?

It works with Java, but it's targeting to Kotlin. It doesn't support Java optimized API, so some API written in Kotlin
might be difficult to use in Java. I recommend always use the library with Kotlin.

There's no plan to support Java API, because the type system of Java is much different from that of Kotlin. I don't
want to put more effort on Java.

## Getting Started

Please read the document [here][doc/getting_started.md].
