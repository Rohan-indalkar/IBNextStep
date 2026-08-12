import { useEffect, useState } from 'react';
import AppShell from '../../components/AppShell';
import StudentSidebar from '../../components/StudentSidebar';
import PageHeader from '../../components/PageHeader';
import StatusBadge from '../../components/StatusBadge';
import States from '../../components/States';
import { getMyResume, uploadResume } from '../../api/studentResume';
import { Box, Typography, Button, Card, CardContent, Stack, Divider } from '@mui/material';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';

export default function Resume() {
  const [resume, setResume] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [uploading, setUploading] = useState(false);

  function load() {
    setLoading(true);
    getMyResume()
      .then((res) => setResume(res.data.data))
      .catch((err) => {
        if (err.response?.status !== 404) setError(err.message);
      })
      .finally(() => setLoading(false));
  }

  useEffect(load, []);

  async function handleUpload(e) {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    setError('');
    try {
      await uploadResume(file);
      load();
    } catch (err) {
      setError(err.message);
    } finally {
      setUploading(false);
      e.target.value = '';
    }
  }

  const latest = resume?.versions?.[resume.versions.length - 1];

  return (
    <AppShell roleLabel="Student" sidebar={<StudentSidebar />}>
      <PageHeader
        title="Resume"
        subtitle="Upload your resume for trainer feedback, scoring, and placement eligibility."
        actions={
          <label htmlFor="resumeUpload">
            <Button variant="contained" component="span" startIcon={<CloudUploadIcon />} disabled={uploading}>
              {uploading ? 'Uploading…' : latest ? 'Upload new version' : 'Upload resume'}
            </Button>
          </label>
        }
      />

      {error && <Typography color="error" sx={{ mb: 2 }}>{error}</Typography>}

      <input id="resumeUpload" type="file" accept=".pdf,.doc,.docx" onChange={handleUpload} style={{ display: 'none' }} />

      {loading ? (
        <States state="loading" />
      ) : !resume ? (
        <States state="empty" message="You haven't uploaded a resume yet." />
      ) : (
        <Card>
          <CardContent>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Box>
                <Typography variant="subtitle1" fontWeight="bold">{latest.fileName}</Typography>
                <Typography variant="body2" color="text.secondary">
                  Version {latest.versionNumber} · uploaded {latest.uploadedAt ? new Date(latest.uploadedAt).toLocaleDateString() : ''}
                </Typography>
              </Box>
              <StatusBadge status={resume.currentStatus} />
            </Box>

            {latest.status && (
              <Box sx={{ p: 2, bgcolor: 'background.default', borderRadius: 1, mb: 3 }}>
                {latest.score != null && <Typography variant="body2" fontWeight="bold" sx={{ mb: 1 }}>Score: {latest.score}/100</Typography>}
                {latest.suggestions && <Typography variant="body2">{latest.suggestions}</Typography>}
                {latest.reviewedByTrainerName && (
                  <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
                    Reviewed by {latest.reviewedByTrainerName}
                  </Typography>
                )}
              </Box>
            )}

            {resume.versions.length > 1 && (
              <Box>
                <Typography variant="subtitle2" fontWeight="bold" sx={{ mb: 1 }}>Previous versions</Typography>
                <Stack spacing={1}>
                  {resume.versions.slice(0, -1).reverse().map((v) => (
                    <Box key={v.versionNumber} sx={{ display: 'flex', justifyContent: 'space-between', p: 1.5, bgcolor: 'background.default', borderRadius: 1 }}>
                      <Typography variant="body2">v{v.versionNumber} · {v.fileName}</Typography>
                      <Typography variant="body2">{v.status || 'Not reviewed'}</Typography>
                    </Box>
                  ))}
                </Stack>
              </Box>
            )}
          </CardContent>
        </Card>
      )}
    </AppShell>
  );
}
