import { useEffect } from 'react';

export default function useDocumentTitle(title) {
  useEffect(() => {
    document.title = title ? `${title} | Healthcare Diagnosis Assistant` : 'Healthcare Diagnosis Assistant';
  }, [title]);
}
