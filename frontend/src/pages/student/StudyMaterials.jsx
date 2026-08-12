import { useEffect, useState } from 'react';
import AppShell from '../../components/AppShell';
import StudentSidebar from '../../components/StudentSidebar';
import Modal from '../../components/Modal';
import PageHeader from '../../components/PageHeader';
import States from '../../components/States';
import { listAvailableMaterials, downloadMaterialFile } from '../../api/studentMaterial';
import { Box, Typography, Button, Stack, Card, CardContent, CardActionArea } from '@mui/material';
import CloudDownloadIcon from '@mui/icons-material/CloudDownload';

export default function StudyMaterials() {
  const [materials, setMaterials] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [detail, setDetail] = useState(null);

  useEffect(() => {
    listAvailableMaterials()
      .then((res) => setMaterials(res.data?.data || []))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  async function handleDownload(materialId, file) {
    try {
      await downloadMaterialFile(materialId, file.fileId, file.fileName);
    } catch (err) {
      setError(err.message);
    }
  }

  return (
    <AppShell roleLabel="Student" sidebar={<StudentSidebar />}>
      <PageHeader
        title="Study materials"
        subtitle="Access course notes, reference files, and learning materials uploaded by your trainers."
      />

      {error && <Typography color="error" sx={{ mb: 2 }}>{error}</Typography>}

      {loading ? (
        <States state="loading" />
      ) : materials.length === 0 ? (
        <States state="empty" message="Nothing published to your batch yet." />
      ) : (
        <Box sx={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: 2 }}>
          {materials.map((m) => (
            <Card key={m.id}>
              <CardActionArea onClick={() => setDetail(m)} sx={{ p: 2, height: '100%', display: 'flex', flexDirection: 'column', alignItems: 'flex-start', justifyContent: 'flex-start' }}>
                <Typography variant="overline" color="primary.main" fontWeight="bold">
                  {m.contentType?.replace('_', ' ')}
                </Typography>
                <Typography variant="h6" sx={{ my: 1, lineHeight: 1.2 }}>
                  {m.title}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {m.courseName} {m.topic && `· ${m.topic}`}
                </Typography>
              </CardActionArea>
            </Card>
          ))}
        </Box>
      )}

      <Modal open={Boolean(detail)} title={detail?.title || ''} onClose={() => setDetail(null)}>
        {detail && (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            {detail.description && <Typography variant="body2" color="text.secondary">{detail.description}</Typography>}
            <Typography variant="body2">
              {detail.courseName} {detail.module && `· ${detail.module}`} {detail.topic && `· ${detail.topic}`}
            </Typography>
            {detail.externalUrl && (
              <Button href={detail.externalUrl} target="_blank" rel="noreferrer" variant="contained" fullWidth>
                Open link
              </Button>
            )}
            {detail.files?.length > 0 && (
              <Stack spacing={1}>
                {detail.files?.map((f) => (
                  <Button
                    key={f.fileId}
                    variant="outlined"
                    startIcon={<CloudDownloadIcon />}
                    onClick={() => handleDownload(detail.id, f)}
                    sx={{ justifyContent: 'space-between', textTransform: 'none' }}
                  >
                    <Box component="span" sx={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                      {f.fileName}
                    </Box>
                    <Box component="span" sx={{ fontSize: '0.75rem', color: 'text.secondary', ml: 1 }}>
                      {Math.round(f.fileSizeBytes / 1024)} KB
                    </Box>
                  </Button>
                ))}
              </Stack>
            )}
          </Box>
        )}
      </Modal>
    </AppShell>
  );
}
