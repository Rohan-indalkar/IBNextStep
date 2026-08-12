import { NavLink } from 'react-router-dom';
import { List, ListItem, ListItemButton, ListItemIcon, ListItemText, Tooltip } from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import BusinessIcon from '@mui/icons-material/Business';
import WorkOutlinedIcon from '@mui/icons-material/WorkOutlined';
import ContactPageIcon from '@mui/icons-material/ContactPage';

const LINKS = [
  { to: '/app/hr', label: 'Dashboard', icon: DashboardIcon, end: true },
  { to: '/app/hr/companies', label: 'Companies', icon: BusinessIcon },
  { to: '/app/hr/placements', label: 'Placement drives', icon: WorkOutlinedIcon },
  { to: '/app/hr/applications', label: 'Applications', icon: ContactPageIcon },
];

export default function HrSidebar({ collapsed = false }) {
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
