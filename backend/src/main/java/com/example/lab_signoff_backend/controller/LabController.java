package com.example.lab_signoff_backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;

@RestController
public class LabController {

    @GetMapping("/labs")
    public List<Map<String, Object>> getLabs() {
        return List.of(
                Map.of("id", "lab1", "title", "Lab 1 - Basics", "points", 3),
                Map.of("id", "lab2", "title", "Lab 2 - Circuits", "points", 5));
    }
}