import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import AppShell from '../../components/AppShell';
import StudentSidebar from '../../components/StudentSidebar';
import Modal from '../../components/Modal';
import PageHeader from '../../components/PageHeader';
import StatusBadge from '../../components/StatusBadge';
import States from '../../components/States';
import { listAssignedAssessments } from '../../api/studentAssessment';
import { Box, Typography, Button, Stack, Card, Divider } from '@mui/material';

export default function CodingAssessments() {
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [confirmItem, setConfirmItem] = useState(null);

  useEffect(() => {
    listAssignedAssessments()
      .then((res) => setItems(res.data.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  const now = Date.now();

  return (
    <AppShell roleLabel="Student" sidebar={<StudentSidebar />}>
      <PageHeader
        title="Coding assessments"
        subtitle="Solve hands-on programming challenges and timed coding tests."
      />

      {error && <Typography color="error" sx={{ mb: 2 }}>{error}</Typography>}

      {loading ? (
        <States state="loading" />
      ) : items.length === 0 ? (
        <States state="empty" message="No assessments assigned yet." />
      ) : (
        <Card>
          <Stack divider={<Divider flexItem />}>
            {items.map((a) => {
              const started = new Date(a.startTime).getTime();
              const ended = new Date(a.endTime).getTime();
              const isOpen = a.status === 'PUBLISHED' && now >= started && now <= ended;
              return (
                <Box key={a.id} sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', p: 2 }}>
                  <Box>
                    <Typography variant="subtitle1" fontWeight="bold">{a.title}</Typography>
                    <Typography variant="body2" color="text.secondary">
                      {a.questionIds?.length ?? 0} questions · {a.durationMinutes} min · {new Date(a.startTime).toLocaleString()} – {new Date(a.endTime).toLocaleString()}
                    </Typography>
                  </Box>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                    <StatusBadge status={a.status} />
                    {isOpen ? (
                      <Button variant="contained" size="small" onClick={() => setConfirmItem(a)}>
                        Start
                      </Button>
                    ) : (
                      <Typography variant="body2" color="text.secondary">
                        {now < started ? 'Not open yet' : now > ended ? 'Closed' : a.status}
                      </Typography>
                    )}
                  </Box>
                </Box>
              );
            })}
          </Stack>
        </Card>
      )}

      <Modal open={Boolean(confirmItem)} title={`Start "${confirmItem?.title || ''}"?`} onClose={() => setConfirmItem(null)}>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <Typography variant="body2" color="text.secondary">
            You'll have {confirmItem?.durationMinutes} minutes. Tab switches, fullscreen exits, and copy/paste
            attempts are recorded. Submitting a question grades it against hidden test cases immediately — you can't undo it.
          </Typography>
          <Button variant="contained" fullWidth onClick={() => navigate(`/app/student/assessments/${confirmItem.id}/attempt`)}>
            Start now
          </Button>
        </Box>
      </Modal>
    </AppShell>
  );
}
