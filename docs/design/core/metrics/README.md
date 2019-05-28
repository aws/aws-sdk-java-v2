**Design:** New Feature, **Status:**
[In Development](../../../README.md)

# SDK Tenets (unless you know better ones)

1. Metrics can be used to provide insights about application behavior to enhance performance, debug operational issues.
2. Enabling metrics should have minimal impact on the application performance.
3. Customers can publish the collected metrics into their desired monitoring platform.
4. Metrics are divided into different categories for granular control.
5. Customers can control the metrics collection cost by having the ability to enable/disable the metrics collection by category.
6. Metrics collected by SDK are namespaced to avoid collision with other application metrics.


# Project Introduction

This project introduces a feature that can collect and report SDK metrics data in your application.
These metrics can be used to gather insights into your application and tune the application for 
the best performance. The metrics can help in debugging when the application is experiencing
high error rates.


# Project Details (WIP)

1. Metrics are disabled by default and should be enabled explicitly by customers. Enabling metrics will introduce small overhead.
2. Collected metrics are stored in customer's account. SDK cannot access the stored metrics from customer's account.


