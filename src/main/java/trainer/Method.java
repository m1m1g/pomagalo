package trainer;

public final class Method {

    public static final String SHOWS_SCORE         = "showsScore";
    public static final String MARKS_ERRORS        = "marksErrors";
    public static final String ORDERED_PROGRESSION = "orderedProgression";

    private Method() {}

    public static boolean is(String switchName) {
        return SHOWS_SCORE.equals(switchName)
            || MARKS_ERRORS.equals(switchName)
            || ORDERED_PROGRESSION.equals(switchName);
    }
}
