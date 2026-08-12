import { NavLink } from 'react-router-dom';
import { List, ListItem, ListItemButton, ListItemIcon, ListItemText, Tooltip } from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import MenuBookIcon from '@mui/icons-material/MenuBook';
import FactCheckIcon from '@mui/icons-material/FactCheck';
import AssignmentIcon from '@mui/icons-material/Assignment';
import QuizIcon from '@mui/icons-material/Quiz';
import CodeIcon from '@mui/icons-material/Code';
import InterpreterModeIcon from '@mui/icons-material/InterpreterMode';
import GradeIcon from '@mui/icons-material/Grade';
import DescriptionIcon from '@mui/icons-material/Description';

const LINKS = [
  { to: '/app/trainer', label: 'Dashboard', icon: DashboardIcon, end: true },
  { to: '/app/trainer/study-materials', label: 'Study materials', icon: MenuBookIcon },
  { to: '/app/trainer/attendance', label: 'Attendance', icon: FactCheckIcon },
  { to: '/app/trainer/assignments', label: 'Assignments', icon: AssignmentIcon },
  { to: '/app/trainer/quizzes', label: 'Quizzes', icon: QuizIcon },
  { to: '/app/trainer/assessments', label: 'Coding assessments', icon: CodeIcon },
  { to: '/app/trainer/mock-interviews', label: 'Mock interviews', icon: InterpreterModeIcon },
  { to: '/app/trainer/evaluations', label: 'Student evaluations', icon: GradeIcon },
  { to: '/app/trainer/resumes', label: 'Resume review', icon: DescriptionIcon },
];

export default function TrainerSidebar({ collapsed = false }) {
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
