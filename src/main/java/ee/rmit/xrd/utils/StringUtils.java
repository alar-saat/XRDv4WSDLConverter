package ee.rmit.xrd.utils;

public final class StringUtils {

    public static boolean isNotBlank(String test) {
        return test != null && !test.trim().isEmpty();
    }

    public static boolean isBlank(String test) {
        return test == null || test.trim().isEmpty();
    }
}
