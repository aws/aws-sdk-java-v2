# Decision Log for AWS SDK for Java v2 EMFMetricPublisher


## 11/19/2024

**Source:** Team design doc review meeting for the issue that CloudWatchMetricPublisher doesn't work well for lambda functions

**Attendees:** Bole, David, Debora, Olivier, Dongie, Zoe

**Closed Decisions:**

1. Should we support a cw client in it? No, just writing to the logs is fine, if a cw client is needed then CloudWatchMetricPublisher is sufficient.
2. Should we create a logger or take an instance of a logger? No, We should avoid taking a logger from the customer to prevent misconfiguration for now. We should also explore possible use cases and try it. Also, passing in the level of the logger might be a good idea. Logger.info is set as default logger level.
3. Do we use jackson(easier but slower) (two way door) in the convert method? Yes, We can use third party Jackson-core in the convert method, because it is a two way door and we can try to implement different method and do performance test afterwards.
4. Should we use SDK's LoggingMetricPublisher as alternative? No, the dealbreaker is that it is under core/metric-spi, we should separate the module from it.
5. Should we use MetricLogger as alternative? No, it depends on jackson-databind, which is prone to CVEs.

**Open Decisions:**

None