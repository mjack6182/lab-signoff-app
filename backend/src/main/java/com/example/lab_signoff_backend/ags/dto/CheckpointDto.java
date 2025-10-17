package com.example.lab_signoff_backend.ags.dto;

import com.example.lab_signoff_backend.ags.CheckpointState;

public class CheckpointDto {
    public String id;                 // "cp-01"
    public Double weight;             // null -> defaults to 1.0
    public Boolean required;          // null -> defaults to true
    public CheckpointState state;     // required

    public CheckpointDto() {}
}
