import { Dialog, DialogTitle, DialogContent, IconButton } from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';

export default function Modal({ open, title, onClose, children }) {
  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
      PaperProps={{
        sx: {
          borderRadius: '24px !important',
          p: 0.5,
          boxShadow: '0 24px 48px rgba(40, 40, 56, 0.16)',
        }
      }}
    >
      <DialogTitle sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', px: 3, pt: 3, pb: 1, fontWeight: 700, fontFamily: "'Sora', sans-serif", fontSize: '1.25rem', color: 'text.primary' }}>
        {title}
        <IconButton
          aria-label="close"
          onClick={onClose}
          sx={{
            color: (theme) => theme.palette.grey[500],
          }}
        >
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      <DialogContent sx={{ px: 3, pb: 3.5, pt: '12px !important' }}>
        {children}
      </DialogContent>
    </Dialog>
  );
}
