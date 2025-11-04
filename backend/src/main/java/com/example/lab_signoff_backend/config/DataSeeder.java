package com.example.lab_signoff_backend.config;

import com.example.lab_signoff_backend.model.Group;
import com.example.lab_signoff_backend.model.Lab;
import com.example.lab_signoff_backend.repository.GroupRepository;
import com.example.lab_signoff_backend.repository.LabRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Database seeding configuration for development and testing.
 * This class populates MongoDB with sample data on application startup.
 *
 * DISABLED: Data seeding is currently disabled.
 * To re-enable, uncomment the @Configuration annotation below.
 */
// @Configuration  // ← DISABLED - Uncomment to re-enable seeding
public class DataSeeder {

    /**
     * Seeds the database with sample labs and groups
     * Runs automatically on application startup
     *
     * @param labRepository   Repository for lab operations
     * @param groupRepository Repository for group operations
     * @return CommandLineRunner that executes the seeding logic
     */
    @Bean
    CommandLineRunner initDatabase(LabRepository labRepository, GroupRepository groupRepository) {
        return args -> {
            // Clear existing groups
            groupRepository.deleteAll();
            // Uncomment the line below if you also want to clear labs
            // labRepository.deleteAll();

            // Seed sample labs if none exist
            if (labRepository.count() == 0) {
                Lab lab1 = new Lab(null, "CSCI-101", "lineitem-001");
                Lab lab2 = new Lab(null, "CSCI-201", "lineitem-002");
                Lab lab3 = new Lab(null, "CSCI-301", "lineitem-003");

                labRepository.save(lab1);
                labRepository.save(lab2);
                labRepository.save(lab3);

                System.out.println("Seeded 3 new labs");
            } else {
                System.out.println("Labs already exist, using existing labs");
            }

            // Get existing labs from database
            List<Lab> labs = labRepository.findAll();
            if (labs.size() < 3) {
                System.out.println("WARNING: Expected at least 3 labs but found " + labs.size());
                return;
            }

            Lab lab1 = labs.get(0);
            Lab lab2 = labs.get(1);
            Lab lab3 = labs.get(2);

            System.out.println("Using labs for seeding:");
            System.out.println(" - Lab 1 ID: " + lab1.getId() + ", Course: " + lab1.getCourseId());
            System.out.println(" - Lab 2 ID: " + lab2.getId() + ", Course: " + lab2.getCourseId());
            System.out.println(" - Lab 3 ID: " + lab3.getId() + ", Course: " + lab3.getCourseId());

            // Seed sample groups for each lab
            Group group1 = new Group(
                    null,
                    "Group-1",
                    lab1.getId(),
                    Arrays.asList("student1@example.com", "student2@example.com"),
                    "in-progress"
            );

            Group group2 = new Group(
                    null,
                    "Group-2",
                    lab1.getId(),
                    Arrays.asList("student3@example.com", "student4@example.com", "student5@example.com"),
                    "in-progress"
            );

            Group group3 = new Group(
                    null,
                    "Group-3",
                    lab1.getId(),
                    Arrays.asList("student6@example.com"),
                    "in-progress"
            );

            Group group4 = new Group(
                    null,
                    "Team-A",
                    lab2.getId(),
                    Arrays.asList("student7@example.com", "student8@example.com"),
                    "in-progress"
            );

            Group group5 = new Group(
                    null,
                    "Team-B",
                    lab2.getId(),
                    Arrays.asList("student9@example.com", "student10@example.com"),
                    "in-progress"
            );

            Group group6 = new Group(
                    null,
                    "Squad-1",
                    lab3.getId(),
                    Arrays.asList("student11@example.com", "student12@example.com", "student13@example.com"),
                    "in-progress"
            );

            List<Group> groups = Arrays.asList(group1, group2, group3, group4, group5, group6);
            groupRepository.saveAll(groups);

            System.out.println("\nSeeded groups:");
            groups.forEach(group -> System.out.println(
                    " - " + group.getGroupId() +
                            " (Lab: " + group.getLabId() +
                            ", Members: " + group.getMembers().size() +
                            ", Status: " + group.getStatus() + ")"
            ));

            System.out.println("\nDatabase seeding complete!");
        };
    }
}
