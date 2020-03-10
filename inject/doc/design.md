Architecture design
===================

The overview architecture is like this:

```
                                                         +---> Binder1 -> bindings
      bind type                                         /
app <-----------> Injector -> Filter1 -> ... -> FilterN -----> ...     -> ...
      bindings        ^                                 \
                      |                                  +---> BinderN -> bindings
                      |                                                      |
                      +--------------------- cache --------------------------+
```

The core of exile-inject is consist of several concepts:

**Binding**

A binding is a mapping from bind type to a factory.

Generally a bind type is interface or abstract class. It can be a type with parameters. To describe
a type in Kotlin, the exile-inject provides the `TypeKey`. `TypeKey` is stand for a reified type
with parameters. That means every parameter must be a reified type, a parameter placeholder is not
accepted. `TypeKey` can be compared with equal operator. So that it can be used as a key of `KType`.

A factory could be a class constructor, a provider or an instance. When injecting a bind type,
the Injector find the bindings, then construct the instance depend on the factory.

For a class constructor, the Injector create instance by calling the constructor using reflection.

For a provider, the Injector create instance by call it.

For an instance, the Injector return the instance directly.

**Binder**

A binder is responsible for create bindings. Since the Injector works in lazy mode, the binder
is call when a bind type is requested to inject for the first time. The binder must answer the
question: What are the bindings for the bind type? The Injector will cache the returned bindings
for further requests, so that for a certain type binders are called exactly once.

Binding is the most important element for fulfilling a injection request. When binding is ready,
the Injector knows how to create instance of bind type.

**Filter**

However binding is too simple to implement advanced feature, e.g. scope. So the exile-inject bring
another concept `Filter` to rescue.

A filter works as a decorator of bindings. After a binder returns a binding, the Injector will call
filter to decorate the binding and return another binding.

The "decorate" could be multiple approaches. It can be a cache, like scope. It can be proxying for
authentication. It can be metric and logging, etc.

The filter pattern is powerful for extending new feature without changing Injector.

The above three concept `Binding`, ``Binder`, `Filter` are the kernel of Injector.

**Caching**

To be performance, the Injector caches many things. Bindings are cached in a map, the key is TypeKey.
Class constructor are cached within factory method, the bindings of parameters are prepared. So when
instantiate a class, no need to find constructor and find parameters by type again, because in the
first injection they are prepared and cached. Scoped instances are cached. Even for a bind type
without any bindings, it's cached so that the following requests the type will not call binders again.

With the caching, Injector works in good speed.

**AOP**

From architecture view, the `Filter` is a good start point for AOP if we are always programming to
interface. Unfortunately, in practice, many application is not so well designed. Interface may not
exist until it's needed in tech layer. So that we have to bring in byte code level solution for AOP.
Well, the exile-inject use ByteBuddy to do that by default.
