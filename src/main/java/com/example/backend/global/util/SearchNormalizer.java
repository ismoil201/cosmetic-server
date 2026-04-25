package com.example.backend.global.util;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Universal SearchNormalizer for ZAVEN (UZ/RU/EN)
 *
 * Goal:
 *  - Normalize query + product text to canonical tokens
 *  - Handle Uzbek apostrophes, Cyrillic transliteration, synonyms, brand typos
 *  - Works for ALL products (not hardcoded 55 items)
 *
 * Usage:
 *  - String normalized = SearchNormalizer.normalize("kallagen ichiladigan");
 *  - search_text = SearchNormalizer.buildSearchText(name, brand, category, description, variantLabels)
 */
public final class SearchNormalizer {

    private SearchNormalizer() {}

    // -----------------------------
    // Config
    // -----------------------------
    private static final Pattern SPLIT = Pattern.compile("[^a-z0-9]+");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    // 1) typo/variant -> canonical token(s)
    private static final Map<String, String> FIX = new HashMap<>();

    // 2) synonyms -> canonical token(s)
    private static final Map<String, String> SYN = new HashMap<>();

    // 3) stopwords (noise words)
    private static final Set<String> STOP = new HashSet<>();

    // 4) Cyrillic -> Latin (RU + UZ cyr)
    private static final Map<Character, String> CYR_TO_LAT = Map.ofEntries(
            // RU
            Map.entry('а',"a"), Map.entry('б',"b"), Map.entry('в',"v"),
            Map.entry('г',"g"), Map.entry('д',"d"), Map.entry('е',"e"),
            Map.entry('ё',"yo"), Map.entry('ж',"zh"), Map.entry('з',"z"),
            Map.entry('и',"i"), Map.entry('й',"y"), Map.entry('к',"k"),
            Map.entry('л',"l"), Map.entry('м',"m"), Map.entry('н',"n"),
            Map.entry('о',"o"), Map.entry('п',"p"), Map.entry('р',"r"),
            Map.entry('с',"s"), Map.entry('т',"t"), Map.entry('у',"u"),
            Map.entry('ф',"f"), Map.entry('х',"h"), Map.entry('ц',"ts"),
            Map.entry('ч',"ch"), Map.entry('ш',"sh"), Map.entry('щ',"sh"),
            Map.entry('ъ',""), Map.entry('ы',"i"), Map.entry('ь',""),
            Map.entry('э',"e"), Map.entry('ю',"yu"), Map.entry('я',"ya"),
            // UZ Cyrillic
            Map.entry('қ',"q"), Map.entry('ў',"o"), Map.entry('ғ',"g"),
            Map.entry('ҳ',"h"), Map.entry('ң',"n")
    );

    static {
        // -----------------------------
        // BRAND FIXES (ENG/RU/UZ variants)
        // -----------------------------
        putFix("axisy", "axis y");
        putFix("axis-y", "axis y");
        putFix("axis", "axis y");            // ehtiyot: siz ko‘p “axis” deb qidirasiz
        putFix("аксис", "axis y");
        putFix("roundlab", "round lab");
        putFix("round", "round lab");        // round -> round lab (sizda ko‘p ishlatiladi)
        putFix("dokdo", "1025 dokdo");
        putFix("1025dokdo", "1025 dokdo");
        putFix("skin1004", "skin1004");
        putFix("скин1004", "skin1004");
        putFix("madagaskar", "madagascar");
        putFix("beautyofjoseon", "beauty of joseon");
        putFix("boj", "beauty of joseon");

        putFix("drjart", "dr jart");
        putFix("dr.jart", "dr jart");
        putFix("althea", "dr althea");
        putFix("dralthea", "dr althea");

        putFix("tirtir", "tirtir");
        putFix("tir tir", "tirtir");

        putFix("anua", "anua");
        putFix("celimax", "celimax");
        putFix("clio", "clio");
        putFix("sulwhasoo", "sulwhasoo");
        putFix("torriden", "torriden");
        putFix("farmstay", "farm stay");
        putFix("skin1004madagascar", "skin1004 madagascar");
        putFix("polmedison", "polmedison");
        putFix("dermashare", "dermashare");
        putFix("aromatica", "aromatica");
        putFix("muzigae", "muzigae");
        putFix("mansion", "mansion");
        putFix("lactofit", "lactofit");
        putFix("laktofit", "lactofit");
        putFix("boto", "boto");

        // -----------------------------
        // CORE SYNONYMS (category words)
        // -----------------------------
        // perfume
        putSyn("atir", "parfum");
        putSyn("parfyum", "parfum");
        putSyn("attar", "parfum");
        putSyn("perfume", "parfum");
        putSyn("fragrance", "parfum");
        putSyn("парфюм", "parfum");
        putSyn("духи", "parfum");

        // cleanser / foam / penka
        putSyn("penka", "cleanser");
        putSyn("пенка", "cleanser");
        putSyn("foam", "cleanser");
        putSyn("cleanser", "cleanser");
        putSyn("facewash", "cleanser");
        putSyn("wash", "cleanser");
        putSyn("yuvish", "cleanser");
        putSyn("yuvishpenka", "cleanser");
        putSyn("yuvishgel", "cleanser");
        putSyn("gel", "cleanser"); // ko‘p gel yuvish vositalari

        // cream / moisturizer
        putSyn("krem", "cream");
        putSyn("крем", "cream");
        putSyn("cream", "cream");
        putSyn("moisturizer", "cream");
        putSyn("balm", "balm");

        // serum / ampoule / essence
        putSyn("serum", "serum");
        putSyn("сыворотка", "serum");
        putSyn("ampoule", "ampoule");
        putSyn("ampula", "ampoule");
        putSyn("эссенция", "essence");
        putSyn("essence", "essence");
        putSyn("shot", "shot");

        // toner / mist
        putSyn("toner", "toner");
        putSyn("tonik", "toner");
        putSyn("тоник", "toner");
        putSyn("mist", "mist");
        putSyn("mister", "mist");
        putSyn("spray", "mist");
        putSyn("спрей", "mist");

        // sunscreen / spf
        putSyn("spf", "sunscreen");
        putSyn("pa", "sunscreen");
        putSyn("sunscreen", "sunscreen");
        putSyn("suncream", "sunscreen");
        putSyn("sun", "sunscreen");
        putSyn("quyosh", "sunscreen");
        putSyn("kuyosh", "sunscreen");
        putSyn("защита", "sunscreen");

        // cushion / makeup
        putSyn("kushon", "cushion");
        putSyn("кушон", "cushion");
        putSyn("cushion", "cushion");
        putSyn("tonalka", "foundation");
        putSyn("тоналка", "foundation");
        putSyn("foundation", "foundation");
        putSyn("ton", "foundation");
        putSyn("тон", "foundation");
        putSyn("maskara", "mascara");
        putSyn("туш", "mascara");
        putSyn("mascara", "mascara");
        putSyn("lip", "lip");
        putSyn("cheek", "cheek");
        putSyn("palette", "palette");

        // patches
        putSyn("patch", "patch");
        putSyn("patche", "patch");
        putSyn("патчи", "patch");
        putSyn("eye", "eye");
        putSyn("ko‘z", "eye");
        putSyn("koz", "eye");

        // supplements (food)
        putSyn("probiotic", "probiotic");
        putSyn("probiotik", "probiotic");
        putSyn("пробиотик", "probiotic");
        putSyn("biotin", "biotin");

        // -----------------------------
        // INGREDIENT / KEYWORD SYNONYMS
        // -----------------------------
        putSyn("retinol", "retinol");
        putSyn("retinal", "retinal");
        putSyn("centella", "centella");
        putSyn("cica", "centella");
        putSyn("мадекассосид", "madecassoside");
        putSyn("madecassoside", "madecassoside");

        putSyn("glutathione", "glutathione");
        putSyn("глутатион", "glutathione");
        putSyn("niacinamide", "niacinamide");
        putSyn("ниацинамид", "niacinamide");
        putSyn("tranexamic", "tranexamic");
        putSyn("транексам", "tranexamic");
        putSyn("vitamin", "vitamin");
        putSyn("vitaminc", "vitamin c");
        putSyn("vitaminс", "vitamin c");
        putSyn("vitamin-c", "vitamin c");
        putSyn("vitc", "vitamin c");

        putSyn("tea", "tea");
        putSyn("tree", "tree");
        putSyn("teatree", "tea tree");

        // ✅ collagen variants (your issue)
        putSyn("collagen", "collagen");
        putSyn("kollagen", "collagen");
        putSyn("kallagen", "collagen");
        putSyn("kalagen", "collagen");
        putSyn("коллаген", "collagen");

        // -----------------------------
        // STOPWORDS (UZ/RU common)
        // -----------------------------
        Collections.addAll(STOP,
                "uchun","bilan","ham","va","yoki","bu","shu","qanday","nima","nechi",
                "ml","g","gr","kg","dona","ta","xil","turi","original","import",
                "yuz","teri","bosh","soch","ko‘z","koz","atrofi","qarshi","mos",
                "koreys","koreya","ayollar","erkaklar","kundalik","kechasi","kunduzi",
                "parvarish","foydasi","tinchlantiradi","namlantiradi","yaxshilaydi",
                "set","mini","nabor","to‘plam","toplam","kit",
                "uchun","the","and","for","with"
        );
    }

    // -----------------------------
    // Public
    // -----------------------------
    public static String normalize(String text) {
        List<String> tokens = normalizeToTokens(text);
        return String.join(" ", tokens);
    }

    /**
     * Build DB search_text from product fields.
     * Put EVERYTHING valuable into one normalized string:
     * - name + brand + category + description + variants (labels)
     */
    public static String buildSearchText(
            String name,
            String brand,
            String category,
            String description,
            Collection<String> variantLabels
    ) {
        StringBuilder sb = new StringBuilder();
        if (name != null) sb.append(name).append(" ");
        if (brand != null) sb.append(brand).append(" ");
        if (category != null) sb.append(category).append(" ");
        if (description != null) sb.append(description).append(" ");
        if (variantLabels != null) {
            for (String v : variantLabels) {
                if (v != null) sb.append(v).append(" ");
            }
        }

        // normalized + raw combo is best (raw helps exact, normalized helps fuzzy)
        String raw = safeLower(sb.toString());
        String norm = normalize(raw);
        return (raw + " " + norm).trim();
    }

    // -----------------------------
    // Pipeline
    // -----------------------------
    public static List<String> normalizeToTokens(String input) {
        if (input == null) return List.of();

        String s = input.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return List.of();

        // 0) UZ apostrophes normalize
        s = normalizeUzApostrophes(s);

        // 1) Cyrillic -> latin
        s = transliterateCyrillic(s);

        // 2) Remove diacritics
        s = removeDiacritics(s);

        // 3) Normalize separators
        s = s.replace('&', ' ')
                .replace('+', ' ')
                .replace('/', ' ')
                .replace('_', ' ')
                .replace('-', ' ');
        s = MULTI_SPACE.matcher(s).replaceAll(" ").trim();

        // 4) Split
        String[] raw = SPLIT.split(s);
        List<String> out = new ArrayList<>(raw.length);

        for (String r : raw) {
            if (r == null) continue;
            String tok = r.trim();
            if (tok.isEmpty()) continue;

            // compact normalize: spf50 pa++++ -> tokens already split but keep numbers
            tok = applyFix(tok);
            tok = applySyn(tok);

            if (tok.contains(" ")) {
                for (String t2 : tok.split("\\s+")) {
                    String t3 = t2.trim();
                    if (!t3.isEmpty()) out.add(t3);
                }
            } else {
                out.add(tok);
            }
        }

        // 5) Cleanup
        List<String> cleaned = new ArrayList<>();
        for (String t : out) {
            if (t.isBlank()) continue;
            if (STOP.contains(t)) continue;
            // keep 1-char only if meaningful
            if (t.length() == 1 && !(t.equals("c") || t.equals("g"))) continue;
            cleaned.add(t);
        }

        // 6) Deduplicate lightly (preserve order)
        return new ArrayList<>(new LinkedHashSet<>(cleaned));
    }

    private static String applyFix(String tok) {
        String fixed = FIX.get(tok);
        if (fixed != null) return fixed;

        // extra compact forms
        if (tok.equals("spf50") || tok.equals("spf50plus") || tok.equals("spf50+")) return "spf 50";
        if (tok.equals("pa++++") || tok.equals("pa+++")) return "pa";
        if (tok.equals("maskfit")) return "mask fit";
        return tok;
    }

    private static String applySyn(String tok) {
        String syn = SYN.get(tok);
        return syn != null ? syn : tok;
    }

    private static String normalizeUzApostrophes(String s) {
        // normalize many apostrophe variants
        s = s.replace("’", "'")
                .replace("‘", "'")
                .replace("`", "'")
                .replace("´", "'");

        // o‘ / g‘
        s = s.replace("o‘", "o").replace("o'", "o");
        s = s.replace("g‘", "g").replace("g'", "g");

        // common words
        s = s.replace("ko‘z", "koz").replace("ko'z", "koz");
        return s;
    }

    private static String transliterateCyrillic(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            sb.append(CYR_TO_LAT.getOrDefault(c, String.valueOf(c)));
        }
        return sb.toString();
    }

    private static String removeDiacritics(String s) {
        String norm = Normalizer.normalize(s, Normalizer.Form.NFKD);
        StringBuilder sb = new StringBuilder(norm.length());
        for (int i = 0; i < norm.length(); i++) {
            char ch = norm.charAt(i);
            if (Character.getType(ch) == Character.NON_SPACING_MARK) continue;
            sb.append(ch);
        }
        return sb.toString();
    }

    private static void putFix(String from, String to) {
        if (from == null || to == null) return;
        FIX.put(from.toLowerCase(Locale.ROOT), to.toLowerCase(Locale.ROOT));
    }

    private static void putSyn(String from, String to) {
        if (from == null || to == null) return;
        SYN.put(from.toLowerCase(Locale.ROOT), to.toLowerCase(Locale.ROOT));
    }

    private static String safeLower(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).trim();
    }
}