# AWS SDK Module Path Tests

## Description
This module is used to test using SDK on module path with Java 9+.

- Mock tests: calling xml/json prococol sync/async clients using mock http clients.
- Integ tests: calling service clients using `UrlConnectionHttpClient`, `ApacheHttpClient` and `NettyNioAsyncHttpClient`.

## How to run
```
mvn clean package
mvn exec:exec -P mock-tests
mvn exec:exec -P integ-tests
```


