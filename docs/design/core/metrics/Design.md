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

Metrics feature is disabled by default. Metrics can be enabled at client level in the following ways.

### Feature Flags (Metrics Provider)

* SDK exposes an [interface](prototype/MetricConfigurationProvider.java) to enable the metrics feature and specify
  options to configure the metrics behavior.
* SDK provides an implementation of this interface based on system properties.
* Here are the system properties SDK supports:
  - **aws.javasdk2x.metrics.enabled** - Metrics feature is enabled if this system property is set
  - **aws.javasdk2x.metrics.category** - Comma separated set of MetricCategory that are enabled for collection
* SDK calls the methods in this interface for each request ie, enabled() method is called for every request to determine
  if the metrics feature is enabled or not (similarly for other configuration options).
  -  This allows customers to control metrics behavior in a more flexible manner; for example using an external database
     like DynamoDB to dynamically control metrics collection. This is useful to enable/disable metrics feature and
     control metrics options at runtime without the need to make code changes or re-deploy the application.
* As the interface methods are called for each request, it is recommended for the implementations to run expensive tasks
  asynchronously in the background, cache the results and periodically refresh the results.

```java
ClientOverrideConfiguration config = ClientOverrideConfiguration
    .builder()
    // If this is not set, SDK uses the default chain with system property
    .metricConfigurationProvider(new SystemSettingsMetricConfigurationProvider())
    .build();

// Set the ClientOverrideConfiguration instance on the client builder
CodePipelineAsyncClient asyncClient =
    CodePipelineAsyncClient
        .builder()
        .overrideConfiguration(config)
        .build();
```

### Metrics Provider Chain

* Customers might want to have different ways of enabling the metrics feature. For example: use SystemProperties by
  default. If not use implementation based on Amazon DynamoDB.
* To support multiple providers, SDK allows setting chain of providers (similar to the CredentialsProviderChain to
  resolve credentials). As provider has multiple configuration options, a single provider is resolved at chain
  construction time and it is used throughout the lifecycle of the application to keep the behavior intuitive.
* If no custom chain is provided, SDK will use a default chain while looks for the System properties defined in above
  section.  SDK can add more providers in the default chain in the future without breaking customers.

```java
MetricConfigurationProvider chain = new MetricConfigurationProviderChain(
    new SystemSettingsMetricConfigurationProvider(),
    // example custom implementation (not provided by the SDK)
    DynamoDBMetricConfigurationProvider.builder()
                          .tableName(TABLE_NAME)
                          .enabledKey(ENABLE_KEY_NAME)
                          ...
                          .build(),
   );

ClientOverrideConfiguration config = ClientOverrideConfiguration
    .builder()
    // If this is not set, SDK uses the default chain with system property
    .metricConfigurationProvider(chain)
    .build();

// Set the ClientOverrideConfiguration instance on the client builder
CodePipelineAsyncClient asyncClient =
    CodePipelineAsyncClient
        .builder()
        .overrideConfiguration(config)
        .build();
```

### Metric Publishers Configuration

* If metrics are enabled, SDK by default uses a single publisher that uploads metrics to CloudWatch using default
  credentials and region.
* Customers might want to use different configuration for the CloudWatch publisher or even use a different publisher to
  publish to a different source.  To provide this flexibility, SDK exposes an option to set
  [MetricPublisherConfiguration](prototype/MetricPublisherConfiguration.java) which can be used to configure custom
  publishers.
* SDK publishes the collected metrics to each of the configured publishers in the MetricPublisherConfiguration.

```java
ClientOverrideConfiguration config = ClientOverrideConfiguration
    .builder()
    .metricPublisherConfiguration(MetricPublisherConfiguration
                                      .builder()
                                      .addPublisher(
                                          CloudWatchPublisher.builder()
                                                             .credentialsProvider(...)
                                                             .region(Region.AP_SOUTH_1)
                                                             .publishFrequency(5, TimeUnit.MINUTES)
                                                             .build(),
                                          CsmPublisher.create()).bu
                                      .build())
    .build();

// Set the ClientOverrideConfiguration instance on the client builder
CodePipelineAsyncClient asyncClient =
    CodePipelineAsyncClient
        .builder()
        .overrideConfiguration(config)
        .build();
```


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
