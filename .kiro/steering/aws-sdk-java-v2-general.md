---
title: AWS SDK for Java v2 General Guidelines
inclusion: always
---

# AWS SDK for Java v2 General Guidelines

## Development Environment

- **Java Version**: Java 8 is the target language version for the AWS SDK for Java v2
- **Build System**: Maven is used for building and dependency management

## Build Instructions

To check if the SDK compiles properly, follow these steps:

1. **Build with dependencies**: Only need to run once. First run the build command with `--am` (also-make) flag to build all dependencies:
   ```bash
   mvn clean install -pl :${module} -P quick --am
   ```
   Example for S3 module:
   ```bash
   mvn clean install -pl :s3 -P quick --am
   ```

2. **Build module only**: Then run the build for just the specific module (skips testing and checkstyles):
   ```bash
   mvn clean install -pl :${module} -P quick
   ```

3. **Run tests**: To run tests, use the standard command without the quick profile:
   ```bash
   mvn clean install -pl :${module}
   ```

## Guidelines

All detailed guidelines are in #[[file:docs/guidelines/aws-sdk-java-v2-general.md]]