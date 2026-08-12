import { useState } from 'react';
import { IconButton, Menu, MenuItem, ListItemIcon, ListItemText } from '@mui/material';
import MoreVertIcon from '@mui/icons-material/MoreVert';

export default function ActionMenu({ items }) {
  const [anchorEl, setAnchorEl] = useState(null);
  const open = Boolean(anchorEl);

  const handleClick = (e) => {
    e.stopPropagation();
    setAnchorEl(e.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const visibleItems = items.filter(Boolean);

  if (visibleItems.length === 0) return null;

  return (
    <>
      <IconButton size="small" onClick={handleClick} sx={{ color: 'text.secondary', '&:hover': { bgcolor: 'action.hover' } }}>
        <MoreVertIcon fontSize="small" />
      </IconButton>
      <Menu
        anchorEl={anchorEl}
        open={open}
        onClose={handleClose}
        transformOrigin={{ horizontal: 'right', vertical: 'top' }}
        anchorOrigin={{ horizontal: 'right', vertical: 'bottom' }}
        slotProps={{
          paper: {
            sx: {
              minWidth: 160,
              boxShadow: '0 8px 24px rgba(0,0,0,0.12)',
              borderRadius: 2,
              border: '1px solid var(--color-border)',
              py: 0.5,
            },
          },
        }}
      >
        {visibleItems.map((item, idx) => (
          <MenuItem
            key={idx}
            disabled={item.disabled}
            onClick={(e) => {
              e.stopPropagation();
              handleClose();
              item.onClick();
            }}
            sx={{
              py: 1,
              px: 2,
              fontSize: '0.85rem',
              fontWeight: 500,
              color: item.color ? `${item.color}.main` : 'text.primary',
              '&:hover': {
                bgcolor: item.color === 'error' ? 'rgba(200, 16, 46, 0.08)' : undefined,
              },
            }}
          >
            {item.icon && (
              <ListItemIcon sx={{ minWidth: 32, color: item.color ? `${item.color}.main` : 'text.secondary' }}>
                {item.icon}
              </ListItemIcon>
            )}
            <ListItemText primaryTypographyProps={{ fontSize: '0.85rem', fontWeight: 600 }}>
              {item.label}
            </ListItemText>
          </MenuItem>
        ))}
      </Menu>
    </>
  );
}
