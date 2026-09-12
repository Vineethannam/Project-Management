export type Action = 'create' | 'read' | 'update' | 'delete';
export type Item = {
    id: string;
    title: string;
    status: string;
    version: number;
    [key: string]: any;
};
export type State = Record<string, Item[]>;
export const modules = [
    { key: 'projects', label: 'Projects', module: 'Project Management' },
    { key: 'tasks', label: 'Tasks', module: 'Task Management' },
    { key: 'bugs', label: 'Bugs', module: 'Task Management' },
    { key: 'sprints', label: 'Sprints', module: 'Agile / Scrum' },
    { key: 'milestones', label: 'Milestones', module: 'Project Management' },
    { key: 'risks', label: 'Risks', module: 'Project Management' },
    { key: 'standups', label: 'Stand-ups', module: 'Agile / Scrum' },
    { key: 'retrospectives', label: 'Retrospectives', module: 'Agile / Scrum' },
    { key: 'users', label: 'People', module: 'User Management' },
    { key: 'teams', label: 'Teams', module: 'Team Management' },
    { key: 'departments', label: 'Departments', module: 'User Management' },
    { key: 'roles', label: 'Roles & permissions', module: 'Role Management' },
    { key: 'attendance', label: 'Attendance', module: 'Attendance' },
    { key: 'assignments', label: 'Assignments', module: 'Assignments' },
    { key: 'courses', label: 'Courses', module: 'Learning Management' },
    { key: 'enrollments', label: 'Learning progress', module: 'Learning Management' },
    { key: 'assessments', label: 'Assessments', module: 'Learning Management' },
    { key: 'attempts', label: 'Assessment attempts', module: 'Learning Management' },
    { key: 'skills', label: 'Skills', module: 'Skills Management' },
    { key: 'reviews', label: 'Performance reviews', module: 'Performance' },
    { key: 'announcements', label: 'Announcements', module: 'Collaboration' },
    { key: 'comments', label: 'Comments', module: 'Collaboration' },
    { key: 'documents', label: 'Documents', module: 'File Management' },
    { key: 'notifications', label: 'Notifications', module: 'Notifications' },
    { key: 'audit', label: 'Audit log', module: 'Administration' },
    { key: 'settings', label: 'Settings', module: 'Administration' },
    { key: 'reports', label: 'Reports', module: 'Analytics' },
];
export const statuses = ['Backlog', 'To do', 'In progress', 'Review', 'Testing', 'Done'];
export const projectKinds = ['tasks', 'bugs', 'sprints', 'milestones', 'risks', 'standups', 'retrospectives', 'announcements'];
export const personalKinds = ['attendance', 'assignments', 'enrollments', 'attempts', 'skills', 'reviews'];
export function can(s: State, me: Item, kind: string, action: Action) { return !!s.roles?.find(r => r.id === me.roleId && r.status === 'Active')?.permissions?.[kind]?.[action]; }
export function reports(s: State, id: string): Set<string> { const result = new Set([id]); let size = 0; while (size !== result.size) {
    size = result.size;
    s.users?.forEach(u => { if (result.has(u.reportingTo))
        result.add(u.id); });
} return result; }
export function members(s: State, p: Item) { return new Set([...(p.managerIds || []), ...s.teams.filter(t => t.status === 'Active' && (p.teamIds || []).includes(t.id)).flatMap(t => t.memberIds || [])]); }
export function manages(s: State, me: Item, p?: Item) { return me.scope === 'organization' || !!p?.managerIds?.includes(me.id); }
export function visible(s: State, me: Item, kind: string, item: Item) {
    if (!can(s, me, kind, 'read'))
        return false;
    if (me.scope === 'organization')
        return true;
    const tree = reports(s, me.id);
    if (kind === 'users')
        return tree.has(item.id) || s.projects.some(p => members(s, p).has(me.id) && members(s, p).has(item.id));
    if (kind === 'roles')
        return item.id === me.roleId;
    if (kind === 'notifications')
        return item.ownerId === me.id;
    if (kind === 'projects')
        return members(s, item).has(me.id);
    if (kind === 'teams')
        return (item.memberIds || []).includes(me.id) || s.projects.some(p => p.managerIds?.includes(me.id) && p.teamIds?.includes(item.id));
    const p = s.projects.find(p => p.id === item.projectId);
    if (p && !members(s, p).has(me.id))
        return false;
    if (['tasks', 'bugs'].includes(kind))
        return manages(s, me, p) || tree.has(item.ownerId);
    if (personalKinds.includes(kind))
        return tree.has(item.ownerId) || (kind === 'assignments' && item.createdBy === me.id);
    if (kind === 'audit')
        return false;
    return true;
}
