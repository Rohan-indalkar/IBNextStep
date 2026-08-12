import { useEffect, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import AppShell from '../../components/AppShell';
import StudentSidebar from '../../components/StudentSidebar';
import Modal from '../../components/Modal';
import PageHeader from '../../components/PageHeader';
import StatusBadge from '../../components/StatusBadge';
import States from '../../components/States';
import { listAssignedQuizzes, getMyResults } from '../../api/studentQuiz';
import { Box, Typography, Button, Tabs, Tab, Card, Stack, Divider, Alert } from '@mui/material';

export default function Quizzes() {
  const navigate = useNavigate();
  const location = useLocation();
  const [tab, setTab] = useState(location.state?.submissionNotice ? 'results' : 'assigned');
  const [quizzes, setQuizzes] = useState([]);
  const [results, setResults] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [confirmQuiz, setConfirmQuiz] = useState(null);

  useEffect(() => {
    setLoading(true);
    Promise.all([listAssignedQuizzes(), getMyResults()])
      .then(([q, r]) => {
        setQuizzes(q.data.data);
        setResults(r.data.data);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (location.state?.submissionNotice) {
      setTab('results');
    }
  }, [location.state?.submissionNotice]);

  const resultByQuizId = Object.fromEntries(results.map((r) => [r.quizId, r]));

  return (
    <AppShell roleLabel="Student" sidebar={<StudentSidebar />}>
      <PageHeader
        title="Quizzes"
        subtitle="Take assigned quizzes and view your score results."
      />

      {location.state?.submissionNotice && (
        <Alert severity="success" sx={{ mb: 3 }}>
          <strong>Quiz submitted and closed.</strong> {location.state.submissionNotice}
        </Alert>
      )}

      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs value={tab} onChange={(e, val) => setTab(val)}>
          <Tab label="Assigned" value="assigned" />
          <Tab label="My results" value="results" />
        </Tabs>
      </Box>

      {error && <Typography color="error" sx={{ mb: 2 }}>{error}</Typography>}

      {loading ? (
        <States state="loading" />
      ) : tab === 'assigned' ? (
        quizzes.length === 0 ? (
          <States state="empty" message="No quizzes assigned yet." />
        ) : (
          <Card>
            <Stack divider={<Divider flexItem />}>
              {quizzes.map((q) => {
                const result = resultByQuizId[q.id];
                const canStart = (q.status === 'ACTIVE' || q.status === 'PUBLISHED') && !result;
                return (
                  <Box
                    key={q.id}
                    role={canStart ? 'button' : undefined}
                    tabIndex={canStart ? 0 : undefined}
                    onClick={canStart ? () => setConfirmQuiz(q) : undefined}
                    onKeyDown={canStart ? (event) => event.key === 'Enter' && setConfirmQuiz(q) : undefined}
                    sx={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      p: 2,
                      cursor: canStart ? 'pointer' : 'default',
                      bgcolor: canStart ? 'action.hover' : 'transparent',
                    }}
                  >
                    <Box>
                      <Typography variant="subtitle1" fontWeight="bold">{q.title}</Typography>
                      <Typography variant="body2" color="text.secondary">
                        {q.topic} · {q.questionCount} questions · {q.durationMinutes} min · pass {q.passingPercentage}%
                      </Typography>
                    </Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
                      <StatusBadge status={q.status} />
                      {result ? (
                        <Typography variant="body2" fontWeight="bold" sx={{ color: result.passed ? 'success.main' : 'error.main' }}>
                          {result.percentage}% {result.passed ? 'Passed' : 'Failed'}
                        </Typography>
                      ) : (q.status === 'ACTIVE' || q.status === 'PUBLISHED') ? (
                        <Button
                          variant="contained"
                          size="small"
                          onClick={(e) => {
                            e.stopPropagation();
                            setConfirmQuiz(q);
                          }}
                        >
                          Start
                        </Button>
                      ) : (
                        <Typography variant="body2" color="text.secondary">Not open</Typography>
                      )}
                    </Box>
                  </Box>
                );
              })}
            </Stack>
          </Card>
        )
      ) : results.length === 0 ? (
        <States state="empty" message="No quiz attempts yet." />
      ) : (
        <Card>
          <Stack divider={<Divider flexItem />}>
            {results.map((r) => (
              <Box key={r.id} sx={{ display: 'flex', justifyContent: 'space-between', p: 2 }}>
                <Typography variant="body2">
                  {r.obtainedMarks}/{r.totalMarks} marks · {r.correctAnswers} correct, {r.wrongAnswers} wrong
                </Typography>
                <Typography variant="body2" fontWeight="bold" sx={{ color: r.passed ? 'success.main' : 'error.main' }}>
                  {r.percentage}%
                </Typography>
              </Box>
            ))}
          </Stack>
        </Card>
      )}

      <Modal open={Boolean(confirmQuiz)} title={`Start "${confirmQuiz?.title || ''}"?`} onClose={() => setConfirmQuiz(null)}>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <Typography variant="body2" color="text.secondary">
            You'll have {confirmQuiz?.durationMinutes} minutes once you start. Tab switches, fullscreen exits, and
            copy/paste attempts are recorded. The quiz auto-submits when time runs out.
          </Typography>
          <Button variant="contained" fullWidth onClick={() => navigate(`/app/student/quizzes/${confirmQuiz.id}/attempt`)}>
            Start now
          </Button>
        </Box>
      </Modal>
    </AppShell>
  );
}
