import { Routes, Route, Navigate } from 'react-router-dom';
import Splash from './pages/Splash';
import Home from './pages/Home';
import Login from './pages/auth/Login';
import VerifyOtp from './pages/auth/VerifyOtp';
import ForgotPassword from './pages/auth/ForgotPassword';
import ResetPassword from './pages/auth/ResetPassword';
import ForcedPasswordChange from './pages/auth/ForcedPasswordChange';
import ProtectedRoute from './routes/ProtectedRoute';
import AppIndex from './pages/dashboard/AppIndex';
import RoleDashboard from './pages/dashboard/RoleDashboard';
import AdminDashboard from './pages/admin/AdminDashboard';
import Departments from './pages/admin/Departments';
import Users from './pages/admin/Users';
import Courses from './pages/admin/Courses';
import Batches from './pages/admin/Batches';
import BatchDetail from './pages/admin/BatchDetail';
import EvaluationRubrics from './pages/admin/EvaluationRubrics';
import Notifications from './pages/admin/Notifications';
import AuditLogs from './pages/admin/AuditLogs';
import Reports from './pages/admin/Reports';
import TrainerDashboard from './pages/trainer/TrainerDashboard';
import StudyMaterials from './pages/trainer/StudyMaterials';
import Attendance from './pages/trainer/Attendance';
import Assignments from './pages/trainer/Assignments';
import Quizzes from './pages/trainer/Quizzes';
import CodingAssessments from './pages/trainer/CodingAssessments';
import MockInterviews from './pages/trainer/MockInterviews';
import StudentEvaluations from './pages/trainer/StudentEvaluations';
import ResumeReview from './pages/trainer/ResumeReview';
import HrDashboard from './pages/hr/HrDashboard';
import Companies from './pages/hr/Companies';
import Placements from './pages/hr/Placements';
import Applications from './pages/hr/Applications';
import StudentDashboard from './pages/student/StudentDashboard';
import StudentStudyMaterials from './pages/student/StudyMaterials';
import StudentAttendance from './pages/student/Attendance';
import StudentAssignments from './pages/student/Assignments';
import Evaluations from './pages/student/Evaluations';
import StudentResume from './pages/student/Resume';
import StudentPlacements from './pages/student/Placements';
import StudentQuizzes from './pages/student/Quizzes';
import QuizAttempt from './pages/student/QuizAttempt';
import StudentCodingAssessments from './pages/student/CodingAssessments';
import AssessmentAttempt from './pages/student/AssessmentAttempt';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Splash />} />
      <Route path="/home" element={<Home />} />

      <Route path="/auth/login" element={<Login />} />
      <Route path="/auth/verify-otp" element={<VerifyOtp />} />
      <Route path="/auth/forgot-password" element={<ForgotPassword />} />
      <Route path="/auth/reset-password" element={<ResetPassword />} />

      <Route element={<ProtectedRoute />}>
        <Route path="/auth/change-password" element={<ForcedPasswordChange />} />
        <Route path="/app" element={<AppIndex />} />
      </Route>

      <Route element={<ProtectedRoute allow={['ADMIN']} />}>
        <Route path="/app/admin" element={<AdminDashboard />} />
        <Route path="/app/admin/departments" element={<Departments />} />
        <Route path="/app/admin/users" element={<Users />} />
        <Route path="/app/admin/courses" element={<Courses />} />
        <Route path="/app/admin/batches" element={<Batches />} />
        <Route path="/app/admin/batches/:id" element={<BatchDetail />} />
        <Route path="/app/admin/evaluation-rubrics" element={<EvaluationRubrics />} />
        <Route path="/app/admin/notifications" element={<Notifications />} />
        <Route path="/app/admin/audit-logs" element={<AuditLogs />} />
        <Route path="/app/admin/reports" element={<Reports />} />
      </Route>
      <Route element={<ProtectedRoute allow={['HR']} />}>
        <Route path="/app/hr" element={<HrDashboard />} />
        <Route path="/app/hr/companies" element={<Companies />} />
        <Route path="/app/hr/placements" element={<Placements />} />
        <Route path="/app/hr/applications" element={<Applications />} />
      </Route>
      <Route element={<ProtectedRoute allow={['TRAINER']} />}>
        <Route path="/app/trainer" element={<TrainerDashboard />} />
        <Route path="/app/trainer/study-materials" element={<StudyMaterials />} />
        <Route path="/app/trainer/attendance" element={<Attendance />} />
        <Route path="/app/trainer/assignments" element={<Assignments />} />
        <Route path="/app/trainer/quizzes" element={<Quizzes />} />
        <Route path="/app/trainer/assessments" element={<CodingAssessments />} />
        <Route path="/app/trainer/mock-interviews" element={<MockInterviews />} />
        <Route path="/app/trainer/evaluations" element={<StudentEvaluations />} />
        <Route path="/app/trainer/resumes" element={<ResumeReview />} />
      </Route>
      <Route element={<ProtectedRoute allow={['STUDENT']} />}>
        <Route path="/app/student" element={<StudentDashboard />} />
        <Route path="/app/student/study-materials" element={<StudentStudyMaterials />} />
        <Route path="/app/student/attendance" element={<StudentAttendance />} />
        <Route path="/app/student/assignments" element={<StudentAssignments />} />
        <Route path="/app/student/evaluations" element={<Evaluations />} />
        <Route path="/app/student/resume" element={<StudentResume />} />
        <Route path="/app/student/placements" element={<StudentPlacements />} />
        <Route path="/app/student/quizzes" element={<StudentQuizzes />} />
        <Route path="/app/student/quizzes/:id/attempt" element={<QuizAttempt />} />
        <Route path="/app/student/assessments" element={<StudentCodingAssessments />} />
        <Route path="/app/student/assessments/:id/attempt" element={<AssessmentAttempt />} />
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
