# Post-Quantum TLS Opt-Out Test

This is a simple test program to verify the Post-Quantum TLS opt-out functionality in the AWS SDK for Java v2.

## What This Program Does

1. Creates an `AwsCrtHttpClient` with `postQuantumTlsEnabled(false)` to explicitly disable PQ TLS
2. Uses the CRT client to make a KMS `ListKeys` API call
3. Prints the results

When PQ TLS is disabled, the SDK will use the `TLS_CIPHER_PREF_TLSv1_0_2023` cipher preference instead of the default `TLS_CIPHER_SYSTEM_DEFAULT` (which enables PQ).

## Prerequisites

1. **AWS Credentials**: Ensure you have AWS credentials configured (via `~/.aws/credentials`, environment variables, or IAM role)
2. **AWS Region**: The program uses `us-east-1` by default
3. **KMS Access**: Your AWS credentials should have permission to call `kms:ListKeys`
4. **Local SDK Build**: The locally built AWS SDK v2 snapshot (2.41.23-SNAPSHOT) must be installed in your local Maven repository

## Building and Running

### Option 1: Using Maven Exec Plugin (Recommended)

```bash
cd /Users/childw/workplace/github/WillChilds-Klein/pq-tls-test
mvn compile exec:java
```

### Option 2: Build JAR and Run

```bash
cd /Users/childw/workplace/github/WillChilds-Klein/pq-tls-test
mvn clean package
java -cp target/pq-tls-test-1.0-SNAPSHOT.jar com.example.PqTlsTest
```

## Expected Output

```
=== Post-Quantum TLS Opt-Out Test ===

Creating AwsCrtHttpClient with postQuantumTlsEnabled=false...
CRT HTTP Client created successfully.

Creating KMS client...
KMS client created successfully.

Calling KMS ListKeys API...
✓ API call successful!

Found X KMS key(s):
----------------------------------------
Key ID:  xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
Key ARN: arn:aws:kms:us-east-1:123456789012:key/xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx

...

=== Test completed successfully ===

NOTE: The connection used TLS cipher preference: TLS_CIPHER_PREF_TLSv1_0_2023
      (Non-PQ cipher suite, as Post-Quantum TLS was disabled)
```

## Verifying the Behavior

To verify that the opt-out is working correctly:

1. **Check the logs**: Look for any TLS-related logs from the AWS CRT
2. **Compare with default**: Run a similar program without `postQuantumTlsEnabled(false)` and compare the cipher suites used
3. **Network capture**: Use Wireshark or similar tools to inspect the TLS handshake and verify the cipher suite

## Testing with PQ Enabled (Default)

To test with PQ TLS enabled (the default behavior), simply change:

```java
AwsCrtHttpClient crtHttpClient = AwsCrtHttpClient.builder()
        .postQuantumTlsEnabled(true)  // or omit this line entirely
        .build();
```

With PQ enabled, the connection will use `TLS_CIPHER_SYSTEM_DEFAULT` which prefers PQ cipher suites.

## Notes

- This uses AWS SDK v2 version `2.41.23-SNAPSHOT` from your local build
- The program limits results to 10 keys for brevity
- Make sure your AWS credentials have appropriate KMS permissions
