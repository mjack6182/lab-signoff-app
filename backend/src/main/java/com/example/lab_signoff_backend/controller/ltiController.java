package com.example.lab_signoff_backend.controller;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.lab_signoff_backend.security.StateNonceStore;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Mock LTI controller for simulating the OIDC login and launch flow.
 *
 * This controller provides mock endpoints for testing LTI integration
 * without requiring a full Canvas LMS setup. It simulates the OIDC login
 * initiation and the LTI launch request handling.
 *
 * @author Lab Signoff App Team
 * @version 1.0
 */
@RestController
@RequestMapping("/lti")
public class ltiController {

    private final StateNonceStore stateNonceStore;

    /**
     * Constructor for ltiController.
     *
     * @param stateNonceStore Store for managing state-nonce pairs
     */
    @Autowired
    public ltiController(StateNonceStore stateNonceStore) {
        this.stateNonceStore = stateNonceStore;
    }

    /**
     * Simulated OIDC login endpoint.
     *
     * Generates a state-nonce pair and returns an auto-submitting HTML form
     * that posts to the launch endpoint, simulating the Canvas OIDC flow.
     *
     * @param login_hint      The login hint parameter (user identifier)
     * @param target_link_uri The target URI to redirect to after login
     * @param response        The HTTP response object
     * @throws IOException if writing to response fails
     */
    @GetMapping("/login")
    public void login(
            @RequestParam(defaultValue = "demo-user") String login_hint,
            @RequestParam(defaultValue = "/lti/launch") String target_link_uri,
            HttpServletResponse response
    ) throws IOException {

        // create a nonce, store (state -> nonce), and remember the state we just issued
        String nonce = java.util.UUID.randomUUID().toString();
        String state = stateNonceStore.issueState(nonce);  // <— IMPORTANT: issue() stores state->nonce

        // auto-post to launch with the exact state/nonce pair we just stored
        String idToken = "mock-id-token-" + System.currentTimeMillis();

        String html = """
        <html><body onload="document.forms[0].submit();">
          <form action="%s" method="post">
            <input type="hidden" name="id_token" value="%s"/>
            <input type="hidden" name="state" value="%s"/>
            <noscript><button type="submit">Continue</button></noscript>
          </form>
        </body></html>
        """.formatted(target_link_uri, idToken, state);

        response.setContentType("text/html");
        response.getWriter().write(html);
    }

    /**
     * Simulated LTI launch endpoint.
     *
     * Receives the ID token and state from the login step and returns
     * a mock response for testing purposes.
     *
     * @param id_token The ID token from the login step
     * @param state    The state parameter from the login step
     * @return A map containing mock launch response data
     */
    @PostMapping("/launch")
    public Object launch(
            @RequestParam(defaultValue = "mock-id-token") String id_token,
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
