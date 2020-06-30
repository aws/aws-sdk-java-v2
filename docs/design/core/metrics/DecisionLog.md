# Decision Log for SDK V2 Metrics

Note: The decision log process was implemented late in this project, so decisions earlier than 7/30/20 are not included 
below.

## Log Entry Template

**Source:** (Meeting/aside/pair programming discussion/daily standup) to (discuss/implement) X

**Attendees:** Anna-Karin, Ben, Dongie, Irene, Matt, Nico, Vinod, Zoe

**Closed Decisions:**

1. Question? Decision. Justification.

**Open Decisions:**

1. (Old/Reopened/New) Question?

## 6/30/20

**Source:** Meeting to discuss https://github.com/aws/aws-sdk-java-v2/pull/1926 (metrics configuration design)

**Attendees:** Anna-Karin, Ben, Dongie, Matt, Nico, Zoe

**Closed Decisions:**

1. Should Option 1 override Option 2 or append? Override. That is what intuitively makes sense to us, and is aligned with
what most other similar settings do.
2. Should we return a builder from the SPI? No. We can't come up with a concrete case for it, and can another method to 
the SPI in the future that takes in a "configuration" object if we find a need.
3. Should we support upper/lowercase boolean environment variables? Be consistent with what we do elsewhere. Consistency
is a core tenet.
4. Should we support a profile-file setting? No. We don't want to be like the CLI and come up with arbitrary SDK-specific
properties, but we can explore that option in the future if customers ask for it.
5. Should we use the service loader? Yes. It's consistent with the way HTTP clients work, and it keeps the module off of
the classpath.

**Open Decisions:**

None