import { createContext, useCallback, useContext, useState } from 'react';
import { Dialog, DialogTitle, DialogContent, DialogContentText, DialogActions, Button } from '@mui/material';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';

const ConfirmContext = createContext(null);

export function ConfirmProvider({ children }) {
  const [state, setState] = useState(null);

  const confirm = useCallback((message, options = {}) => {
    return new Promise((resolve) => {
      setState({ message, title: options.title || 'Are you sure?', tone: options.tone || 'default', resolve });
    });
  }, []);

  function handleClose(result) {
    state?.resolve(result);
    setState(null);
  }

  return (
    <ConfirmContext.Provider value={confirm}>
      {children}
      <Dialog
        open={Boolean(state)}
        onClose={() => handleClose(false)}
        maxWidth="xs"
        fullWidth
        PaperProps={{ sx: { borderRadius: '24px !important', p: 1.5, boxShadow: '0 20px 48px rgba(0, 0, 0, 0.16)' } }}
      >
        {state && (
          <>
            <DialogTitle sx={{ display: 'flex', alignItems: 'center', gap: 1.5, pb: 1 }}>
              {state.tone === 'danger' && <WarningAmberIcon color="error" />}
              {state.title}
            </DialogTitle>
            <DialogContent>
              <DialogContentText sx={{ color: 'text.secondary' }}>
                {state.message}
              </DialogContentText>
            </DialogContent>
            <DialogActions sx={{ px: 3, pb: 2 }}>
              <Button variant="outlined" color="inherit" onClick={() => handleClose(false)}>
                Cancel
              </Button>
              <Button
                variant="contained"
                color={state.tone === 'danger' ? 'error' : 'primary'}
                onClick={() => handleClose(true)}
              >
                Confirm
              </Button>
            </DialogActions>
          </>
        )}
      </Dialog>
    </ConfirmContext.Provider>
  );
}

export function useConfirm() {
  const ctx = useContext(ConfirmContext);
  if (!ctx) throw new Error('useConfirm must be used within ConfirmProvider');
  return ctx;
}
