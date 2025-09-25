//package com.example.lab_signoff_backend.controller;
//
//import org.springframework.web.bind.annotation.*;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/lti")
//public class loginController {
//
//    @GetMapping("/login")
//    public Map<String, Object> login(
//            @RequestParam(defaultValue = "") String login_hint,
//            @RequestParam(defaultValue = "/lti/launch") String target_link_uri
//    ) {
//        return Map.of(
//                "endpoint", "/lti/login",
//                "status", "ok",
//                "login_hint", login_hint,
//                "target_link_uri", target_link_uri != null ? target_link_uri : "/lti/launch",
//                "note", "This is a mock OIDC login endpoint"
//        );
//    }
//}
