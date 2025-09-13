// MongoDB initialization script
db = db.getSiblingDB('labsignoff');

// Create collections
db.createCollection('users');
db.createCollection('labs');
db.createCollection('signoffs');
db.createCollection('courses');

// Create indexes
db.users.createIndex({ "email": 1 }, { unique: true });
db.users.createIndex({ "ltiUserId": 1 }, { unique: true });
db.labs.createIndex({ "courseId": 1 });
db.signoffs.createIndex({ "labId": 1 });
db.signoffs.createIndex({ "studentId": 1 });
db.signoffs.createIndex({ "instructorId": 1 });

print('Database initialized successfully');