import { statuses } from './model';
export type Field = {
    key: string;
    label: string;
    type?: string;
    source?: string;
    options?: string[];
    required?: boolean;
};
const title = { key: 'title', label: 'Title', required: true };
const description = { key: 'description', label: 'Description', type: 'textarea' };
const owner = { key: 'ownerId', label: 'Assigned to', source: 'users', required: true };
const project = { key: 'projectId', label: 'Project', source: 'projects', required: true };
const due = { key: 'dueDate', label: 'Due date', type: 'date' };
const priority = { key: 'priority', label: 'Priority', options: ['Low', 'Medium', 'High', 'Critical'] };
const status = (options: string[]): Field => ({ key: 'status', label: 'Status', options });
export const fields: Record<string, Field[]> = {
    users: [{ ...title, label: 'Full name' }, { key: 'email', label: 'Email', type: 'email', required: true }, { key: 'password', label: 'Password (12+ characters; leave blank to retain)', type: 'password' }, { key: 'position', label: 'Job title' }, { key: 'roleId', label: 'Role', source: 'roles', required: true }, { key: 'reportingTo', label: 'Reporting to', source: 'users' }, { key: 'scope', label: 'Data visibility', options: ['hierarchy', 'organization'] }, { key: 'department', label: 'Department' }, { key: 'capacity', label: 'Weekly capacity (hours)', type: 'number' }, status(['Active', 'Inactive'])],
    teams: [title, description, { key: 'memberIds', label: 'Team members', source: 'users', type: 'multi' }, { key: 'leadId', label: 'Team lead', source: 'users' }, status(['Active', 'Inactive'])],
    projects: [{ ...title, label: 'Project name' }, description, { key: 'client', label: 'Client' }, { key: 'technology', label: 'Technology' }, { key: 'teamIds', label: 'Project teams', source: 'teams', type: 'multi' }, { key: 'managerIds', label: 'Project managers / leads', source: 'users', type: 'multi' }, { key: 'startDate', label: 'Start date', type: 'date' }, due, { key: 'budget', label: 'Budget', type: 'number' }, { key: 'bugsEnabled', label: 'Enable bug tracking', type: 'boolean' }, status(['Planning', 'Active', 'On hold', 'Completed', 'Archived'])],
    tasks: [title, description, project, owner, { key: 'sprintId', label: 'Sprint', source: 'sprints' }, priority, status(statuses), due, { key: 'estimatedHours', label: 'Estimated hours', type: 'number' }, { key: 'actualHours', label: 'Actual hours', type: 'number' }, { key: 'storyPoints', label: 'Story points', type: 'number' }, { key: 'dependencies', label: 'Dependencies', source: 'tasks', type: 'multi' }],
    bugs: [title, description, project, owner, priority, { key: 'severity', label: 'Severity', options: ['Minor', 'Major', 'Critical'] }, status(statuses), due],
    sprints: [title, project, { key: 'goal', label: 'Sprint goal', type: 'textarea' }, { key: 'startDate', label: 'Start date', type: 'date' }, due, { key: 'capacity', label: 'Capacity (hours)', type: 'number' }, status(['Planned', 'Active', 'Under review', 'Completed'])],
    milestones: [title, project, description, due, status(['Planned', 'In progress', 'Completed'])],
    risks: [title, project, owner, description, { key: 'probability', label: 'Probability', options: ['Low', 'Medium', 'High'] }, { key: 'impact', label: 'Impact', options: ['Low', 'Medium', 'High'] }, { key: 'mitigation', label: 'Mitigation', type: 'textarea' }, status(['Open', 'Mitigated', 'Closed'])],
    standups: [title, project, { key: 'yesterday', label: 'Yesterday’s work', type: 'textarea' }, { key: 'today', label: 'Today’s plan', type: 'textarea' }, { key: 'blockers', label: 'Blockers', type: 'textarea' }],
    retrospectives: [title, project, { key: 'sprintId', label: 'Sprint', source: 'sprints' }, { key: 'wentWell', label: 'What went well', type: 'textarea' }, { key: 'improve', label: 'What could improve', type: 'textarea' }, { key: 'actions', label: 'Actions for next sprint', type: 'textarea' }],
    departments: [title, description, status(['Active', 'Inactive'])],
    assignments: [title, description, owner, due, priority, { key: 'expectedOutput', label: 'Expected output', type: 'textarea' }, { key: 'submission', label: 'Submission', type: 'textarea' }, status(['Assigned', 'In progress', 'Submitted', 'Evaluated'])],
    courses: [title, description, { key: 'category', label: 'Category' }, { key: 'difficulty', label: 'Difficulty', options: ['Beginner', 'Intermediate', 'Advanced'] }, { key: 'duration', label: 'Duration (minutes)', type: 'number' }, { key: 'lessons', label: 'Lessons', type: 'lessons' }, status(['Draft', 'Published', 'Archived'])],
    enrollments: [title, { key: 'courseId', label: 'Course', source: 'courses', required: true }, owner, due, status(['Assigned', 'In progress', 'Completed'])],
    assessments: [title, { key: 'courseId', label: 'Course', source: 'courses' }, { key: 'passingScore', label: 'Passing score (%)', type: 'number' }, { key: 'maxAttempts', label: 'Maximum attempts', type: 'number' }, { key: 'questions', label: 'Multiple-choice questions', type: 'questions' }, status(['Draft', 'Published', 'Archived'])],
    skills: [{ ...title, label: 'Skill' }, owner, status(['Beginner', 'Intermediate', 'Advanced', 'Expert'])],
    reviews: [title, owner, { key: 'period', label: 'Review period' }, { key: 'qualityScore', label: 'Quality score (0–100)', type: 'number' }, { key: 'collaborationScore', label: 'Collaboration score (0–100)', type: 'number' }, { key: 'feedback', label: 'Manager feedback', type: 'textarea' }, status(['Draft', 'Published'])],
    announcements: [title, project, description, status(['Draft', 'Published'])],
    documents: [title, { key: 'entityKind', label: 'Related module', options: ['projects', 'tasks', 'bugs', 'assignments', 'courses', 'sprints'] }, { key: 'entityId', label: 'Related record', source: 'parent' }, description],
    roles: [{ ...title, label: 'Role name' }, description, status(['Active', 'Inactive'])],
    settings: [title, ...['task', 'delivery', 'sprint', 'quality', 'learning', 'collaboration'].map(k => ({ key: k + 'Weight', label: k[0].toUpperCase() + k.slice(1) + ' weight (%)', type: 'number' }))],
};
