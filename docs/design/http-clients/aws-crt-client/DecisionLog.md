# Decision Log for AWS CRT HTTP Client

Note: The decision log process was implemented late in this project, so decisions earlier than 8/24/20 are not included 
below.

## Log Entry Template

**Source:** (Meeting/aside/pair programming discussion/daily standup) to (discuss/implement) X

**Attendees:** Anna-Karin, Ben, Dongie, Irene, Matt, Nico, Vinod, John, Zoe

**Closed Decisions:**

1. Question? Decision. Justification.

**Open Decisions:**

1. (Old/Reopened/New) Question?

## 8/24/20

**Source:** Meeting to review the API surface-area of [AWS CRT HTTP Client](https://github.com/aws/aws-sdk-java-v2/tree/aws-crt-dev-preview/http-clients)

**Attendees:** Anna-Karin, Ben, Dongie, John, Matt, Nico, Vinod, Zoe

**Closed Decisions:**

1. We should add the static factory `create` methods in all HTTP Clients. It's convenient and consistent.
2. We should make the configuration classes we expose follow our SDK conventions to be consistent with what we do elsewhere
3. We should consider renaming `initialWindowSize` because it can be confused with HTTP/2 initial window size. Possible option:
`readBufferSize`
4. We should consider renaming `httpMonitoringOptions` because it does more than monitoring. Possible option: `connectionHealthConfig` 
5. We should add the service loader class. It's consistent with the way other HTTP clients work and it'll be backwards incompatible to add one later
6. We should test and support cancelling the HTTP request futures. It's supported in the Netty Http Client
7. Follow up on the features that are yet to be supported (post-preview)
   1. Proxy TLS trust store configuration & TLS mutual auth
   2. HTTP/2 support
   3. Exposing connection pooling metrics
   
**Open Decisions:**

None