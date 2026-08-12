import { useEffect, useRef, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Logo from '../../components/Logo';
import { startQuiz, autoSaveQuiz, submitQuiz, reportViolation, getQuizInstructions } from '../../api/studentQuiz';
import { Box, Typography, Button, Card, CardContent, Checkbox, Radio, FormControlLabel, Stack, Grid, Container } from '@mui/material';

export default function QuizAttempt() {
  const { id } = useParams();
  const navigate = useNavigate();
  const containerRef = useRef(null);

  const [quiz, setQuiz] = useState(null);
  const [questions, setQuestions] = useState([]);
  const [answers, setAnswers] = useState({});
  const [current, setCurrent] = useState(0);
  const [secondsLeft, setSecondsLeft] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [result, setResult] = useState(null);
  const [violationCount, setViolationCount] = useState(0);

  const answersRef = useRef(answers);
  answersRef.current = answers;
  const submittedRef = useRef(false);

  useEffect(() => {
    getQuizInstructions(id)
      .then((res) => setQuiz(res.data.data))
      .catch((err) => setError(err.message));

    startQuiz(id)
      .then((res) => {
        setQuestions(res.data.data);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message);
        setLoading(false);
      });

    containerRef.current?.requestFullscreen?.().catch(() => {});
  }, [id]);

  useEffect(() => {
    if (!quiz) return;
    setSecondsLeft(quiz.durationMinutes * 60);
  }, [quiz]);

  const handleSubmit = useCallback(
    async () => {
      if (submittedRef.current) return;
      submittedRef.current = true;
      setError('');
      setSubmitting(true);
      try {
        const answerPayload = answersRef.current;
        const payload = Array.isArray(answerPayload)
          ? answerPayload.reduce((acc, item) => {
              if (item?.questionId) acc[item.questionId] = item.answers || [];
              return acc;
            }, {})
          : answerPayload;
        await submitQuiz(id, payload);
        navigate('/app/student/quizzes', {
          replace: true,
          state: {
            submissionNotice: quiz?.title
              ? `Quiz "${quiz.title}" has been submitted and closed.`
              : 'Quiz submitted and closed.',
          },
        });
      } catch (err) {
        setError(err.message);
        submittedRef.current = false;
      } finally {
        setSubmitting(false);
        if (document.fullscreenElement) document.exitFullscreen?.catch(() => {});
      }
    },
    [id, questions]
  );

  useEffect(() => {
    if (secondsLeft === null || result) return;
    if (secondsLeft <= 0) {
      handleSubmit();
      return;
    }
    const t = setTimeout(() => setSecondsLeft((s) => s - 1), 1000);
    return () => clearTimeout(t);
  }, [secondsLeft, result, handleSubmit]);

  useEffect(() => {
    if (result || loading) return;
    const interval = setInterval(() => {
      autoSaveQuiz(id, answersRef.current).catch(() => {});
    }, 15000);
    return () => clearInterval(interval);
  }, [id, result, loading]);

  const logViolation = useCallback(
    (type) => {
      if (submittedRef.current) return;
      reportViolation(id, type).catch(() => {});
      setViolationCount((c) => c + 1);
    },
    [id]
  );

  useEffect(() => {
    if (result) return;
    function onVisibility() {
      if (document.hidden) logViolation('TAB_SWITCH');
    }
    function onFullscreenChange() {
      if (!document.fullscreenElement) logViolation('FULLSCREEN_EXIT');
    }
    function onCopy(e) {
      e.preventDefault();
      logViolation('COPY_ATTEMPT');
    }
    function onPaste(e) {
      e.preventDefault();
      logViolation('PASTE_ATTEMPT');
    }
    function onContextMenu(e) {
      e.preventDefault();
      logViolation('RIGHT_CLICK_ATTEMPT');
    }
    document.addEventListener('visibilitychange', onVisibility);
    document.addEventListener('fullscreenchange', onFullscreenChange);
    document.addEventListener('copy', onCopy);
    document.addEventListener('paste', onPaste);
    document.addEventListener('contextmenu', onContextMenu);
    return () => {
      document.removeEventListener('visibilitychange', onVisibility);
      document.removeEventListener('fullscreenchange', onFullscreenChange);
      document.removeEventListener('copy', onCopy);
      document.removeEventListener('paste', onPaste);
      document.removeEventListener('contextmenu', onContextMenu);
    };
  }, [logViolation, result]);

  function questionKey(q) {
    return q?.id ?? q?.questionId ?? q?.assignmentId;
  }

  function setAnswer(qid, values) {
    setAnswers((a) => ({ ...a, [qid]: values }));
  }

  function toggleOption(qid, option, multi) {
    setAnswers((a) => {
      const existing = a[qid] || [];
      if (multi) {
        return { ...a, [qid]: existing.includes(option) ? existing.filter((o) => o !== option) : [...existing, option] };
      }
      return { ...a, [qid]: [option] };
    });
  }

  function formatTime(s) {
    const m = Math.floor(s / 60);
    const sec = s % 60;
    return `${m}:${String(sec).padStart(2, '0')}`;
  }

  if (error && !questions.length) {
    return (
      <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', p: 2 }}>
        <Card sx={{ p: 4, maxWidth: 420, textAlign: 'center' }}>
          <Typography color="error" sx={{ mb: 2 }}>{error}</Typography>
          <Button variant="contained" onClick={() => navigate('/app/student/quizzes')}>Back to quizzes</Button>
        </Card>
      </Box>
    );
  }

  if (result) {
    return (
      <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', p: 2, bgcolor: 'background.default' }}>
        <Card sx={{ p: 5, maxWidth: 440, textAlign: 'center' }}>
          <Logo size={32} withWordmark={false} />
          <Typography variant="h5" sx={{ mt: 3, mb: 1 }}>{result.passed ? 'Quiz passed!' : 'Quiz submitted'}</Typography>
          <Typography variant="h2" fontWeight="bold" sx={{ color: result.passed ? 'success.main' : 'error.main', my: 2 }}>
            {result.percentage}%
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
            {result.obtainedMarks}/{result.totalMarks} marks · {result.correctAnswers} correct, {result.wrongAnswers} wrong
            {result.pendingManualGrading > 0 && ` · ${result.pendingManualGrading} pending manual grading`}
          </Typography>
          <Button variant="contained" onClick={() => navigate('/app/student/quizzes')}>Back to quizzes</Button>
        </Card>
      </Box>
    );
  }

  if (loading || !quiz) {
    return (
      <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Typography color="text.secondary">Loading quiz…</Typography>
      </Box>
    );
  }

  const q = questions[current];
  const isMulti = q?.type === 'MULTIPLE_SELECT';
  const isText = q?.type === 'FILL_BLANK' || q?.type === 'SHORT_ANSWER';

  return (
    <Box ref={containerRef} sx={{ minHeight: '100vh', bgcolor: 'background.default' }}>
      <Box component="header" sx={{ bgcolor: 'background.paper', borderBottom: 1, borderColor: 'divider', p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center', position: 'sticky', top: 0, zIndex: 10 }}>
        <Box>
          <Typography variant="subtitle1" fontWeight="bold" component="span">{quiz.title}</Typography>
          {violationCount > 0 && <Typography variant="caption" color="error.main" sx={{ ml: 2 }}>{violationCount} violation(s) logged</Typography>}
        </Box>
        <Typography variant="h6" fontWeight="bold" sx={{ color: secondsLeft < 60 ? 'error.main' : 'primary.main' }}>
          {formatTime(secondsLeft ?? 0)}
        </Typography>
      </Box>

      <Container maxWidth="md" sx={{ py: 4 }}>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
          Question {current + 1} of {questions.length} · {q?.marks} mark{q?.marks !== 1 ? 's' : ''}
        </Typography>
        <Card sx={{ p: 4, mb: 3 }}>
          <Typography variant="h6" sx={{ mb: 3 }}>{q?.questionText}</Typography>

          {error && (
            <Typography color="error" sx={{ mb: 2 }}>{error}</Typography>
          )}

          {isText ? (
            <input
              value={(answers[questionKey(q)] || [''])[0]}
              onChange={(e) => setAnswer(questionKey(q), [e.target.value])}
              placeholder="Your answer"
              style={{ width: '100%', padding: '10px 14px', borderRadius: '4px', border: '1px solid #ccc', fontFamily: 'inherit', fontSize: '14px' }}
            />
          ) : (
            <Stack spacing={2}>
              {(q?.options || []).map((opt) => {
                const selected = (answers[questionKey(q)] || []).includes(opt);
                return (
                  <Box
                    key={opt}
                    onClick={() => toggleOption(questionKey(q), opt, isMulti)}
                    sx={{
                      display: 'flex',
                      alignItems: 'center',
                      p: 2,
                      borderRadius: 1,
                      border: '1.5px solid',
                      borderColor: selected ? 'primary.main' : 'divider',
                      bgcolor: selected ? 'action.selected' : 'transparent',
                      cursor: 'pointer',
                    }}
                  >
                    <FormControlLabel
                      control={
                        isMulti ? (
                          <Checkbox checked={selected} onChange={() => {}} onClick={(e) => e.stopPropagation()} disableRipple />
                        ) : (
                          <Radio checked={selected} onChange={() => {}} onClick={(e) => e.stopPropagation()} disableRipple />
                        )
                      }
                      label={opt}
                      sx={{ m: 0, w: '100%' }}
                    />
                  </Box>
                );
              })}
            </Stack>
          )}
        </Card>

        <Box sx={{ display: 'flex', justifyContent: 'space-between', gap: 2, flexWrap: 'wrap' }}>
          <Button variant="outlined" disabled={current === 0} onClick={() => setCurrent((c) => c - 1)}>
            Previous
          </Button>
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            {questions.map((_, i) => (
              <Box
                key={i}
                component="button"
                onClick={() => setCurrent(i)}
                sx={{
                  width: 32,
                  height: 32,
                  borderRadius: '50%',
                  border: '1.5px solid',
                  borderColor: 'divider',
                  bgcolor: i === current ? 'primary.main' : answers[questionKey(questions[i])] ? 'action.selected' : 'transparent',
                  color: i === current ? '#fff' : 'text.primary',
                  fontSize: 12,
                  cursor: 'pointer',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  p: 0,
                }}
              >
                {i + 1}
              </Box>
            ))}
          </Box>
          {current < questions.length - 1 ? (
            <Button variant="outlined" onClick={() => setCurrent((c) => c + 1)}>
              Next
            </Button>
          ) : (
            <Button variant="contained" onClick={() => handleSubmit()} disabled={submitting}>
              {submitting ? 'Submitting…' : 'Submit quiz'}
            </Button>
          )}
        </Box>
      </Container>
    </Box>
  );
}
