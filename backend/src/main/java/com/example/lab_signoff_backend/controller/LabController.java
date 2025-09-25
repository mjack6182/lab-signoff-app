package com.example.lab_signoff_backend.controller;

import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.service.LabService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class LabController {

    private final LabService service;

    public LabController(LabService service) {
        this.service = service;
    }

    @GetMapping("/labs")
    public List<Lab> getLabs() {
        return service.getAll();
    }

    @PostMapping("/labs")
    public Lab createOrUpdateLab(@RequestBody Lab lab) {
        return service.upsert(lab);
    }
}