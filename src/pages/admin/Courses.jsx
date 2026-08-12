import { useEffect, useState, useCallback } from 'react';
import { Box, TextField, InputAdornment, Button, Typography, Stack, Tabs, Tab, Card, CardContent, Chip, Checkbox, FormControlLabel, FormGroup } from '@mui/material';
import { SearchOutlined } from '@mui/icons-material';
import AppShell from '../../components/AppShell';
import AdminSidebar from '../../components/AdminSidebar';
import PageHeader from '../../components/PageHeader';
import DataTable from '../../components/DataTable';
import Modal from '../../components/Modal';
import StatusBadge from '../../components/StatusBadge';
import ActionMenu from '../../components/ActionMenu';
import EditOutlinedIcon from '@mui/icons-material/EditOutlined';
import PsychologyOutlinedIcon from '@mui/icons-material/PsychologyOutlined';
import BlockOutlinedIcon from '@mui/icons-material/BlockOutlined';
import { ErrorState, EmptyState } from '../../components/States';
import { useConfirm } from '../../components/ConfirmDialog';
import { useToast } from '../../context/ToastContext';
import useDebouncedValue from '../../hooks/useDebouncedValue';
import {
  searchCourses,
  createCourse,
  updateCourse,
  assignCourseSkills,
  deactivateCourse,
  listSkills,
  createSkill,
} from '../../api/course';

const EMPTY_COURSE = { name: '', description: '' };
const EMPTY_SKILL = { name: '', description: '' };

function newestFirst(skills) {
  return [...skills].sort((a, b) => (a.id < b.id ? 1 : a.id > b.id ? -1 : 0));
}

export default function Courses() {
  const confirm = useConfirm();
  const toast = useToast();

  const [tab, setTab] = useState(0);
  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [nameFilter, setNameFilter] = useState('');
  const debouncedFilter = useDebouncedValue(nameFilter, 350);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [skills, setSkills] = useState([]);

  const [courseModalOpen, setCourseModalOpen] = useState(false);
  const [editingId, setEditingId] = useState(null);
  const [form, setForm] = useState(EMPTY_COURSE);
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);

  const [skillsModalCourse, setSkillsModalCourse] = useState(null);
  const [selectedSkillIds, setSelectedSkillIds] = useState([]);
  const [savingSkills, setSavingSkills] = useState(false);

  const [skillModalOpen, setSkillModalOpen] = useState(false);
  const [skillForm, setSkillForm] = useState(EMPTY_SKILL);
  const [skillFormError, setSkillFormError] = useState('');
  const [savingSkill, setSavingSkill] = useState(false);

  const loadCourses = useCallback(() => {
    setLoading(true);
    setError('');
    searchCourses({ name: debouncedFilter || undefined, page, size: 10 })
      .then((res) => {
        const paged = res.data.data;
        setRows(paged.content);
        setTotalPages(paged.totalPages);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [debouncedFilter, page]);

  const loadSkills = useCallback(() => {
    listSkills()
      .then((res) => setSkills(newestFirst(res.data.data)))
      .catch((err) => setError(err.message));
  }, []);

  useEffect(() => {
    loadCourses();
  }, [loadCourses]);

  useEffect(() => {
    loadSkills();
  }, [loadSkills]);

  useEffect(() => {
    setPage(0);
  }, [debouncedFilter]);

  function openCreateCourse() {
    setEditingId(null);
    setForm(EMPTY_COURSE);
    setFormError('');
    setCourseModalOpen(true);
  }

  function openEditCourse(course) {
    setEditingId(course.id);
    setForm({ name: course.name, description: course.description || '' });
    setFormError('');
    setCourseModalOpen(true);
  }

  async function handleSaveCourse(e) {
    e.preventDefault();
    setFormError('');
    setSaving(true);
    try {
      if (editingId) {
        await updateCourse(editingId, form);
        toast.success('Course updated.');
      } else {
        await createCourse(form);
        toast.success('Course created.');
      }
      setCourseModalOpen(false);
      loadCourses();
    } catch (err) {
      setFormError(err.message);
    } finally {
      setSaving(false);
    }
  }

  async function handleDeactivate(course) {
    const ok = await confirm(`Deactivate "${course.name}"? Trainers and students will no longer see it as active.`, {
      title: 'Deactivate course?',
      tone: 'danger',
    });
    if (!ok) return;
    try {
      await deactivateCourse(course.id);
      toast.success('Course deactivated.');
      loadCourses();
    } catch (err) {
      setError(err.message);
    }
  }

  function openSkillsModal(course) {
    setSkillsModalCourse(course);
    setSelectedSkillIds(course.skillIds || []);
  }

  async function handleSaveSkills() {
    setSavingSkills(true);
    try {
      await assignCourseSkills(skillsModalCourse.id, selectedSkillIds);
      toast.success('Skills updated.');
      setSkillsModalCourse(null);
      loadCourses();
    } catch (err) {
      setError(err.message);
    } finally {
      setSavingSkills(false);
    }
  }

  function toggleSkill(id) {
    setSelectedSkillIds((prev) => (prev.includes(id) ? prev.filter((s) => s !== id) : [...prev, id]));
  }

  async function handleCreateSkill(e) {
    e.preventDefault();
    setSkillFormError('');
    setSavingSkill(true);
    try {
      const res = await createSkill(skillForm);
      setSkills((prev) => [res.data.data, ...prev]);
      setSkillModalOpen(false);
      setSkillForm(EMPTY_SKILL);
      toast.success('Skill added.');
    } catch (err) {
      setSkillFormError(err.message);
    } finally {
      setSavingSkill(false);
    }
  }

  const skillNameById = Object.fromEntries(skills.map((s) => [s.id, s.name]));

  const columns = [
    { key: 'name', header: 'Name' },
    { key: 'description', header: 'Description', render: (r) => r.description || '—' },
    {
      key: 'skills',
      header: 'Skills',
      render: (r) => (r.skillIds?.length ? r.skillIds.map((id) => skillNameById[id] || id).join(', ') : '—'),
    },
    { key: 'active', header: 'Status', render: (r) => <StatusBadge status={r.active ? 'ACTIVE' : 'INACTIVE'} /> },
    {
      key: 'actions',
      header: '',
      align: 'right',
      render: (r) => (
        <ActionMenu
          items={[
            { label: 'Edit course', icon: <EditOutlinedIcon fontSize="small" />, onClick: () => openEditCourse(r) },
            { label: 'Manage skills', icon: <PsychologyOutlinedIcon fontSize="small" />, onClick: () => openSkillsModal(r) },
            r.active && {
              label: 'Deactivate course',
              icon: <BlockOutlinedIcon fontSize="small" />,
              color: 'error',
              onClick: () => handleDeactivate(r),
            },
          ]}
        />
      ),
    },
  ];

  return (
    <AppShell roleLabel="Administrator" sidebar={<AdminSidebar />}>
      <PageHeader
        title="Courses & Skills"
        subtitle="What batches are built around, and the skill tags trainers score against."
        actions={
          tab === 0 ? (
            <Button variant="contained" color="primary" onClick={openCreateCourse}>
              + New course
            </Button>
          ) : (
            <Button variant="contained" color="primary" onClick={() => setSkillModalOpen(true)}>
              + New skill
            </Button>
          )
        }
      />

      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs value={tab} onChange={(_, val) => setTab(val)} aria-label="courses and skills tabs">
          <Tab label="Courses" />
          <Tab label="Skills" />
        </Tabs>
      </Box>

      {tab === 0 ? (
        <>
          <Box sx={{ mb: 2, maxWidth: 320 }}>
            <TextField
              fullWidth
              size="small"
              placeholder="Search by name…"
              value={nameFilter}
              onChange={(e) => setNameFilter(e.target.value)}
              slotProps={{
                input: {
                  startAdornment: (
                    <InputAdornment position="start">
                      <SearchOutlined fontSize="small" />
                    </InputAdornment>
                  ),
                },
              }}
              sx={{ bgcolor: 'background.paper' }}
            />
          </Box>
          {error ? (
            <ErrorState message={error} onRetry={loadCourses} />
          ) : (
            <DataTable
              columns={columns}
              rows={rows}
              loading={loading}
              emptyLabel="No courses yet — create one to get started."
              page={page}
              totalPages={totalPages}
              onPageChange={setPage}
            />
          )}
        </>
      ) : (
        <Card>
          <CardContent>
            {skills.length === 0 ? (
              <EmptyState title="No skills in the catalog yet" description="Add one to start scoring trainees against it." />
            ) : (
              <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
                {skills.map((s) => (
                  <Chip
                    key={s.id}
                    label={s.name}
                    title={s.description}
                    color="default"
                    sx={{ fontWeight: 500 }}
                  />
                ))}
              </Box>
            )}
          </CardContent>
        </Card>
      )}

      <Modal open={courseModalOpen} title={editingId ? 'Edit course' : 'New course'} onClose={() => setCourseModalOpen(false)}>
        <form onSubmit={handleSaveCourse} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <TextField
            label="Name"
            required
            fullWidth
            value={form.name}
            onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
            size="small"
          />
          <TextField
            label="Description"
            fullWidth
            value={form.description}
            onChange={(e) => setForm((f) => ({ ...f, description: e.target.value }))}
            size="small"
          />
          {formError && <Typography color="error" variant="body2">{formError}</Typography>}
          
          <Box sx={{ mt: 1 }}>
            <Button variant="contained" color="primary" type="submit" disabled={saving} fullWidth>
              {saving ? 'Saving…' : editingId ? 'Save changes' : 'Create course'}
            </Button>
          </Box>
        </form>
      </Modal>

      <Modal open={Boolean(skillsModalCourse)} title={`Skills for ${skillsModalCourse?.name || ''}`} onClose={() => setSkillsModalCourse(null)}>
        {skills.length === 0 ? (
          <EmptyState title="No skills yet" description="Add some from the Skills tab first." />
        ) : (
          <FormGroup sx={{ mb: 2, display: 'flex', flexDirection: 'column', gap: 1 }}>
            {skills.map((s) => (
              <FormControlLabel
                key={s.id}
                control={
                  <Checkbox
                    checked={selectedSkillIds.includes(s.id)}
                    onChange={() => toggleSkill(s.id)}
                  />
                }
                label={s.name}
              />
            ))}
          </FormGroup>
        )}
        <Box sx={{ mt: 2 }}>
          <Button variant="contained" color="primary" onClick={handleSaveSkills} disabled={savingSkills || skills.length === 0} fullWidth>
            {savingSkills ? 'Saving…' : 'Save skills'}
          </Button>
        </Box>
      </Modal>

      <Modal open={skillModalOpen} title="New skill" onClose={() => setSkillModalOpen(false)}>
        <form onSubmit={handleCreateSkill} style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <TextField
            label="Name"
            required
            fullWidth
            value={skillForm.name}
            onChange={(e) => setSkillForm((f) => ({ ...f, name: e.target.value }))}
            size="small"
          />
          <TextField
            label="Description"
            fullWidth
            value={skillForm.description}
            onChange={(e) => setSkillForm((f) => ({ ...f, description: e.target.value }))}
            size="small"
          />
          {skillFormError && <Typography color="error" variant="body2">{skillFormError}</Typography>}
          
          <Box sx={{ mt: 1 }}>
            <Button variant="contained" color="primary" type="submit" disabled={savingSkill} fullWidth>
              {savingSkill ? 'Saving…' : 'Create skill'}
            </Button>
          </Box>
        </form>
      </Modal>
    </AppShell>
  );
}
