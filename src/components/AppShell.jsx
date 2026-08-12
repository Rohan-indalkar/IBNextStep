import { useEffect, useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { AppBar, Toolbar, Box, IconButton, Drawer, Chip, useMediaQuery, useTheme, Tooltip, ListItemButton, ListItemIcon, ListItemText } from '@mui/material';
import MenuIcon from '@mui/icons-material/Menu';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import LogoutIcon from '@mui/icons-material/Logout';
import Logo from './Logo';
import ProfileDropdown from './ProfileDropdown';
import NotificationButton from './NotificationButton';
import ThemeToggle from './ThemeToggle';
import { useAuth } from '../context/AuthContext';
import { useConfirm } from './ConfirmDialog';

const DRAWER_WIDTH = 260;
const COLLAPSED_WIDTH = 72;

export default function AppShell({ roleLabel, sidebar, children }) {
  const { signOut } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const confirm = useConfirm();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  
  const [mobileOpen, setMobileOpen] = useState(false);
  const [collapsed, setCollapsed] = useState(() => {
    return localStorage.getItem('ibns_sidebar_collapsed') === 'true';
  });

  useEffect(() => {
    setMobileOpen(false);
  }, [location.pathname]);

  const toggleCollapsed = () => {
    const next = !collapsed;
    setCollapsed(next);
    localStorage.setItem('ibns_sidebar_collapsed', String(next));
  };

  const handleDrawerToggle = () => {
    setMobileOpen(!mobileOpen);
  };

  async function handleLogout() {
    const ok = await confirm('You will be signed out of IBNextStep.', { title: 'Log out?' });
    if (!ok) return;
    await signOut();
    navigate('/auth/login', { replace: true });
  }

  const drawerContent = (
    <Box sx={{ display: 'flex', flexDirection: 'column', height: '100%', pt: 2, overflowX: 'hidden' }}>
      <Box sx={{ flex: 1, overflowY: 'auto', px: collapsed && !isMobile ? 1 : 2 }}>
        {sidebar}
      </Box>
      <Box sx={{ p: 1.5, borderTop: 1, borderColor: 'divider', display: 'flex', flexDirection: 'column', gap: 0.5 }}>
        <Tooltip title={collapsed && !isMobile ? "Log out" : ""} placement="right">
          <ListItemButton
            onClick={handleLogout}
            sx={{
              minHeight: 44,
              justifyContent: collapsed && !isMobile ? 'center' : 'initial',
              px: 2.5,
              borderRadius: 1,
              color: 'error.main',
              '&:hover': { bgcolor: 'rgba(200, 16, 46, 0.08)' }
            }}
          >
            <ListItemIcon sx={{ minWidth: 0, mr: collapsed && !isMobile ? 0 : 2, justifyContent: 'center', color: 'error.main' }}>
              <LogoutIcon fontSize="small" />
            </ListItemIcon>
            <ListItemText
              primary="Log out"
              sx={{
                opacity: collapsed && !isMobile ? 0 : 1,
                display: collapsed && !isMobile ? 'none' : 'block',
                '& .MuiTypography-root': { fontWeight: 700, fontSize: '0.875rem' }
              }}
            />
          </ListItemButton>
        </Tooltip>
        {!isMobile && sidebar && (
          <Box sx={{ display: 'flex', justifyContent: collapsed ? 'center' : 'flex-end', pt: 0.5 }}>
            <IconButton onClick={toggleCollapsed} size="small">
              <ChevronLeftIcon sx={{ transform: collapsed ? 'rotate(180deg)' : 'none', transition: 'transform 0.3s' }} />
            </IconButton>
          </Box>
        )}
      </Box>
    </Box>
  );

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh', bgcolor: 'background.default' }}>
      <AppBar 
        position="fixed" 
        elevation={0}
        sx={{ 
          bgcolor: '#282838', 
          color: '#FFFFFF',
          borderBottom: '1px solid rgba(255, 255, 255, 0.10)',
          zIndex: (theme) => theme.zIndex.drawer + 1,
        }}
      >
        <Toolbar sx={{ justifyContent: 'space-between', minHeight: '64px !important', px: { xs: 2, sm: 3 } }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            {isMobile && sidebar && (
              <IconButton color="inherit" edge="start" onClick={handleDrawerToggle}>
                <MenuIcon sx={{ color: '#FFFFFF' }} />
              </IconButton>
            )}
            <Logo size={32} dark />
          </Box>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Chip 
              label={roleLabel} 
              size="small" 
              sx={{ 
                bgcolor: '#E81838', 
                color: '#FFFFFF',
                fontWeight: 700,
                mr: { xs: 0, sm: 1 },
                display: { xs: 'none', sm: 'flex' }
              }} 
            />
            <ThemeToggle />
            <NotificationButton />
            <ProfileDropdown dark />
          </Box>
        </Toolbar>
      </AppBar>

      {sidebar && (
        <Box component="nav" sx={{ width: { md: collapsed ? COLLAPSED_WIDTH : DRAWER_WIDTH }, flexShrink: { md: 0 }, transition: 'width 0.3s' }}>
          {isMobile ? (
            <Drawer
              variant="temporary"
              open={mobileOpen}
              onClose={handleDrawerToggle}
              ModalProps={{ keepMounted: true }}
              sx={{
                '& .MuiDrawer-paper': { width: DRAWER_WIDTH, boxSizing: 'border-box', pt: '64px' },
              }}
            >
              {drawerContent}
            </Drawer>
          ) : (
            <Drawer
              variant="permanent"
              sx={{
                '& .MuiDrawer-paper': { 
                  width: collapsed ? COLLAPSED_WIDTH : DRAWER_WIDTH, 
                  boxSizing: 'border-box', 
                  pt: '64px',
                  transition: 'width 0.3s',
                  overflowX: 'hidden'
                },
              }}
            >
              {drawerContent}
            </Drawer>
          )}
        </Box>
      )}

      <Box 
        component="main" 
        sx={{ 
          flexGrow: 1, 
          p: { xs: 2, sm: 3, md: 4 }, 
          pt: { xs: '80px', sm: '88px', md: '96px' },
          width: { md: `calc(100% - ${collapsed ? COLLAPSED_WIDTH : DRAWER_WIDTH}px)` },
          maxWidth: 1400,
          mx: 'auto'
        }}
      >
        {children}
      </Box>
    </Box>
  );
}
