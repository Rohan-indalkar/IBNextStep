import { useEffect, useState } from 'react';
import AppShell from '../../components/AppShell';
import StudentSidebar from '../../components/StudentSidebar';
import PageHeader from '../../components/PageHeader';
import States from '../../components/States';
import { getMyEvaluations, getMyCombinedEvaluation } from '../../api/studentEvaluationView';
import { Box, Typography, Card, CardContent, Grid, Stack, Divider } from '@mui/material';

function RubricSide({ label, evaluation }) {
  if (!evaluation) {
    return (
      <Card sx={{ height: '100%' }}>
        <CardContent>
          <Typography variant="h6" sx={{ mb: 1 }}>{label}</Typography>
          <Typography variant="body2" color="text.secondary">Not evaluated yet.</Typography>
        </CardContent>
      </Card>
    );
  }
  return (
    <Card sx={{ height: '100%' }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 1 }}>{label}</Typography>
        <Typography variant="h3" fontWeight="bold" color="primary.main" sx={{ mb: 1 }}>
          {evaluation.overallRubricScore ?? '—'}/10
        </Typography>
        <Typography variant="body2" fontWeight="bold" sx={{ color: evaluation.finalEligible ? 'success.main' : 'error.main', mb: 2 }}>
          {evaluation.finalEligible ? 'Eligible' : 'Not eligible'}
        </Typography>
        <Stack spacing={0.5}>
          {evaluation.skillScores && Object.entries(evaluation.skillScores).map(([k, v]) => (
            <Box key={k} sx={{ display: 'flex', justifyContent: 'space-between' }}>
              <Typography variant="body2">{k}</Typography>
              <Typography variant="body2" fontWeight="bold">{v}/10</Typography>
            </Box>
          ))}
        </Stack>
        {evaluation.remarks && <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>{evaluation.remarks}</Typography>}
      </CardContent>
    </Card>
  );
}

export default function Evaluations() {
  const [combined, setCombined] = useState(null);
  const [history, setHistory] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([getMyCombinedEvaluation(), getMyEvaluations()])
      .then(([c, h]) => {
        setCombined(c.data.data);
        setHistory(h.data.data);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  return (
    <AppShell roleLabel="Student" sidebar={<StudentSidebar />}>
      <PageHeader
        title="Evaluations"
        subtitle="Review your technical & soft skills trainer evaluations and eligibility score."
      />

      {error && <Typography color="error" sx={{ mb: 2 }}>{error}</Typography>}

      {loading ? (
        <States state="loading" />
      ) : (
        <>
          <Grid container spacing={3} sx={{ mb: 4 }}>
            <Grid item xs={12} sm={6}>
              <RubricSide label="Technical" evaluation={combined?.technical} />
            </Grid>
            <Grid item xs={12} sm={6}>
              <RubricSide label="Soft Skill" evaluation={combined?.softSkill} />
            </Grid>
          </Grid>

          {combined?.combinedRubricScore != null && (
            <Card sx={{ mb: 4 }}>
              <CardContent>
                <Typography variant="body2" color="text.secondary">Combined score</Typography>
                <Typography variant="h4" fontWeight="bold" color="primary.main">{combined.combinedRubricScore}/10</Typography>
                {combined.combinedFinalEligible != null && (
                  <Typography variant="body2" fontWeight="bold" sx={{ color: combined.combinedFinalEligible ? 'success.main' : 'error.main' }}>
                    {combined.combinedFinalEligible ? 'Eligible overall' : 'Not eligible overall'}
                  </Typography>
                )}
              </CardContent>
            </Card>
          )}

          <Card>
            <CardContent>
              <Typography variant="h6" sx={{ mb: 2 }}>Full history</Typography>
              {history.length === 0 ? (
                <Typography variant="body2" color="text.secondary">No evaluations yet.</Typography>
              ) : (
                <Stack spacing={2}>
                  {history.map((h) => (
                    <Box key={h.id} sx={{ p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
                      <Box sx={{ display: 'flex', justifyContent: 'space-between' }}>
                        <Typography variant="body2" fontWeight="bold">{h.trainerType?.replace('_', ' ')} by {h.trainerName}</Typography>
                        <Typography variant="body2">{h.overallRubricScore}/10</Typography>
                      </Box>
                      <Typography variant="caption" color="text.secondary">
                        {h.evaluatedAt ? new Date(h.evaluatedAt).toLocaleDateString() : ''}
                      </Typography>
                    </Box>
                  ))}
                </Stack>
              )}
            </CardContent>
          </Card>
        </>
      )}
    </AppShell>
  );
}
