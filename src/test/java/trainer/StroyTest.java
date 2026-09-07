package trainer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Строят на осмогласието")
class StroyTest {

    private static final double ТОЧНОСТ = 0.001;

    private static final int[] СТЕПЕНИ = {0, 2, 4, 5, 7, 9, 11};

    private static final String[] ИМЕНА =
        {"Νη", "Πα", "Βου", "Γα", "Δη", "Κε", "Ζω"};

    private static final int[] УСТОЙ = {1, 4, 3, 4, 1, 1, 3, 0};

    private static final double КВАРТА = 30.0;

    private static int полутон(int i) {
        return 12 * Math.floorDiv(i, 7) + СТЕПЕНИ[Math.floorMod(i, 7)];
    }

    private static double мории(int глас, int отКлавиш, int доКлавиш) {
        double от = ToneEngine.freqOf(глас, отКлавиш);
        double до = ToneEngine.freqOf(глас, доКлавиш);
        return 72.0 * Math.log(до / от) / Math.log(2.0);
    }

    @Test
    @DisplayName("Νη звучи на основата на строя при всеки глас")
    void osnovata() {
        for (int глас = 0; глас < 8; глас++) {
            assertEquals(ToneEngine.niHz(), ToneEngine.freqOf(глас, 60), ТОЧНОСТ,
                "гласът " + (глас + 1) + " измества основата");
        }
    }

    @Test
    @DisplayName("Дванадесет полутона нагоре удвояват честотата")
    void oktavata() {
        for (int глас = 0; глас < 8; глас++) {
            assertEquals(2.0, ToneEngine.freqOf(глас, 72) / ToneEngine.freqOf(глас, 60),
                1e-9, "октавата не е чиста при глас " + (глас + 1));
        }
    }

    @Test
    @DisplayName("Всеки звукоред расте: няма стъпка назад")
    void zvukoredat_raste() {
        for (int глас = 0; глас < ToneEngine.SCALE_MORIA.length; глас++) {
            int[] ред = ToneEngine.SCALE_MORIA[глас];
            for (int i = 1; i < ред.length; i++) {
                assertTrue(ред[i] > ред[i - 1],
                    "глас " + (глас + 1) + ": стъпка " + i + " не расте ("
                    + ред[i - 1] + " → " + ред[i] + ")");
            }
        }
    }

    @Test
    @DisplayName("Всеки звукоред започва от нула и остава в октавата")
    void zvukoredat_e_v_oktavata() {
        for (int глас = 0; глас < ToneEngine.SCALE_MORIA.length; глас++) {
            int[] ред = ToneEngine.SCALE_MORIA[глас];
            assertEquals(12, ред.length, "глас " + (глас + 1) + ": не са 12 стойности");
            assertEquals(0, ред[0], "глас " + (глас + 1) + ": не започва от нула");
            assertTrue(ред[ред.length - 1] < 72,
                "глас " + (глас + 1) + ": последната стъпка излиза от октавата");
        }
    }

    @Test
    @DisplayName("От устоя до квартата има точно тридесет мории")
    void tetrahordat() {
        for (int глас = 0; глас < 8; глас++) {
            int у   = УСТОЙ[глас];
            int дол = 60 + полутон(у);
            int гор = 60 + полутон(у + 3);

            double м = мории(глас, дол, гор);
            assertEquals(КВАРТА, м, 0.5,
                "глас " + (глас + 1) + " (устой " + ИМЕНА[у] + "): тетрахордът е "
                + Math.round(м) + " мории, а не " + (int) КВАРТА);

            assertEquals(Math.round(м), ToneEngine.moriiMezhdu(глас, дол, гор),
                "глас " + (глас + 1) + ": целочисленият и честотният път се разминават");
        }
    }

    @Test
    @DisplayName("Седми глас стъпва на Γα заедно с трети")
    void sedmi_glas_stapva_na_ga() {
        assertEquals(УСТОЙ[2], УСТОЙ[6],
            "Варис и трети глас трябва да стъпват на една и съща степен");

        assertArrayEquals(ToneEngine.SCALE_MORIA[2], ToneEngine.SCALE_MORIA[6],
            "трети и седми глас трябва да ползват един и същи звукоред");
    }

    @Test
    @DisplayName("Различните гласове дават различни разстояния")
    void glasovete_se_razlichavat() {
        double първи = ToneEngine.freqOf(0, 62);
        double втори = ToneEngine.freqOf(1, 62);
        assertTrue(Math.abs(първи - втори) > 0.5,
            "първи и втори глас дават еднаква височина на Πα");
    }

    @Test
    @DisplayName("Твърдият хроматичен род съдържа стъпка над тон и половина")
    void tvardiyat_hromatichen_ima_edra_stapka() {
        int[] ред = ToneEngine.SCALE_MORIA[5];
        int най = 0;
        for (int i = 1; i < ред.length; i++) най = Math.max(най, ред[i] - ред[i - 1]);
        assertTrue(най >= 18,
            "най-едрата стъпка в шести глас е " + най + " мории; очаква се поне 18");
    }
}
