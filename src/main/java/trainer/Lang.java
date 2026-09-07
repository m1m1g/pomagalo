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
        n("btnAdd", "Добави нов");
        n("btnDelete", "Изтрий избраното");
        n("btnRefresh", "Обнови");
        n("btnRemove", "Изтрий");
        n("btnSaveChanges", "Запази промените");
        n("btnSaveLibrary", "Запази в библиотеката");
        n("btnSearch", "Търси");
        n("cmAscPattern", "Възходящ оборот: ");
        n("cmCadence", "Каданс: ");
        n("cmCardParams", "НАСТРОЙКИ");
        n("cmCardScale", "СКАЛА НА ГЛАСА");
        n("cmCardVoice", "ГЛАС");
        n("cmChantOf", " — учебно песнопение от ");
        n("cmCharacter", "Характер: ");
        n("cmCompose", "Генерирай песнопение");
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
        n("cmOutputPrompt", "Изберете глас, традиция и стил, след което натиснете „Състави“. Приложението ще състави учебно песнопение по данните от онтологията. Всичко се извършва на самото устройство, без връзка с мрежата.");
        n("cmSavedLibrary", "Записано в библиотеката.");
        n("cmSaveFailed", "Записът не се получи: ");
        n("cmStyle", "Стил");
        n("cmSyllables", " срички");
        n("cmTitle", "Заглавие");
        n("cmTitlePrompt", "по желание");
        n("cmTonesPerSyllable", " тона на сричка");
        n("cmTradition", "Традиция");
        n("exAlternate", " и редувайте с мелодичните стъпки. ");
        n("exAnabasis", "Възход по стълбицата");
        n("exAnabasisHint", "Изпейте нагоре, степен по степен, като започнете от устоя. Въведете сричките в същия ред.");
        n("exApechema", "Установете гласа с апихимата: ");
        n("exAscending", "Възходяща последователност");
        n("exButEntered", ", а въведохте ");
        n("exCadence", "Кадансов оборот");
        n("exCadenceHint", "Кадансът е оборотът, с който гласът се затваря на устоя. Изпейте го и въведете сричките: ");
        n("exCountMismatch", " срички, а въведохте ");
        n("exDescending", "Низходяща последователност");
        n("exEnterAll", "Въведете цялата последователност.");
        n("exExpectedWord", ": очаква се ");
        n("exFirstDiff", "Първата разлика е на позиция ");
        n("exHoldIson", "Задръжте исон на ");
        n("exIson", "Упражнение с исон");
        n("exKatabasis", "Слизане по стълбицата");
        n("exKatabasisHint", "Изпейте надолу, степен по степен, до устоя. Въведете сричките в същия ред.");
        n("exMatched", "Точно така — цялата последователност съвпада.");
        n("exMetrophony", "Метрофония по невмите");
        n("exMetrophonyHint", "Прочетете знаците един след друг, като започнете от устоя, и въведете сричката, на която извежда всеки знак: ");
        n("exModeFallback", "Първи глас");
        n("exNoExpected", "Няма очаквана последователност.");
        n("exNothing", "Не сте въвели нищо.");
        n("exThirds", "Упражнение по терци");
        n("exThirdsHint", "Прескачайте през една степен нагоре и се връщайте. Упражнението изгражда усета за разстояние, а не само за съседство.");
        n("exTrochos", "Колелото — свързани тетрахорди");
        n("exTrochosHint", "Колелото свързва тетрахордите: последната степен на единия започва следващия. Изпейте ги един след друг и въведете всички срички.");
        n("exWereExpected", "Очакваха се ");
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
        n("genDiatonic", "диатоничен");
        n("genEnharmonic", "енхармоничен");
        n("genHardChromatic", "твърд хроматичен");
        n("genSoftChromatic", "мек хроматичен");
        n("kbMode", "Глас:");
        n("libDetails", "Подробности:");
        n("libRecorded", "Записано: ");
        n("libSavedCh", "Съставени песнопения: ");
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
        n("refAdded", "Записът е добавен.");
        n("refDeleted", "Записът е изтрит.");
        n("refDescription", "Описание:");
        n("refError", "Грешка: ");
        n("refFound", "Намерени ");
        n("refPickClass", "Изберете клас и въведете идентификатор.");
        n("refPickEntry", "Изберете запис.");
        n("refRecordsFor", " записа за ");
        n("refRecordsFromClass", " записа от класа ");
        n("refSavedChanges", "Промените са запазени.");
        n("refSearchHint", "Търсене по дума в етикети и описания…");
        n("refShown", "Показани ");
        n("setHintTheme", "Сменя само цветовете; подредбата остава същата и смяната е мигновена.");
        n("setLead", "Изборите по-долу се пазят до затварянето на приложението.");
        n("setLook", "Облик");
        n("stAgents", "Агентите работят");
        n("stStandalone", "Самостоятелен режим — без агенти");
        n("stTheme", "Тема");
        n("stTriples", " тройки");
        n("svcTimeout", "Агентът не отговори навреме.");
        n("svcUnknown", "Няма такава услуга: ");
        n("tabComposer", "Генерирай");
        n("tabGuide", "Справочник");
        n("tabLibrary", "Библиотека");
        n("tabOntology", "Онтология");
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
