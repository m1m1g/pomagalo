package trainer;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class Pathove {

    private static final Logger LOG = Logger.getLogger(Pathove.class.getName());

    private static final String BELEG = "data/byzantine_music3.owl";

    private static final String SVOYSTVO = "paralagia.dir";

    private static Path osnova;
    private static boolean tarseno;

    private Pathove() {}

    public static synchronized Path osnova() {
        if (tarseno) return osnova;
        tarseno = true;

        for (Path kandidat : kandidati()) {
            if (kandidat != null && Files.isReadable(kandidat.resolve(BELEG))) {
                osnova = kandidat.normalize();
                LOG.info("[Pathove] Данните са в " + osnova);
                return osnova;
            }
        }

        osnova = Paths.get("").toAbsolutePath();
        LOG.warning("[Pathove] Не намирам „" + BELEG + "“. Търсих в: "
                  + opis() + ". Приложението ще се отвори празно. Пуснете го "
                  + "от папката pomagalo или задайте "
                  + "-D" + SVOYSTVO + "=<папка>.");
        return osnova;
    }

    public static Path danni(String ime) {
        return osnova().resolve("data").resolve(ime);
    }

    public static Path do_dannite(String ime) {
        return osnova().resolve(ime);
    }

    private static List<Path> kandidati() {
        List<Path> spisak = new ArrayList<>();

        String zadadeno = System.getProperty(SVOYSTVO);
        if (zadadeno != null && !zadadeno.isBlank())
            spisak.add(Paths.get(zadadeno));

        Path rabotna = Paths.get("").toAbsolutePath();
        spisak.add(rabotna);
        spisak.add(rabotna.resolve("pomagalo"));

        Path nagore = rabotna;
        for (int i = 0; i < 3 && nagore != null; i++) {
            nagore = nagore.getParent();
            if (nagore != null) {
                spisak.add(nagore);
                spisak.add(nagore.resolve("pomagalo"));
            }
        }

        Path kod = kade_e_kodyt();
        if (kod != null) {
            spisak.add(kod);
            Path p = kod;
            for (int i = 0; i < 3 && p != null; i++) {
                p = p.getParent();
                if (p != null) spisak.add(p);
            }
        }
        return spisak;
    }

    private static Path kade_e_kodyt() {
        try {
            URI u = Pathove.class.getProtectionDomain()
                                 .getCodeSource().getLocation().toURI();
            File f = new File(u);
            return f.isDirectory() ? f.toPath() : f.toPath().getParent();
        } catch (Exception e) {
            return null;
        }
    }

    private static String opis() {
        StringBuilder sb = new StringBuilder();
        for (Path k : kandidati()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(k);
        }
        return sb.toString();
    }
}
