import { useEffect, useState } from 'react';
import AppShell from '../../components/AppShell';
import StudentSidebar from '../../components/StudentSidebar';
import DataTable from '../../components/DataTable';
import Modal from '../../components/Modal';
import PageHeader from '../../components/PageHeader';
import StatusBadge from '../../components/StatusBadge';
import States from '../../components/States';
import { browsePlacements, getPlacement, applyToPlacement, getMyApplications, downloadPlacementPdf } from '../../api/studentPlacement';
import { Box, Typography, Button, Tabs, Tab, Card, Stack, Divider } from '@mui/material';

export default function Placements() {
  const [tab, setTab] = useState('browse');
  const [rows, setRows] = useState([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [applications, setApplications] = useState([]);

  const [detail, setDetail] = useState(null);
  const [applying, setApplying] = useState(false);

  function loadBrowse() {
    setLoading(true);
    setError('');
    browsePlacements(page, 10)
      .then((res) => {
        const paged = res.data.data;
        setRows(paged.content);
        setTotalPages(paged.totalPages);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }

  function loadApplications() {
    setLoading(true);
    getMyApplications()
      .then((res) => setApplications(res.data.data))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    if (tab === 'browse') loadBrowse();
    else loadApplications();
  }, [tab, page]);

  function openDetail(p) {
    getPlacement(p.id)
      .then((res) => setDetail(res.data.data))
      .catch((err) => setError(err.message));
  }

  async function handleApply() {
    setApplying(true);
    setError('');
    try {
      await applyToPlacement(detail.id);
      const res = await getPlacement(detail.id);
      setDetail(res.data.data);
      loadBrowse();
    } catch (err) {
      setError(err.message);
    } finally {
      setApplying(false);
    }
  }

  async function handleDownloadPdf() {
    try {
      await downloadPlacementPdf(detail.id, detail.pdfFileName);
    } catch (err) {
      setError(err.message);
    }
  }

  const columns = [
    { key: 'title', header: 'Title' },
    { key: 'company', header: 'Company', render: (r) => r.companyName },
    { key: 'type', header: 'Type', render: (r) => r.type.replace('_', ' ') },
    { key: 'package', header: 'Package', render: (r) => (r.packageLpa ? `${r.packageLpa} LPA` : '—') },
    { key: 'eligible', header: 'Eligible', render: (r) => (r.eligible ? <Typography variant="body2" fontWeight="bold" color="success.main">Yes</Typography> : <Typography variant="body2" color="text.secondary">No</Typography>) },
    {
      key: 'status',
      header: 'Status',
      render: (r) => (r.alreadyApplied ? <StatusBadge status={r.myApplicationStatus} /> : '—'),
    },
    {
      key: 'actions',
      header: '',
      render: (r) => (
        <Button variant="text" size="small" onClick={() => openDetail(r)}>
          View
        </Button>
      ),
    },
  ];

  return (
    <AppShell roleLabel="Student" sidebar={<StudentSidebar />}>
      <PageHeader
        title="Placement drives"
        subtitle="Browse campus and off-campus opportunities and track your application status."
      />

      <Box sx={{ borderBottom: 1, borderColor: 'divider', mb: 3 }}>
        <Tabs value={tab} onChange={(e, val) => setTab(val)}>
          <Tab label="Browse drives" value="browse" />
          <Tab label="My applications" value="applications" />
        </Tabs>
      </Box>

      {error && <Typography color="error" sx={{ mb: 2 }}>{error}</Typography>}

      {tab === 'browse' ? (
        <DataTable columns={columns} rows={rows} loading={loading} emptyLabel="No placement drives available." page={page} totalPages={totalPages} onPageChange={setPage} />
      ) : loading ? (
        <States state="loading" />
      ) : applications.length === 0 ? (
        <States state="empty" message="You haven't applied to any drives yet." />
      ) : (
        <Card>
          <Stack divider={<Divider flexItem />}>
            {applications.map((a) => (
              <Box key={a.id} sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', p: 2 }}>
                <Box>
                  <Typography variant="subtitle1" fontWeight="bold">{a.placementTitle}</Typography>
                  <Typography variant="body2" color="text.secondary">
                    {a.companyName} · applied {a.appliedAt ? new Date(a.appliedAt).toLocaleDateString() : ''}
                  </Typography>
                </Box>
                <StatusBadge status={a.status} />
              </Box>
            ))}
          </Stack>
        </Card>
      )}

      <Modal open={Boolean(detail)} title={detail?.title || ''} onClose={() => setDetail(null)}>
        {detail && (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            <Typography variant="body2" color="text.secondary">{detail.description}</Typography>
            <Typography variant="body2">
              {detail.companyName} · {detail.type.replace('_', ' ')} {detail.packageLpa && `· ${detail.packageLpa} LPA`}
            </Typography>
            {detail.applicationDeadline && (
              <Typography variant="body2" color="text.secondary">
                Deadline: {new Date(detail.applicationDeadline).toLocaleString()}
              </Typography>
            )}

            {!detail.eligible && detail.eligibility?.failedCriteria?.length > 0 && (
              <Box sx={{ p: 2, bgcolor: 'background.default', borderRadius: 1 }}>
                <Typography variant="subtitle2" color="error.main" fontWeight="bold" sx={{ mb: 1 }}>Not eligible:</Typography>
                {detail.eligibility.failedCriteria.map((c, i) => (
                  <Typography key={i} variant="body2">
                    {c.criterion}: need {c.required}, you have {c.current}
                  </Typography>
                ))}
              </Box>
            )}

            <Stack direction="row" spacing={2}>
              {detail.pdfDownloadEnabled && (
                <Button variant="outlined" onClick={handleDownloadPdf}>Download JD</Button>
              )}
              {detail.externalApplyLink && (
                <Button variant="outlined" href={detail.externalApplyLink} target="_blank" rel="noreferrer">
                  External link
                </Button>
              )}
            </Stack>

            {detail.alreadyApplied ? (
              <Typography variant="body2" fontWeight="bold" sx={{ mt: 1 }}>
                You've applied — status: <StatusBadge status={detail.myApplicationStatus} />
              </Typography>
            ) : detail.applyEnabled ? (
              <Button variant="contained" onClick={handleApply} disabled={applying} fullWidth sx={{ mt: 1 }}>
                {applying ? 'Applying…' : 'Apply now'}
              </Button>
            ) : (
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                Applications aren't open for this drive.
              </Typography>
            )}
          </Box>
        )}
      </Modal>
    </AppShell>
  );
}
