# SDK Metrics System
## Concepts
### Metric
* A measure of some aspect of the SDK. Examples include request latency, number
  of pooled connections and retries executed.

* A metric is associated to a category. Some of the metric categories are
  `Default`, `HttpClient` and `Streaming`. This enables customers to enable
  metrics only for categories they are interested in.

Refer to the [Metrics List](./MetricsList.md) document for a complete list of
standard metrics collected by the SDK.

### Metric Collector

* `MetricCollector` is a typesafe aggregator of of metrics. This is the primary
  interface through which other SDK components report metrics they emit, using
  the `reportMetric(SdkMetric,Object)` method.

* `MetricCollector` objects allow for nesting. This enables metrics to be
  collected in the context of other metric events. For example, for a single
  API call, there may be multiple request attempts if there are retries. Each
  attempt's associated metric events can be stored in their own
  `MetricCollector`, all of which are children of another collector that
  represents metrics for the entire API call.

  A child of a collector is created by calling its `childCollector(String)`
  method.

* The `collect()` method returns a `MetricCollection`. This class essentially
  returns an immutable version of the tree formed by the collector and its
  children, which are also represented by `MetricCollection` objects.

  Note that calling `collect()` implies that child collectors are are also
  collected.

* Each collector has a name. Often this is will be used to describe the class of
  metrics that it collects; e.g. `"ApiCall"` and `"ApiCallAttempt"`.

* [Interface prototype](prototype/MetricCollector.java)

### MetricPublisher

* A `MetricPublisher` publishes collected metrics to a system(s) outside of the
  SDK. It takes a `MetricCollection` object, potentially transforms the data
  into richer metrics, and also into a format the receiver expects.

* By default, the SDK will provide implementations to publish metrics to [Amazon
  CloudWatch](https://aws.amazon.com/cloudwatch/) and [Client Side
  Monitoring](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/sdk-metrics.html)
  (also known as AWS SDK Metrics for Enterprise Support).

* Metrics publishers are pluggable within the SDK, allowing customers to
  provide their own custom implementations.

* Metric publishers can have different behaviors in terms of list of metrics to
  publish, publishing frequency, configuration needed to publish etc.

* [Interface prototype](prototype/MetricPublisher.java)

## Enabling Metrics

The metrics feature is disabled by default. Metrics can be enabled and configured in the following ways:

### Option 1: Configuring MetricPublishers on a request

A publisher can be configured directly on the `RequestOverrideConfiguration`:

```java
MetricPublisher metricPublisher = CloudWatchMetricPublisher.create();
DynamoDbClient dynamoDb = DynamoDbClient.create();
dynamoDb.listTables(ListTablesRequest.builder()
                                     .overrideConfiguration(c -> c.addMetricPublisher(metricPublisher))
                                     .build());
```

The methods exposed for setting metric publishers follow the pattern established by `ExecutionInterceptor`s:

```java
class RequestOverrideConfiguration {
    // ...
    class Builder {
        // ...
        Builder metricPublishers(List<MetricPublisher> metricsPublishers);
        Builder addMetricPublisher(MetricPublisher metricsPublisher);
    }
}
```

### Option 2: Configuring MetricPublishers on a client

A publisher can be configured directly on the `ClientOverrideConfiguration`. A publisher specified in this way is used
with lower priority than **Option 1** above.

```java
MetricPublisher metricPublisher = CloudWatchMetricPublisher.create();
DynamoDbClient dynamoDb = DynamoDbClient.builder()
                                        .overrideConfiguration(c -> c.addMetricPublisher(metricPublisher))
                                        .build();
```

The methods exposed for setting metric publishers follow the pattern established by `ExecutionInterceptor`s:

```java
class ClientOverrideConfiguration {
    // ...
    class Builder {
        // ...
        Builder metricPublishers(List<MetricPublisher> metricsPublishers);
        Builder addMetricPublisher(MetricPublisher metricsPublisher);
    }
}
```

**Note:** As with the `httpClient` setting, calling `close()` on the `DynamoDbClient` *will not* close the configured
`metricPublishers`. You must close the `metricPublishers` yourself when you're done using them.

### Option 3: Configuring MetricPublishers using System Properties or Environment Variables

This option allows the customer to enable metric publishing by default, without needing to enable it via **Option 1** 
or **Option 2** above. This means that a customer can enable metrics without needing to make a change to their runtime 
code.

This option is enabled using an environment variable or system property. If both are specified, the system property 
will be used. If metrics are enabled at the client level using **Option 2** above, this option is ignored. Overriding 
the metric publisher at request time using **Option 1** overrides any publishers that have been enabled globally.

**System Property:** `aws.metricPublishingEnabled=true` 

**Environment Variable:** `AWS_METRIC_PUBLISHING_ENABLED=true`

The value specified must be one of `"true"` or `"false"`. Specifying any other string values will result in 
a value of `"false"` being used, and a warning being logged each time an SDK client is created.

When the value is `"false"`, no metrics will be published by a client.

When the value is `"true"`, metrics will be published by every client to a set of "global metric publishers". The set
of global metric publishers is loaded automatically using the same mechanism currently used to discover HTTP 
clients. This means that including the `cloudwatch-metric-publisher` module and enabling the system property or 
environment variable above is sufficient to enable metric publishing to CloudWatch on all AWS clients. 

The set of "Global Metric Publishers" is static and is used for *all* AWS SDK clients instantiated by the application 
(while **Option 3** remains enabled). A JVM shutdown hook will be registered to invoke `MetricPublisher.close()` on 
every publisher (in case the publishers use non-daemon threads that would otherwise block JVM shutdown).

#### Updating a MetricPublisher to work as a global metric publisher

**Option 3** above references the concept of "Global Metric Publishers", which are a set of publishers that are 
discovered automatically by the SDK. This section outlines how global metric publishers are discovered and created. 

Each `MetricPublisher` that supports loading when **Option 3** is enabled must:
1. Provide an `SdkMetricPublisherService` implementation. An `SdkMetricPublisherService` implementation is a class with 
a zero-arg constructor, used to instantiate a specific type of `MetricPublisher` (e.g. a 
`CloudWatchMetricPublisherService` that is a factory for `CloudWatchMetricPublisher`s).
2. Provide a resource file: `META-INF/services/software.amazon.awssdk.metrics.SdkMetricPublisherService`. This file 
contains the list of fully-qualified `SdkMetricPublisherService` implementation class names.

The `software.amazon.awssdk.metrics.SdkMetricPublisherService` interface that must be implemented by all global metric 
publisher candidates is defined as:

```java
public interface SdkMetricPublisherService {
    MetricPublisher createMetricPublisher();
}
```

**`SdkMetricPublisherService` Example**

Enabling the `CloudWatchMetricPublisher` as a global metric publisher can be done by implementing the 
`SdkMetricPublisherService` interface:

```java
package software.amazon.awssdk.metrics.publishers.cloudwatch;

public final class CloudWatchSdkMetricPublisherService implements SdkMetricPublisherService {
    @Override
    public MetricPublisher createMetricPublisher() {
        return CloudWatchMetricPublisher.create();
    }
}
```

And creating a `META-INF/services/software.amazon.awssdk.metrics.SdkMetricPublisherService` resource file in the 
`cloudwatch-metric-publisher` module with the following contents:

```
software.amazon.awssdk.metrics.publishers.cloudwatch.CloudWatchSdkMetricPublisherService
```

#### Option 3 Implementation Details and Edge Cases

**How the SDK loads `MetricPublisher`s when Option 3 is enabled** 

When a client is created with **Option 3** enabled (and **Option 2** "not specified"), the client retrieves the list of 
global metric publishers to use via a static "global metric publisher list" singleton. This singleton is initialized 
exactly once using the following process:
1. The singleton uses `java.util.ServiceLoader` to locate all `SdkMetricPublisherService` implementations configured
as described above. The classloader used with the service loader is chosen in the same manner as the one chosen for the 
HTTP client service loader (`software.amazon.awssdk.core.internal.http.loader.SdkServiceLoader`). That is, the first 
classloader present in the following list: (1) the classloader that loaded the SDK, (2) the current thread's classloader, 
then (3) the system classloader.
2. The singleton creates an instance of every `SdkMetricPublisherService` located in this manner.
3. The singleton creates an instance of each `MetricPublisher` instance using the metrics publisher services.

**How Option 3 and Option 1 behave when Option 2 is "not specified"** 

The SDK treats **Option 3** as the default set of client-level metric publishers to be
used when **Option 2** is "not specified". This means that if a customer: (1) enables global metric publishing using 
**Option 3**, (2) does not specify client-level publishers using **Option 2**, and (3) specifies metric publishers at 
the request level with **Option 1**, then the global metric publishers are still *instantiated* but will not be used. 
This nuance prevents the SDK from needing to consult the global metric configuration with every request.

**How Option 2 is considered "not specified" for the purposes of considering Option 3**

Global metric publishers (**Option 3**) are only considered for use when **Option 2** is "not specified". 

"Not specified" is defined to be when the customer either: (1) does not invoke 
`ClientOverrideConfiguration.Builder.addMetricPublisher()` / `ClientOverrideConfiguration.Builder.metricPublishers()`, 
or (2) invokes `ClientOverrideConfiguration.Builder.metricPublishers(null)` as the last `metricPublisher`-mutating 
action on the client override configuration builder. 

This definition purposefully excludes `ClientOverrideConfiguration.Builder.metricPublishers(emptyList())`. Setting 
the `metricPublishers` to an empty list is equivalent to setting the `metricPublishers` to the `NoOpMetricPublisher`.

**Implementing an SdkMetricPublisherService that depends on an AWS clients** 

Any `MetricPublisher`s that supports creation via a `SdkMetricPublisherService` and depends on an AWS service client 
**must** disable metric publishing on those AWS service clients using **Option 2** when they are created via the 
`SdkMetricPublisherService`. This is to prevent a scenario where the global metric publisher singleton's initialization 
process depends on the global metric publishers singleton already being initialized. 

## Modules
New modules are created to support metrics feature.

### metrics-spi
* Contains the metrics interfaces and default implementations that don't require other dependencies
* This is a sub module under `core`
* `sdk-core` has a dependency on `metrics-spi`, so customers will automatically get a dependency on this module.

### metrics-publishers
* This is a new module that contains implementations of all SDK supported publishers
* Under this module, a new sub-module is created for each publisher (`cloudwatch-publisher`, `csm-publisher`)
* Customers has to **explicitly add dependency** on these modules to use the sdk provided publishers

## Performance
One of the main tenets for metrics is “Enabling default metrics should have
minimal impact on the application performance". The following design choices are
made to ensure enabling metrics does not effect performance significantly.

* When collecting metrics, a No-op metric collector is used if metrics are
  disabled. All methods in this collector are no-op and return immediately.

* Metric publisher implementations can involve network calls and impact latency
  if done in blocking way. Therefore, all SDK publisher implementations will
  process the metrics asynchronously to not block the request thread.

* Performance tests will be written and run with each release to ensure that the
  SDK performs well even when metrics are enabled and being collected and
  published.
