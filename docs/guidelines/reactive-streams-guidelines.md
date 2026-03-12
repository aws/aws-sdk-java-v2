# Reactvie Streams Guidelines

The AWS SDK for Java v2 uses Reactive Streams for asynchronous, non-blocking data processing with backpressure support. All implementations must adhere to the following requirements to ensure proper functionality and compatibility.

## Implementation Guidelines

### Compliance Requirements

- All implementations **MUST** fully comply with the [Reactive Streams Specification](https://github.com/reactive-streams/reactive-streams-jvm)
- All implementations **MUST** pass the [Reactive Streams Technology Compatibility Kit (TCK)](https://github.com/reactive-streams/reactive-streams-jvm/tree/master/tck) tests
- Any code changes to Reactive Streams implementations **MUST** include TCK verification tests

### Best Practices
- Developers **SHOULD NOT** implement new Publisher or Subscriber interfaces from scratch
- Developers **SHOULD** utilize existing utility classes such as:
  - `SimplePublisher` - A simple implementation of the Publisher interface
  - `ByteBufferStoringSubscriber` - A subscriber that stores received ByteBuffers
  - Methods in `SdkPublisher` - Utility methods for common Publisher operations

## Common Patterns

- Use `SdkPublisher` for SDK-specific publisher implementations
- Implement proper resource cleanup in both success and failure scenarios
- Handle cancellation gracefully, including cleanup of any allocated resources
- Ensure thread safety in all implementations
- Document thread safety characteristics and any assumptions about execution context

## Testing Requirements

- All Reactive Streams implementations **MUST** include TCK verification tests
- Tests **SHOULD** cover both normal operation and edge cases:
  - Cancellation during active streaming
  - Error propagation
  - Backpressure handling under various request scenarios
  - Resource cleanup in all termination scenarios

## References

- [Reactive Streams Specification](https://github.com/reactive-streams/reactive-streams-jvm)
- [Reactive Streams TCK](https://github.com/reactive-streams/reactive-streams-jvm/tree/master/tck)