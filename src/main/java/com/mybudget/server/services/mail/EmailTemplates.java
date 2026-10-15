package com.mybudget.server.services.mail;


import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;


@Component
public class EmailTemplates {
    public record Mail (String subject, String html) {}

    private  record  Texts(String subject, String greeting, String body, String button, String footer){}

    public Mail activation(String lang, String firstName, String link) {
        Texts t = switch (normalize(lang)) {
            case "ar" -> new Texts(
                    "فعّل حسابك في MyBudget",
                    "مرحباً",
                    "شكراً لتسجيلك. أكّد بريدك الإلكتروني لتفعيل حسابك:",
                    "تفعيل الحساب",
                    "الرابط صالح لمدة 24 ساعة. إذا لم تنشئ حساباً فتجاهل هذه الرسالة.");
            case "sv" -> new Texts(
                    "Aktivera ditt MyBudget-konto",
                    "Hej",
                    "Tack för att du registrerade dig. Bekräfta din e-postadress för att aktivera kontot:",
                    "Aktivera konto",
                    "Länken gäller i 24 timmar. Om du inte skapade ett konto kan du ignorera det här mejlet.");
            default -> new Texts(
                    "Activate your MyBudget account",
                    "Hi",
                    "Thanks for signing up. Confirm your email address to activate your account:",
                    "Activate account",
                    "This link expires in 24 hours. If you didn't create an account, you can ignore this email.");
        };
        return new Mail(t.subject(), layout(normalize(lang), firstName, t, link));
    }

    public Mail passwordReset(String lang, String firstName, String link) {
        Texts t = switch (normalize(lang)) {
            case "ar" -> new Texts(
                    "إعادة تعيين كلمة المرور في MyBudget",
                    "مرحباً",
                    "وصلنا طلب لإعادة تعيين كلمة المرور الخاصة بك. اضغط على الزر لاختيار كلمة مرور جديدة:",
                    "إعادة تعيين كلمة المرور",
                    "الرابط صالح لمدة ساعة واحدة. إذا لم تطلب ذلك فتجاهل هذه الرسالة وستبقى كلمة مرورك كما هي.");
            case "sv" -> new Texts(
                    "Återställ ditt MyBudget-lösenord",
                    "Hej",
                    "Vi har fått en begäran om att återställa ditt lösenord. Klicka på knappen för att välja ett nytt:",
                    "Återställ lösenord",
                    "Länken gäller i 1 timme. Om du inte begärde detta kan du ignorera mejlet, ditt lösenord ändras inte.");
            default -> new Texts(
                    "Reset your MyBudget password",
                    "Hi",
                    "We received a request to reset your password. Click the button to choose a new one:",
                    "Reset password",
                    "This link expires in 1 hour. If you didn't request this, you can ignore this email and your password will stay the same.");
        };
        return new Mail(t.subject(), layout(normalize(lang), firstName, t, link));
    }

    private static String normalize(String lang){
        if(lang == null) return "en";
        String l = lang.toLowerCase();
        return (l.equals("ar") || l.equals("sv")) ? l : "en";
    }

    private static String layout(String lang, String firstName, Texts t, String link) {
        String dir = "ar".equals(lang) ? "rtl" : "ltr";
        String name = (firstName == null || firstName.isBlank()) ? "" : " " + HtmlUtils.htmlEscape(firstName.trim());
        String safeLink = HtmlUtils.htmlEscape(link);
        return """
                <div dir="%s" style="font-family:Segoe UI,Tahoma,Arial,sans-serif;max-width:520px;margin:0 auto;padding:24px;color:#1c2420;line-height:1.7">
                  <h2 style="color:#1f6f5c;margin:0 0 16px">MyBudget</h2>
                  <p>%s%s,</p>
                  <p>%s</p>
                  <p style="margin:28px 0"><a href="%s" style="background:#1f6f5c;color:#ffffff;padding:12px 22px;border-radius:6px;text-decoration:none;display:inline-block">%s</a></p>
                  <p style="color:#5b665f;font-size:14px">%s</p>
                  <p style="color:#5b665f;font-size:13px;word-break:break-all;direction:ltr">%s</p>
                </div>
                """.formatted(dir, t.greeting(), name, t.body(), safeLink, t.button(), t.footer(), safeLink);
    }
}
