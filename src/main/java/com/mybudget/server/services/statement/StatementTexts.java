package com.mybudget.server.services.statement;

import java.util.Map;

// The words of the statement in the user's language (ar, en, sv); Arabic when unknown
public final class StatementTexts {

    private StatementTexts() {
    }

    private static final Map<String, Map<String, String>> TEXTS = Map.of(
            "ar", Map.ofEntries(
                    Map.entry("title", "كشف حساب"),
                    Map.entry("printed", "طُبع بتاريخ"),
                    Map.entry("currency", "العملة"),
                    Map.entry("period", "الفترة"),
                    Map.entry("previous", "رصيد سابق"),
                    Map.entry("final", "رصيد نهائي"),
                    Map.entry("date", "تاريخ الحركة"),
                    Map.entry("amount", "المبلغ"),
                    Map.entry("balance", "الرصيد"),
                    Map.entry("description", "البيان"),
                    Map.entry("operation", "العملية"),
                    Map.entry("previousRow", "الرصيد السابق"),
                    Map.entry("totalIn", "مجموع الوارد"),
                    Map.entry("totalOut", "مجموع الصادر"),
                    Map.entry("weAreOwed", "لنا عندكم"),
                    Map.entry("weOwe", "لكم عندنا"),
                    Map.entry("cancelled", "ملغاة"),
                    Map.entry("reason", "السبب"),
                    Map.entry("management", "الإدارة"),
                    Map.entry("page", "صفحة"),
                    Map.entry("of", "من"),
                    Map.entry("noTransactions", "لا توجد حركات في هذه الفترة"),
                    Map.entry("incomse", "دخل"),
                    Map.entry("expense", "مصروف"),
                    Map.entry("in-transfer", "تحويل وارد"),
                    Map.entry("out-transfer", "تحويل صادر"),
                    Map.entry("transfer-fee-in", "عمولة لنا"),
                    Map.entry("transfer-fee-out", "عمولة"),
                    Map.entry("debt-in", "دين وارد"),
                    Map.entry("debt-out", "دين صادر"),
                    Map.entry("to", "إلى"),
                    Map.entry("from", "من")),
            "en", Map.ofEntries(
                    Map.entry("title", "Account statement"),
                    Map.entry("printed", "Printed"),
                    Map.entry("currency", "Currency"),
                    Map.entry("period", "Period"),
                    Map.entry("previous", "Opening balance"),
                    Map.entry("final", "Closing balance"),
                    Map.entry("date", "Date"),
                    Map.entry("amount", "Amount"),
                    Map.entry("balance", "Balance"),
                    Map.entry("description", "Description"),
                    Map.entry("operation", "Operation"),
                    Map.entry("previousRow", "Opening balance"),
                    Map.entry("totalIn", "Total in"),
                    Map.entry("totalOut", "Total out"),
                    Map.entry("weAreOwed", "owed to us"),
                    Map.entry("weOwe", "we owe"),
                    Map.entry("cancelled", "cancelled"),
                    Map.entry("reason", "Reason"),
                    Map.entry("management", "Management"),
                    Map.entry("page", "Page"),
                    Map.entry("of", "of"),
                    Map.entry("noTransactions", "No transactions in this period"),
                    Map.entry("incomse", "Income"),
                    Map.entry("expense", "Expense"),
                    Map.entry("in-transfer", "Transfer in"),
                    Map.entry("out-transfer", "Transfer out"),
                    Map.entry("transfer-fee-in", "Fee received"),
                    Map.entry("transfer-fee-out", "Fee"),
                    Map.entry("debt-in", "Debt in"),
                    Map.entry("debt-out", "Debt out"),
                    Map.entry("to", "To"),
                    Map.entry("from", "From")),
            "sv", Map.ofEntries(
                    Map.entry("title", "Kontoutdrag"),
                    Map.entry("printed", "Utskrivet"),
                    Map.entry("currency", "Valuta"),
                    Map.entry("period", "Period"),
                    Map.entry("previous", "Ingående saldo"),
                    Map.entry("final", "Utgående saldo"),
                    Map.entry("date", "Datum"),
                    Map.entry("amount", "Belopp"),
                    Map.entry("balance", "Saldo"),
                    Map.entry("description", "Beskrivning"),
                    Map.entry("operation", "Transaktion"),
                    Map.entry("previousRow", "Ingående saldo"),
                    Map.entry("totalIn", "Summa in"),
                    Map.entry("totalOut", "Summa ut"),
                    Map.entry("weAreOwed", "ni är skyldiga oss"),
                    Map.entry("weOwe", "vi är skyldiga er"),
                    Map.entry("cancelled", "annullerad"),
                    Map.entry("reason", "Orsak"),
                    Map.entry("management", "Ledningen"),
                    Map.entry("page", "Sida"),
                    Map.entry("of", "av"),
                    Map.entry("noTransactions", "Inga transaktioner under perioden"),
                    Map.entry("incomse", "Inkomst"),
                    Map.entry("expense", "Utgift"),
                    Map.entry("in-transfer", "Överföring in"),
                    Map.entry("out-transfer", "Överföring ut"),
                    Map.entry("transfer-fee-in", "Avgift in"),
                    Map.entry("transfer-fee-out", "Avgift"),
                    Map.entry("debt-in", "Skuld in"),
                    Map.entry("debt-out", "Skuld ut"),
                    Map.entry("to", "Till"),
                    Map.entry("from", "Från"))
    );

    public static String language(String lang) {
        return lang != null && TEXTS.containsKey(lang) ? lang : "ar";
    }

    public static Map<String, String> of(String lang) {
        return TEXTS.get(language(lang));
    }

    public static boolean isRtl(String lang) {
        return "ar".equals(language(lang));
    }
}