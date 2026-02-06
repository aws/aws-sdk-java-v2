package com.example;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.ListKeysRequest;
import software.amazon.awssdk.services.kms.model.ListKeysResponse;

/**
 * Test program to verify Post-Quantum TLS configuration options.
 *
 * This program tests three different PQ TLS configurations:
 * 1. Default (postQuantumTlsEnabled not set) - uses system default
 * 2. postQuantumTlsEnabled(true) - explicitly enables PQ TLS
 * 3. postQuantumTlsEnabled(false) - explicitly disables PQ TLS (opt-out)
 */
public class PqTlsTest {

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   Post-Quantum TLS Configuration Test");
        System.out.println("========================================\n");

        boolean allTestsPassed = true;

        // Test 1: Default behavior (no postQuantumTlsEnabled set)
        allTestsPassed &= testPqConfiguration("DEFAULT (not set)", null);

        // Test 2: Explicitly enable PQ TLS
        allTestsPassed &= testPqConfiguration("ENABLED (true)", true);

        // Test 3: Explicitly disable PQ TLS (opt-out)
        allTestsPassed &= testPqConfiguration("DISABLED (false)", false);

        // Summary
        System.out.println("\n========================================");
        if (allTestsPassed) {
            System.out.println("   ✓ ALL TESTS PASSED");
        } else {
            System.out.println("   ✗ SOME TESTS FAILED");
        }
        System.out.println("========================================\n");

        System.exit(allTestsPassed ? 0 : 1);
    }

    /**
     * Test KMS ListKeys with a specific PQ TLS configuration.
     *
     * @param configName Human-readable name for this configuration
     * @param pqEnabled  null=default, true=enabled, false=disabled
     * @return true if test passed, false otherwise
     */
    private static boolean testPqConfiguration(String configName, Boolean pqEnabled) {
        System.out.println("----------------------------------------");
        System.out.println("Test: postQuantumTlsEnabled = " + configName);
        System.out.println("----------------------------------------");

        SdkHttpClient httpClient = null;
        KmsClient kmsClient = null;

        try {
            // Create HTTP client with appropriate PQ configuration
            System.out.print("Creating AwsCrtHttpClient... ");
            AwsCrtHttpClient.Builder clientBuilder = AwsCrtHttpClient.builder();

            if (pqEnabled != null) {
                clientBuilder.postQuantumTlsEnabled(pqEnabled);
            }
            // If pqEnabled is null, don't set it (use default)

            httpClient = clientBuilder.build();
            System.out.println("✓");

            // Create KMS client
            System.out.print("Creating KMS client... ");
            kmsClient = KmsClient.builder()
                    .region(Region.US_EAST_1)
                    .httpClient(httpClient)
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build();
            System.out.println("✓");

            // Make API call
            System.out.print("Calling KMS ListKeys API... ");
            ListKeysRequest request = ListKeysRequest.builder()
                    .limit(5)  // Just need a few keys to verify connectivity
                    .build();

            ListKeysResponse response = kmsClient.listKeys(request);
            System.out.println("✓");

            // Display results
            System.out.println("\nResult: SUCCESS");
            System.out.println("  Keys returned: " + response.keys().size());
            System.out.println("  Expected cipher: " + getExpectedCipher(pqEnabled));
            System.out.println();

            return true;

        } catch (Exception e) {
            System.out.println("✗");
            System.out.println("\nResult: FAILED");
            System.out.println("  Error: " + e.getClass().getSimpleName());
            System.out.println("  Message: " + e.getMessage());
            System.out.println();
            return false;

        } finally {
            // Clean up resources
            if (kmsClient != null) {
                try {
                    kmsClient.close();
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
            }
            if (httpClient != null) {
                try {
                    httpClient.close();
                } catch (Exception e) {
                    // Ignore cleanup errors
                }
            }
        }
    }

    /**
     * Get the expected cipher preference description based on configuration.
     */
    private static String getExpectedCipher(Boolean pqEnabled) {
        if (pqEnabled == null) {
            return "TLS_CIPHER_SYSTEM_DEFAULT (PQ preferred by default since CRT 0.39.3)";
        } else if (pqEnabled) {
            return "TLS_CIPHER_SYSTEM_DEFAULT (PQ explicitly enabled)";
        } else {
            return "TLS_CIPHER_PREF_TLSv1_0_2023 (PQ explicitly disabled/opted-out)";
        }
    }
}
