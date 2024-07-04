# Decision Log for AWS SDK fro Java v2 Migration Tool

## Log Entry Template

**Source**: (Meeting/aside/pair programming discussion/daily standup) to (discuss/implement) X

**Attendees**: Anirudh, Anna-Karin, David, Dongie, Debora, Olivier, Matt, Jason, John, Zoe

**Closed Decisions:**

1. Question? Decision. Justification

**Open Decisions:**

1. (Old/Reopened/new) Question?

## 01/26/2024

**Source:** Daily standup and offline discussion to discuss where we should host the source code of the v2 migration tool

**Attendees:** Anna-Karin, David, Debora, Olivier, Matt, Jason, John, Zoe

**Closed Decisions:** 

1. Should we host the source code in the same aws-sdk-java-v2 repo? Yes, because 1) no extra release infrastructure is needed since it can be released as part of the SDK release 2) it's easier to write scripts to generate recipes, for example, we need to write script to retrieve service IDs for all services and current version. 3) it has better discoverability. The only disadvantage is that it will increase the scope of the repo and increase the build and release time slightly. The alternatives are: 1) setting up a new GitHub repo, which require us to set up and maintain new CICD pipeline. 2) hosting the code internally, which would be a bad customer experience since the code is not public and there is no place for users to raise questions/PRs.

2. Should we publish the tool to Maven central instead of vending a JAR through S3? Yes, because most customers, if not all, prefer to consume the library from the package manager instead of a JAR. 

**Open Decisions:**

None
