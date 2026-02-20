package com.example.backend;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/**
 * SearchNormalizer (ZAVEN / cosmetics)
 *
 * ✅ Maqsad:
 *  - Uzbek (lotin), rus (kiril), inglizcha querylarni bir xil ko‘rinishga keltirish
 *  - sinonimlar: penka/foam/cleanser, atir/parfyum/perfume, kushon/cushion, tonal/tonalka, SPF, mist/toner...
 *  - brend variantlari va typo fix
 *  - 18 ta siz bergan product uchun "tez topish" (token-index + scoring)
 *
 * Qanday ishlatish:
 *  - SearchNormalizer.normalize("Роундлаб докдо пенка") -> "round lab 1025 dokdo cleanser"
 *  - SearchNormalizer.search("dokdo penka", 10) -> top natijalar
 */
public final class SearchNormalizer {

    // -----------------------------
    // Public DTO
    // -----------------------------
    public static final class SearchResult {
        public final int id;
        public final String title;
        public final double score;
        public final List<String> matched;

        public SearchResult(int id, String title, double score, List<String> matched) {
            this.id = id;
            this.title = title;
            this.score = score;
            this.matched = matched;
        }

        @Override public String toString() {
            return "SearchResult{id=" + id + ", score=" + score + ", title='" + title + "', matched=" + matched + "}";
        }
    }

    // -----------------------------
    // Internal Product Entry
    // -----------------------------
    private static final class ProductEntry {
        final int id;
        final String type;   // SERUM / CREAM / CLEANSER / TONER / MAKEUP / COSMETICS / SKINCARE...
        final String title;  // siz bergan “Title”
        final Set<String> tokens;      // asosiy tokenlar
        final Set<String> strongTokens; // kuchli tokenlar (brand/line/spf/retinol...) ko‘proq ball beradi

        ProductEntry(int id, String type, String title, Set<String> tokens, Set<String> strongTokens) {
            this.id = id;
            this.type = type;
            this.title = title;
            this.tokens = tokens;
            this.strongTokens = strongTokens;
        }
    }

    // -----------------------------
    // Fix & Synonyms
    // -----------------------------
    // 1) bir so‘zlik typo/variant -> canonical token
    private static final Map<String, String> FIX = new HashMap<>();

    // 2) sinonimlar: query token -> canonical token (bir nechta token ham bo‘lishi mumkin)
    //    Masalan: "yuvish" -> "cleanser", "tonalka" -> "cushion", "kushon" -> "cushion"
    private static final Map<String, String> SYN = new HashMap<>();

    // 3) stopwords (ball bermaydigan / shovqin so‘zlar)
    private static final Set<String> STOP = new HashSet<>();

    // 4) Cyrillic -> Latin (RU + UZ Cyrillic)
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
            // UZ Cyrillic (ko‘p uchraydigan)
            Map.entry('қ',"q"), Map.entry('ў',"o"), Map.entry('ғ',"g"),
            Map.entry('ҳ',"h"), Map.entry('ң',"n")
    );

    // Regex split: hamma harf/raqam emas narsalarni bo‘luvchi qilamiz
    private static final Pattern SPLIT = Pattern.compile("[^a-z0-9]+");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    // -----------------------------
    // Product Index
    // -----------------------------
    private static final List<ProductEntry> PRODUCTS = new ArrayList<>();

    // token -> productIds (tez topish)
    private static final Map<String, Set<Integer>> INVERTED = new HashMap<>();
    private static final Map<Integer, ProductEntry> BY_ID = new HashMap<>();

    // -----------------------------
    // Static init
    // -----------------------------
    static {
        // -------- FIX (brend / yozilish variantlari) --------
        putFix("roundlab", "round lab");
        putFix("round", "round lab"); // ehtiyot: umumiy, lekin sizda Round Lab ko‘p
        putFix("dokdo", "1025 dokdo");
        putFix("dokdo1025", "1025 dokdo");
        putFix("tirtir", "tirtir");
        putFix("tir tir", "tirtir");
        putFix("skin1004", "skin1004");
        putFix("скин1004", "skin1004");
        putFix("centella", "centella");
        putFix("센텔라", "centella");
        putFix("axisy", "axis-y");
        putFix("axis", "axis-y");
        putFix("althea", "dr althea");
        putFix("drjart", "dr jart");
        putFix("dr.jart", "dr jart");
        putFix("aromatica", "aromatica");
        putFix("winkgirl", "wink girl");
        putFix("wink", "wink girl");
        putFix("dermashare", "dermashare");
        putFix("derma", "dermashare"); // sizda DermaShare bor
        putFix("polmedison", "polmedison");

        // -------- SYN (UZ/RU/ENG sinonimlar) --------
        // perfume
        putSyn("atir", "parfum");
        putSyn("attar", "parfum");
        putSyn("parfyum", "parfum");
        putSyn("perfume", "parfum");
        putSyn("fragrance", "parfum");
        putSyn("bombshell", "bombshell");

        // cleanser / foam / penka
        putSyn("penka", "cleanser");
        putSyn("пенка", "cleanser");
        putSyn("foam", "cleanser");
        putSyn("cleanser", "cleanser");
        putSyn("yuvish", "cleanser");
        putSyn("yuvishpenka", "cleanser");
        putSyn("facewash", "cleanser");
        putSyn("washing", "cleanser");

        // cream
        putSyn("krem", "cream");
        putSyn("крем", "cream");
        putSyn("cream", "cream");
        putSyn("moisturizer", "cream");

        // serum / essence / ampoule
        putSyn("serum", "serum");
        putSyn("сыворотка", "serum");
        putSyn("essence", "essence");
        putSyn("ampoule", "ampoule");

        // toner / mist
        putSyn("toner", "toner");
        putSyn("tonik", "toner");
        putSyn("тоник", "toner");
        putSyn("mist", "mist");
        putSyn("mister", "mist");
        putSyn("спрей", "mist");
        putSyn("spray", "mist");

        // sunscreen / spf
        putSyn("spf", "sunscreen");
        putSyn("pa", "sunscreen");
        putSyn("sunscreen", "sunscreen");
        putSyn("suncream", "sunscreen");
        putSyn("sun", "sunscreen");
        putSyn("quyosh", "sunscreen");
        putFix("axis-y", "axis y");
        putFix("axisy", "axis y");
        putFix("аксис", "axis"); // xohlasang

        // cushion / makeup
        putSyn("kushon", "cushion");
        putSyn("кушон", "cushion");
        putSyn("cushion", "cushion");
        putSyn("tonalka", "cushion");
        putSyn("тоналка", "cushion");
        putSyn("foundation", "foundation");
        putSyn("тон", "foundation");
        putSyn("maskara", "mascara");
        putSyn("туш", "mascara");
        putSyn("mascara", "mascara");

        // ingredient tokens
        putSyn("retinal", "retinal");
        putSyn("retinol", "retinol");
        putSyn("vitaminc", "vitamin c");
        putSyn("vitamin", "vitamin");
        putSyn("glutathione", "glutathione");
        putSyn("teatree", "tea tree");
        putSyn("cica", "cica");
        putSyn("centella", "centella");
        putSyn("rosemary", "rosemary");

        // -------- STOPWORDS --------
        Collections.addAll(STOP,
                "uchun","bilan","ham","va","yoki","bu","shu","qanday","nima","nechi","ml","g",
                "yuz","teri","koz","ko‘z","atrofi","qarshi","mos","koreys","koreya","import",
                "sifatli","original","parvarish","kuchli","yengil","kundalik","kunduzi","kechqurun",
                "ideal","turi","hajmi","asosiy","brend","mahsulot","mahsulotlar","toifa"
        );

        // -----------------------------
        // YOUR 18 PRODUCTS (hardcoded index)
        // -----------------------------
        // Eslatma: 14-16 bir xil title, lekin siz ro‘yxatda 3 marta bergansiz -> ID bo‘yicha alohida entry qoldirdim.
        addProduct(1,  "SERUM",     "Retinol Roll-On Eye Serum (30 ml) – ko‘z atrofi ajinlarga qarshi serum",
                "retinol roll on eye serum 30 ml ko‘z atrof ajin qora doira shish");
        addProduct(2,  "CREAM",     "Polmedison Glutathione + Vitamin C Eye Cream (20 ml) – qora doiralar va ajinlarga qarshi krem",
                "polmedison glutathione vitamin c eye cream 20 ml qora doira ajin brightening");
        addProduct(3,  "CREAM",     "DermaShare Vegan Waterful Vitamin Sun Cream SPF50+ PA++++ (50 g) – quyosh kremi",
                "dermashare vegan waterful vitamin sun cream spf50 pa++++ 50g sunscreen");
        addProduct(4,  "SKINCARE",  "DermaShare Tea Tree Acne pH Balance Foam Cleanser (150 ml) – aknega qarshi penka",
                "dermashare tea tree acne ph balance foam cleanser 150 ml akne husnbuzar");
        addProduct(5,  "CLEANSER",  "Round Lab 1025 Dokdo Cleanser (150 ml) – pH-balansli yuz yuvish penkasi",
                "round lab 1025 dokdo cleanser 150 ml penka ph");
        addProduct(6,  "CREAM",     "Round Lab 1025 Dokdo Cream (80 ml) – pH-balansli namlovchi yuz kremi",
                "round lab 1025 dokdo cream 80 ml moisturizer namlovchi");
        addProduct(7,  "SERUM",     "Aromatica Rosemary Root Enhancer (100 ml) – scalp essence spray",
                "aromatica rosemary root enhancer 100 ml scalp essence spray haircare soch ildiz");
        addProduct(8,  "MAKEUP",    "Wink Girl Idol Volume Mascara (10 ml) – kirpik hajm beruvchi tush",
                "wink girl idol volume mascara 10 ml туш maskara kirpik");
        addProduct(9,  "CLEANSER",  "Round Lab Birch Juice Cleanser (150 ml) – namlovchi yuz yuvish penkasi",
                "round lab birch juice cleanser 150 ml namlovchi");
        addProduct(10, "COSMETICS", "Come Inside Me Bombshell – ayollar uchun parfum / atir",
                "come inside me bombshell parfum atir ayollar fragrance");
        addProduct(11, "COSMETICS", "TIRTIR Mask Fit Red Cushion SPF40 PA++ – cushion / tonal",
                "tirtir mask fit red cushion spf40 pa++ kushon tonalka foundation");
        addProduct(12, "COSMETICS", "Centella mini nabor ko‘k rangli – set / kit",
                "centella mini nabor kok rangli set kit skin1004 madagascar");
        addProduct(13, "COSMETICS", "Centella mini nabor ko‘k rangli – namlantiruvchi tinchlantiruvchi set",
                "centella mini nabor kok rangli set kit namlantiruvchi tinchlantiradi");
        addProduct(14, "CREAM",     "SKIN1004 Madagascar Centella Cream – sariq krem (namlantiruvchi)",
                "skin1004 madagascar centella cream sariq krem cica barrier");
        addProduct(15, "CREAM",     "SKIN1004 Madagascar Centella Cream – sariq krem (namlantiruvchi)",
                "skin1004 madagascar centella cream sariq krem cica barrier");
        addProduct(16, "CREAM",     "SKIN1004 Madagascar Centella Cream – sariq krem (namlantiruvchi)",
                "skin1004 madagascar centella cream sariq krem cica barrier");
        addProduct(17, "SKINCARE",  "AXIS-Y foam cleanser – ugrili va yog‘li teri uchun penka",
                "axis-y cleanser penka ugri yogli teri pores");
        addProduct(18, "TONER",     "Dr. Althea 345 Relief Cream Mist – toner + mist (spray)",
                "dr althea 345 relief cream mist toner spray panthenol hyaluronic");

        // build inverted index
        buildIndex();
    }

    private SearchNormalizer() {}

    // -----------------------------
    // Public API
    // -----------------------------

    /** Faqat query normalize (DB LIKE, fulltext, va h.k. uchun) */
    public static String normalize(String q) {
        List<String> tokens = normalizeToTokens(q);
        return String.join(" ", tokens);
    }

    /**
     * Tez search:
     * - query tokenlari bo‘yicha candidate IDs topiladi (inverted index)
     * - scoring: exact/strong/prefix
     * - top N natija
     */
    public static List<SearchResult> search(String query, int limit) {
        if (limit <= 0) limit = 10;

        List<String> qTokens = normalizeToTokens(query);
        if (qTokens.isEmpty()) return List.of();

        // Candidate IDs: tokenlar bo‘yicha union
        Set<Integer> candidates = new LinkedHashSet<>();
        for (String t : qTokens) {
            Set<Integer> ids = INVERTED.get(t);
            if (ids != null) candidates.addAll(ids);
        }

        // Agar umuman topilmasa: "prefix fallback" (masalan: "dok" -> dokdo)
        if (candidates.isEmpty()) {
            for (String qt : qTokens) {
                for (String key : INVERTED.keySet()) {
                    if (key.startsWith(qt) || qt.startsWith(key)) {
                        candidates.addAll(INVERTED.getOrDefault(key, Set.of()));
                    }
                }
            }
        }

        if (candidates.isEmpty()) return List.of();

        List<SearchResult> results = new ArrayList<>();
        for (int id : candidates) {
            ProductEntry p = BY_ID.get(id);
            if (p == null) continue;

            Score sc = scoreProduct(p, qTokens);
            if (sc.score > 0) {
                results.add(new SearchResult(p.id, p.title, sc.score, sc.matched));
            }
        }

        results.sort((a, b) -> {
            int c = Double.compare(b.score, a.score);
            if (c != 0) return c;
            return Integer.compare(a.id, b.id);
        });

        if (results.size() > limit) return results.subList(0, limit);
        return results;
    }

    /** ID bo‘yicha product title olish (debug) */
    public static String titleById(int id) {
        ProductEntry p = BY_ID.get(id);
        return p == null ? null : p.title;
    }

    // -----------------------------
    // Scoring
    // -----------------------------
    private static final class Score {
        final double score;
        final List<String> matched;
        Score(double score, List<String> matched) {
            this.score = score;
            this.matched = matched;
        }
    }

    private static Score scoreProduct(ProductEntry p, List<String> qTokens) {
        double score = 0.0;
        List<String> matched = new ArrayList<>();

        // bonus: queryda "type" bo‘lsa (cream/cleanser/serum/toner/parfum/mascara/cushion)
        for (String qt : qTokens) {
            if (qt.equalsIgnoreCase(p.type.toLowerCase())) score += 2.0;
        }

        for (String qt : qTokens) {
            if (STOP.contains(qt)) continue;

            if (p.strongTokens.contains(qt)) {
                score += 6.0;
                matched.add(qt);
                continue;
            }
            if (p.tokens.contains(qt)) {
                score += 3.0;
                matched.add(qt);
                continue;
            }

            // prefix match (masalan: "dok" -> "dokdo", "centel" -> "centella")
            String best = bestPrefixMatch(qt, p.strongTokens);
            if (best != null) {
                score += 3.5;
                matched.add(qt + "→" + best);
                continue;
            }
            best = bestPrefixMatch(qt, p.tokens);
            if (best != null) {
                score += 1.5;
                matched.add(qt + "→" + best);
            }
        }

        // Agar query tokenlarining katta qismi topilgan bo‘lsa qo‘shimcha bonus
        int meaningful = 0;
        for (String qt : qTokens) if (!STOP.contains(qt)) meaningful++;
        if (meaningful > 0) {
            double hitRatio = Math.min(1.0, matched.size() / (double) meaningful);
            score += hitRatio * 2.0;
        }

        return new Score(score, matched);
    }

    private static String bestPrefixMatch(String q, Set<String> tokens) {
        if (q.length() < 3) return null;
        String best = null;
        for (String t : tokens) {
            if (t.startsWith(q) || q.startsWith(t)) {
                if (best == null || t.length() > best.length()) best = t;
            }
        }
        return best;
    }

    // -----------------------------
    // Index building
    // -----------------------------
    private static void addProduct(int id, String type, String title, String extraKeywords) {
        String base = (type + " " + title + " " + extraKeywords);
        List<String> toks = normalizeToTokens(base);

        Set<String> tokenSet = new LinkedHashSet<>(toks);

        // strong tokens: brendlar/line/ingredient/spf va “model” sonlari
        Set<String> strong = new LinkedHashSet<>();
        for (String t : tokenSet) {
            if (isStrongToken(t)) strong.add(t);
        }

        ProductEntry p = new ProductEntry(id, type, title, tokenSet, strong);
        PRODUCTS.add(p);
        BY_ID.put(id, p);
    }

    private static boolean isStrongToken(String t) {
        if (t == null || t.isBlank()) return false;

        // brend/line
        if (t.equals("round") || t.equals("lab") || t.equals("roundlab") || t.equals("roundlab")) return true;
        if (t.equals("round") || t.equals("lab")) return true; // round lab split
        if (t.equals("dermashare") || t.equals("polmedison") || t.equals("aromatica") || t.equals("tirtir")
                || t.equals("skin1004") || t.equals("axis-y") || t.equals("althea") || t.equals("dr")) return true;

        // ingredient / category
        if (t.equals("retinol") || t.equals("retinal") || t.equals("glutathione") || t.equals("vitamin")
                || t.equals("c") || t.equals("vitamin c") || t.equals("tea") || t.equals("tree")
                || t.equals("centella") || t.equals("cica") || t.equals("rosemary")) return true;

        // SPF / PA / line numbers
        if (t.startsWith("spf") || t.equals("pa") || t.equals("pa++") || t.equals("pa++++")) return true;
        if (t.equals("1025") || t.equals("345")) return true;

        // product identity words
        if (t.equals("dokdo") || t.equals("birch") || t.equals("juice") || t.equals("mask") || t.equals("fit")
                || t.equals("cushion") || t.equals("mascara") || t.equals("bombshell")) return true;

        // uzun tokenlar (ko‘proq signal)
        return t.length() >= 8;
    }

    private static void buildIndex() {
        INVERTED.clear();
        for (ProductEntry p : PRODUCTS) {
            for (String t : p.tokens) {
                if (STOP.contains(t)) continue;
                INVERTED.computeIfAbsent(t, k -> new LinkedHashSet<>()).add(p.id);
            }
            // strongTokens ham indexga kiritiladi (baribir tokens ichida bo‘ladi, lekin shunaqa ham ok)
            for (String t : p.strongTokens) {
                if (STOP.contains(t)) continue;
                INVERTED.computeIfAbsent(t, k -> new LinkedHashSet<>()).add(p.id);
            }
        }
    }

    // -----------------------------
    // Normalization pipeline
    // -----------------------------
    private static List<String> normalizeToTokens(String input) {
        if (input == null) return List.of();

        String s = input.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return List.of();

        // 0) Uzbek apostrophes normalization (o‘ g‘ etc.)
        s = normalizeUzApostrophes(s);

        // 1) Cyrillic -> latin (ru/uz cyr)
        s = transliterateCyrillic(s);

        // 2) Unicode diacritics remove (NFKD)
        s = removeDiacritics(s);

        // 3) replace some separators with space
        s = s.replace('&', ' ');
        s = s.replace('+', ' ');
        s = s.replace('/', ' ');
        s = s.replace('_', ' ');
        s = MULTI_SPACE.matcher(s).replaceAll(" ").trim();

        // 4) split to raw tokens
        String[] raw = SPLIT.split(s);
        List<String> out = new ArrayList<>(raw.length);

        for (String r : raw) {
            if (r == null) continue;
            String tok = r.trim();
            if (tok.isEmpty()) continue;

            // 5) apply FIX (single token or known compact forms)
            tok = applyFix(tok);

            // 6) apply SYN
            tok = applySyn(tok);

            // 7) split again if mapping produced spaces (e.g. "round lab", "vitamin c")
            if (tok.contains(" ")) {
                for (String t2 : tok.split("\\s+")) {
                    String t3 = t2.trim();
                    if (!t3.isEmpty()) out.add(t3);
                }
            } else {
                out.add(tok);
            }
        }

        // 8) final cleanup: remove stopwords and 1-char noise (except c / g maybe)
        List<String> cleaned = new ArrayList<>();
        for (String t : out) {
            if (t.isBlank()) continue;
            if (STOP.contains(t)) continue;
            if (t.length() == 1 && !(t.equals("c") || t.equals("g"))) continue;
            cleaned.add(t);
        }

        return cleaned;
    }

    private static String applyFix(String tok) {
        String fixed = FIX.get(tok);
        if (fixed != null) return fixed;

        // Ba’zi “yopishib yozilgan” holatlar:
        // "beautyofjoseon" -> "beauty of joseon" (siz keyin qo‘shishingiz mumkin)
        // hozir minimal:
        if (tok.equals("roundlab")) return "round lab";
        if (tok.equals("maskfit")) return "mask fit";
        return tok;
    }

    private static String applySyn(String tok) {
        String syn = SYN.get(tok);
        return syn != null ? syn : tok;
    }

    private static String normalizeUzApostrophes(String s) {
        // o‘, g‘, shuningdek turli apostrof turlari: ’ ‘ ` ´
        return s
                .replace("o‘", "o")
                .replace("o'", "o")
                .replace("o’", "o")
                .replace("g‘", "g")
                .replace("g'", "g")
                .replace("g’", "g")
                .replace("ko‘z", "koz")
                .replace("ko'z", "koz")
                .replace("ko’z", "koz");
    }

    private static String transliterateCyrillic(String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            sb.append(CYR_TO_LAT.getOrDefault(c, String.valueOf(c)));
        }
        return sb.toString();
    }

    private static String removeDiacritics(String s) {
        // NFKD -> combining marks remove
        String norm = Normalizer.normalize(s, Normalizer.Form.NFKD);
        StringBuilder sb = new StringBuilder(norm.length());
        for (int i = 0; i < norm.length(); i++) {
            char ch = norm.charAt(i);
            // skip diacritic marks
            if (Character.getType(ch) == Character.NON_SPACING_MARK) continue;
            sb.append(ch);
        }
        return sb.toString();
    }

    // -----------------------------
    // Helpers to fill maps
    // -----------------------------
    private static void putFix(String from, String to) {
        if (from == null || to == null) return;
        FIX.put(from.toLowerCase(Locale.ROOT), to.toLowerCase(Locale.ROOT));
    }

    private static void putSyn(String from, String to) {
        if (from == null || to == null) return;
        SYN.put(from.toLowerCase(Locale.ROOT), to.toLowerCase(Locale.ROOT));
    }
}
