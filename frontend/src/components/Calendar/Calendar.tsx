import { useState, useMemo } from 'react';
import './Calendar.css';

interface Props {
  value: string;          // yyyy-MM-dd
  onChange: (date: string) => void;
  minDate?: string;       // yyyy-MM-dd
}

const DAYS = ['일', '월', '화', '수', '목', '금', '토'];

function pad(n: number) { return String(n).padStart(2, '0'); }

function toStr(y: number, m: number, d: number) {
  return `${y}-${pad(m + 1)}-${pad(d)}`;
}

export default function Calendar({ value, onChange, minDate }: Props) {
  const today = new Date();
  const todayStr = toStr(today.getFullYear(), today.getMonth(), today.getDate());

  const selected = value || todayStr;
  const [viewYear, setViewYear] = useState(() => Number(selected.slice(0, 4)));
  const [viewMonth, setViewMonth] = useState(() => Number(selected.slice(5, 7)) - 1);

  const cells = useMemo(() => {
    const firstDay = new Date(viewYear, viewMonth, 1).getDay();
    const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();
    const rows: (number | null)[][] = [];
    let week: (number | null)[] = Array(firstDay).fill(null);

    for (let d = 1; d <= daysInMonth; d++) {
      week.push(d);
      if (week.length === 7) {
        rows.push(week);
        week = [];
      }
    }
    if (week.length > 0) {
      while (week.length < 7) week.push(null);
      rows.push(week);
    }
    return rows;
  }, [viewYear, viewMonth]);

  const prevMonth = () => {
    if (viewMonth === 0) { setViewYear(y => y - 1); setViewMonth(11); }
    else setViewMonth(m => m - 1);
  };

  const nextMonth = () => {
    if (viewMonth === 11) { setViewYear(y => y + 1); setViewMonth(0); }
    else setViewMonth(m => m + 1);
  };

  const isDisabled = (day: number) => {
    const dateStr = toStr(viewYear, viewMonth, day);
    if (minDate && dateStr < minDate) return true;
    return false;
  };

  const isPrevDisabled = minDate
    ? viewYear === Number(minDate.slice(0, 4)) && viewMonth <= Number(minDate.slice(5, 7)) - 1
    : false;

  return (
    <div className="cal">
      <div className="cal-header">
        <button type="button" className="cal-nav" onClick={prevMonth} disabled={isPrevDisabled}>&lt;</button>
        <span className="cal-title">{viewYear}년 {viewMonth + 1}월</span>
        <button type="button" className="cal-nav" onClick={nextMonth}>&gt;</button>
      </div>

      <div className="cal-grid">
        {DAYS.map(d => (
          <div key={d} className={`cal-day-label ${d === '일' ? 'sun' : d === '토' ? 'sat' : ''}`}>{d}</div>
        ))}
        {cells.flat().map((day, i) => {
          if (day === null) return <div key={`e-${i}`} className="cal-cell empty" />;
          const dateStr = toStr(viewYear, viewMonth, day);
          const disabled = isDisabled(day);
          const isToday = dateStr === todayStr;
          const isSelected = dateStr === selected;

          return (
            <button
              key={dateStr}
              type="button"
              className={`cal-cell${isSelected ? ' selected' : ''}${isToday ? ' today' : ''}${disabled ? ' disabled' : ''}`}
              onClick={() => !disabled && onChange(dateStr)}
              disabled={disabled}
            >
              {day}
            </button>
          );
        })}
      </div>
    </div>
  );
}
