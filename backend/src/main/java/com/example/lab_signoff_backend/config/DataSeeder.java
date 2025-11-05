package com.example.lab_signoff_backend.config;

import com.example.lab_signoff_backend.model.*;
import com.example.lab_signoff_backend.model.embedded.GroupMember;
import com.example.lab_signoff_backend.model.enums.*;
import com.example.lab_signoff_backend.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Database seeding configuration for development and testing.
 * Seeds MongoDB with realistic data based on Canvas CSV structure.
 *
 * Data includes: Users, Class, Enrollments, Labs, Groups, and HelpQueueItems
 */
// @Configuration  // ✅ ENABLED - Comment out to disable seeding
public class DataSeeder {

    @Bean
    CommandLineRunner initDatabase(
            UserRepository userRepository,
            ClassRepository classRepository,
            EnrollmentRepository enrollmentRepository,
            LabRepository labRepository,
            GroupRepository groupRepository,
            HelpQueueItemRepository helpQueueRepository,
            SignoffEventRepository signoffEventRepository) {

        return args -> {
            System.out.println("\n========================================");
            System.out.println("🌱 Starting Database Seeding...");
            System.out.println("========================================\n");

            // Clear existing data (for clean demo)
            System.out.println("🧹 Clearing existing data...");
            helpQueueRepository.deleteAll();
            signoffEventRepository.deleteAll();
            groupRepository.deleteAll();
            labRepository.deleteAll();
            enrollmentRepository.deleteAll();
            classRepository.deleteAll();
            // Note: Not clearing users to preserve Auth0 synced users

            // ========================================
            // 1. CREATE OR GET USERS
            // ========================================
            System.out.println("\n👥 Creating Users...");

            // Create instructor
            User instructor = createOrGetUser(userRepository,
                    "instructor@uwp.edu",
                    "Dr. Smith",
                    "Dr.",
                    "Smith",
                    Arrays.asList("Teacher", "Admin"));

            // Create TA
            User ta = createOrGetUser(userRepository,
                    "ta@uwp.edu",
                    "Alice Johnson",
                    "Alice",
                    "Johnson",
                    Arrays.asList("TA", "Student"));

            // Create students (based on Canvas CSV patterns)
            User student1 = createOrGetUser(userRepository,
                    "student1@uwp.edu",
                    "Bob Martinez",
                    "Bob",
                    "Martinez",
                    List.of("Student"));

            User student2 = createOrGetUser(userRepository,
                    "student2@uwp.edu",
                    "Carol Chen",
                    "Carol",
                    "Chen",
                    List.of("Student"));

            User student3 = createOrGetUser(userRepository,
                    "student3@uwp.edu",
                    "David Thompson",
                    "David",
                    "Thompson",
                    List.of("Student"));

            User student4 = createOrGetUser(userRepository,
                    "student4@uwp.edu",
                    "Emma Wilson",
                    "Emma",
                    "Wilson",
                    List.of("Student"));

            User student5 = createOrGetUser(userRepository,
                    "student5@uwp.edu",
                    "Frank Rodriguez",
                    "Frank",
                    "Rodriguez",
                    List.of("Student"));

            User student6 = createOrGetUser(userRepository,
                    "student6@uwp.edu",
                    "Grace Lee",
                    "Grace",
                    "Lee",
                    List.of("Student"));

            System.out.println("  ✅ Created/Retrieved 8 users (1 instructor, 1 TA, 6 students)");

            // ========================================
            // 2. CREATE CLASS (Based on Canvas CSV)
            // ========================================
            System.out.println("\n📚 Creating Class...");

            com.example.lab_signoff_backend.model.Class labSignoffClass = new com.example.lab_signoff_backend.model.Class(
                    "CSCI-475",
                    "Software Engineering Lab Signoff Project",
                    "Fall 2025",
                    instructor.getId()
            );
            labSignoffClass.setSection("001");

            // Add roster (all students)
            labSignoffClass.addStudentToRoster(student1.getId());
            labSignoffClass.addStudentToRoster(student2.getId());
            labSignoffClass.addStudentToRoster(student3.getId());
            labSignoffClass.addStudentToRoster(student4.getId());
            labSignoffClass.addStudentToRoster(student5.getId());
            labSignoffClass.addStudentToRoster(student6.getId());

            // Add TA
            labSignoffClass.addTA(ta.getId());

            labSignoffClass = classRepository.save(labSignoffClass);
            System.out.println("  ✅ Created class: " + labSignoffClass.getCourseCode() +
                    " (" + labSignoffClass.getRoster().size() + " students)");

            // ========================================
            // 3. CREATE ENROLLMENTS
            // ========================================
            System.out.println("\n📝 Creating Enrollments...");

            // Enroll instructor
            enrollmentRepository.save(new Enrollment(
                    instructor.getId(),
                    labSignoffClass.getId(),
                    EnrollmentRole.TEACHER));

            // Enroll TA
            Enrollment taEnrollment = new Enrollment(
                    ta.getId(),
                    labSignoffClass.getId(),
                    EnrollmentRole.TA);
            taEnrollment.setUpgradeRequestedBy(instructor.getId());
            enrollmentRepository.save(taEnrollment);

            // Enroll students
            List<User> students = Arrays.asList(student1, student2, student3, student4, student5, student6);
            for (User student : students) {
                enrollmentRepository.save(new Enrollment(
                        student.getId(),
                        labSignoffClass.getId(),
                        EnrollmentRole.STUDENT));
            }

            System.out.println("  ✅ Created 8 enrollments (1 teacher, 1 TA, 6 students)");

            // ========================================
            // 4. CREATE LABS (Based on Canvas CSV lab assignments)
            // ========================================
            System.out.println("\n🔬 Creating Labs...");

            // Lab 1: Module 01 (3 points = 3 checkpoints)
            Lab lab1 = new Lab(
                    labSignoffClass.getId(),
                    "Laboratory for Module 01",
                    3,
                    instructor.getId());
            lab1.setDescription("Introduction to version control and Git basics");
            lab1.activate(); // Make it active
            lab1 = labRepository.save(lab1);

            // Lab 2: Module 02 (4 points = 4 checkpoints)
            Lab lab2 = new Lab(
                    labSignoffClass.getId(),
                    "Laboratory for Module 02",
                    4,
                    instructor.getId());
            lab2.setDescription("Object-oriented programming fundamentals");
            lab2.activate();
            lab2 = labRepository.save(lab2);

            // Lab 3: Module 03 (4 points = 4 checkpoints)
            Lab lab3 = new Lab(
                    labSignoffClass.getId(),
                    "Laboratory for Module 03",
                    4,
                    instructor.getId());
            lab3.setDescription("Data structures and algorithms");
            lab3.setStatus(LabStatus.DRAFT); // Not yet active
            lab3 = labRepository.save(lab3);

            System.out.println("  ✅ Created 3 labs:");
            System.out.println("     - " + lab1.getTitle() + " (" + lab1.getPoints() + " checkpoints) - " +
                    lab1.getStatus() + " - Code: " + lab1.getJoinCode());
            System.out.println("     - " + lab2.getTitle() + " (" + lab2.getPoints() + " checkpoints) - " +
                    lab2.getStatus() + " - Code: " + lab2.getJoinCode());
            System.out.println("     - " + lab3.getTitle() + " (" + lab3.getPoints() + " checkpoints) - " +
                    lab3.getStatus() + " - Code: " + lab3.getJoinCode());

            // ========================================
            // 5. CREATE GROUPS
            // ========================================
            System.out.println("\n👥 Creating Groups...");

            // Lab 1 Groups
            Group lab1Group1 = createGroup(lab1.getId(), "Group-1", 1,
                    Arrays.asList(student1, student2));
            Group lab1Group2 = createGroup(lab1.getId(), "Group-2", 2,
                    Arrays.asList(student3, student4));
            Group lab1Group3 = createGroup(lab1.getId(), "Group-3", 3,
                    Arrays.asList(student5, student6));

            // Lab 2 Groups
            Group lab2Group1 = createGroup(lab2.getId(), "Team-A", 1,
                    Arrays.asList(student1, student3, student5));
            Group lab2Group2 = createGroup(lab2.getId(), "Team-B", 2,
                    Arrays.asList(student2, student4, student6));

            // Save all groups
            List<Group> groups = Arrays.asList(
                    lab1Group1, lab1Group2, lab1Group3,
                    lab2Group1, lab2Group2);

            groupRepository.saveAll(groups);

            System.out.println("  ✅ Created 5 groups:");
            System.out.println("     - Lab 1: 3 groups (2 members each)");
            System.out.println("     - Lab 2: 2 groups (3 members each)");

            // ========================================
            // 6. CREATE HELP QUEUE ITEMS
            // ========================================
            System.out.println("\n🖐️ Creating Help Queue Items...");

            // Group 1 from Lab 1 - waiting
            HelpQueueItem queue1 = new HelpQueueItem(
                    lab1.getId(),
                    lab1Group1.getId(),
                    student1.getId(),
                    1);
            queue1.setDescription("Need help with Git merge conflicts");

            // Group 2 from Lab 1 - claimed by TA
            HelpQueueItem queue2 = new HelpQueueItem(
                    lab1.getId(),
                    lab1Group2.getId(),
                    student3.getId(),
                    2);
            queue2.setDescription("Question about checkpoint 2");
            queue2.claim(ta.getId());

            // Group 1 from Lab 2 - waiting (urgent)
            HelpQueueItem queue3 = new HelpQueueItem(
                    lab2.getId(),
                    lab2Group1.getId(),
                    student1.getId(),
                    1);
            queue3.setDescription("Stuck on array implementation");
            queue3.setUrgent();

            helpQueueRepository.saveAll(Arrays.asList(queue1, queue2, queue3));

            System.out.println("  ✅ Created 3 help queue items:");
            System.out.println("     - 2 waiting, 1 claimed");
            System.out.println("     - 1 marked as urgent");

            // ========================================
            // 7. CREATE SAMPLE SIGNOFF EVENTS
            // ========================================
            System.out.println("\n✅ Creating Sample Signoff Events...");

            // Lab 1, Group 1 passed checkpoint 1
            SignoffEvent event1 = new SignoffEvent();
            event1.setLabId(lab1.getId());
            event1.setGroupId(lab1Group1.getId());
            event1.setCheckpointNumber(1);
            event1.setAction(SignoffAction.PASS);
            event1.setPerformedBy(ta.getId());
            event1.setPerformerRole("TA");
            event1.setNotes("Good work on Git basics");
            event1.setPointsAwarded(1);

            // Lab 1, Group 2 passed checkpoint 1
            SignoffEvent event2 = new SignoffEvent();
            event2.setLabId(lab1.getId());
            event2.setGroupId(lab1Group2.getId());
            event2.setCheckpointNumber(1);
            event2.setAction(SignoffAction.PASS);
            event2.setPerformedBy(instructor.getId());
            event2.setPerformerRole("Teacher");
            event2.setPointsAwarded(1);

            signoffEventRepository.saveAll(Arrays.asList(event1, event2));

            System.out.println("  ✅ Created 2 signoff events");

            // ========================================
            // SUMMARY
            // ========================================
            System.out.println("\n========================================");
            System.out.println("✨ Database Seeding Complete!");
            System.out.println("========================================");
            System.out.println("📊 Summary:");
            System.out.println("   Users: " + userRepository.count());
            System.out.println("   Classes: " + classRepository.count());
            System.out.println("   Enrollments: " + enrollmentRepository.count());
            System.out.println("   Labs: " + labRepository.count());
            System.out.println("   Groups: " + groupRepository.count());
            System.out.println("   Help Queue Items: " + helpQueueRepository.count());
            System.out.println("   Signoff Events: " + signoffEventRepository.count());
            System.out.println("========================================\n");
        };
    }

    /**
     * Helper method to create or retrieve a user
     */
    private User createOrGetUser(UserRepository repo, String email, String name,
                                  String firstName, String lastName, List<String> roles) {
        return repo.findByEmail(email).orElseGet(() -> {
            User user = new User();
            user.setAuth0Id("auth0|" + email.split("@")[0]); // Mock Auth0 ID
            user.setEmail(email);
            user.setName(name);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setRoles(new ArrayList<>(roles));
            user.setPrimaryRole(roles.get(0));
            return repo.save(user);
        });
    }

    /**
     * Helper method to create a group with members
     */
    private Group createGroup(String labId, String groupName, int groupNumber, List<User> students) {
        Group group = new Group();
        group.setLabId(labId);
        group.setGroupId(groupName);
        group.setGroupNumber(groupNumber);
        group.setStatus(GroupStatus.IN_PROGRESS);

        // Add members
        List<GroupMember> members = new ArrayList<>();
        for (User student : students) {
            GroupMember member = new GroupMember(
                    student.getId(),
                    student.getName(),
                    student.getEmail());
            members.add(member);
        }
        group.setMembers(members);

        return group;
    }
}
