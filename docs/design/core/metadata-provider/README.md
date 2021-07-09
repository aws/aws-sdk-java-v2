**Design:** New Feature

The AWS Java SDK V2 currently generates all of its service, region and partition metadata at build time and is immutable at runtime.
Consumers are free to manually override service endpoints on individual clients, but it is desirable for the SDK itself to have first-class
support for hotswap of metadata at runtime. A simple example is for applications to automatically support new AWS regions for
existing services simply br providing updated metadata.

## Proposed solution
The SDK provides mechanisms for controlling both global and per-client behavior for other tunable options. This design is focused on
solely a global configuration mechanism. While it may be interesting to support per-client configuration of SDK endpoints metadata,
there doesn't seem to be a compelling reason to support this functionality.

### New service interface:
The SDK provides a new interface for customers to register their own `MetadataLoaderProvider`. Its definition is as follows:
```java
@SdkPublicApi
public interface MetadataLoaderProvider {
    PartitionMetadataProvider partitionMetadataProvider();
    RegionMetadataProvider    regionMetadataProvider();
    ServiceMetadataProvider   serviceMetadataProvider();
}
```
The SDK provides a default implementation, which simply returns a generated view of the endpoints metadata at SDK build-time.

```java
@SdkInternalApi
public final class GeneratedMetadataLoaderProvider implements MetadataLoaderProvider {
    private static final RegionMetadataProvider REGION_METADATA_PROVIDER = new GeneratedRegionMetadataProvider();
    private static final ServiceMetadataProvider SERVICE_METADATA_PROVIDER = new GeneratedServiceMetadataProvider();
    private static final PartitionMetadataProvider PARTITION_METADATA_PROVIDER = new GeneratedPartitionMetadataProvider();
    private GeneratedMetadataLoaderProvider {}

    public PartitionMetadataProvider partitionMetadataProvider() { return PARTITION_METADATA_PROVIDER; }
    public RegionMetadataProvider regionMetadataProvider() { return REGION_METADATA_PROVIDER; }
    public ServiceMetadataProvider serviceMetadataProvider() { return SERVICE_METADATA_PROVIDER; }
    public static GeneratedMetadataLoaderProvider INSTANCE = new GeneratedMetadataLoaderProvider();
}
```

#### Exposing endpoints.json parsing logic to consumers
To support the expected common use-case of replacing the SDK's metadata with the latest available `endpoints.json`, the existing endpoints parsing and model will be broken
out from the `codegen-lite` module and be available to consumers. Optionally the SDK will also provide an abstract base that performs all the work of turning an
`endpoints.json` into a full-fledged `MetadataLoaderProvider`

### Registering `MetadataLoaderProvider`s with the SDK
Providers registered with the SDK vend out metadata solely through the updated `MetadataLoader` class. It retains its `@SdkInternalApi` annotation.
The mechanism for registering a provider and choosing a provider for metadata are described as follows. 

---
**Decision**: Which of the following options seem the best fit for the SDK?

---

#### Option 1: using `java.util.ServiceLoader`
Implementations, including the SDK's generated default, register themselves using the standard `META-INF/services` mechanism.
On each query, `MetadataLoader` selects the service with the newest value returned by `lastUpdatedAt` and attempts to locate metadata through that provider.
On failure, it falls back to the provider with the next newest value, and so on. In other words, providers are allowed to provide a subset of metadata and the SDK will attempt to perform fallback.  
Additionally, this behavior allows providers to specify themselves as a fallback *after* the SDK default by providing an `Instant` that is older than the SDK value.

Implementations of the `MetadataLoaderProviderService` are free to update and return new instances of `MetadataLoaderProvider` as desired,
however each instance returned should have immutable views of its data.
The SDK never propagates changes to existing client instances. Consumers are expected to manage the lifecycle of their clients if they require the latest metadata.

```java
@SdkPublicApi
public interface MetadataLoaderProviderService {
    Instant                lastUpdatedAt();
    MetadataLoaderProvider provider();
}

@SdkInternalApi
public final class GeneratedMetadataLoaderProviderService implements MetadataLoaderProviderService {
    private static final Instant BUILD_TIME = Instant.ofEpochMilli(...);

    public Instant lastUpdatedAt() { return BUILD_TIME; }
    public MetadataLoaderProvider provider() { return GeneratedMetadataLoaderProvider.INSTANCE; }
}
```

#### Option 2: Singleton service
The SDK provides a singleton that allows applications to register new metadata loader providers at runtime. Applications
are free to unregister providers as desired, including the SDK default provider. However, there must be one available at all times.

`MetadataLoader` queries providers for metadata in order of registered priority. The SDK default provider has a value of priority `0`.
Negative values specify ordering after the SDK default. Positive values designate priority higher than the SDK default.
The SDK never propagates changes to existing client instances. Consumers are expected to manage the lifecycle of their clients if they require the latest metadata.

```java
@SdkPublicApi
public class MetadataLoaderProviderService {
    ...
    ...
    private MetadataLoaderProviderService() {
        registerProvider(0, GeneratedMetadataLoaderProvider.INSTANCE);
    }
    
    Collection<MetadataLoaderProvider> allProviders();
    void registerProvider(int priority, MetadataLoaderProvider provider);
    void unregisterProvider(MetadataLoaderProvider provider);
    public static MetadataLoaderProviderService INSTANCE = new MetadataLoaderProviderService();
}
```

---
### Per-client metadata overrides
As a thought exercise, let's consider how client overrides would work alongside the global metadata loader.

##### New client option
A straight-forward new option for client configuration
```java
public static final SdkAdvancedClientOption<MetadataLoaderProvider> METADATA_LOADER_PROVIDER = new SdkAdvancedClientOption<>(MetadataLoaderProvider.class);
```

##### Loader pointers in metadata
The default client builder queries service metadata for an appropriate endpoint on client creation. The service metadata, in turn, queries
partition, and region metadata. As such in the per-client override case, metadata instances need to be able to query through the same loader (is this actually true?)

A pointer is therefore maintained so each view remains distinct from each other

```java
public interface (Service|Partition|Region)Metadata {
    ...
    MetadataLoaderProvider metadataLoaderProvider();
    ...
}
```

##### Loader pointers in execution context
Request interceptors occasionally query region/partition metadata to help users correct issues with their client configuration.
In order to add support for client-isolated metadata views, the metadata loader needs to be plumbed into the ExecutionContext

This might also be accomplished more easily using loader-specific `Region` instances.
Each `Region` instance provides a pointer to its backing metadata provider source. The default generated static/final values always uses the SDK default metadata,
but new instances may be created that refer to an alternate metadata source.
