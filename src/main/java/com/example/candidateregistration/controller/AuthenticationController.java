package com.example.candidateregistration.controller;

import com.example.candidateregistration.dto.*;
import com.example.candidateregistration.service.AuthenticationService;
import com.example.candidateregistration.exception.InvalidRequestException;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {
    private final AuthenticationService authenticationService;
    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }
    @GetMapping("/me")
    public java.util.Map<String, String> profile(java.security.Principal principal) {
        return authenticationService.profile(principal.getName());
    }
    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authenticationService.signup(request));
    }
    @GetMapping(value = "/verify-email", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verify(@RequestParam String token) {
        try { return page(HttpStatus.OK, "Email verification", HtmlUtils.htmlEscape(authenticationService.verifyEmail(token))); }
        catch (InvalidRequestException e) { return page(HttpStatus.BAD_REQUEST, "Email verification", HtmlUtils.htmlEscape(e.getMessage())); }
    }
    // GET previews the decision; only an explicit POST consumes the token. Email scanners cannot approve/reject.
    @GetMapping(value = "/admin/review", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> review(@RequestParam String token, @RequestParam String decision) {
        try {
            if (!"approve".equals(decision) && !"reject".equals(decision))
                throw new InvalidRequestException("Invalid review decision");
            String email = authenticationService.reviewEmail(token);
            boolean approving = "approve".equals(decision);
            String form = "<p class=\"intro\">Review the account below and confirm your decision.</p>"
                    + "<div class=\"account\"><span class=\"account-label\">ADMIN ACCOUNT</span><p>"
                    + HtmlUtils.htmlEscape(email) + "</p></div><form method=\"post\" action=\"review\">"
                    + "<input type=\"hidden\" name=\"token\" value=\"" + HtmlUtils.htmlEscape(token) + "\">"
                    + "<input type=\"hidden\" name=\"decision\" value=\"" + decision + "\">"
                    + "<button class=\"decision-button " + (approving ? "approve" : "reject")
                    + "\" type=\"submit\">" + (approving ? "Confirm approval" : "Confirm rejection") + "</button></form>"
                    + "<p class=\"note\">Email verification is required before this admin can sign in.</p>";
            return page(HttpStatus.OK, "Review admin registration", form);
        } catch (InvalidRequestException e) { return page(HttpStatus.BAD_REQUEST, "Review admin registration", HtmlUtils.htmlEscape(e.getMessage())); }
    }
    @PostMapping(value = "/admin/review", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> decide(@RequestParam String token, @RequestParam String decision) {
        try { return page(HttpStatus.OK, "Admin registration", HtmlUtils.htmlEscape(authenticationService.reviewAdmin(token, decision))); }
        catch (InvalidRequestException e) { return page(HttpStatus.BAD_REQUEST, "Admin registration", HtmlUtils.htmlEscape(e.getMessage())); }
    }
    private ResponseEntity<String> page(HttpStatus status, String title, String content) {
        return ResponseEntity.status(status).contentType(MediaType.TEXT_HTML)
                .header("Cache-Control", "no-store").header("Referrer-Policy", "no-referrer")
                .body("<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>"
                    + HtmlUtils.htmlEscape(title) + "</title><style>" + PAGE_CSS + "</style></head><body>"
                    + "<main class=\"review-card" + (status.isError() ? " error-card" : "") + "\">"
                    + "<div class=\"brand\"><span class=\"brand-mark\" aria-hidden=\"true\">S</span>StarTech<span class=\"portal-label\">ADMIN PORTAL</span></div>"
                    + "<div class=\"card-content\"><span class=\"eyebrow\">ACCOUNT REGISTRATION</span><h1>"
                    + HtmlUtils.htmlEscape(title) + "</h1><div class=\"message\">" + content + "</div></div>"
                    + "<footer>StarTech &middot; Admin registration</footer></main></body></html>");
    }

    private static final String PAGE_CSS = """
            * { box-sizing: border-box; }
            body {
                margin: 0; min-height: 100vh; min-height: 100svh; padding: 32px 20px;
                display: grid; place-items: center; font-family: 'Segoe UI', Arial, sans-serif;
                color: #24304b; line-height: 1.6;
                background: radial-gradient(ellipse at 10% 15%, #b9e9ff 0%, transparent 48%),
                    radial-gradient(ellipse at 90% 85%, #f9c9e4 0%, transparent 48%),
                    linear-gradient(135deg, #e0e8ff, #eee4ff 55%, #fff0e6);
            }
            .review-card {
                width: 100%; max-width: 590px; background: #fff; border-radius: 24px;
                overflow: hidden; border: 1px solid #ffffff;
                box-shadow: 0 24px 70px #514b9826, 0 4px 14px #514b9810;
            }
            .brand {
                padding: 22px 32px; display: flex; align-items: center; gap: 12px;
                font-size: 21px; font-weight: 750; color: #fff;
                background: linear-gradient(110deg, #245fce, #6648cf 65%, #a343bd);
            }
            .brand-mark {
                display: grid; place-items: center; width: 38px; height: 38px;
                border-radius: 12px; background: #ffffff26; border: 1px solid #ffffff50;
            }
            .portal-label { margin-left: auto; font-size: 10px; letter-spacing: 1.5px; color: #f0eaff; }
            .card-content { padding: 36px 34px; }
            .eyebrow { color: #6650b6; font-size: 11px; font-weight: 750; letter-spacing: 1.8px; }
            h1 { margin: 10px 0 18px; font-size: clamp(26px, 5vw, 34px); line-height: 1.2; letter-spacing: -.7px; }
            .intro { margin: 0 0 24px; color: #64708a; }
            .account {
                padding: 20px; margin: 0 0 26px; border: 1px solid #dce5fa;
                border-left: 4px solid #7860df; border-radius: 12px;
                background: linear-gradient(110deg, #f0f6ff, #f8f2ff);
            }
            .account-label { color: #6b7490; font-size: 10px; letter-spacing: 1.4px; font-weight: 700; }
            .account p { margin: 6px 0 0; color: #334b87; font-size: 18px; font-weight: 650; overflow-wrap: anywhere; }
            .decision-button {
                width: 100%; padding: 15px 20px; border: 0; border-radius: 12px; color: #fff;
                font: inherit; font-weight: 700; cursor: pointer;
                transition: transform .15s ease, box-shadow .15s ease;
            }
            .approve { background: linear-gradient(110deg, #087f5b, #087f72); box-shadow: 0 6px 18px #087f5b28; }
            .reject { background: linear-gradient(110deg, #bd2645, #ab275f); box-shadow: 0 6px 18px #bd264528; }
            .decision-button:hover { transform: translateY(-2px); box-shadow: 0 9px 22px #263a6c30; }
            .decision-button:active { transform: translateY(0); }
            .decision-button:focus-visible { outline: 3px solid #405bd3; outline-offset: 4px; }
            .note { margin: 20px 0 0; font-size: 13px; color: #6b7490; text-align: center; }
            .message { overflow-wrap: anywhere; }
            .error-card .message { padding: 18px; background: #fff0f2; border: 1px solid #f5cbd3; border-radius: 12px; color: #9c2540; }
            footer { padding: 17px 24px; text-align: center; background: #f8f9fe; border-top: 1px solid #edf0f9; color: #77809a; font-size: 12px; }
            @media (max-width: 480px) {
                body { padding: 20px 12px; }
                .brand { padding: 20px 22px; }
                .card-content { padding: 28px 22px; }
                .portal-label { font-size: 9px; letter-spacing: 1px; }
                .account p { font-size: 16px; }
            }
            @media (prefers-reduced-motion: reduce) { .decision-button { transition: none; } }
            """;
}
