package com.example.lab_signoff_backend.controller;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

// Marks this class as a REST controller so it can handle HTTP requests
@RestController
// Base path for all endpoints in this controller will start with /lti

@RequestMapping("/lti")
public class ltiController {

    // -------------------------------
    // Simulated OIDC login endpoint
    // -------------------------------

    // Handles GET requests to /lti/login
    @GetMapping("/login")
    public void login(

            // A fake "login_hint" parameter with default value if not provided
            @RequestParam(defaultValue = "demo-user") String login_hint,

            // The endpoint where the login flow should redirect to (default: /lti/launch)
            @RequestParam(defaultValue = "/lti/launch") String target_link_uri,

            // Used to build and send the response
            HttpServletResponse response
    ) throws IOException {

        // Create mock id_token and state values (would normally come from the IdP in real LTI/OIDC flow)
        String idToken = "mock-id-token-" + System.currentTimeMillis();
        String state = "mock-state-" + System.nanoTime();

        // Build an HTML page with a form that auto-submits via POST to the /lti/launch endpoint
        // This simulates the OIDC redirect behavior of a real LMS
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

        // Tell the client browser that the response is HTML
        response.setContentType("text/html");
        // Write the HTML directly to the HTTP response body
        response.getWriter().write(html);
    }

    // -------------------------------
    // Simulated LTI launch endpoint
    // -------------------------------

    // Handles POST requests to /lti/launch
    @PostMapping("/launch")
    public Object launch(
            // Receive the id_token passed from the login step (default: mock value)
            @RequestParam(defaultValue = "mock-id-token") String id_token,
            // Receive the state parameter from the login step (default: mock value)
            @RequestParam(defaultValue = "mock-state") String state
    ) {
        // Return a simple JSON object (as a Map) describing the mock launch response
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
