import { NavLink } from 'react-router-dom';
import { List, ListItem, ListItemButton, ListItemIcon, ListItemText, Tooltip } from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import ApartmentIcon from '@mui/icons-material/Apartment';
import PeopleIcon from '@mui/icons-material/People';
import SchoolIcon from '@mui/icons-material/School';
import EventIcon from '@mui/icons-material/Event';
import GradingIcon from '@mui/icons-material/Grading';
import NotificationsIcon from '@mui/icons-material/Notifications';
import PolicyIcon from '@mui/icons-material/Policy';
import AssessmentIcon from '@mui/icons-material/Assessment';

const LINKS = [
  { to: '/app/admin', label: 'Dashboard', icon: DashboardIcon, end: true },
  { to: '/app/admin/departments', label: 'Departments', icon: ApartmentIcon },
  { to: '/app/admin/users', label: 'Users', icon: PeopleIcon },
  { to: '/app/admin/courses', label: 'Courses & skills', icon: SchoolIcon },
  { to: '/app/admin/batches', label: 'Batches', icon: EventIcon },
  { to: '/app/admin/evaluation-rubrics', label: 'Evaluation rubrics', icon: GradingIcon },
  { to: '/app/admin/notifications', label: 'Notifications', icon: NotificationsIcon },
  { to: '/app/admin/audit-logs', label: 'Audit logs', icon: PolicyIcon },
  { to: '/app/admin/reports', label: 'Reports', icon: AssessmentIcon },
];

export default function AdminSidebar({ collapsed = false }) {
  return (
    <List sx={{ width: '100%', p: 0, display: 'flex', flexDirection: 'column', gap: 0.5 }}>
      {LINKS.map((link) => (
        <ListItem key={link.to} disablePadding sx={{ display: 'block' }}>
          <Tooltip title={collapsed ? link.label : ''} placement="right">
            <ListItemButton
              component={NavLink}
              to={link.to}
              end={link.end}
              sx={{
                minHeight: 48,
                justifyContent: collapsed ? 'center' : 'initial',
                px: 2.5,
                borderRadius: 1,
                mb: 0.5,
                '&.active': {
                  bgcolor: 'action.selected',
                  color: 'primary.main',
                  borderLeft: '4px solid',
                  borderColor: 'primary.main',
                  '& .MuiListItemIcon-root': {
                    color: 'primary.main',
                  }
                },
                '&:not(.active)': {
                  borderLeft: '4px solid transparent',
                }
              }}
            >
              <ListItemIcon
                sx={{
                  minWidth: 0,
                  mr: collapsed ? 0 : 2,
                  justifyContent: 'center',
                }}
              >
                <link.icon />
              </ListItemIcon>
              <ListItemText 
                primary={link.label} 
                sx={{ 
                  opacity: collapsed ? 0 : 1, 
                  display: collapsed ? 'none' : 'block',
                  '& .MuiTypography-root': {
                    fontWeight: 600,
                    fontSize: '0.875rem'
                  }
                }} 
              />
            </ListItemButton>
          </Tooltip>
        </ListItem>
      ))}
    </List>
  );
}
