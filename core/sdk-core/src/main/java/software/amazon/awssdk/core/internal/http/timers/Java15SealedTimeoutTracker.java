package software.amazon.awssdk.core.internal.http.timers;

/**
 * Sealed classes
 *
 * https://openjdk.java.net/jeps/360
 */
sealed public interface Java15SealedTimeoutTracker permits NoOpTimeoutTracker,
    ApiCallTimeoutTracker, NonSealedImplementation {
}

non-sealed class NonSealedImplementation implements Java15SealedTimeoutTracker {

    // pattern matching
//    void futureSwitch() {
//        SealedTimeoutTracker tracker;
//        switch (tracker) {
//            case NoOpTimeoutTracker -> ... ;
//            case ApiCallTimeoutTracker -> ...;
//        }
//    }

}

