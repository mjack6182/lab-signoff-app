package com.example.lab_signoff_backend.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class LtiControllerTest {

    @Test
    void launch_returnsMockPayload() {
        StateNonceStore store = org.mockito.Mockito.mock(StateNonceStore.class);
        LtiController controller = new LtiController(store);

        Object result = controller.launch("token123", "stateXYZ");

        assertTrue(result.toString().contains("token123"));
    }

    @Test
    void login_writesAutoSubmitHtml() throws Exception {
        StateNonceStore store = org.mockito.Mockito.mock(StateNonceStore.class);
        org.mockito.Mockito.when(store.issueState(org.mockito.ArgumentMatchers.anyString())).thenReturn("state123");
        LtiController controller = new LtiController(store);
        MockHttpServletResponse resp = new MockHttpServletResponse();

        controller.login("user", "/target", resp);

        String content = resp.getContentAsString();
        assertTrue(content.contains("form action=\"/target\""));
        assertTrue(content.contains("id_token"));
        assertEquals("text/html", resp.getContentType());
    }
}
