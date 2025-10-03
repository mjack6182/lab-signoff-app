# Role-Based UI Guard System

## Overview

This document describes the role-based access control (RBAC) system implemented in the Lab Sign-Off App. The system ensures that UI elements are only visible to users with appropriate permissions.

## Architecture

### Components

1. **AuthContext** (`/src/contexts/AuthContext.jsx`)
   - Manages user authentication state
   - Provides role checking utilities
   - Handles login/logout functionality

2. **RoleGuard Components** (`/src/components/RoleGuard.jsx`)
   - Conditional rendering based on user roles
   - Reusable guard components for different role combinations
   - Hook for role-based conditional logic

3. **RoleSwitcher** (`/src/components/RoleSwitcher.jsx`)
   - Development tool for testing different user roles
   - Should be removed in production

## Usage

### Basic Role Guard

```jsx
import { RoleGuard } from '../components/RoleGuard';

// Single role
<RoleGuard roles="Teacher">
  <div>Teacher only content</div>
</RoleGuard>

// Multiple roles
<RoleGuard roles={['Teacher', 'TA']}>
  <div>Staff only content</div>
</RoleGuard>

// With fallback
<RoleGuard 
  roles="Teacher"
  fallback={<div>Access Denied</div>}
  showFallback={true}
>
  <div>Teacher content</div>
</RoleGuard>
```

### Specific Role Guards

```jsx
import { TeacherOnly, TAOnly, StaffOnly, StudentOnly } from '../components/RoleGuard';

// Teacher only
<TeacherOnly>
  <button>Create Assignment</button>
</TeacherOnly>

// TA only
<TAOnly>
  <button>Bulk Grade</button>
</TAOnly>

// Staff (Teacher or TA)
<StaffOnly>
  <button>Manage Checkpoints</button>
</StaffOnly>

// Student only
<StudentOnly>
  <button>Submit Work</button>
</StudentOnly>
```

### Using the Role Guard Hook

```jsx
import { useRoleGuard } from '../components/RoleGuard';

function MyComponent() {
  const { isTeacher, isTA, isStudent, isTeacherOrTA } = useRoleGuard();
  
  return (
    <div>
      {isTeacher && <TeacherDashboard />}
      {isTA && <TAAssistant />}
      {isStudent && <StudentPortal />}
      {isTeacherOrTA && <StaffTools />}
    </div>
  );
}
```

## Role Types

| Role | Description | Permissions |
|------|-------------|-------------|
| **Teacher** | Course instructor | Full access to all features, can create/edit assignments, view analytics |
| **TA** | Teaching assistant | Can grade assignments, help students, limited admin access |
| **Student** | Course participant | Can view assignments, submit work, check grades |

## Implementation Guidelines

### Frontend Guards

✅ **DO:**
- Use role guards for conditional rendering
- Show appropriate fallback messages
- Test with different user roles
- Handle edge cases (no user, invalid role)

❌ **DON'T:**
- Rely solely on frontend guards for security
- Hide critical functionality without backend enforcement
- Assume role checks are sufficient for data protection

### Backend Enforcement

**Note:** Frontend role guards are for UX only. All security-critical operations must be validated on the backend.

```java
// Example backend role check
@PreAuthorize("hasRole('TEACHER') or hasRole('TA')")
@PostMapping("/checkpoints/{id}/approve")
public ResponseEntity<?> approveCheckpoint(@PathVariable String id) {
    // Implementation
}
```

## Testing

### Unit Tests

```jsx
import { renderWithAuth, mockUsers } from '../tests/RoleGuard.test.js';

test('teacher can access teacher content', () => {
  renderWithAuth(
    <TeacherOnly>
      <div>Teacher Content</div>
    </TeacherOnly>,
    mockUsers.teacher
  );
  
  expect(screen.getByText('Teacher Content')).toBeInTheDocument();
});
```

### Integration Tests

1. **Navigation Tests**: Verify role-based routing works correctly
2. **Feature Tests**: Test that features are accessible to correct roles
3. **Security Tests**: Ensure unauthorized access attempts are blocked

## Performance Considerations

- Role checks are lightweight and cached
- No noticeable performance impact on UI rendering
- Guards should complete in <1ms per check

## Future Enhancements

### Planned Features

1. **Permission-Based Guards**: More granular permissions beyond roles
2. **Dynamic Role Assignment**: Admin ability to change user roles
3. **Audit Logging**: Track role-based access attempts
4. **Multi-Tenant Support**: Role isolation by organization

### API Integration

```javascript
// Future: Dynamic role fetching
const { user, refreshRoles } = useAuth();

useEffect(() => {
  // Refresh roles from server periodically
  refreshRoles();
}, []);
```

## Security Best Practices

### Frontend Security

1. **Defense in Depth**: UI guards + backend validation
2. **Fail Secure**: Default to denying access
3. **Clear Feedback**: Show appropriate error messages
4. **Audit Trail**: Log access attempts (backend)

### Common Pitfalls

❌ **Security Anti-Patterns:**
- Only checking roles on frontend
- Storing sensitive data in client-side role checks
- Trusting user-provided role information

✅ **Security Best Practices:**
- Validate every backend request
- Use signed JWT tokens for role information
- Implement session timeout and renewal

## Troubleshooting

### Common Issues

1. **Role not updating**: Check localStorage and context state
2. **Guards not working**: Verify AuthProvider wraps components
3. **Performance issues**: Check for unnecessary re-renders

### Debug Tools

```jsx
// Debug current user and permissions
const { user, isTeacher, isTA, isStudent } = useAuth();
console.log('User:', user);
console.log('Permissions:', { isTeacher, isTA, isStudent });
```

## Migration Guide

### From Hardcoded Roles

```jsx
// Before
const isTeacher = currentUserId === 'teacher123';

// After
const { isTeacher } = useRoleGuard();
```

### Adding New Roles

1. Update `AuthContext` role types
2. Add new guard components if needed
3. Update backend role validation
4. Add tests for new role behavior

## API Reference

### AuthContext

| Method | Description | Returns |
|--------|-------------|---------|
| `useAuth()` | Get auth context | `{ user, login, logout, ... }` |
| `hasRole(role)` | Check specific role | `boolean` |
| `hasAnyRole(roles)` | Check multiple roles | `boolean` |
| `isTeacher()` | Check if user is teacher | `boolean` |
| `isTA()` | Check if user is TA | `boolean` |
| `isStudent()` | Check if user is student | `boolean` |
| `isTeacherOrTA()` | Check if user is staff | `boolean` |

### RoleGuard Props

| Prop | Type | Description | Default |
|------|------|-------------|---------|
| `roles` | `string \| string[]` | Required roles | Required |
| `children` | `ReactNode` | Content to guard | Required |
| `fallback` | `ReactNode` | Fallback content | `null` |
| `showFallback` | `boolean` | Show fallback on deny | `false` |

## Examples

See `/src/pages/role-demo.jsx` for comprehensive examples of role-based UI patterns.