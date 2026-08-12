import { useEffect, useRef, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Logo from '../../components/Logo';
import {
  listAssignedAssessments,
  startAssessment,
  getQuestion,
  navigate as navigateQuestion,
  saveDraft,
  runCode,
  submitQuestion,
  reviewSubmissions,
  completeAssessment,
  reportWarning,
} from '../../api/studentAssessment';
import { Box, Typography, Button, Card, Grid, Select, MenuItem, Stack } from '@mui/material';

const TEMPLATES = {
  JAVA: 'public class Main {\n    public static void main(String[] args) {\n        \n    }\n}',
  PYTHON3: '# your code here\n',
  CPP: '#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    \n    return 0;\n}',
  C: '#include <stdio.h>\n\nint main() {\n    \n    return 0;\n}',
  JAVASCRIPT: '// your code here\n',
};

export default function AssessmentAttempt() {
  const { id } = useParams();
  const navigate = useNavigate();
  const containerRef = useRef(null);

  const [assessment, setAssessment] = useState(null);
  const [session, setSession] = useState(null);
  const [question, setQuestion] = useState(null);
  const [language, setLanguage] = useState('');
  const [code, setCode] = useState('');
  const [error, setError] = useState('');
  const [running, setRunning] = useState(false);
  const [runResult, setRunResult] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [submitResult, setSubmitResult] = useState(null);
  const [warningCount, setWarningCount] = useState(0);
  const [secondsLeft, setSecondsLeft] = useState(null);
  const [submissions, setSubmissions] = useState(null);

  const codeRef = useRef('');
  codeRef.current = code;
  const languageRef = useRef('');
  languageRef.current = language;
  const doneRef = useRef(false);

  useEffect(() => {
    listAssignedAssessments()
      .then((res) => {
        const a = res.data.data.find((x) => x.id === id);
        setAssessment(a);
      })
      .catch((err) => setError(err.message));

    startAssessment(id)
      .then((res) => setSession(res.data.data))
      .catch((err) => setError(err.message));

    containerRef.current?.requestFullscreen?.().catch(() => {});
  }, [id]);

  const loadQuestion = useCallback(
    (questionId, drafts) => {
      getQuestion(id, questionId)
        .then((res) => {
          const q = res.data.data;
          setQuestion(q);
          const draft = drafts?.[questionId];
          const lang = draft?.language || q.allowedLanguages[0];
          setLanguage(lang);
          setCode(draft?.code || TEMPLATES[lang] || '');
          setRunResult(null);
          setSubmitResult(null);
        })
        .catch((err) => setError(err.message));
    },
    [id]
  );

  useEffect(() => {
    if (!session || !assessment?.questionIds) return;
    const qid = assessment.questionIds[session.currentQuestionIndex];
    if (qid) loadQuestion(qid, session.drafts);
  }, [session, assessment, loadQuestion]);

  useEffect(() => {
    if (!assessment || secondsLeft !== null) return;
    setSecondsLeft(assessment.durationMinutes * 60);
  }, [assessment, secondsLeft]);

  const handleComplete = useCallback(async () => {
    if (doneRef.current) return;
    doneRef.current = true;
    try {
      try {
        await completeAssessment(id);
      } catch (completeErr) {
        // fall through
      }
      const res = await reviewSubmissions(id);
      setSubmissions(res.data.data);
    } catch (err) {
      setError(err.message);
    } finally {
      if (document.fullscreenElement) document.exitFullscreen?.().catch(() => {});
    }
  }, [id]);

  useEffect(() => {
    if (secondsLeft === null || submissions) return;
    if (secondsLeft <= 0) {
      handleComplete();
      return;
    }
    const t = setTimeout(() => setSecondsLeft((s) => s - 1), 1000);
    return () => clearTimeout(t);
  }, [secondsLeft, submissions, handleComplete]);

  useEffect(() => {
    if (!question || submissions) return;
    const interval = setInterval(() => {
      saveDraft(id, question.id, languageRef.current, codeRef.current).catch(() => {});
    }, 10000);
    return () => clearInterval(interval);
  }, [id, question, submissions]);

  const logWarning = useCallback(
    (type) => {
      if (doneRef.current) return;
      reportWarning(id, type)
        .then((res) => {
          setWarningCount((c) => c + 1);
          if (res.data?.data?.autoSubmitted) {
            handleComplete();
          }
        })
        .catch(() => {});
    },
    [id, handleComplete]
  );

  useEffect(() => {
    if (submissions) return;
    function onVisibility() {
      if (document.hidden) logWarning('TAB_SWITCH');
    }
    function onFullscreenChange() {
      if (!document.fullscreenElement) logWarning('FULLSCREEN_EXIT');
    }
    function onCopy(e) {
      e.preventDefault();
      logWarning('COPY_ATTEMPT');
    }
    function onPaste(e) {
      e.preventDefault();
      logWarning('PASTE_ATTEMPT');
    }
    document.addEventListener('visibilitychange', onVisibility);
    document.addEventListener('fullscreenchange', onFullscreenChange);
    document.addEventListener('copy', onCopy);
    document.addEventListener('paste', onPaste);
    return () => {
      document.removeEventListener('visibilitychange', onVisibility);
      document.removeEventListener('fullscreenchange', onFullscreenChange);
      document.removeEventListener('copy', onCopy);
      document.removeEventListener('paste', onPaste);
    };
  }, [logWarning, submissions]);

  async function handleRun() {
    setRunning(true);
    setError('');
    try {
      const res = await runCode(id, question.id, language, code);
      setRunResult(res.data.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setRunning(false);
    }
  }

  async function handleSubmitQuestion() {
    if (!window.confirm('Submit this question? It will be graded against all test cases, including hidden ones.')) return;
    setSubmitting(true);
    setError('');
    try {
      const res = await submitQuestion(id, question.id, language, code);
      setSubmitResult(res.data.data);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  async function goToIndex(idx) {
    try {
      await saveDraft(id, question.id, language, code);
      const res = await navigateQuestion(id, idx);
      setSession(res.data.data);
    } catch (err) {
      setError(err.message);
    }
  }

  function formatTime(s) {
    const m = Math.floor(s / 60);
    const sec = s % 60;
    return `${m}:${String(sec).padStart(2, '0')}`;
  }

  if (submissions) {
    return (
      <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', p: 2, bgcolor: 'background.default' }}>
        <Card sx={{ p: 5, maxWidth: 480, width: '100%', textAlign: 'center' }}>
          <Logo size={32} withWordmark={false} />
          <Typography variant="h5" sx={{ mt: 3, mb: 3 }}>Assessment complete</Typography>
          <Stack spacing={1} sx={{ mb: 4, textAlign: 'left' }}>
            {submissions.map((s) => (
              <Box key={s.id} sx={{ display: 'flex', justifyContent: 'space-between', p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
                <Typography variant="body2">{s.language}</Typography>
                <Typography variant="body2" fontWeight="bold" sx={{ color: s.status === 'ACCEPTED' ? 'success.main' : 'error.main' }}>
                  {s.status} — {s.marksAwarded} marks
                </Typography>
              </Box>
            ))}
          </Stack>
          <Button variant="contained" onClick={() => navigate('/app/student/assessments')}>Back to assessments</Button>
        </Card>
      </Box>
    );
  }

  if (error && !question) {
    return (
      <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', p: 2 }}>
        <Card sx={{ p: 4, maxWidth: 420, textAlign: 'center' }}>
          <Typography color="error" sx={{ mb: 2 }}>{error}</Typography>
          <Button variant="contained" onClick={() => navigate('/app/student/assessments')}>Back</Button>
        </Card>
      </Box>
    );
  }

  if (!assessment || !session || !question) {
    return (
      <Box sx={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <Typography color="text.secondary">Loading assessment…</Typography>
      </Box>
    );
  }

  return (
    <Box ref={containerRef} sx={{ minHeight: '100vh', bgcolor: 'background.default', display: 'flex', flexDirection: 'column' }}>
      <Box component="header" sx={{ bgcolor: 'background.paper', borderBottom: 1, borderColor: 'divider', p: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center', position: 'sticky', top: 0, zIndex: 10 }}>
        <Box>
          <Typography variant="subtitle1" fontWeight="bold" component="span">{assessment.title}</Typography>
          {warningCount > 0 && <Typography variant="caption" color="error.main" sx={{ ml: 2 }}>{warningCount} warning(s) logged</Typography>}
        </Box>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Typography variant="h6" fontWeight="bold" sx={{ color: secondsLeft < 60 ? 'error.main' : 'primary.main' }}>
            {formatTime(secondsLeft ?? 0)}
          </Typography>
          <Button variant="outlined" size="small" onClick={handleComplete}>
            Finish assessment
          </Button>
        </Box>
      </Box>

      <Box sx={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        <Box sx={{ width: 80, borderRight: 1, borderColor: 'divider', p: 2, display: 'flex', flexDirection: 'column', gap: 1, alignItems: 'center', overflowY: 'auto' }}>
          {assessment.questionIds.map((qid, i) => (
            <Box
              component="button"
              key={qid}
              onClick={() => goToIndex(i)}
              sx={{
                width: 36,
                height: 36,
                borderRadius: '50%',
                border: '1.5px solid',
                borderColor: 'divider',
                bgcolor: i === session.currentQuestionIndex ? 'primary.main' : session.drafts?.[qid] ? 'action.selected' : 'transparent',
                color: i === session.currentQuestionIndex ? '#fff' : 'text.primary',
                fontSize: 13,
                cursor: 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                p: 0,
                flexShrink: 0
              }}
            >
              {i + 1}
            </Box>
          ))}
        </Box>

        <Box sx={{ flex: 1, p: 3, overflowY: 'auto', borderRight: 1, borderColor: 'divider', maxWidth: 460 }}>
          <Typography variant="overline" color="primary.main" fontWeight="bold" sx={{ display: 'block', mb: 1 }}>
            {question.difficulty} · {question.marks} marks
          </Typography>
          <Typography variant="h6" sx={{ mb: 2 }}>{question.title}</Typography>
          <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap', mb: 3 }}>{question.problemStatement}</Typography>
          {question.inputFormat && <Typography variant="body2" sx={{ mb: 1 }}><strong>Input:</strong> {question.inputFormat}</Typography>}
          {question.outputFormat && <Typography variant="body2" sx={{ mb: 1 }}><strong>Output:</strong> {question.outputFormat}</Typography>}
          {question.constraints && <Typography variant="body2" sx={{ mb: 3 }}><strong>Constraints:</strong> {question.constraints}</Typography>}
          
          <Stack spacing={1}>
            {question.publicTestCases?.map((tc) => (
              <Box key={tc.id} sx={{ p: 1.5, bgcolor: 'action.hover', borderRadius: 1, fontFamily: 'monospace', fontSize: '0.875rem' }}>
                <Box>Input: {tc.input}</Box>
                <Box>Expected: {tc.expectedOutput}</Box>
              </Box>
            ))}
          </Stack>
        </Box>

        <Box sx={{ flex: 1.5, display: 'flex', flexDirection: 'column', p: 3 }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Select
              value={language}
              onChange={(e) => {
                setLanguage(e.target.value);
                if (!code || code === TEMPLATES[language]) setCode(TEMPLATES[e.target.value] || '');
              }}
              size="small"
              sx={{ minWidth: 120 }}
            >
              {question.allowedLanguages.map((l) => (
                <MenuItem key={l} value={l}>{l}</MenuItem>
              ))}
            </Select>
            {submitResult ? (
              <Typography variant="body2" fontWeight="bold" sx={{ color: submitResult.status === 'ACCEPTED' ? 'success.main' : 'error.main' }}>
                {submitResult.status} — {submitResult.marksAwarded} marks
              </Typography>
            ) : (
              <Box sx={{ display: 'flex', gap: 1 }}>
                <Button variant="outlined" size="small" onClick={handleRun} disabled={running}>
                  {running ? 'Running…' : 'Run'}
                </Button>
                <Button variant="contained" size="small" onClick={handleSubmitQuestion} disabled={submitting}>
                  {submitting ? 'Submitting…' : 'Submit'}
                </Button>
              </Box>
            )}
          </Box>

          <Box
            component="textarea"
            value={code}
            onChange={(e) => setCode(e.target.value)}
            spellCheck={false}
            sx={{
              flex: 1,
              minHeight: 320,
              fontFamily: 'monospace',
              fontSize: '14px',
              p: 2,
              borderRadius: 1,
              border: '1px solid',
              borderColor: 'divider',
              bgcolor: '#282838',
              color: '#F5F4F2',
              resize: 'vertical',
            }}
          />

          {error && <Typography color="error" sx={{ mt: 2 }}>{error}</Typography>}

          {runResult && (
            <Box sx={{ mt: 2 }}>
              {!runResult.compiled && (
                <Box component="pre" sx={{ fontSize: '0.75rem', color: 'error.main', whiteSpace: 'pre-wrap', bgcolor: 'action.hover', p: 2, borderRadius: 1 }}>
                  {runResult.compilationOutput}
                </Box>
              )}
              {runResult.results?.map((r) => (
                <Box key={r.testCaseId} sx={{ display: 'flex', justifyContent: 'space-between', p: 1.5, bgcolor: 'action.hover', borderRadius: 1, fontSize: '0.875rem', mb: 1 }}>
                  <Typography variant="body2" fontFamily="monospace">Input: {r.input}</Typography>
                  <Typography variant="body2" fontWeight="bold" sx={{ color: r.passed ? 'success.main' : 'error.main' }}>
                    {r.passed ? 'Passed' : 'Failed'}
                  </Typography>
                </Box>
              ))}
            </Box>
          )}
        </Box>
      </Box>
    </Box>
  );
}
