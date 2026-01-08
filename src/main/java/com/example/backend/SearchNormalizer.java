package com.example.backend;

import java.util.*;

public class SearchNormalizer {

    // user variant/typo -> canonical token
    public static final Map<String, String> FIX = new HashMap<>();

    // canonical product tokens (400 ta atrofida)
    public static final Set<String> CANONICAL_PRODUCTS = new LinkedHashSet<>();

    static {
        // 1) Brand typo/variant fix (UZ/RU/ENG)
        // (xohlasangiz keyin yana ko‘paytiramiz)
        putFix("doir", "dior");
        putFix("dior", "dior");
        putFix("anua", "anua");
        putFix("ануа", "anua");
        putFix("roundlab", "round lab");
        putFix("round", "round lab"); // ehtiyot: juda umumiy bo‘lishi mumkin
        putFix("cosrx", "cosrx");
        putFix("косрх", "cosrx");
        putFix("boj", "beauty of joseon");
        putFix("beautyofjoseon", "beauty of joseon");
        putFix("torriden", "torriden");
        putFix("торриден", "torriden");
        putFix("skin1004", "skin1004");
        putFix("скин1004", "skin1004");
        putFix("mediheal", "mediheal");
        putFix("медихил", "mediheal");
        putFix("vt", "vt");
        putFix("aestura", "aestura");
        putFix("goodal", "goodal");
        putFix("manyo", "ma:nyo");
        putFix("ma:nyo", "ma:nyo");
        putFix("drjart", "dr jart");
        putFix("dr.jart", "dr jart");
        putFix("laneige", "laneige");
        putFix("innisfree", "innisfree");
        putFix("numbuzin", "numbuzin");
        putFix("medicube", "medicube");
        putFix("somebymi", "some by mi");
        putFix("some by mi", "some by mi");
        putFix("isntree", "isntree");
        putFix("haruharu", "haruharu wonder");
        putFix("purito", "purito");
        putFix("iunik", "iunik");
        putFix("benton", "benton");
        putFix("klairs", "klairs");
        putFix("pyunkangyul", "pyunkang yul");
        putFix("missha", "missha");
        putFix("etude", "etude");
        putFix("romand", "rom&nd");
        putFix("rom&nd", "rom&nd");
        putFix("peripera", "peripera");
        putFix("clio", "clio");
        putFix("hera", "hera");
        putFix("sulwhasoo", "sulwhasoo");
        putFix("iope", "iope");
        putFix("thefaceshop", "the face shop");
        putFix("banilaco", "banila co");
        putFix("heimish", "heimish");
        putFix("aprilskin", "aprilskin");
        putFix("dralthea", "dr althea");
        putFix("celimax", "celimax");
        putFix("rejuran", "rejuran");
        putFix("illiyoon", "illiyoon");
        putFix("roundlab", "round lab");
        putFix("skinfood", "skinfood");
        putFix("tirtir", "tirtir");

        // 2) Synonyms (UZ/RU/ENG) -> canonical tokens
        putFix("atir", "parfum");
        putFix("parfyum", "parfum");
        putFix("fragrance", "parfum");
        putFix("erkaklar", "men");
        putFix("ayollar", "women");
        putFix("krem", "cream");
        putFix("toner", "toner");
        putFix("serum", "serum");
        putFix("ampoule", "ampoule");
        putFix("essence", "essence");
        putFix("penka", "cleanser");
        putFix("yuvish", "cleanser");
        putFix("sunscreen", "sunscreen");
        putFix("spf", "sunscreen");
        putFix("mask", "mask");
        putFix("pad", "pad");

        // 3) 400 ta mahsulot tokenini avtomatik yasaymiz:
        // 50 brend × 8 “hero product/line”
        Map<String, List<String>> brandToProducts = new LinkedHashMap<>();

        // Bu yerda “hero”lar SNG/UZ bozorida eng ko‘p uchraydigan line nomlari:
        // Anua Heartleaf, Round Lab Dokdo/Birch, VT Reedle Shot, Aestura Atobarrier,
        // Beauty of Joseon Relief Sun, Torriden Dive-In, Mediheal pads, etc. :contentReference[oaicite:3]{index=3}

        brandToProducts.put("anua", List.of(
                "heartleaf 77 soothing toner", "heartleaf toner pad", "heartleaf cleansing foam",
                "niacinamide 10 txa 4 serum", "peach niacin serum", "pdrn + hyaluron serum",
                "heartleaf cream", "heartleaf clear pad"
        ));

        brandToProducts.put("round lab", List.of(
                "1025 dokdo toner", "1025 dokdo cleanser", "birch juice sunscreen",
                "birch juice moisturizer", "dokdo lotion", "dokdo ampoule",
                "birch toner", "dokdo sun cream"
        ));

        brandToProducts.put("cosrx", List.of(
                "advanced snail 96 mucin essence", "snail 92 all in one cream", "low ph good morning cleanser",
                "aha bha pha toner", "bha blackhead power liquid", "propolis synergy toner",
                "vitamin c serum", "pimple patch"
        ));

        brandToProducts.put("beauty of joseon", List.of(
                "relief sun rice probiotics spf50", "ginseng essence water", "glow serum propolis niacinamide",
                "revive eye serum ginseng retinal", "dynasty cream", "cleansing balm",
                "glow deep serum rice arbutin", "calming serum green tea panthenol"
        ));

        brandToProducts.put("torriden", List.of(
                "dive in serum", "dive in toner", "dive in cream",
                "dive in sheet mask", "solid in ceramide cream", "solid in essence",
                "balanceful cica serum", "lip essence"
        ));

        brandToProducts.put("skin1004", List.of(
                "madagascar centella ampoule", "centella toner", "centella foam cleanser",
                "hyalu cica sunscreen", "centella soothing cream", "poremizing clay mask",
                "brightening toner", "centella pad"
        ));

        brandToProducts.put("mediheal", List.of(
                "teatree care solution mask", "n.m.f aquaring mask", "collagen ampoule pad",
                "madecassoside blemish pad", "vitamin c brightening mask", "retinol collagen pad",
                "hyaluronate watermide mask", "cica toner pad"
        ));

        brandToProducts.put("vt", List.of(
                "reedle shot 100", "reedle shot 300", "reedle shot 700",
                "cica capsule mask", "cica cream", "pdrn reedle serum",
                "cica mild foam cleanser", "cica toner"
        ));

        brandToProducts.put("aestura", List.of(
                "atobarrier 365 cream", "atobarrier 365 lotion", "atobarrier 365 mist",
                "atobarrier toner", "atobarrier cleanser", "atobarrier essence",
                "atobarrier ampoule", "atobarrier body lotion"
        ));

        brandToProducts.put("goodal", List.of(
                "green tangerine vitamin c serum", "vitamin c toner pad", "vitamin c cream",
                "houttuynia calming toner", "calming moisturizing cream", "cleansing foam",
                "vitamin c eye cream", "vitamin c sheet mask"
        ));

        brandToProducts.put("ma:nyo", List.of(
                "pure cleansing oil", "bifida biome complex ampoule", "galactomy essence",
                "deep pore cleansing soda foam", "panthetoin cream", "bifida toner",
                "cleansing balm", "v collagen heart fit serum"
        ));

        // Qolgan 40 brend (har biriga 8 ta) — umumiy SNG/Uzb bozorida tez uchraydigan line’lar:
        brandToProducts.put("dr jart", List.of("cicapair tiger grass cream","cicapair serum","ceramidin cream","ceramidin toner","rubber mask","vital hydra solution mask","cicapair color correcting","ceramidin lip balm"));
        brandToProducts.put("laneige", List.of("water sleeping mask","lip sleeping mask","water bank cream","cream skin toner","neo cushion","water bank cleanser","radian-c cream","water bank serum"));
        brandToProducts.put("innisfree", List.of("green tea seed serum","green tea seed cream","volcanic pore mask","jeju cherry blossom cream","no sebum powder","green tea toner","retinol cica repair ampoule","sunscreen spf50"));
        brandToProducts.put("numbuzin", List.of("no.3 toner","no.5 vitamin serum","no.1 calming toner","no.3 serum","no.2 protein cream","no.5 toner pad","no.6 deep sleep mask","no.4 collagen cream"));
        brandToProducts.put("medicube", List.of("zero pore pad","zero pore serum","collagen jelly cream","triple collagen serum","age-r booster gel","red serum","deep vita c ampoule","pdrn pink peptide serum"));
        brandToProducts.put("some by mi", List.of("aha bha pha miracle toner","miracle serum","miracle cream","snail truecica toner","yuzu niacin serum","retinol intense serum","bye bye blackhead cleanser","cica peptide cream"));
        brandToProducts.put("isntree", List.of("hyaluronic acid toner","hyaluronic acid watery sun gel","green tea fresh toner","chestnut bha clear liquid","mugwort calming cream","onion newpair gel cream","yam root vegan milk cream","real rose calming mask"));
        brandToProducts.put("haruharu wonder", List.of("black rice toner","black rice serum","black rice cream","black rice sunscreen","centella sunscreen","cleansing gel","bakuchiol eye cream","mask pack"));
        brandToProducts.put("purito", List.of("centella unscented serum","deep sea water cream","b5 panthenol re-barrier cream","from green cleansing oil","oat-in calming gel cream","cica clearing bb cream","centella toner","sunscreen"));
        brandToProducts.put("iunik", List.of("tea tree relief serum","beta glucan daily moisture cream","propolis vitamin synergy serum","centella calming gel cream","cleansing oil","black snail restore serum","vitamin hyaluronic acid toner","sleeping mask"));
        brandToProducts.put("benton", List.of("snail bee high content essence","snail bee serum","aloe bha toner","fermentation essence","honest cleansing foam","deep green tea lotion","bakuchiol serum","sheet mask"));
        brandToProducts.put("klairs", List.of("supple preparation toner","freshly juiced vitamin drop","rich moist soothing cream","midnight blue calming cream","gentle black deep cleansing oil","soft airy uv essence","fundamental eye butter","sheet mask"));
        brandToProducts.put("pyunkang yul", List.of("essence toner","nutrition cream","ato cream blue label","calming moisture serum","low ph pore deep cleansing foam","mist toner","eye cream","peeling gel"));
        brandToProducts.put("missha", List.of("time revolution essence","time revolution ampoule","perfect cover bb cream","sun milk spf50","vita c plus serum","super aqua snail cream","cleansing foam","sheet mask"));
        brandToProducts.put("etude", List.of("soon jung 2x barrier cream","soon jung toner","soon jung foam cleanser","dear darling tint","fixing tint","double lasting foundation","sunprise sunscreen","lip balm"));
        brandToProducts.put("rom&nd", List.of("juicy lasting tint","glasting water tint","zero velvet tint","better than palette","han all fix mascara","nu zero cushion","lip mate pencil","tint blur"));
        brandToProducts.put("peripera", List.of("ink velvet","ink airy velvet","all take mood palette","ink mood glowy tint","mascara","cushion","lip liner","blush"));
        brandToProducts.put("clio", List.of("kill cover cushion","kill cover foundation","sharp so simple pencil liner","pro eye palette","mascara","fixer","concealer","blush"));
        brandToProducts.put("hera", List.of("black cushion","sensual spicy nude balm","foundation","uv protector","lip tint","setting powder","eye shadow","primer"));
        brandToProducts.put("sulwhasoo", List.of("first care activating serum","concentrated ginseng cream","ginseng renewing serum","overnight vitalizing mask","cleansing oil","snowise brightening","timtreasure","uv wise sunscreen"));
        brandToProducts.put("iope", List.of("bio essence","retinol expert","super vital cream","air cushion","stem iii ampoule","derma repair cica","cleanser","sun protector"));
        brandToProducts.put("the face shop", List.of("rice water bright cleansing oil","rice water foam cleanser","ink lasting cushion","jeju aloe gel","mask sheet","toner","cream","sunscreen"));
        brandToProducts.put("banila co", List.of("clean it zero cleansing balm","clean it zero purifying","clean it zero nourishing","cleansing balm brightening","foam cleanser","cleansing water","toner","cream"));
        brandToProducts.put("heimish", List.of("all clean balm","all clean green foam","bulgarian rose water toner","artless glow base","matcha biome ampoule","cleansing oil","cream","mask"));
        brandToProducts.put("aprilskin", List.of("real calendula peel off mask","carrotene ipmp toner","pink aloe pack","magic snow cushion","cleansing foam","serum","cream","sunscreen"));
        brandToProducts.put("dr althea", List.of("345 relief cream","azulene 147ha serum","cica mild cleanser","pro lab retinol","aqua peeling gel","soothing toner","sun cream","mask"));
        brandToProducts.put("celimax", List.of("noni ampoule","noni toner","noni cream","vita a retinal shot","dual barrier cream","jiwoogae heartleaf","cleanser","sunscreen"));
        brandToProducts.put("rejuran", List.of("healer turnover ampoule","healer toner","healer cream","cica recovery","pdrn ampoule","mask","eye cream","essence"));
        brandToProducts.put("illiyoon", List.of("ceramide ato concentrate cream","ceramide ato lotion","ceramide ato wash","ultra repair cream","body lotion","face cream","cleanser","sun cream"));
        brandToProducts.put("skinfood", List.of("carrot carotene calming water pad","royal honey propolis essence","black sugar mask","rice mask wash off","egg white pore foam","tomato toner","honey sugar scrub","carrot pad refill"));
        brandToProducts.put("tirtir", List.of("mask fit red cushion","mask fit tone up essence","mask fit foundation","milk skin toner","ceramide cream","ampoule","sunscreen","lip tint"));

        // Hali 50 ga yetkazish uchun yana 10 ta brend qo‘shamiz:
        brandToProducts.put("tonymoly", List.of("i'm real mask","wonder ceramide mocchi toner","chok chok green tea cream","banana hand milk","lip tint","cleanser","toner","cream"));
        brandToProducts.put("neogen", List.of("bio-peel gauze peeling wine","real ferment micro essence","dermalogy sunscreen","green tea foam cleanser","toner pad","serum","cream","mask"));
        brandToProducts.put("ahc", List.of("eye cream for face","essential real eye cream","hyaluronic toner","ampoule","sunscreen","mask","cleanser","cream"));
        brandToProducts.put("espoir", List.of("pro tailor foundation","be glow cushion","lip tint","eye palette","concealer","primer","setting powder","sunscreen"));
        brandToProducts.put("make p:rem", List.of("safe me relief moisture cream","safe me cleanser","safe me toner","uv defense me sunscreen","sleeping pack","serum","ampoule","mask"));
        brandToProducts.put("axis-y", List.of("dark spot correcting glow serum","artichoke intensive skin barrier ampoule","heartleaf my type calming cream","cleanser","toner","sunscreen","mask","peeling gel"));
        brandToProducts.put("dear, klairs", List.of("supple preparation toner","vitamin c serum","midnight blue cream","rich moist cream","cleansing oil","sunscreen","mask","eye butter"));
        brandToProducts.put("huxley", List.of("secret of sahara oil essence","conditioning essence","cream more than moist","sun cream","cleansing gel","mask","toner","serum"));
        brandToProducts.put("rire", List.of("silky & light sun milk","collagen ampoule","cleanser","toner","cream","mask","lip balm","serum"));
        brandToProducts.put("kahi", List.of("wrinkle bounce multi balm","multi balm refill","eye balm","sun stick","collagen balm","moisture balm","mask","cream"));

        // 4) CANONICAL_PRODUCTS ni yig‘amiz (≈ 50*8 = 400)
        for (var e : brandToProducts.entrySet()) {
            String brand = e.getKey();
            for (String prod : e.getValue()) {
                CANONICAL_PRODUCTS.add((brand + " " + prod).toLowerCase());
            }
        }
    }

    private static void putFix(String from, String to) {
        FIX.put(from.toLowerCase(), to.toLowerCase());
    }

    // ===============================
// CYRILLIC → LATIN TRANSLITERATION
// ===============================
    private static final Map<Character, String> CYR_TO_LAT = Map.ofEntries(
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
            Map.entry('қ',"q"), Map.entry('ў',"o"), Map.entry('ғ',"g"),
            Map.entry('ҳ',"h"), Map.entry('ң',"n")
    );

    private static String transliterate(String text) {
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            sb.append(CYR_TO_LAT.getOrDefault(c, String.valueOf(c)));
        }
        return sb.toString();
    }

    // Asosiy normalize: tokenlarni normal holatga keltiradi
    public static String normalize(String q) {
        if (q == null) return "";
        String normalized = transliterate(q.toLowerCase().trim());
        String[] parts = normalized.split("\\s+");

        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (FIX.containsKey(p)) parts[i] = FIX.get(p);
        }
        return String.join(" ", Arrays.stream(parts).filter(s -> !s.isBlank()).toList());
    }
}
