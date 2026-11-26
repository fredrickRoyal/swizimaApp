# Swizima StaffTrack (`stafftrack-web`)

Angular 22 application for tracking officer movements, daily duty assignments, structured engagement debriefs, executive management actions, and consolidated weekly activity reports.

## Modules

1. **Management Dashboard (`/dashboard`)**
   - Automatically generates real-time summaries showing who is away, where they are deployed, duration, purpose, acting cover officer, and key management actions required (pending approvals, escalated field issues, and blocked duty follow-ups).
2. **Out-of-Office Notifications (`/out-of-office`)**
   - Record and manage officers travelling, attending external meetings, workshops, field assignments, and other official engagements.
3. **Duty Activity Tracker (`/duty-tracker`)**
   - Capture daily activities, assignments, completion progress (`0%–100%`), achieved outcomes, and follow-up actions with responsible officers and due dates.
4. **Activity Reporting (`/activity-reports`)**
   - Structured 4-quadrant reporting for what was done, what was discussed, what was achieved, and any outstanding issues or management escalations.
5. **Weekly Activity Reports (`/weekly-reports`)**
   - Auto-consolidate activities undertaken during the week, key outcomes, pending actions, and responsible officers with CSV export and print view.

## Running Locally

```bash
cd stafftrack-web
npm start
# Opens on http://localhost:4300
```

