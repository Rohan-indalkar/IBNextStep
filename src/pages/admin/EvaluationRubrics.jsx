import { useEffect, useState } from 'react';
import { Card, CardContent, Typography, Box, Chip, TextField, Button, Grid, IconButton } from '@mui/material';
import { Close as CloseIcon } from '@mui/icons-material';
import AppShell from '../../components/AppShell';
import AdminSidebar from '../../components/AdminSidebar';
import PageHeader from '../../components/PageHeader';
import { LoadingState, ErrorState } from '../../components/States';
import { useToast } from '../../context/ToastContext';
import { getAllRubricConfigs, updateRubricConfig } from '../../api/evaluationRubric';

function RubricCard({ label, config, onSaved }) {
  const toast = useToast();
  const [skills, setSkills] = useState(config.skills);
  const [newSkill, setNewSkill] = useState('');
  const [error, setError] = useState('');
  const [saving, setSaving] = useState(false);

  function addSkill() {
    const trimmed = newSkill.trim();
    if (!trimmed || skills.includes(trimmed)) return;
    setSkills((s) => [...s, trimmed]);
    setNewSkill('');
  }

  function removeSkill(skill) {
    setSkills((s) => s.filter((x) => x !== skill));
  }

  async function handleSave() {
    setError('');
    if (skills.length < 3) {
      setError('A rubric needs at least 3 distinct skills.');
      return;
    }
    setSaving(true);
    try {
      await updateRubricConfig(config.trainerType, skills);
      toast.success(`${label} saved.`);
      onSaved();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <Card sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      <CardContent sx={{ flexGrow: 1 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', mb: 1 }}>
          <Typography variant="h6">{label}</Typography>
          <Typography variant="caption" color="text.secondary">
            {config.customized ? `Customised${config.updatedByAdminName ? ` by ${config.updatedByAdminName}` : ''}` : 'Using default'}
          </Typography>
        </Box>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Skills scored (0–10) each time a trainer submits an evaluation. Already-submitted evaluations keep the
          skill names they were scored against.
        </Typography>

        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, mb: 2 }}>
          {skills.map((s) => (
            <Chip
              key={s}
              label={s}
              onDelete={() => removeSkill(s)}
              deleteIcon={<CloseIcon fontSize="small" />}
              color="default"
            />
          ))}
        </Box>

        <Box sx={{ display: 'flex', gap: 1, mb: 2 }}>
          <TextField
            size="small"
            fullWidth
            value={newSkill}
            onChange={(e) => setNewSkill(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), addSkill())}
            placeholder="Add a skill…"
          />
          <Button variant="outlined" onClick={addSkill}>
            Add
          </Button>
        </Box>

        {error && <Typography color="error" variant="body2" sx={{ mb: 1.5 }}>{error}</Typography>}
        <Button variant="contained" color="primary" onClick={handleSave} disabled={saving}>
          {saving ? 'Saving…' : 'Save rubric'}
        </Button>
      </CardContent>
    </Card>
  );
}

export default function EvaluationRubrics() {
  const [configs, setConfigs] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  function load() {
    setLoading(true);
    getAllRubricConfigs()
      .then((res) => setConfigs(res.data.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  return (
    <AppShell roleLabel="Administrator" sidebar={<AdminSidebar />}>
      <PageHeader title="Evaluation rubrics" subtitle="The skill sets trainers score students against, per trainer type." />
      {loading ? (
        <LoadingState rows={2} />
      ) : error ? (
        <ErrorState message={error} onRetry={load} />
      ) : (
        <Grid container spacing={3}>
          {configs.map((c) => (
            <Grid item xs={12} md={6} key={c.trainerType}>
              <RubricCard
                label={c.trainerType === 'SOFT_SKILL' ? 'Soft Skill rubric' : 'Technical rubric'}
                config={c}
                onSaved={load}
              />
            </Grid>
          ))}
        </Grid>
      )}
    </AppShell>
  );
}
