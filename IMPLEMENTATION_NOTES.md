# Lab Signoff App - Implementation Notes

**Date:** October 14, 2025
**Status:** Working version ready for team rebase

## Overview

This document describes the changes made to create a functional working version of the Lab Signoff App. The application now successfully connects the frontend to the backend, implements the user flow of Labs → Groups, and retrieves mock data from MongoDB Atlas.

---

## Changes Made

### 1. Backend Fixes

#### 1.1 File Naming Issue Fixed
**File:** `backend/src/main/java/com/example/lab_signoff_backend/controller/ltiController.java`

**Issue:** Java class name `LtiController` did not match the filename `ltiController.java` (lowercase 'l' vs uppercase 'L').

**Fix:** Renamed file from `ltiController.java` to `LtiController.java`

**Location:** `backend/src/main/java/com/example/lab_signoff_backend/controller/LtiController.java`

---

#### 1.2 Environment Configuration Updated
**File:** `.env`

**Issue:** Missing `LTI_CLIENT_ID` environment variable was causing the backend to fail on startup.

**Fix:** Added placeholder LTI configuration to the `.env` file:

```env
# LTI Configuration (placeholder values until Canvas developer key is obtained)
LTI_CLIENT_ID=placeholder-client-id
```

**Note:** Since the Canvas developer key is not yet available, we're using placeholder values. The LTI authentication is not active yet, which is acceptable per requirements.

---

### 2. Frontend Implementation

#### 2.1 Lab Selector Page Updated
**File:** `frontend/src/pages/lab-selector/lab-selector.jsx`

**Changes:**
- Replaced mock data with actual API calls to the backend
- Added data fetching from `GET /lti/labs` endpoint
- Implemented loading and error states
- Updated navigation to route to lab-specific groups page
- Added comprehensive JSDoc documentation
- Modified UI to display actual lab data (courseId, lineItemId) from backend

**Key Features:**
- Fetches all labs from MongoDB via backend API
- Displays labs in a grid layout with status indicators
- Each lab card shows course ID and line item information
- "View Groups" button navigates to the groups page for that specific lab

**Backend Integration:**
```javascript
// Fetches labs from backend on component mount
useEffect(() => {
  fetch('http://localhost:8080/lti/labs')
    .then((res) => {
      if (!res.ok) throw new Error('Failed to fetch labs')
      return res.json()
    })
    .then((data) => {
      setLabs(Array.isArray(data) ? data : [])
      setLoading(false)
    })
    .catch((err) => {
      console.error('Error fetching labs:', err)
      setError(err.message)
      setLoading(false)
    })
}, [])
```

---

#### 2.2 Lab Groups Page Created (NEW)
**Files Created:**
- `frontend/src/pages/lab-groups/lab-groups.jsx`
- `frontend/src/pages/lab-groups/lab-groups.css`

**Purpose:** Displays all groups for a selected lab, implementing the second step in the user flow.

**Key Features:**
- Fetches lab information and groups for a specific lab ID
- Uses URL parameters to determine which lab to display (`/labs/:labId/groups`)
- Shows group cards with member information and status
- Displays group status with color-coded badges (pending, in-progress, completed, signed-off)
- Includes back navigation to return to lab selector
- Fully documented with JSDoc comments

**Backend Integration:**
```javascript
// Fetches both lab info and groups for the lab
Promise.all([
  fetch(`http://localhost:8080/lti/labs`).then(res => res.json()),
  fetch(`http://localhost:8080/lti/labs/${labId}/groups`).then(res => {
    if (!res.ok) throw new Error('Failed to fetch groups')
    return res.json()
  })
])
```

**UI Components:**
- Header with back button and lab information
- Grid layout showing all groups for the lab
- Each group card displays:
  - Group name (groupId)
  - Status badge with color coding
  - List of all member emails
  - Member count
  - "View Checkpoints" button (placeholder for future checkpoint functionality)

---

#### 2.3 Routing Configuration Updated
**File:** `frontend/src/App.jsx`

**Changes:**
- Imported new `LabGroups` component
- Added new route for lab-specific groups page
- Removed navigation bar component (per requirements)
- Changed default route from `/login` to `/lab-selector`
- Added checkpoint route with labId and groupId parameters

**New Routes Added:**
```jsx
<Route path="/labs/:labId/groups" element={<LabGroups />} />
<Route path="/labs/:labId/groups/:groupId/checkpoints" element={<CheckpointPage />} />
```

**User Flow:**
1. App lands on lab selector → `/lab-selector` (default)
2. User clicks on a lab → `/labs/{labId}/groups`
3. User can view groups and their members for that specific lab
4. User clicks "View Checkpoints" on a group → `/labs/{labId}/groups/{groupId}/checkpoints`

---

#### 2.4 Universal Header Component Created (NEW)
**Files Created:**
- `frontend/src/components/Header/Header.jsx`
- `frontend/src/components/Header/Header.css`

**Purpose:** Provides a consistent, fixed header across all pages with app branding and user controls.

**Key Features:**
- Fixed positioning at top of viewport (z-index: 1000)
- Displays application name: "Lab Signoff App"
- Shows authenticated user's name and role
- Includes logout button that returns to lab selector
- Responsive design with proper spacing

**Implementation:**
```jsx
export default function Header() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/lab-selector')
  }

  return (
    <header className="universal-header">
      <div className="header-container">
        <div className="header-left">
          <h1 className="app-name">Lab Signoff App</h1>
        </div>
        <div className="header-right">
          {user && (
            <>
              <div className="user-info">
                <span className="user-name">{user.name}</span>
                <span className="user-role">({user.role})</span>
              </div>
              <button className="logout-button" onClick={handleLogout}>
                Logout
              </button>
            </>
          )}
        </div>
      </div>
    </header>
  )
}
```

**CSS Styling:**
- Header height: 64px
- Fixed position with white background
- Box shadow for depth
- Flexbox layout for alignment
- Responsive font sizes

---

#### 2.5 Lab Selector Page - Header Integration
**File:** `frontend/src/pages/lab-selector/lab-selector.jsx`
**CSS:** `frontend/src/pages/lab-selector/lab-selector.css`

**Changes:**
- Imported and integrated universal Header component
- Added Header to all return paths (loading, error, main)
- Removed local header elements in favor of universal header
- Updated CSS padding-top to account for fixed header (64px)

**Structure:**
```jsx
return (
  <>
    <Header />
    <main className="lab-selector-shell">
      {/* content */}
    </main>
  </>
)
```

---

#### 2.6 Lab Groups Page - Header Integration & Navigation
**File:** `frontend/src/pages/lab-groups/lab-groups.jsx`
**CSS:** `frontend/src/pages/lab-groups/lab-groups.css`

**Changes:**
- Imported and integrated universal Header component
- Added Header to all return paths (loading, error, main)
- Updated navigation to support checkpoint view
- Added CSS padding-top: 88px (64px header + 24px spacing)

**Navigation Flow:**
- Back button navigates to `/lab-selector`
- Group cards are clickable and navigate to `/labs/${labId}/groups/${groupId}/checkpoints`
- "View Checkpoints" button in group card footer

**Click Handler:**
```javascript
const handleGroupClick = (group) => {
  navigate(`/labs/${labId}/groups/${group.id}/checkpoints`)
}
```

---

#### 2.7 Checkpoints Page - Header Integration & Backend Data
**File:** `frontend/src/pages/checkpoints/checkpoint.jsx`
**CSS:** `frontend/src/pages/checkpoints/checkpoints.css`

**Changes:**
- Imported and integrated universal Header component
- Added labId and groupId from URL parameters
- Fetches lab and group data from backend on component mount
- Updated back navigation to return to groups page with proper labId
- Renamed CSS class from `.checkpoint-header` to `.checkpoint-page-header`
- Added CSS padding-top: 96px (64px header + 32px spacing)

**Backend Integration:**
```javascript
useEffect(() => {
  if (labId && groupId) {
    // Fetch lab info
    fetch(`http://localhost:8080/lti/labs`)
      .then(res => res.json())
      .then(labs => {
        const foundLab = labs.find(l => l.id === labId)
        setLab(foundLab)
      })

    // Fetch group info
    fetch(`http://localhost:8080/lti/labs/${labId}/groups`)
      .then(res => res.json())
      .then(groups => {
        const foundGroup = groups.find(g => g.id === groupId)
        setGroup(foundGroup)
      })
  }
}, [labId, groupId])
```

**Navigation:**
- Back button navigates to `/labs/${labId}/groups`
- Displays actual lab courseId and group groupId in header

---

### 3. Backend Endpoints Verified

All backend endpoints are working correctly and returning data from MongoDB Atlas:

#### 3.1 Get All Labs
**Endpoint:** `GET http://localhost:8080/lti/labs`

**Response:**
```json
[
  {
    "id": "68dd98422c934423a74270d9",
    "courseId": "CSCI-101",
    "lineItemId": "lineitem-001"
  },
  {
    "id": "68dd98422c934423a74270da",
    "courseId": "CSCI-201",
    "lineItemId": "lineitem-002"
  },
  {
    "id": "68dd98422c934423a74270db",
    "courseId": "CSCI-301",
    "lineItemId": "lineitem-003"
  }
]
```

#### 3.2 Get Groups by Lab ID
**Endpoint:** `GET http://localhost:8080/lti/labs/{labId}/groups`

**Example:** `GET http://localhost:8080/lti/labs/68dd98422c934423a74270d9/groups`

**Response:**
```json
[
  {
    "id": "68eee1a06b42caf49fe6b403",
    "groupId": "Group-1",
    "labId": "68dd98422c934423a74270d9",
    "members": ["student1@example.com", "student2@example.com"],
    "status": "pending"
  },
  {
    "id": "68eee1a06b42caf49fe6b404",
    "groupId": "Group-2",
    "labId": "68dd98422c934423a74270d9",
    "members": [
      "student3@example.com",
      "student4@example.com",
      "student5@example.com"
    ],
    "status": "in-progress"
  },
  {
    "id": "68eee1a06b42caf49fe6b405",
    "groupId": "Group-3",
    "labId": "68dd98422c934423a74270d9",
    "members": ["student6@example.com"],
    "status": "completed"
  }
]
```

#### 3.3 Get All Groups
**Endpoint:** `GET http://localhost:8080/groups`

**Purpose:** Returns all groups across all labs (used for testing)

---

### 4. Database Seeding

The backend automatically seeds MongoDB Atlas with mock data on startup:

**Seeded Data:**
- **3 Labs:** CSCI-101, CSCI-201, CSCI-301
- **6 Groups:** Distributed across the three labs with various statuses

**Seeding Output:**
```
Labs already exist, using existing labs
Using labs for seeding:
 - Lab 1 ID: 68dd98422c934423a74270d9, Course: CSCI-101
 - Lab 2 ID: 68dd98422c934423a74270da, Course: CSCI-201
 - Lab 3 ID: 68dd98422c934423a74270db, Course: CSCI-301

Seeded groups:
 - Group-1 (Lab: 68dd98422c934423a74270d9, Members: 2, Status: pending)
 - Group-2 (Lab: 68dd98422c934423a74270d9, Members: 3, Status: in-progress)
 - Group-3 (Lab: 68dd98422c934423a74270d9, Members: 1, Status: completed)
 - Team-A (Lab: 68dd98422c934423a74270da, Members: 2, Status: pending)
 - Team-B (Lab: 68dd98422c934423a74270da, Members: 2, Status: signed-off)
 - Squad-1 (Lab: 68dd98422c934423a74270db, Members: 3, Status: in-progress)

Database seeding complete!
```

**Location:** `backend/src/main/java/com/example/lab_signoff_backend/config/DataSeeder.java`

---

## Current Application State

### What's Working
✅ Backend successfully connects to MongoDB Atlas
✅ Backend serves labs and groups data via REST API
✅ Frontend fetches and displays labs from backend
✅ User can navigate from labs to lab-specific groups
✅ Groups page shows all groups for a selected lab
✅ All data is retrieved from MongoDB (no mock data in use)
✅ Proper error handling and loading states implemented
✅ Comprehensive documentation added to all new code
✅ Universal fixed header across all pages
✅ Navigation bar removed (landing directly on lab selector)
✅ Complete user flow: Labs → Groups → Checkpoints
✅ Checkpoint page displays lab and group information from backend
✅ Proper back navigation maintaining context throughout the flow

### User Flow
1. **Lab Selector** → User lands on page and sees all available labs from MongoDB
2. **Select Lab** → User clicks "View Groups" on a lab card
3. **View Groups** → User sees all groups associated with that lab
4. **Select Group** → User clicks "View Checkpoints" on a group card
5. **View Checkpoints** → User sees checkpoints for the selected group with lab and group info
6. **Navigation** → User can navigate back through the flow using back buttons

---

## Running the Application

### Prerequisites
- Java 21
- Node.js and npm
- MongoDB Atlas account (credentials in `.env`)

### Backend
```bash
cd backend
./gradlew bootRun
```
Backend will start on `http://localhost:8080`

### Frontend
```bash
cd frontend
npm install
npm run dev
```
Frontend will start on `http://localhost:5173`

---

## API Endpoints

### Backend REST API

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/lti/labs` | Get all labs |
| POST | `/lti/labs` | Create or update a lab |
| GET | `/lti/labs/{id}/groups` | Get all groups for a specific lab |
| GET | `/groups` | Get all groups (across all labs) |
| GET | `/groups/{groupId}` | Get a specific group by ID |

---

## Not Yet Implemented

The following features are acknowledged but not yet implemented:
- Canvas LTI authentication (awaiting developer key)
- Checkpoint signoff functionality for groups
- Real-time WebSocket updates for checkpoint status
- Group management (add/remove members)
- Instructor-specific features (dashboard)

---

## Notes for Team

### Authentication Status
Canvas authentication is currently disabled as we are waiting for the Canvas developer key. The placeholder `LTI_CLIENT_ID` has been added to allow the backend to start successfully. Once the developer key is obtained, update the `.env` file with the actual credentials.

### Code Documentation
All new code includes comprehensive JSDoc comments explaining:
- Component purpose
- Backend integration points
- User flow context
- API endpoints used

### No Major UI/UX Changes
As requested, no major UI/UX changes were made. The existing design patterns and styling were preserved. Only necessary functional changes were implemented to connect the frontend to the backend.

### Database
The app uses MongoDB Atlas with the credentials stored in the `.env` file. The database is automatically seeded with sample data on backend startup if labs don't already exist.

---

## Files Modified

### Backend
1. `backend/src/main/java/com/example/lab_signoff_backend/controller/ltiController.java` → Renamed to `LtiController.java`
2. `.env` → Added `LTI_CLIENT_ID` placeholder

### Frontend

#### New Files Created
1. `frontend/src/components/Header/Header.jsx` → Universal fixed header component
2. `frontend/src/components/Header/Header.css` → Header styles
3. `frontend/src/pages/lab-groups/lab-groups.jsx` → Lab groups page component
4. `frontend/src/pages/lab-groups/lab-groups.css` → Lab groups page styles

#### Modified Files
1. `frontend/src/App.jsx` → Removed nav bar, added routes, changed default route
2. `frontend/src/pages/lab-selector/lab-selector.jsx` → Added Header, backend integration
3. `frontend/src/pages/lab-selector/lab-selector.css` → Added padding for fixed header
4. `frontend/src/pages/checkpoints/checkpoint.jsx` → Added Header, labId/groupId params, backend integration
5. `frontend/src/pages/checkpoints/checkpoints.css` → Renamed CSS class, added padding for fixed header

### Documentation
1. `IMPLEMENTATION_NOTES.md` → **NEW FILE** Comprehensive documentation of all changes

---

## Testing the Application

### Manual Testing Steps

1. **Start Backend:**
   ```bash
   cd backend
   ./gradlew bootRun
   ```
   Verify you see "Started LabSignoffBackendApplication" and "Database seeding complete!"

2. **Start Frontend:**
   ```bash
   cd frontend
   npm run dev
   ```

3. **Test User Flow:**
   - Open browser to `http://localhost:5173`
   - Login (if auth is enabled) or navigate to `/lab-selector`
   - Verify you see 3 labs (CSCI-101, CSCI-201, CSCI-301)
   - Click "View Groups" on CSCI-101
   - Verify you see 3 groups (Group-1, Group-2, Group-3) with their members
   - Click back button to return to lab selector
   - Test with other labs to verify data is correctly filtered by lab ID

### API Testing

Test endpoints using curl:
```bash
# Get all labs
curl http://localhost:8080/lti/labs

# Get groups for a specific lab
curl http://localhost:8080/lti/labs/68dd98422c934423a74270d9/groups

# Get all groups
curl http://localhost:8080/groups
```

---

## Next Steps

1. Obtain Canvas developer key and update LTI configuration
2. Implement checkpoint signoff functionality
3. Add WebSocket real-time updates
4. Implement instructor dashboard features
5. Add group management capabilities

---

## Rebasing Your Branch with Dev

Once these changes are pushed to the `dev` branch, team members should rebase their feature branches to get the latest updates. Follow these steps:

### Step 1: Commit Your Current Work
Before rebasing, make sure all your work is committed:
```bash
# Check status of your current branch
git status

# If you have uncommitted changes, commit them
git add .
git commit -m "Your commit message"
```

### Step 2: Fetch Latest Changes
Fetch the latest changes from the remote repository:
```bash
# Fetch all branches from remote
git fetch origin

# Or fetch just dev branch
git fetch origin dev
```

### Step 3: Rebase Your Branch onto Dev
```bash
# Make sure you're on your feature branch
git checkout your-feature-branch

# Rebase your branch onto the latest dev
git rebase origin/dev
```

### Step 4: Resolve Conflicts (If Any)
If there are merge conflicts, Git will pause the rebase and show you which files have conflicts:

```bash
# View files with conflicts
git status

# Open conflicted files in your editor and resolve conflicts
# Look for conflict markers: <<<<<<<, =======, >>>>>>>

# After resolving conflicts in a file, stage it
git add path/to/resolved-file

# Continue the rebase
git rebase --continue

# If you want to abort the rebase and go back
git rebase --abort
```

### Step 5: Force Push Your Rebased Branch (If Already Pushed)
If you've already pushed your branch to remote, you'll need to force push after rebasing:

```bash
# CAUTION: Only do this on your own feature branch, never on main/dev
git push --force-with-lease origin your-feature-branch
```

**Note:** Use `--force-with-lease` instead of `--force` for safety. It will prevent overwriting work if someone else has pushed to your branch.

### Step 6: Verify Your Changes
After rebasing, verify everything still works:

```bash
# Backend
cd backend
./gradlew bootRun

# Frontend (in a new terminal)
cd frontend
npm install  # Install any new dependencies
npm run dev
```

### Alternative: Merge Instead of Rebase
If you prefer merging over rebasing:
```bash
# Make sure you're on your feature branch
git checkout your-feature-branch

# Merge dev into your branch
git merge origin/dev

# Resolve any conflicts
# Then commit the merge
git commit
```

### Common Rebase Issues

**Issue 1: "Cannot rebase with unstaged changes"**
```bash
# Stash your changes temporarily
git stash

# Do the rebase
git rebase origin/dev

# Apply your stashed changes back
git stash pop
```

**Issue 2: Multiple conflicts during rebase**
```bash
# If rebase becomes too complex, you can abort and try merging instead
git rebase --abort
git merge origin/dev
```

**Issue 3: Lost commits after rebase**
```bash
# Don't panic! Git keeps a reflog
git reflog

# Find your commit and reset to it
git reset --hard HEAD@{n}  # where n is the number from reflog
```

### Best Practices
1. **Rebase frequently** - Don't let your branch get too far behind dev
2. **Communicate** - Let your team know when you're rebasing shared branches
3. **Test after rebasing** - Always verify your code still works after rebasing
4. **Keep commits clean** - Use `git rebase -i` for interactive rebasing to clean up commit history
5. **Never rebase public branches** - Only rebase your own feature branches, not dev or main

### Getting Help
If you run into issues during rebase:
1. Check `git status` to see what's happening
2. Use `git reflog` to see your recent Git history
3. Ask a team member for help - don't force push if you're unsure
4. When in doubt, `git rebase --abort` and start over

---

## Summary

This working version successfully implements the complete Labs → Groups → Checkpoints user flow with full backend integration. The application retrieves real data from MongoDB Atlas, features a universal fixed header across all pages, and provides seamless navigation throughout the user journey. All changes have been thoroughly documented and tested.

The application is now ready to be pushed to the `dev` branch for the team to rebase from. Team members should follow the rebasing instructions above to update their feature branches with these changes.
