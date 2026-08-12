import { useEffect, useState } from 'react';
import { getMyBatches, getMyBatchStats } from '../api/trainerBatch';

export default function useMyBatches() {
  const [batches, setBatches] = useState([]);
  const [stats, setStats] = useState({ myBatches: 0, activeBatches: 0, totalStudents: 0 });
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    Promise.all([getMyBatches(), getMyBatchStats()])
      .then(([batchesRes, statsRes]) => {
        if (cancelled) return;
        setBatches(batchesRes.data.data);
        setStats(statsRes.data.data);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return { batches, stats, loading, error };
}