import { createTheme } from '@mui/material/styles';

export const getMuiTheme = (mode = 'light') =>
  createTheme({
    palette: {
      mode,
      primary: {
        main: '#E81838',
        light: '#F8D0D8',
        dark: '#C4102C',
        contrastText: '#FFFFFF',
      },
      secondary: {
        main: '#282838',
        contrastText: '#FFFFFF',
      },
      background: {
        default: mode === 'light' ? '#F8F8E8' : '#1B1C24',
        paper: mode === 'light' ? '#FFFFFF' : '#242530',
      },
      text: {
        primary: mode === 'light' ? '#282838' : '#F5F4F2',
        secondary: mode === 'light' ? '#6F6F76' : '#9E9EA5',
      },
      divider: mode === 'light' ? 'rgba(40, 40, 56, 0.10)' : 'rgba(255, 255, 255, 0.10)',
      error: {
        main: '#E81838',
      },
      success: {
        main: mode === 'light' ? '#1E8E3E' : '#34D067',
      },
      warning: {
        main: mode === 'light' ? '#B2790A' : '#E8AC3D',
      },
    },
    typography: {
      fontFamily: "'Inter', system-ui, sans-serif",
      h1: { fontFamily: "'Sora', sans-serif", fontWeight: 700, color: mode === 'light' ? '#282838' : '#F5F4F2' },
      h2: { fontFamily: "'Sora', sans-serif", fontWeight: 700, color: mode === 'light' ? '#282838' : '#F5F4F2' },
      h3: { fontFamily: "'Sora', sans-serif", fontWeight: 700, color: mode === 'light' ? '#282838' : '#F5F4F2' },
      h4: { fontFamily: "'Sora', sans-serif", fontWeight: 700, color: mode === 'light' ? '#282838' : '#F5F4F2' },
      h5: { fontFamily: "'Sora', sans-serif", fontWeight: 700, color: mode === 'light' ? '#282838' : '#F5F4F2' },
      h6: { fontFamily: "'Sora', sans-serif", fontWeight: 700, color: mode === 'light' ? '#282838' : '#F5F4F2' },
      subtitle1: { fontWeight: 600 },
      button: { textTransform: 'none', fontWeight: 600 },
    },
    shape: {
      borderRadius: 12,
    },
    components: {
      MuiButton: {
        styleOverrides: {
          root: {
            borderRadius: 8,
            fontWeight: 600,
            padding: '8px 18px',
            boxShadow: 'none',
            '&:hover': {
              boxShadow: '0 4px 12px rgba(232, 24, 56, 0.20)',
            },
          },
          containedPrimary: {
            backgroundColor: '#E81838',
            color: '#FFFFFF',
            '&:hover': {
              backgroundColor: '#C4102C',
            },
          },
          outlined: {
            borderColor: mode === 'light' ? '#282838' : 'rgba(255, 255, 255, 0.20)',
            color: mode === 'light' ? '#282838' : '#F5F4F2',
            '&:hover': {
              borderColor: '#E81838',
              backgroundColor: mode === 'light' ? '#F8D0D8' : 'rgba(232, 24, 56, 0.15)',
              color: mode === 'light' ? '#E81838' : '#F5F4F2',
            },
          },
        },
      },
      MuiCard: {
        styleOverrides: {
          root: {
            borderRadius: 16,
            backgroundImage: 'none',
            backgroundColor: mode === 'light' ? '#FFFFFF' : '#242530',
            boxShadow: mode === 'light'
              ? '0 6px 24px rgba(40, 40, 56, 0.06)'
              : '0 6px 24px rgba(0, 0, 0, 0.35)',
            border: `1px solid ${mode === 'light' ? 'rgba(40, 40, 56, 0.10)' : 'rgba(255, 255, 255, 0.10)'}`,
          },
        },
      },
      MuiOutlinedInput: {
        styleOverrides: {
          root: {
            borderRadius: 8,
            fontSize: '0.875rem',
            backgroundColor: mode === 'light' ? '#FFFFFF' : '#242530',
            color: mode === 'light' ? '#282838' : '#F5F4F2',
            '& .MuiOutlinedInput-notchedOutline': {
              borderWidth: '1.5px',
              borderColor: mode === 'light' ? 'rgba(40, 40, 56, 0.15)' : 'rgba(255, 255, 255, 0.15)',
            },
            '&:hover .MuiOutlinedInput-notchedOutline': {
              borderColor: mode === 'light' ? '#282838' : '#8B92A0',
            },
            '&.Mui-focused': {
              boxShadow: '0 0 0 3px rgba(232, 24, 56, 0.10)',
            },
            '&.Mui-focused .MuiOutlinedInput-notchedOutline': {
              borderColor: '#E81838',
              borderWidth: '2px',
            },
            '& .MuiOutlinedInput-notchedOutline legend': {
              fontSize: '0.75em',
            },
          },
        },
      },
      MuiInputLabel: {
        styleOverrides: {
          root: {
            fontSize: '0.875rem',
            fontWeight: 500,
            color: mode === 'light' ? '#6F6F76' : '#9E9EA5',
            '&.MuiInputLabel-shrink': {
              padding: '0 4px',
              backgroundColor: mode === 'light' ? '#FFFFFF' : '#242530',
              borderRadius: 2,
              zIndex: 2,
            },
          },
        },
      },
      MuiDialog: {
        styleOverrides: {
          paper: {
            borderRadius: '24px !important',
            boxShadow: mode === 'light' ? '0 20px 48px rgba(0, 0, 0, 0.16)' : '0 20px 48px rgba(0, 0, 0, 0.65)',
            backgroundImage: 'none',
            padding: '8px',
          },
        },
      },
      MuiDialogContent: {
        styleOverrides: {
          root: {
            paddingTop: '16px !important',
          },
        },
      },
      MuiSelect: {
        styleOverrides: {
          select: {
            display: 'flex',
            alignItems: 'center',
            minHeight: 'auto',
          },
        },
      },
      MuiTableCell: {
        styleOverrides: {
          root: {
            borderBottom: `1px solid ${mode === 'light' ? 'rgba(40, 40, 56, 0.10)' : 'rgba(255, 255, 255, 0.10)'}`,
            fontSize: '0.875rem',
            color: mode === 'light' ? '#282838' : '#F5F4F2',
          },
          head: {
            fontWeight: 700,
            color: mode === 'light' ? '#6F6F76' : '#9E9EA5',
            backgroundColor: mode === 'light' ? '#F8F8E8' : '#1F202A',
            textTransform: 'uppercase',
            letterSpacing: '0.04em',
            fontSize: '0.75rem',
          },
        },
      },
      MuiListItemButton: {
        styleOverrides: {
          root: {
            borderRadius: 8,
            marginBottom: 2,
            '&.Mui-selected': {
              borderLeft: '4px solid #E81838',
              borderRadius: '0 8px 8px 0',
              backgroundColor: mode === 'light' ? '#F8D0D8' : 'rgba(232, 24, 56, 0.15)',
              color: '#E81838',
              fontWeight: 700,
              '& .MuiListItemIcon-root': {
                color: '#E81838',
              },
            },
            '&:hover': {
              backgroundColor: mode === 'light' ? 'rgba(248, 208, 216, 0.5)' : 'rgba(232, 24, 56, 0.10)',
            },
          },
        },
      },
    },
  });
