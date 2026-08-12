import { motion } from 'framer-motion';
import { useNavigate } from 'react-router-dom';
import Logo from '../components/Logo';
import {
  Box,
  Container,
  Typography,
  Button,
  Grid,
  Card,
  Chip,
  Stack,
} from '@mui/material';
import {
  SchoolOutlined,
  PsychologyOutlined,
  BusinessCenterOutlined,
  AdminPanelSettingsOutlined,
  ArrowForwardOutlined,
  AutoAwesome,
  CheckCircleOutlined,
  StarsOutlined,
} from '@mui/icons-material';

const FEATURES = [
  {
    title: 'Governance & Scaling',
    badge: 'Admins',
    description: 'Provision departments, courses, multi-trainer batches, user roles, security & audit logs.',
    icon: AdminPanelSettingsOutlined,
    accent: '#6A1B9A',
    highlights: ['Department & Batch Setup', 'Role-Based Security', 'System Audit Compliance'],
  },
  {
    title: 'Faculty & Classroom Ops',
    badge: 'Trainers',
    description: 'Topic-based AI quiz generator, study notes publishing, attendance, grading & evaluation rubrics.',
    icon: PsychologyOutlined,
    accent: '#1565C0',
    highlights: ['AI Quiz Generator', 'Attendance Management', 'Evaluation Scorecards'],
  },
  {
    title: 'Recruitment & Drives',
    badge: 'HR & Recruiters',
    description: 'Post placement drives, automated eligibility criteria, multi-round interviews & offer management.',
    icon: BusinessCenterOutlined,
    accent: '#2E7D32',
    highlights: ['Placement Drives', 'Interview Scheduler', 'Candidate Shortlisting'],
  },
  {
    title: 'Trainee Learning & Growth',
    badge: 'Students',
    description: 'Interactive coursework, AI coding assessments, MCQ quizzes, resume scoring & placement tracking.',
    icon: SchoolOutlined,
    accent: '#E81838',
    highlights: ['AI Coding Assessments', 'Resume Scoring Engine', 'Placement Drive Tracker'],
  },
];

// Previous exact motion animation variants
const fadeInUp = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0, transition: { duration: 0.5, ease: [0.16, 1, 0.3, 1] } },
};

const staggerContainer = {
  hidden: { opacity: 0 },
  visible: {
    opacity: 1,
    transition: { staggerChildren: 0.1 },
  },
};

export default function Home() {
  const navigate = useNavigate();

  return (
    <Box
      sx={{
        background: '#F8F8E8',
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        color: '#282838',
        overflowX: 'hidden',
      }}
    >
      {/* Top Header - Logo & Brand Badge Only */}
      <Box
        component="header"
        sx={{
          borderBottom: '1px solid rgba(40, 40, 56, 0.10)',
          background: 'rgba(255, 255, 255, 0.95)',
          backdropFilter: 'blur(12px)',
          py: 2,
        }}
      >
        <Container maxWidth="xl" sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <Logo size={36} />
          <Chip
            icon={<AutoAwesome fontSize="small" sx={{ color: '#E81838 !important' }} />}
            label="InfoBeans Foundation"
            variant="outlined"
            size="small"
            sx={{
              bgcolor: '#F8D0D8',
              borderColor: 'rgba(232, 24, 56, 0.3)',
              color: '#E81838',
              fontWeight: 700,
              px: 1.2,
              py: 0.5,
              fontSize: '0.8125rem',
              borderRadius: 999,
            }}
          />
        </Container>
      </Box>

      {/* Main Showcase Container */}
      <Container maxWidth="xl" sx={{ py: { xs: 4, md: 6 }, flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center' }}>
        {/* Centered Hero Heading */}
        <motion.div initial="hidden" animate="visible" variants={staggerContainer} style={{ textAlign: 'center', maxWidth: 720, margin: '0 auto 36px' }}>
          <motion.div variants={fadeInUp}>
            <Box
              sx={{
                display: 'inline-flex',
                alignItems: 'center',
                gap: 1,
                px: 2.2,
                py: 0.6,
                borderRadius: 999,
                background: '#F8D0D8',
                border: '1px solid rgba(232, 24, 56, 0.25)',
                color: '#E81838',
                fontSize: '0.8125rem',
                fontWeight: 700,
                mb: 2,
              }}
            >
              <AutoAwesome fontSize="small" />
              <span>NextStep Enterprise Digital Talent Platform</span>
            </Box>
          </motion.div>

          <motion.div variants={fadeInUp}>
            <Typography
              variant="h1"
              sx={{
                fontSize: { xs: '2.25rem', sm: '3rem', md: '3.5rem' },
                lineHeight: 1.12,
                fontFamily: "'Sora', sans-serif",
                fontWeight: 800,
                letterSpacing: '-0.02em',
                mb: 1.5,
                color: '#282838',
              }}
            >
              <Box component="span" sx={{ color: '#E81838' }}>
                "Creating WOW!"
              </Box>{' '}
              in Enterprise Talent &amp; Placement.
            </Typography>
          </motion.div>

          <motion.div variants={fadeInUp}>
            <Typography variant="body1" color="text.secondary" sx={{ fontSize: { xs: '1rem', md: '1.125rem' }, mb: 3.5, lineHeight: 1.6 }}>
              A unified digital ecosystem empowering trainees, trainers, recruiters, and directors through a single connected workspace.
            </Typography>
          </motion.div>

          {/* SINGLE CENTRAL LOGIN BUTTON */}
          <motion.div variants={fadeInUp}>
            <Button
              variant="contained"
              color="primary"
              size="large"
              onClick={() => navigate('/auth/login')}
              endIcon={<ArrowForwardOutlined />}
              sx={{
                px: 5,
                py: 1.65,
                fontSize: '1.0625rem',
                fontWeight: 700,
                borderRadius: 2.5,
                bgcolor: '#E81838',
                color: '#FFFFFF',
                boxShadow: '0 8px 24px rgba(232, 24, 56, 0.25)',
                transition: 'all 0.25s ease',
                '&:hover': {
                  bgcolor: '#C4102C',
                  transform: 'translateY(-2px)',
                  boxShadow: '0 12px 32px rgba(232, 24, 56, 0.35)',
                },
              }}
            >
              Sign In to Workspace →
            </Button>
          </motion.div>
        </motion.div>

        {/* Feature Pillars Showcase - Forced into ONE SINGLE HORIZONTAL LINE on desktop */}
        <motion.div initial="hidden" animate="visible" variants={staggerContainer} style={{ width: '100%' }}>
          <Box
            sx={{
              display: 'grid',
              gridTemplateColumns: { xs: '1fr', sm: 'repeat(2, 1fr)', lg: 'repeat(4, 1fr)' },
              gap: 2.5,
              width: '100%',
            }}
          >
            {FEATURES.map((f, idx) => (
              <motion.div key={idx} variants={fadeInUp} whileHover={{ y: -6 }}>
                <Card
                  sx={{
                    p: 3,
                    height: '100%',
                    borderRadius: 4,
                    background: '#FFFFFF',
                    border: '1px solid rgba(40, 40, 56, 0.10)',
                    boxShadow: '0 6px 24px rgba(40, 40, 56, 0.05)',
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    transition: 'border-color 0.25s, box-shadow 0.25s',
                    '&:hover': {
                      borderColor: '#E81838',
                      boxShadow: '0 12px 32px rgba(40, 40, 56, 0.10)',
                    },
                  }}
                >
                  <Box>
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                      <Box
                        sx={{
                          width: 44,
                          height: 44,
                          borderRadius: 2.5,
                          bgcolor: '#F8D0D8',
                          color: '#E81838',
                          display: 'flex',
                          alignItems: 'center',
                          justifyContent: 'center',
                        }}
                      >
                        <f.icon fontSize="medium" />
                      </Box>
                      <Chip label={f.badge} size="small" sx={{ fontWeight: 800, fontSize: '0.7rem', bgcolor: '#F8F8E8', color: '#282838', border: '1px solid rgba(40, 40, 56, 0.10)' }} />
                    </Box>

                    <Typography variant="h6" sx={{ fontFamily: "'Sora', sans-serif", fontWeight: 700, mb: 1, fontSize: '1.05rem', color: '#282838' }}>
                      {f.title}
                    </Typography>

                    <Typography variant="body2" color="text.secondary" sx={{ mb: 2.5, lineHeight: 1.5, fontSize: '0.8125rem' }}>
                      {f.description}
                    </Typography>
                  </Box>

                  <Stack spacing={0.75} sx={{ pt: 2, borderTop: '1px solid rgba(40, 40, 56, 0.08)' }}>
                    {f.highlights.map((item, i) => (
                      <Box key={i} sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                        <CheckCircleOutlined sx={{ color: '#1E8E3E', fontSize: 14 }} />
                        <Typography variant="caption" fontWeight={600} color="#282838">
                          {item}
                        </Typography>
                      </Box>
                    ))}
                  </Stack>
                </Card>
              </motion.div>
            ))}
          </Box>
        </motion.div>
      </Container>

      {/* Premium Enterprise Footer */}
      <Box
        component="footer"
        sx={{
          background: '#181824',
          color: '#FFFFFF',
          pt: 6,
          pb: 4,
          mt: 8,
          borderTop: '1px solid rgba(255, 255, 255, 0.08)',
          width: '100%',
        }}
      >
        <Container maxWidth="lg" sx={{ mx: 'auto' }}>
          <Box
            sx={{
              display: 'flex',
              flexDirection: { xs: 'column', md: 'row' },
              justifyContent: 'space-between',
              alignItems: 'flex-start',
              gap: { xs: 4, md: 3 },
              mb: 5,
              width: '100%',
            }}
          >
            {/* Column 1: Brand & Slogan */}
            <Box sx={{ flex: '1.2 1 0%', minWidth: { xs: '100%', md: 240 } }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.25, mb: 2 }}>
                <Box
                  sx={{
                    width: 32,
                    height: 32,
                    borderRadius: 2,
                    bgcolor: '#E81838',
                    color: '#FFFFFF',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontWeight: 900,
                    fontSize: '1rem',
                  }}
                >
                  »
                </Box>
                <Typography
                  variant="h6"
                  sx={{
                    fontFamily: "'Sora', sans-serif",
                    fontWeight: 800,
                    color: '#FFFFFF',
                    letterSpacing: '-0.02em',
                  }}
                >
                  IBNextStep
                </Typography>
              </Box>
              <Typography
                variant="body2"
                sx={{ color: '#A0A0B8', lineHeight: 1.6, fontSize: '0.875rem', mb: 2.5, maxWidth: 300 }}
              >
                From first class to offer letter — an end-to-end connected platform for InfoBeans Foundation training & placement.
              </Typography>
              <Chip
                icon={<StarsOutlined sx={{ '&&': { color: '#E81838', fontSize: 16 } }} />}
                label="Creating WOW!"
                size="small"
                sx={{
                  bgcolor: 'rgba(232, 24, 56, 0.12)',
                  color: '#FFFFFF',
                  fontWeight: 700,
                  fontSize: '0.75rem',
                  border: '1px solid rgba(232, 24, 56, 0.3)',
                }}
              />
            </Box>

            {/* Column 2: User Role Portals */}
            <Box sx={{ flex: '0.8 1 0%', minWidth: { xs: '100%', sm: 160 } }}>
              <Typography
                variant="subtitle2"
                sx={{ fontFamily: "'Sora', sans-serif", fontWeight: 700, mb: 2, color: '#FFFFFF', letterSpacing: '0.04em', textTransform: 'uppercase', fontSize: '0.8rem' }}
              >
                Portals
              </Typography>
              <Stack spacing={1.25}>
                {['Admin Dashboard', 'Trainer Portal', 'Student Hub', 'HR & Recruiter Portal'].map((label, i) => (
                  <Typography
                    key={i}
                    variant="body2"
                    onClick={() => navigate('/auth/login')}
                    sx={{
                      color: '#A0A0B8',
                      cursor: 'pointer',
                      fontSize: '0.85rem',
                      transition: 'color 0.2s',
                      '&:hover': { color: '#E81838' },
                    }}
                  >
                    {label}
                  </Typography>
                ))}
              </Stack>
            </Box>

            {/* Column 3: Platform Features */}
            <Box sx={{ flex: '1 1 0%', minWidth: { xs: '100%', sm: 180 } }}>
              <Typography
                variant="subtitle2"
                sx={{ fontFamily: "'Sora', sans-serif", fontWeight: 700, mb: 2, color: '#FFFFFF', letterSpacing: '0.04em', textTransform: 'uppercase', fontSize: '0.8rem' }}
              >
                Key Modules
              </Typography>
              <Stack spacing={1.25}>
                {['Batch & Attendance Mgmt', 'AI Quizzes & Coding Tests', 'Rubric Student Evaluation', 'Placement Drive Pipeline'].map((label, i) => (
                  <Typography
                    key={i}
                    variant="body2"
                    sx={{ color: '#A0A0B8', fontSize: '0.85rem' }}
                  >
                    {label}
                  </Typography>
                ))}
              </Stack>
            </Box>

            {/* Column 4: Info & System Status */}
            <Box sx={{ flex: '1 1 0%', minWidth: { xs: '100%', sm: 200 } }}>
              <Typography
                variant="subtitle2"
                sx={{ fontFamily: "'Sora', sans-serif", fontWeight: 700, mb: 2, color: '#FFFFFF', letterSpacing: '0.04em', textTransform: 'uppercase', fontSize: '0.8rem' }}
              >
                Foundation
              </Typography>
              <Typography variant="body2" sx={{ color: '#A0A0B8', fontSize: '0.85rem', lineHeight: 1.6, mb: 2 }}>
                InfoBeans Foundation
                <br />
                Student Training & Placement System
              </Typography>
              <Box sx={{ display: 'inline-flex', alignItems: 'center', gap: 1, px: 1.5, py: 0.5, borderRadius: 10, bgcolor: 'rgba(30, 142, 62, 0.15)', border: '1px solid rgba(30, 142, 62, 0.3)' }}>
                <Box sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: '#1E8E3E' }} />
                <Typography variant="caption" sx={{ color: '#81C784', fontWeight: 600, fontSize: '0.75rem' }}>
                  All Systems Operational
                </Typography>
              </Box>
            </Box>
          </Box>

          {/* Bottom Bar */}
          <Box
            sx={{
              pt: 3,
              borderTop: '1px solid rgba(255, 255, 255, 0.08)',
              display: 'flex',
              flexDirection: { xs: 'column', sm: 'row' },
              justifyContent: 'space-between',
              alignItems: 'center',
              gap: 2,
            }}
          >
            <Typography variant="caption" sx={{ color: '#A0A0B8', fontWeight: 500 }}>
              © {new Date().getFullYear()} IBNextStep · InfoBeans Foundation. All rights reserved.
            </Typography>
            <Typography variant="caption" sx={{ color: '#A0A0B8', fontWeight: 500 }}>
              Empowering Trainees from Class to Career 🚀
            </Typography>
          </Box>
        </Container>
      </Box>
    </Box>
  );
}
