import { useEffect, useState, useCallback, useRef, useMemo } from 'react';
import { useParams, Link as RouterLink } from 'react-router-dom';
import { Box, Card, CardContent, Typography, Button, TextField, MenuItem, Grid, Link, Stack, Checkbox, FormGroup, InputAdornment } from '@mui/material';
import { SearchOutlined } from '@mui/icons-material';
import AppShell from '../../components/AppShell';
import AdminSidebar from '../../components/AdminSidebar';
import { LoadingState, ErrorState, EmptyState } from '../../components/States';
import { useConfirm } from '../../components/ConfirmDialog';
import { useToast } from '../../context/ToastContext';
import {
  getBatch,
  assignTechnicalTrainer,
  changeTechnicalTrainer,
  assignSoftSkillTrainer,
  changeSoftSkillTrainer,
  addTimetableEntry,
  assignStudents,
  removeStudent,
  bulkImportStudents,
} from '../../api/batch';
import { searchUsers } from '../../api/user';

export default function BatchDetail() {
  const { id } = useParams();
  const fileInputRef = useRef(null);
  const confirm = useConfirm();
  const toast = useToast();

  const [batch, setBatch] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const [technicalTrainers, setTechnicalTrainers] = useState([]);
  const [softSkillTrainers, setSoftSkillTrainers] = useState([]);
  const [students, setStudents] = useState([]);

  const [techTrainerId, setTechTrainerId] = useState('');
  const [softTrainerId, setSoftTrainerId] = useState('');
  const [savingTrainer, setSavingTrainer] = useState(false);

  const [entryDate, setEntryDate] = useState('');
  const [entryTopic, setEntryTopic] = useState('');
  const [entryTrainerId, setEntryTrainerId] = useState('');
  const [savingEntry, setSavingEntry] = useState(false);

  const [studentSearch, setStudentSearch] = useState('');
  const [selectedToAdd, setSelectedToAdd] = useState(new Set());
  const [savingStudents, setSavingStudents] = useState(false);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState(null);
  const [removingId, setRemovingId] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    getBatch(id)
      .then((res) => {
        const b = res.data.data;
        setBatch(b);
        setTechTrainerId(b.technicalTrainerId || '');
        setSoftTrainerId(b.softSkillTrainerId || '');
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [id]);

  useEffect(() => {
    load();
  }, [load]);

  useEffect(() => {
    searchUsers({ role: 'TRAINER', page: 0, size: 100 })
      .then((res) => {
        const all = res.data.data.content;
        setTechnicalTrainers(all.filter((u) => u.trainerType === 'TECHNICAL'));
        setSoftSkillTrainers(all.filter((u) => u.trainerType === 'SOFT_SKILL'));
      })
      .catch(() => {});
    searchUsers({ role: 'STUDENT', page: 0, size: 200 })
      .then((res) => setStudents(res.data.data.content))
      .catch(() => {});
  }, []);

  const trainerNameById = Object.fromEntries(
    [...technicalTrainers, ...softSkillTrainers].map((t) => [t.id, `${t.firstName} ${t.lastName}`])
  );
  const studentById = Object.fromEntries(students.map((s) => [s.id, s]));
  const assignedStudentIds = new Set(batch?.studentIds || []);

  const availableStudents = useMemo(() => {
    const term = studentSearch.trim().toLowerCase();
    return students
      .filter((s) => !assignedStudentIds.has(s.id))
      .filter((s) => !term || `${s.firstName} ${s.lastName} ${s.email}`.toLowerCase().includes(term));
  }, [students, studentSearch, assignedStudentIds]);

  async function handleSaveTechTrainer() {
    if (!techTrainerId) return;
    setSavingTrainer(true);
    setError('');
    try {
      if (batch.technicalTrainerId) await changeTechnicalTrainer(id, techTrainerId);
      else await assignTechnicalTrainer(id, techTrainerId);
      toast.success('Technical trainer updated.');
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setSavingTrainer(false);
    }
  }

  async function handleSaveSoftTrainer() {
    if (!softTrainerId) return;
    setSavingTrainer(true);
    setError('');
    try {
      if (batch.softSkillTrainerId) await changeSoftSkillTrainer(id, softTrainerId);
      else await assignSoftSkillTrainer(id, softTrainerId);
      toast.success('Soft skill trainer updated.');
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setSavingTrainer(false);
    }
  }

  async function handleAddEntry(e) {
    e.preventDefault();
    setError('');
    setSavingEntry(true);
    try {
      await addTimetableEntry(id, { date: entryDate, topic: entryTopic, trainerId: entryTrainerId });
      setEntryDate('');
      setEntryTopic('');
      setEntryTrainerId('');
      toast.success('Timetable entry added.');
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setSavingEntry(false);
    }
  }

  function toggleSelect(studentId) {
    setSelectedToAdd((prev) => {
      const next = new Set(prev);
      if (next.has(studentId)) next.delete(studentId);
      else next.add(studentId);
      return next;
    });
  }

  function selectAllVisible() {
    setSelectedToAdd((prev) => new Set([...prev, ...availableStudents.map((s) => s.id)]));
  }

  function clearSelection() {
    setSelectedToAdd(new Set());
  }

  async function handleBulkAssign() {
    if (selectedToAdd.size === 0) return;
    setSavingStudents(true);
    setError('');
    try {
      await assignStudents(id, [...(batch.studentIds || []), ...selectedToAdd]);
      toast.success(`${selectedToAdd.size} student${selectedToAdd.size === 1 ? '' : 's'} assigned.`);
      setSelectedToAdd(new Set());
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setSavingStudents(false);
    }
  }

  async function handleRemoveStudent(studentId, name) {
    const ok = await confirm(`Remove ${name} from this batch?`, { title: 'Remove student?', tone: 'danger' });
    if (!ok) return;
    setRemovingId(studentId);
    try {
      await removeStudent(id, studentId);
      toast.success('Student removed.');
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setRemovingId(null);
    }
  }

  async function handleBulkImport(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    setImporting(true);
    setError('');
    setImportResult(null);
    try {
      const res = await bulkImportStudents(id, file);
      setImportResult(res.data.data);
      toast.success(`Imported ${res.data.data.successCount} of ${res.data.data.totalRows} rows.`);
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setImporting(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  }

  if (loading) {
    return (
      <AppShell roleLabel="Administrator" sidebar={<AdminSidebar />}>
        <LoadingState rows={4} />
      </AppShell>
    );
  }

  if (!batch) {
    return (
      <AppShell roleLabel="Administrator" sidebar={<AdminSidebar />}>
        <ErrorState message={error || 'Batch not found.'} />
      </AppShell>
    );
  }

  return (
    <AppShell roleLabel="Administrator" sidebar={<AdminSidebar />}>
      <Link component={RouterLink} to="/app/admin/batches" sx={{ fontSize: 13, fontWeight: 600, textDecoration: 'none', mb: 1, display: 'inline-block' }}>
        ← Back to batches
      </Link>
      <Typography variant="h4" sx={{ mb: 3, fontFamily: 'var(--font-display)' }}>
        {batch.name}
      </Typography>

      {error && <Typography color="error" sx={{ mb: 2 }}>{error}</Typography>}

      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Technical trainer</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Currently: {batch.technicalTrainerId ? trainerNameById[batch.technicalTrainerId] || batch.technicalTrainerId : 'Unassigned'}
              </Typography>
              <Stack direction="row" spacing={1}>
                <TextField
                  select
                  size="small"
                  fullWidth
                  value={techTrainerId}
                  onChange={(e) => setTechTrainerId(e.target.value)}
                >
                  <MenuItem value="">Select trainer…</MenuItem>
                  {technicalTrainers.map((t) => (
                    <MenuItem key={t.id} value={t.id}>
                      {t.firstName} {t.lastName}
                    </MenuItem>
                  ))}
                </TextField>
                <Button variant="contained" color="primary" disabled={savingTrainer || !techTrainerId} onClick={handleSaveTechTrainer}>
                  {batch.technicalTrainerId ? 'Change' : 'Assign'}
                </Button>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
        
        <Grid item xs={12} md={6}>
          <Card>
            <CardContent>
              <Typography variant="h6" gutterBottom>Soft skill trainer</Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
                Currently: {batch.softSkillTrainerId ? trainerNameById[batch.softSkillTrainerId] || batch.softSkillTrainerId : 'Unassigned'}
              </Typography>
              <Stack direction="row" spacing={1}>
                <TextField
                  select
                  size="small"
                  fullWidth
                  value={softTrainerId}
                  onChange={(e) => setSoftTrainerId(e.target.value)}
                >
                  <MenuItem value="">Select trainer…</MenuItem>
                  {softSkillTrainers.map((t) => (
                    <MenuItem key={t.id} value={t.id}>
                      {t.firstName} {t.lastName}
                    </MenuItem>
                  ))}
                </TextField>
                <Button variant="contained" color="primary" disabled={savingTrainer || !softTrainerId} onClick={handleSaveSoftTrainer}>
                  {batch.softSkillTrainerId ? 'Change' : 'Assign'}
                </Button>
              </Stack>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Card sx={{ mb: 3 }}>
        <CardContent>
          <Typography variant="h6" gutterBottom>Timetable</Typography>
          <Box component="form" onSubmit={handleAddEntry} sx={{ display: 'flex', gap: 1.5, flexWrap: 'wrap', mb: 3, alignItems: 'center' }}>
            <TextField
              label="Date"
              type="date"
              required
              size="small"
              value={entryDate}
              onChange={(e) => setEntryDate(e.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
            />
            <TextField
              label="Topic"
              required
              size="small"
              value={entryTopic}
              onChange={(e) => setEntryTopic(e.target.value)}
              placeholder="e.g. Spring Boot basics"
              sx={{ flex: 1, minWidth: 180 }}
            />
            <TextField
              select
              label="Trainer"
              required
              size="small"
              value={entryTrainerId}
              onChange={(e) => setEntryTrainerId(e.target.value)}
              sx={{ minWidth: 150 }}
            >
              <MenuItem value="">Select…</MenuItem>
              {[...technicalTrainers, ...softSkillTrainers].map((t) => (
                <MenuItem key={t.id} value={t.id}>
                  {t.firstName} {t.lastName}
                </MenuItem>
              ))}
            </TextField>
            <Button variant="contained" color="primary" type="submit" disabled={savingEntry}>
              {savingEntry ? 'Adding…' : '+ Add entry'}
            </Button>
          </Box>

          {batch.timetable?.length ? (
            <Stack spacing={1}>
              {batch.timetable.map((entry, i) => (
                <Box key={i} sx={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 1, p: 1.5, bgcolor: 'background.default', borderRadius: 1 }}>
                  <Typography variant="body2" fontWeight={600}>{entry.date}</Typography>
                  <Typography variant="body2">{entry.topic}</Typography>
                  <Typography variant="body2" color="text.secondary">{trainerNameById[entry.trainerId] || entry.trainerId}</Typography>
                </Box>
              ))}
            </Stack>
          ) : (
            <EmptyState title="No timetable entries yet" />
          )}
        </CardContent>
      </Card>

      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2, flexWrap: 'wrap', gap: 2 }}>
            <Typography variant="h6">Enrolled students ({batch.studentIds?.length || 0})</Typography>
            <Box>
              <input
                ref={fileInputRef}
                type="file"
                accept=".csv,.xlsx,.xls"
                onChange={handleBulkImport}
                style={{ display: 'none' }}
                id="bulk-import-input"
              />
              <label htmlFor="bulk-import-input">
                <Button variant="outlined" component="span" disabled={importing}>
                  {importing ? 'Importing…' : 'Bulk import (CSV/Excel)'}
                </Button>
              </label>
            </Box>
          </Box>

          {importResult && (
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Imported {importResult.successCount} of {importResult.totalRows} rows
              {importResult.failureCount > 0 && ` — ${importResult.failureCount} failed`}.
            </Typography>
          )}

          {batch.studentIds?.length > 0 && (
            <Stack spacing={0} sx={{ mb: 3 }}>
              {batch.studentIds.map((sid) => {
                const s = studentById[sid];
                const name = s ? `${s.firstName} ${s.lastName}` : sid;
                return (
                  <Box key={sid} sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', py: 1, borderBottom: 1, borderColor: 'divider' }}>
                    <Typography variant="body2">{s ? `${name} (${s.email})` : sid}</Typography>
                    <Button
                      size="small"
                      color="error"
                      onClick={() => handleRemoveStudent(sid, name)}
                      disabled={removingId === sid}
                    >
                      {removingId === sid ? 'Removing…' : 'Remove'}
                    </Button>
                  </Box>
                );
              })}
            </Stack>
          )}

          <Box sx={{ borderTop: 1, borderColor: 'divider', pt: 3 }}>
            <Typography variant="subtitle1" gutterBottom>Add students</Typography>

            <Box sx={{ display: 'flex', gap: 1, mb: 2, alignItems: 'center', flexWrap: 'wrap' }}>
              <TextField
                size="small"
                placeholder="Search students…"
                value={studentSearch}
                onChange={(e) => setStudentSearch(e.target.value)}
                sx={{ flex: 1, minWidth: 200 }}
                slotProps={{
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">
                        <SearchOutlined fontSize="small" />
                      </InputAdornment>
                    ),
                  },
                }}
              />
              <Button size="small" variant="text" onClick={selectAllVisible} disabled={availableStudents.length === 0}>
                Select all {availableStudents.length} visible
              </Button>
              <Button size="small" variant="text" onClick={clearSelection} disabled={selectedToAdd.size === 0}>
                Clear selection
              </Button>
            </Box>

            {availableStudents.length === 0 ? (
              <EmptyState title={studentSearch ? 'No matching students' : 'All students are already in this batch'} />
            ) : (
              <Box sx={{ maxHeight: 260, overflowY: 'auto', border: 1, borderColor: 'divider', borderRadius: 1, mb: 2 }}>
                <FormGroup>
                  {availableStudents.map((s) => (
                    <Box
                      key={s.id}
                      sx={{
                        display: 'flex',
                        alignItems: 'center',
                        px: 2,
                        py: 0.5,
                        borderBottom: 1,
                        borderColor: 'divider',
                        bgcolor: selectedToAdd.has(s.id) ? 'action.selected' : 'transparent',
                        '&:hover': { bgcolor: 'action.hover' },
                        cursor: 'pointer'
                      }}
                      onClick={() => toggleSelect(s.id)}
                    >
                      <Checkbox
                        checked={selectedToAdd.has(s.id)}
                        onChange={() => toggleSelect(s.id)}
                        size="small"
                        onClick={(e) => e.stopPropagation()}
                      />
                      <Typography variant="body2">
                        {s.firstName} {s.lastName} <Typography component="span" variant="caption" color="text.secondary">({s.email})</Typography>
                      </Typography>
                    </Box>
                  ))}
                </FormGroup>
              </Box>
            )}

            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 1 }}>
              <Typography variant="body2" color="text.secondary" fontWeight={600}>
                {selectedToAdd.size} selected
              </Typography>
              <Button variant="contained" color="primary" onClick={handleBulkAssign} disabled={selectedToAdd.size === 0 || savingStudents}>
                {savingStudents ? 'Assigning…' : `Assign ${selectedToAdd.size || ''} student${selectedToAdd.size === 1 ? '' : 's'}`}
              </Button>
            </Box>
          </Box>
        </CardContent>
      </Card>
    </AppShell>
  );
}
