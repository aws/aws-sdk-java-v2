# JAVA-8699: Write Throughput Metric - Design Document

## Overview

Add a `WRITE_THROUGHPUT` metric to measure request body upload speed (bytes/sec), complementing the existing `READ_THROUGHPUT` metric. This addresses customer requests from SDK v1 migration ([GitHub #3704](https://github.com/aws/aws-sdk-java-v2/issues/3704)).

## Formula

```
WRITE_THROUGHPUT = RequestBytesWritten / (LastReadTime - WriteStartTime)
```

- **WriteStartTime**: Timestamp when first byte is read from request body stream
- **LastReadTime**: Timestamp when last byte is read from request body stream

## Request Timeline

```
AttemptStart    WriteStart      LastReadTime     TTFB                TTLB
     |              |                |             |                    |
     v              v                v             v                    v
     [-- Setup --][-- Sending Body --][-- Server --][-- Receive Response --]
```

## Implementation

### Sync Client

Track timestamps in `BytesWrittenTrackingInputStream`:

```java
public final class BytesWrittenTrackingInputStream extends SdkFilterInputStream {
    private final AtomicLong bytesWritten;
    private final AtomicLong writeStartTime;
    private final AtomicLong lastReadTime;

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        writeStartTime.compareAndSet(0, System.nanoTime());  // First read only
        int read = super.read(b, off, len);
        if (read > 0) {
            bytesWritten.addAndGet(read);
            lastReadTime.set(System.nanoTime());  // Every read
        }
        return read;
    }
}
```

### Async Client

Similar pattern with `BytesWrittenTrackingPublisher` wrapping the request body publisher.

### Metric Reporting

In `HandleResponseStage`, calculate and report the metric:

```java
double writeThroughput = bytesWritten / ((lastReadTime - writeStartTime) / 1_000_000_000.0);
attemptMetricCollector.reportMetric(CoreMetric.WRITE_THROUGHPUT, writeThroughput);
```

## Rationale

1. **Accuracy** - On fast networks (e.g., EC2 in same region as S3), server processing time (~60-110ms) can be a significant portion of total upload time. Using TTFB as WriteEnd would underreport throughput by up to 96%.

2. **Negligible overhead** - Performance benchmarks show tracking lastReadTime adds <0.01% overhead to real uploads.

3. **Consistency** - Works regardless of how HTTP client reads the stream.

4. **Sync/Async parity** - Same approach works for both client types.

---

## Appendix A: Alternatives Considered

### Alternative 1: Track EOF/onComplete
Capture timestamp when stream ends (`read()` returns `-1` for sync, `onComplete()` for async).

**Pros:**
- Captures exact moment stream is exhausted
- Clean semantic meaning

**Cons:**
- HTTP client may not call after content-length bytes reached
- Sync: `read()` returning `-1` not guaranteed
- Async: `onComplete()` not guaranteed - client may cancel subscription

### Alternative 2: Use TTFB
Use Time to First Byte as proxy for write end.

**Pros:**
- Simple - already available
- No additional tracking needed

**Cons:**
- Includes server processing time
- Significantly underreports throughput on fast networks (see Appendix B)

### Alternative 3: HTTP Client Layer Implementation
Track actual bytes written to socket in each HTTP client (Apache, Netty, CRT).

**Pros:**
- Most accurate - measures actual network writes
- Accounts for client buffering

**Cons:**
- Must implement in all HTTP clients (Apache, Netty, CRT, URL Connection)
- Significant implementation effort
- Inconsistent with READ_THROUGHPUT (which is tracked in SDK core)
- Maintenance burden across multiple clients

---

## Appendix B: Accuracy Comparison

Tested with real S3 uploads comparing Alternative 2 (TTFB) vs recommended approach (lastReadTime):

**Local machine (slower network):**

| Object Size | Alternative 2 (TTFB) | Recommended (lastRead) | Diff | Server Processing |
|-------------|----------------------|------------------------|------|-------------------|
| 10MB | 19.92 MB/s | 24.80 MB/s | 24.5% | 103 ms |
| 100MB | 33.66 MB/s | 34.94 MB/s | 3.8% | 113 ms |

**EC2 instance (fast network, same region as S3):**

| Object Size | Alternative 2 (TTFB) | Recommended (lastRead) | Diff | Server Processing |
|-------------|----------------------|------------------------|------|-------------------|
| 10MB | 34.22 MB/s | 43.09 MB/s | 25.9% | 63 ms |
| 10MB | 75.61 MB/s | 147.87 MB/s | 95.6% | 67 ms |

**Conclusion:** Server processing time is relatively constant (~60-110ms), but on fast networks it becomes a significant portion of total upload time. Alternative 2 can underreport throughput by up to 96%.

---

## Appendix C: Performance Benchmark

JMH benchmark comparing `BytesWrittenTrackingInputStream` implementations:

| Data Size | Buffer | Baseline | Alternative 2 | Recommended | Alt2 - Baseline | Rec - Baseline |
|-----------|--------|----------|---------------|-------------|-----------------|----------------|
| 10MB | 16KB | 420 µs | 468 µs | 480 µs | +48 µs | +60 µs |
| 10MB | 64KB | 460 µs | 470 µs | 457 µs | +10 µs | -3 µs (noise) |
| 10MB | 128KB | 461 µs | 467 µs | 443 µs | +6 µs | -18 µs (noise) |
| 100MB | 16KB | 7,359 µs | 8,127 µs | 8,346 µs | +768 µs | +987 µs |
| 100MB | 64KB | 8,118 µs | 8,057 µs | 8,004 µs | -61 µs (noise) | -114 µs (noise) |
| 100MB | 128KB | 7,925 µs | 7,769 µs | 7,990 µs | -156 µs (noise) | +65 µs |

- 16KB buffer used by `FileAsyncRequestBody`
- 64KB/128KB buffers used by sync HTTP clients

**Conclusion:** Recommended approach adds ~60-987 µs overhead vs baseline. Compared to real network I/O (~3 seconds for 100MB at 33 MB/s), this is **<0.03%** - negligible.

### Raw JMH Results

```
Benchmark                                                    (bufferSize)  (dataSize)  Mode  Cnt     Score     Error  Units
BytesWrittenTrackingBenchmark.baseline                              16384    10485760  avgt    5   420.387 ±   1.665  us/op
BytesWrittenTrackingBenchmark.baseline                              16384   104857600  avgt    5  7359.279 ±  95.049  us/op
BytesWrittenTrackingBenchmark.baseline                              65536    10485760  avgt    5   460.210 ±   6.793  us/op
BytesWrittenTrackingBenchmark.baseline                              65536   104857600  avgt    5  8117.617 ± 984.170  us/op
BytesWrittenTrackingBenchmark.baseline                             131072    10485760  avgt    5   460.559 ±   2.650  us/op
BytesWrittenTrackingBenchmark.baseline                             131072   104857600  avgt    5  7924.954 ± 232.629  us/op
BytesWrittenTrackingBenchmark.option2_writeStartTimeOnly            16384    10485760  avgt    5   467.664 ±   6.503  us/op
BytesWrittenTrackingBenchmark.option2_writeStartTimeOnly            16384   104857600  avgt    5  8127.128 ± 362.653  us/op
BytesWrittenTrackingBenchmark.option2_writeStartTimeOnly            65536    10485760  avgt    5   470.295 ±   1.482  us/op
BytesWrittenTrackingBenchmark.option2_writeStartTimeOnly            65536   104857600  avgt    5  8057.417 ± 368.637  us/op
BytesWrittenTrackingBenchmark.option2_writeStartTimeOnly           131072    10485760  avgt    5   466.987 ±   2.365  us/op
BytesWrittenTrackingBenchmark.option2_writeStartTimeOnly           131072   104857600  avgt    5  7768.661 ± 296.360  us/op
BytesWrittenTrackingBenchmark.option3_lastReadTimeEveryRead         16384    10485760  avgt    5   480.264 ±   7.447  us/op
BytesWrittenTrackingBenchmark.option3_lastReadTimeEveryRead         16384   104857600  avgt    5  8345.863 ± 356.510  us/op
BytesWrittenTrackingBenchmark.option3_lastReadTimeEveryRead         65536    10485760  avgt    5   457.162 ±   2.356  us/op
BytesWrittenTrackingBenchmark.option3_lastReadTimeEveryRead         65536   104857600  avgt    5  8004.015 ± 247.785  us/op
BytesWrittenTrackingBenchmark.option3_lastReadTimeEveryRead        131072    10485760  avgt    5   442.916 ±   3.713  us/op
BytesWrittenTrackingBenchmark.option3_lastReadTimeEveryRead        131072   104857600  avgt    5  7989.967 ± 437.852  us/op
```

---

## Appendix D: SDK v1 Approach

SDK v1 uses `ByteThroughputHelper` which tracks timing around each I/O operation. Write throughput is implemented in `MetricInputStreamEntity` at the Apache HTTP client layer.

**V1:**
- Timing: Sum of time spent in each I/O call
- Reporting: Periodic (every 10s of accumulated I/O time)
- Scope: Aggregated across multiple requests
- Implementation: HTTP client layer (Apache entity)

**V2:**
- Timing: Wall-clock time (first read to last read)
- Reporting: Per-request (at end of API call attempt)
- Scope: Per-request granularity
- Implementation: SDK core layer
