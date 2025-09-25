package com.example.lab_signoff_backend.service;

import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.repository.LabRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LabService {
    private final LabRepository repo;

    public LabService(LabRepository repo) {
        this.repo = repo;
    }

    public List<Lab> getAll() {
        return repo.findAll();
    }

    public Lab upsert(Lab lab) {
        return repo.save(lab);
    }
}