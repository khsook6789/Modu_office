import './Skeleton.css';

interface Props {
  width?: string;
  height?: string;
  borderRadius?: string;
  className?: string;
}

export function Skeleton({ width = '100%', height = '1rem', borderRadius = 'var(--radius-md)', className = '' }: Props) {
  return <div className={`skeleton ${className}`} style={{ width, height, borderRadius }} />;
}

/** 카드 형태 스켈레톤 — 회의실 목록 등에서 사용 */
export function SkeletonCard() {
  return (
    <div className="skeleton-card">
      <Skeleton height="160px" borderRadius="var(--radius-lg) var(--radius-lg) 0 0" />
      <div className="skeleton-card-body">
        <Skeleton width="70%" height="1.2rem" />
        <Skeleton width="50%" height="0.9rem" />
        <div className="skeleton-card-row">
          <Skeleton width="30%" height="0.85rem" />
          <Skeleton width="25%" height="0.85rem" />
        </div>
      </div>
    </div>
  );
}

/** 테이블 행 스켈레톤 — 대시보드 등에서 사용 */
export function SkeletonRow({ cols = 5 }: { cols?: number }) {
  return (
    <div className="skeleton-row">
      {Array.from({ length: cols }).map((_, i) => (
        <Skeleton key={i} width={i === 0 ? '40%' : '60%'} height="0.9rem" />
      ))}
    </div>
  );
}

/** 테이블 여러 행 */
export function SkeletonTable({ rows = 5, cols = 5 }: { rows?: number; cols?: number }) {
  return (
    <div className="skeleton-table">
      {Array.from({ length: rows }).map((_, i) => (
        <SkeletonRow key={i} cols={cols} />
      ))}
    </div>
  );
}
