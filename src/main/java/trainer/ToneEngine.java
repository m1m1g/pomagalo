package trainer;

public final class ToneEngine {

    private static final double NI_HZ = 261.6256;

    private static final double OKTAVA_MORII = 72.0;

    static final int[][] SCALE_MORIA = {

        {   0,   6,  12,  17,  22,  30,  36,  42,  48,  54,  59,  64 },

        {   0,   4,   8,  15,  22,  30,  36,  42,  46,  50,  57,  64 },

        {   0,   6,  12,  18,  24,  30,  36,  42,  48,  54,  57,  60 },

        {   0,   6,  12,  17,  22,  30,  36,  42,  48,  54,  59,  64 },

        {   0,   6,  12,  17,  22,  30,  36,  42,  48,  54,  59,  64 },

        {   0,   2,   4,   7,  10,  30,  32,  34,  40,  46,  49,  52 },

        {   0,   6,  12,  18,  24,  30,  36,  42,  48,  54,  57,  60 },

        {   0,   6,  12,  17,  22,  30,  36,  42,  48,  54,  59,  64 },
    };

    private ToneEngine() {}

    public static double freqOf(int modeIndex, int midiNote) {
        int mode = Math.max(0, Math.min(modeIndex, SCALE_MORIA.length - 1));
        int semFromNi  = midiNote - 60;
        int octave     = Math.floorDiv(semFromNi, 12);
        int pitchClass = Math.floorMod(semFromNi, 12);
        double moriaTotal = octave * OKTAVA_MORII + SCALE_MORIA[mode][pitchClass];
        return NI_HZ * Math.pow(2.0, moriaTotal / OKTAVA_MORII);
    }

    public static int moriiMezhdu(int modeIndex, int otKlavish, int doKlavish) {
        return moriiOt(modeIndex, doKlavish) - moriiOt(modeIndex, otKlavish);
    }

    static int moriiOt(int modeIndex, int midiNote) {
        int mode = Math.max(0, Math.min(modeIndex, SCALE_MORIA.length - 1));
        int semFromNi  = midiNote - 60;
        int octave     = Math.floorDiv(semFromNi, 12);
        int pitchClass = Math.floorMod(semFromNi, 12);
        return (int) (octave * OKTAVA_MORII) + SCALE_MORIA[mode][pitchClass];
    }

    static double niHz() { return NI_HZ; }
}
