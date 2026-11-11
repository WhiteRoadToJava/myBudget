package com.mybudget.server.services.statement;

import com.mybudget.server.dto.Transaction;
import com.mybudget.server.exeptions.ResourceNotFoundException;
import com.mybudget.server.modules.Account;
import com.mybudget.server.modules.User;
import com.mybudget.server.repositories.AccountRepository;
import com.mybudget.server.services.account.AccountBalances;
import com.mybudget.server.services.account.AccountService;
import com.mybudget.server.util.UserUtils;
import com.openhtmltopdf.bidi.support.ICUBidiReorderer;
import com.openhtmltopdf.bidi.support.ICUBidiSplitter;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.helper.W3CDom;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

/**
 * The account statement as a PDF (one account, one currency, one period), like the ledgers
 * money transfer offices send: the newest movement first, the balance after each one, the
 * opening balance at the bottom. Rendered from templates/statement.html with an embedded
 * Arabic font, so Arabic letters are joined and right to left.
 */
@Service
@RequiredArgsConstructor
public class StatementService {

    private static final String ARABIC_FONT = "fonts/NotoNaskhArabic-Regular.ttf";
    private static final String ARABIC_FONT_BOLD = "fonts/NotoNaskhArabic-Bold.ttf";
    private static final String LATIN_FONT = "fonts/NotoSans-Regular.ttf";
    private static final String LATIN_FONT_BOLD = "fonts/NotoSans-Bold.ttf";
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MOMENT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final ZoneId ZONE = ZoneId.of("Asia/Damascus");

    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final UserUtils userUtils;
    private final TemplateEngine templateEngine;

    public byte[] statementPdf(String accountId, String currency, LocalDate from, LocalDate to, String lang) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("Choose a period: the start must be before the end");
        }
        User user = userUtils.getCurrentAuthenticatedUser();
        Account account = accountRepository.findByIdAndUser(accountId, user);
        if (account == null) {
            throw new ResourceNotFoundException("Account not found");
        }
        String code = AccountBalances.resolve(account, currency);
        double balanceNow = AccountBalances.find(account, code).map(p -> p.getTotalBalance()).orElse(0.0);
        Map<String, String> words = StatementTexts.of(lang);

        StatementCalculator.Result result = StatementCalculator.calculate(
                accountService.getAllAccountTransactions(account), code, from, to, balanceNow,
                t -> describe(t, account, words));

        Context context = new Context();
        context.setVariable("t", words);
        context.setVariable("dir", StatementTexts.isRtl(lang) ? "rtl" : "ltr");
        context.setVariable("lang", StatementTexts.language(lang));
        context.setVariable("owner", ownerName(user));
        context.setVariable("account", account.getName());
        context.setVariable("currency", code);
        context.setVariable("from", from.format(DAY));
        context.setVariable("to", to.format(DAY));
        context.setVariable("printed", LocalDateTime.now(ZONE).format(MOMENT));
        context.setVariable("opening", money(result.opening()));
        context.setVariable("openingSide", side(result.opening(), words));
        context.setVariable("closing", money(result.closing()));
        context.setVariable("closingSide", side(result.closing(), words));
        context.setVariable("totalIn", money(result.totalIn()));
        context.setVariable("totalOut", money(result.totalOut()));
        context.setVariable("openingDate", from.format(DAY));
        context.setVariable("rows", result.rows().stream().map(r -> Map.of(
                "date", r.date().format(DAY),
                "operation", words.getOrDefault(r.type(), r.type()) + (r.number() != null ? " " + r.number() : ""),
                "description", r.description() == null ? "" : r.description(),
                "amount", money(r.amount()),
                "incoming", r.incoming(),
                "balance", money(r.balance()),
                "cancelled", r.cancelled(),
                "reason", r.cancelReason() == null ? "" : r.cancelReason()
        )).toList());

        String html = templateEngine.process("statement", context);
        return render(html, StatementTexts.isRtl(lang));
    }

    // a file name the browser can save: "statement-USD-2026-11-01_2026-11-05.pdf" (no Arabic, no spaces)
    public static String fileName(String accountName, String currency, LocalDate from, LocalDate to) {
        String safe = accountName == null ? "account" : accountName.replaceAll("[^A-Za-z0-9_-]+", "-").replaceAll("^-+|-+$", "");
        return (safe.isEmpty() ? "statement" : safe) + "-" + currency + "-" + from + "_" + to + ".pdf";
    }

    private byte[] render(String html, boolean rtl) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            // Arabic: letters joined (shaping) and right to left (bidi)
            builder.useUnicodeBidiSplitter(new ICUBidiSplitter.ICUBidiSplitterFactory());
            builder.useUnicodeBidiReorderer(new ICUBidiReorderer());
            builder.defaultTextDirection(rtl ? BaseRendererBuilder.TextDirection.RTL : BaseRendererBuilder.TextDirection.LTR);
            font(builder, ARABIC_FONT, "Naskh", 400);
            font(builder, ARABIC_FONT_BOLD, "Naskh", 700);
            font(builder, LATIN_FONT, "NotoSans", 400);
            font(builder, LATIN_FONT_BOLD, "NotoSans", 700);
            // jsoup turns the HTML into the XML document the renderer needs
            builder.withW3cDocument(new W3CDom().fromJsoup(Jsoup.parse(html)), "/");
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not make the PDF", e);
        }
    }

    private static void font(PdfRendererBuilder builder, String path, String family, int weight) {
        builder.useFont(() -> {
            try {
                InputStream in = new ClassPathResource(path).getInputStream();
                return in;
            } catch (IOException e) {
                throw new UncheckedIOException("Missing font " + path, e);
            }
        }, family, weight, BaseRendererBuilder.FontStyle.NORMAL, true);
    }

    // the line of a row: its description, else where the money went or came from, else its type
    private static String describe(Transaction t, Account account, Map<String, String> words) {
        if (t.getDescription() != null && !t.getDescription().isBlank()) {
            return t.getDescription().trim();
        }
        if ("out-transfer".equals(t.getType()) && t.getDestinationAccount() != null) {
            return words.get("to") + " " + t.getDestinationAccount().getName();
        }
        if ("in-transfer".equals(t.getType()) && t.getSourceAccount() != null) {
            return words.get("from") + " " + t.getSourceAccount().getName();
        }
        return "";
    }

    private static String ownerName(User user) {
        String name = ((user.getFirstName() == null ? "" : user.getFirstName()) + " "
                + (user.getLastName() == null ? "" : user.getLastName())).trim();
        return name.isEmpty() ? user.getUsername() : name;
    }

    // a positive balance: the customer owes us ("لنا عندكم"), like the text statement
    private static String side(double balance, Map<String, String> words) {
        if (balance > 0) return words.get("weAreOwed");
        if (balance < 0) return words.get("weOwe");
        return "";
    }

    // 22,616.82 with Latin digits in every language
    private static String money(double value) {
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        return format.format(value);
    }
}