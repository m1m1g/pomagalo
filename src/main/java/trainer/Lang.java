package trainer;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class Lang {

    private static final Logger LOG = Logger.getLogger(Lang.class.getName());

    public static final String EZIK = "bg";

    private static final Map<String, String> NADPISI = new HashMap<>();

    private static void n(String klyuch, String nadpis) { NADPISI.put(klyuch, nadpis); }

    static {
        n("appTitle", "Паралагия");
        n("btnDelete", "Изтрий избраното");
        n("btnRefresh", "Обнови");
        n("btnSaveLibrary", "Запази в библиотеката");
        n("btnSearch", "Търси");
        n("cmApechema", "Апихима: ");
        n("cmAscPattern", "Възходящ оборот: ");
        n("cmCadence", "Каданс: ");
        n("cmCardParams", "НАСТРОЙКИ");
        n("cmCardScale", "СКАЛА НА ГЛАСА");
        n("cmCardVoice", "ГЛАС");
        n("cmChantOf", " — упражнение от ");
        n("cmCharacter", "Характер: ");
        n("cmCompose", "Генерирай упражнение");
        n("cmComposed", "Съставено. Исонът се държи на ");
        n("cmCopied", "Текстът е копиран в паметта за обмен.");
        n("cmCopy", "Копирай текста");
        n("cmDescPattern", "Низходящ оборот: ");
        n("cmFinal", "Устой: ");
        n("cmGenerating", "Съставя се…");
        n("cmGenus", "Род: ");
        n("cmIson", "Исон: ");
        n("cmLength", "Дължина:");
        n("cmMelisma", "Разпев: ");
        n("cmNoModes", "Няма данни за гласовете — проверете дали онтологията е заредена.");
        n("cmOutputPrompt", "Изберете глас и стил, след което натиснете „Генерирай упражнение“. Приложението ще състави упражнението по данните от онтологията. Всичко се извършва на самото устройство, без връзка с мрежата.");
        n("cmSavedLibrary", "Упражнението е записано в библиотеката.");
        n("cmSaveFailed", "Записът не се получи: ");
        n("cmStyle", "Стил");
        n("cmSyllables", " срички");
        n("cmTitle", "Заглавие");
        n("cmTitlePrompt", "по желание");
        n("cmTonesPerSyllable", " тона на сричка");
        n("exAscending", "Възходящо движение");
        n("exDescending", "Низходящо движение");
        n("exIson", "Без движение (исон)");
        n("gApechema", "Апихима — оборот за установяване на гласа");
        n("gBody", "Телесни знаци — мелодично движение");
        n("gCategory", "Дял:");
        n("gDegrees", "Степени");
        n("gEchos", "Осмогласие — гласовете");
        n("gGreekName", "Гръцко наименование");
        n("gHistory", "История на православното пеене");
        n("gKind", "Вид");
        n("gNeumes", "Невми — знаци на нотацията");
        n("gNoResults", "Няма намерени записи.");
        n("gOrnamental", "Украсни знаци");
        n("gOverview", "Обобщение");
        n("gPersons", "Личности — псалти, химнографи, светци");
        n("gStyles", "Стилове на песнопението");
        n("gNotesPerSyllable", "Тонове на сричка: ");
        n("gRhythmic", "Ритмични знаци");
        n("gSign", "Знак");
        n("gSteps", "Срички и стълбица");
        n("gStepsIntro", "Стълбицата се състои от седем степени, а осмата повтаря първата октава по-високо — това повторение се нарича антифония. Степените се учат със сричките, а не с числа: така се запомня не височината, а разстоянието.");
        n("kbMode", "Глас:");
        n("libDetails", "Подробности:");
        n("libRecorded", "Записано: ");
        n("libSavedCh", "Съставени упражнения: ");
        n("modeWord", "глас");
        n("qCharacter", "За кой глас се отнася описанието: „");
        n("qFinalTone", "Кой е финалният тон на ");
        n("qGreekName", "Кое е гръцкото наименование на ");
        n("qNoteCharacter", "Всеки глас има свой характер и подходящ богослужебен репертоар.");
        n("qNoteFinal", "Финалният тон е устоят, на който завършва песнопението в този глас.");
        n("qNoteGreek", "Гръцките наименования идват от византийската осмогласна система.");
        n("qNoteSyllable", "Сричките са системата, с която се солфежира византийското пеене.");
        n("qSyllable", "Коя е сричката на: ");
        n("quizCheck", "Провери отговора");
        n("quizLoadFail", "Не успях да заредя въпрос: ");
        n("quizLoading", "Зареждане на въпрос…");
        n("quizNext", "Нов въпрос");
        n("quizNoData", "Онтологията не е заредена или няма достатъчно данни за въпрос.");
        n("quizOf", " от ");
        n("quizPickFirst", "Изберете отговор, преди да проверите.");
        n("quizScore", "Верни отговори: ");
        n("quizShow", "Покажи отговора");
        n("refDescription", "Описание:");
        n("refError", "Грешка: ");
        n("refFound", "Намерени ");
        n("refSearchHint", "Търсене по дума в етикети и описания…");
        n("setHintTheme", "Сменя само цветовете; подредбата остава същата и смяната е мигновена.");
        n("setLead", "Изборите по-долу се пазят до затварянето на приложението.");
        n("setLook", "Облик");
        n("stAgents", "Агентите работят");
        n("stStandalone", "Агентите не тръгнаха — работи само „Справочник“");
        n("stTheme", "Тема");
        n("svcNoAgents", "Агентите не са налични — този раздел няма да работи.");
        n("svcTimeout", "Агентът не отговори навреме.");
        n("tabComposer", "Генерирай");
        n("tabGuide", "Справочник");
        n("tabLibrary", "Библиотека");
        n("tabQuiz", "Тест");
        n("tabSettings", "Настройки");
        n("themeDark", "Тъмна");
        n("themeLight", "Светла");
    }

    private Lang() {}

    public static String t(String klyuch) {
        String v = NADPISI.get(klyuch);
        if (v != null) return v;
        LOG.warning("[Lang] Липсва надпис с ключ: " + klyuch);
        return klyuch;
    }

    public static String labelBlock(String obekt, String promenliva) {
        return textBlock(obekt, "rdfs:label", promenliva);
    }

    public static String textBlock(String obekt, String svoystvo, String promenliva) {
        return "  OPTIONAL { " + obekt + " " + svoystvo + " " + promenliva +
               " . FILTER(lang(" + promenliva + ") = \"" + EZIK + "\") }\n";
    }

    public static String code() { return EZIK; }
}
