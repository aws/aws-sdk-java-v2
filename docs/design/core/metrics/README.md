**Design:** New Feature, **Status:**
[In Development](../../../README.md)

# Project Tenets (unless you know better ones)

1. Metrics can be used to provide insights about application behavior to enhance performance and debug operational
   issues.
2. Enabling default metrics should have minimal impact on the application performance.
3. Customers can publish the collected metrics to their choice of platform.
4. Metrics are divided into different categories for granular control.
5. Customers can control the cost by having the ability to enable/disable the metrics collection by category.
6. Metrics collected by SDK are namespaced to avoid collision with other application metrics.


# Project Introduction

This project adds a feature to the AWS SDK for Java that can collect and report client side SDK metrics in your
application.  Metrics helps developers, ops engineers to detect and diagnose issues in their applications.  The metrics
can also be used to gather insights into the application over time and tune the application for optimal performance.


# Project Details

1. Metrics are disabled by default and should be enabled explicitly by customers. Enabling metrics will introduce small
   overhead.
2. Metrics can be enabled quickly during large scale events with need for code change or deployments.
3. Customers may publish metrics using their existing credentials.
4. Metrics are stored and accessed by AWS only with explicit permissions from the customer.
5. New Metrics can be added and published by the SDK into existing categories.


# Metrics Meters
Meters define the way a metric is measured. Here are the list of meters:

**Counter :** Number of times a metric is reported. These kind of metrics can be incremented or decremented.  
For example: number of requests made since the start of application

**Timer :** Records the time between start of an event and end of an event. An example is the time taken (latency) to
complete a request.

**Gauge :** A value recorded at a point in time. An example is the number of connections in the client pool.

**Constant Gauge :** There are metrics that have a static value which doesn't change after it is set. Some examples are
service name, API name, status code, request id.  To support this, a constant implementation of gauge is used

Reference: Some Meter names are taken from open source
[spectator](http://netflix.github.io/spectator/en/latest/intro/counter/) project (Apache 2.0 license).

# Naming

1. Metric names should be in CamelCase format.
2. Only Alphabets and numbers are allowed in metric names.

## Collected Metrics

The full list of metrics collected by the SDK are documented [here](MetricsList.md) along with their definitions.


# Metric Publishers

Metric Publishers are the implementations that are used to publish metrics to different platforms. 
SDK provides default publishers to publish to following platforms for convenience. 
Customers can implement custom publishers to publish metrics to platforms not supported by SDK.

## Supported platforms
1) CloudWatch

2) CSM - Client Side Monitoring (also known as [AWS SDK Metrics for Enterprise
Support](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/sdk-metrics.html))
