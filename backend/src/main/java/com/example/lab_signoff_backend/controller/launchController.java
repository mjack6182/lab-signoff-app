//package com.example.lab_signoff_backend.controller;
//
//import org.springframework.web.bind.annotation.*;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/lti")
//public class launchController {
//    @PostMapping("/launch")
//    public Map<String, Object> launch(
//            @RequestParam(defaultValue = "mock-token") String id_token,
//            @RequestParam(defaultValue = "mock-state") String state
//    ) {
//        return Map.of(
//                "endpoint", "/lti/launch",
//                "status", "ok",
//                "id_token", id_token != null ? id_token : "mock-token",
//                "state", state != null ? state : "mock-state",
//                "note", "This is a mock LTI launch endpoint"
//        );
//    }
//}
