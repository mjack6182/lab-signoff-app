package com.example.lab_signoff_backend.controller;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
@RestController
@RequestMapping("/lti")
public class ltiController {
    @GetMapping("/login")
    public void login(
            @RequestParam(defaultValue = "demo-user") String login_hint,
            @RequestParam(defaultValue = "/lti/launch") String target_link_uri,
            HttpServletResponse response
    ) throws IOException {

        // Just manufacture some fake values
        String idToken = "mock-id-token-" + System.currentTimeMillis();
        String state = "mock-state-" + System.nanoTime();

        // Return an HTML page that immediately POSTs to /lti/launch
        String html = """
            <html>
              <body onload="document.forms[0].submit();">
                <form action="%s" method="post">
                  <input type="hidden" name="id_token" value="%s"/>
                  <input type="hidden" name="state" value="%s"/>
                  <noscript><button type="submit">Continue</button></noscript>
                </form>
              </body>
            </html>
            """.formatted(target_link_uri, idToken, state);

        response.setContentType("text/html");
        response.getWriter().write(html);
    }

    @PostMapping("/launch")
    public Object launch(
            @RequestParam(defaultValue = "mock-id-token") String id_token,
            @RequestParam(defaultValue = "mock-state") String state
    ) {
        return java.util.Map.of(
                "endpoint", "/lti/launch",
                "method", "POST",
                "status", "ok",
                "id_token", id_token,
                "state", state,
                "note", "This is a mock LTI launch endpoint"
        );
    }
}
