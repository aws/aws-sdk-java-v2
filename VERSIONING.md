# Versioning Policy

The AWS SDK for Java v2 uses a versioning scheme that follows the format: `MAJOR.MINOR.PATCH[-QUALIFIER]`. Revisions to different version components communicate the risk associated with changes when upgrading to newer versions of the SDK:

* `MAJOR` - Huge changes, expect API incompatibilities and other breaking changes.
* `MINOR` - Medium risk changes. Upgrading SHOULD usually just work, but check the [release notes](CHANGELOG.md). Example changes might include incrementing the minimum Java version, deprecating APIs, significant changes to core runtime components, or introducing new major features such as the transfer manager or S3 multipart client. Changes to the `MINOR` version MAY contain backward-incompatible changes in certain scenarios.
* `PATCH` - Zero to low risk changes. New features and bug fixes that should be safe to consume without much worry.
* `QUALIFIER` - (Optional) Additional release version qualifier (e.g. PREVIEW). Most releases are not expected to have a qualifier.

The AWS SDK for Java v2 does NOT follow strict [semantic versioning](https://semver.org/). Patch releases may contain new features in addition to bug fixes. See the FAQ for rationale.

## Stability of the AWS SDK for Java

For information about maintenance and support of SDK major versions and their underlying dependencies, see the
following in the AWS SDKs and Tools Shared Configuration and Credentials Reference Guide:

* [AWS SDKs and Tools Maintenance Policy](https://docs.aws.amazon.com/credref/latest/refdocs/maint-policy.html)
* [AWS SDKs and Tools Version Support Matrix](https://docs.aws.amazon.com/credref/latest/refdocs/version-support-matrix.html)


## Component Versioning

The SDK versions all service clients (e.g. `S3`, `EC2`, `DynamoDb`, etc) and the runtime (e.g. `aws-config`) together under a single version. This allows customers to easily upgrade multiple SDK clients at once and keep dependencies, such as the core runtime, compatible. The SDK may in the future consider versioning service clients separately from one another.

## Component Version Compatibility

**Best Practice: Use matching versions across all SDK modules.**

The SDK supports limited mixed version combinations within the same minor version boundary only. Cross minor version mixing is not supported and may cause runtime exceptions when newer core components call methods that older service modules only implement as default stubs.

### Supported Version Combinations

- ✅ **Recommended**: All modules at the same version (e.g., `2.34.5`)
- ✅ **Limited Support**: New core + old service within the same minor version (e.g., `sdk-core 2.34.5` with `s3 2.34.2`)


- ❌ **Not Supported**: Cross minor-version mixing (e.g., `sdk-core 2.34.x` with `s3 2.32.y`)
- ❌ **Not Supported**: Old core + new service (causes compile-time failures)

## Internal APIs

Any API marked with either `@SdkInternalApi` or `@SdkTestInternalApi` is not subject to any backwards compatibility guarantee. These are meant to be consumed only by the SDK and may be changed or removed without notice. The SDK MAY bump the `MINOR` version when making such a change.

## FAQ

**Why does the SDK not follow semantic versioning (semver)?**

Under semver we’d NEVER use the `PATCH` component. The SDK does *daily* releases containing updates to AWS service models. These changes go out with bug fixes every day. That means for us and customers the `PATCH` version would just be useless, it would always be zero. The intention behind this versioning scheme is to allow customers to weigh the relative risk and/or cost associated with updating to a newer SDK version.
