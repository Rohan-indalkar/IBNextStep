import { useState } from 'react';
import { Card, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Box, Typography, Button, TextField } from '@mui/material';
import { LoadingState, EmptyState } from './States';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';

function buildPageWindow(page, totalPages) {
  const pages = new Set([0, totalPages - 1, page, page - 1, page + 1]);
  return [...pages]
    .filter((p) => p >= 0 && p < totalPages)
    .sort((a, b) => a - b);
}

export default function DataTable({ columns, rows, loading, emptyLabel = 'No records found.', page = 0, totalPages = 0, onPageChange }) {
  const [jumpValue, setJumpValue] = useState('');

  function handleJump(e) {
    e.preventDefault();
    const target = Number(jumpValue) - 1;
    if (Number.isInteger(target) && target >= 0 && target < totalPages) {
      onPageChange(target);
    }
    setJumpValue('');
  }

  const pageWindow = totalPages > 1 ? buildPageWindow(page, totalPages) : [];

  return (
    <Card sx={{ overflow: 'hidden', width: '100%', borderRadius: 3, border: '1px solid var(--color-border)', boxShadow: '0 4px 16px rgba(0,0,0,0.04)' }}>
      {loading ? (
        <Box sx={{ p: 4 }}>
          <LoadingState rows={5} />
        </Box>
      ) : rows.length === 0 ? (
        <Box sx={{ py: 6 }}>
          <EmptyState title={emptyLabel} />
        </Box>
      ) : (
        <>
          {/* Desktop/tablet: responsive scrollable table */}
          <Box sx={{ display: { xs: 'none', md: 'block' }, width: '100%', overflowX: 'auto' }}>
            <TableContainer sx={{ width: '100%', overflowX: 'auto' }}>
              <Table sx={{ minWidth: 650 }}>
                <TableHead sx={{ bgcolor: 'var(--color-surface-alt)' }}>
                  <TableRow>
                    {columns.map((col) => (
                      <TableCell
                        key={col.key}
                        align={col.align || (col.key === 'actions' ? 'right' : 'left')}
                        sx={{
                          color: 'var(--color-text-muted)',
                          fontWeight: 700,
                          fontSize: '0.72rem',
                          textTransform: 'uppercase',
                          letterSpacing: '0.06em',
                          whiteSpace: 'nowrap',
                          py: 1.75,
                          px: 2.5,
                          borderBottom: '1px solid var(--color-border)',
                        }}
                      >
                        {col.header}
                      </TableCell>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rows.map((row, i) => (
                    <TableRow
                      key={row.id ?? i}
                      hover
                      sx={{
                        '&:hover': { bgcolor: 'rgba(0,0,0,0.02)' },
                        '&:last-child td': { borderBottom: 'none' },
                      }}
                    >
                      {columns.map((col) => (
                        <TableCell
                          key={col.key}
                          align={col.align || (col.key === 'actions' ? 'right' : 'left')}
                          sx={{
                            py: 1.75,
                            px: 2.5,
                            fontSize: '0.875rem',
                            whiteSpace: 'nowrap',
                            borderBottom: '1px solid var(--color-border)',
                            verticalAlign: 'middle',
                          }}
                        >
                          {col.render ? col.render(row) : row[col.key]}
                        </TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </Box>

          {/* Mobile: stacked cards */}
          <Box sx={{ display: { xs: 'flex', md: 'none' }, flexDirection: 'column', gap: 2, p: 2 }}>
            {rows.map((row, i) => (
              <Card key={row.id ?? i} variant="outlined" sx={{ p: 2, borderRadius: 2 }}>
                {columns
                  .filter((col) => col.header)
                  .map((col) => (
                    <Box key={col.key} sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', py: 0.75, borderBottom: '1px border-dotted var(--color-border)' }}>
                      <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 600, fontSize: '0.78rem' }}>
                        {col.header}
                      </Typography>
                      <Box sx={{ textAlign: 'right', fontSize: '0.875rem' }}>
                        {col.render ? col.render(row) : row[col.key]}
                      </Box>
                    </Box>
                  ))}
                {columns.find((col) => !col.header || col.key === 'actions') && (
                  <Box sx={{ mt: 1.5, pt: 1.5, borderTop: '1px solid var(--color-border)', display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
                    {(() => {
                      const actionsCol = columns.find((col) => !col.header || col.key === 'actions');
                      return actionsCol?.render ? actionsCol.render(row) : null;
                    })()}
                  </Box>
                )}
              </Card>
            ))}
          </Box>
        </>
      )}

      {totalPages > 1 && (
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', px: 3, py: 2, borderTop: '1px solid var(--color-border)', flexWrap: 'wrap', gap: 2 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <Button
              startIcon={<ChevronLeftIcon />}
              disabled={page <= 0}
              onClick={() => onPageChange(page - 1)}
              size="small"
              color="inherit"
              sx={{ fontWeight: 600 }}
            >
              Prev
            </Button>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              {pageWindow.map((p, idx) => (
                <span key={p} style={{ display: 'flex', alignItems: 'center' }}>
                  {idx > 0 && pageWindow[idx - 1] !== p - 1 && (
                    <Typography variant="body2" color="text.secondary" sx={{ px: 0.5 }}>…</Typography>
                  )}
                  <Button
                    onClick={() => onPageChange(p)}
                    variant={p === page ? 'contained' : 'outlined'}
                    color={p === page ? 'primary' : 'inherit'}
                    sx={{
                      minWidth: 32,
                      width: 32,
                      height: 32,
                      p: 0,
                      borderRadius: 1,
                      fontWeight: 700,
                      fontSize: '0.8rem',
                    }}
                  >
                    {p + 1}
                  </Button>
                </span>
              ))}
            </Box>
            <Button
              endIcon={<ChevronRightIcon />}
              disabled={page >= totalPages - 1}
              onClick={() => onPageChange(page + 1)}
              size="small"
              color="inherit"
              sx={{ fontWeight: 600 }}
            >
              Next
            </Button>
          </Box>

          {totalPages > 5 && (
            <Box component="form" onSubmit={handleJump} sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
              <Typography variant="body2" color="text.secondary" sx={{ fontWeight: 600, fontSize: '0.8rem' }}>
                Go to page
              </Typography>
              <TextField
                size="small"
                type="number"
                inputProps={{ min: 1, max: totalPages }}
                value={jumpValue}
                onChange={(e) => setJumpValue(e.target.value)}
                placeholder={String(page + 1)}
                sx={{ width: 70, '& input': { py: 0.5, px: 1, textAlign: 'center', fontSize: '0.8rem' } }}
              />
              <Button type="submit" variant="outlined" size="small" color="inherit" sx={{ minWidth: 'auto', px: 1.5, height: 30 }}>
                Go
              </Button>
            </Box>
          )}
        </Box>
      )}
    </Card>
  );
}
