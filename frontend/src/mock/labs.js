export const labs = [
  {
    id: 'lab-01',
    title: 'Lab 1: Variables & Data Types',
    course: 'CS 101: Introduction to Programming',
    status: 'open',
    progress: 80,
    description: 'Basic variable declaration, primitive types, and type conversion',
    due: 'Sept 25, 2024',
  },
  {
    id: 'lab-02',
    title: 'Lab 2: Control Structures',
    course: 'CS 101: Introduction to Programming',
    status: 'open',
    progress: 35,
    description: 'If statements, loops, and conditional logic implementation',
    due: 'Oct 2, 2024',
  },
  {
    id: 'lab-03',
    title: 'Lab 3: Functions & Methods',
    course: 'CS 101: Introduction to Programming',
    status: 'upcoming',
    progress: 0,
    description: 'Function definition, parameters, return values, and scope',
    due: 'Oct 9, 2024',
  },
  {
    id: 'lab-04',
    title: 'Lab 4: Arrays & Collections',
    course: 'CS 101: Introduction to Programming',
    status: 'upcoming',
    progress: 0,
    description: 'Array manipulation, iteration, and basic data structures',
    due: 'Oct 16, 2024',
  },
  {
    id: 'lab-05',
    title: 'Lab 5: Object-Oriented Programming',
    course: 'CS 101: Introduction to Programming',
    status: 'open',
    progress: 60,
    description: 'Classes, objects, inheritance, and polymorphism',
    due: 'Oct 23, 2024',
  },
  {
    id: 'lab-06',
    title: 'Lab 6: Data Structures',
    course: 'CS 101: Introduction to Programming',
    status: 'closed',
    progress: 100,
    description: 'Lists, stacks, queues, and basic algorithms',
    due: 'Oct 30, 2024',
  },
]

export const statusLabels = {
  open: 'Open',
  upcoming: 'Upcoming',
  closed: 'Closed',
}

export const statusColors = {
  open: { bg: '#dcfce7', color: '#166534', border: '#bbf7d0' },
  upcoming: { bg: '#fef3c7', color: '#92400e', border: '#fde68a' },
  closed: { bg: '#f3f4f6', color: '#6b7280', border: '#d1d5db' },
}