Scope is the SPI for supporting scoped injection and caching.

For example, in a web app with user authentication using JWT token, for every service,
it may need to access to the user information. But we don't want to parse the JWT every time
a service query the user information, we want it parsed for the first time querying, then
cached for current request, so that in the following process, service can get the user information
from cache without parsing the JWT token again.

Code example:

```kotlin
interface UserToken {
  val name: String
  val roles: Set<String>
}

@Inject
class Service(
  private val user: UserToken
)
```

In the case above, we have multiple choices to inject the UserToken.

The first approach is create a binder for UserToken. But if we also need another request related resource,
what shall we do?

e.g. user's personal information:

```
interface UserProfile {
  val email: String
  val address: String
  val birthday: Date
}
```

Well, we can create another binder for UserProfile. But is there a simpler way instead?

The second approach is to use Scope API.

Scope API is designed from practices. As we know, almost every web request type is a container of general properties.
We can use it to store request related resources, then it works like a request scope cache.

Scope API is based on the simple idea that we take a scope like a cache container (e.g. request), than using
@ScopeQualifier we can define which class should be store in the container, than Injector gets/puts the cached
instance for you. So that as a developer, you don't need to write binders for each type of resources, instead
just need to get the binder of request prepared.
