/* global React */
const { useState } = React;

// ============================================================
// Shared data + primitives
// ============================================================
const TYPES = [
  { key: 'features',  name: 'Features',   sub: 'Home page slots',     count: 4,   accent: 'gold',   recent: 'Sunday Welcome — edited 2h ago' },
  { key: 'blog',      name: 'Blog Posts', sub: 'Pastor Jim Reflects', count: 47,  accent: 'terra',  recent: 'On stillness — published Apr 28' },
  { key: 'events',    name: 'Events',     sub: 'Calendar & dates',    count: 12,  accent: 'sage',   recent: 'Spring Picnic — May 18' },
  { key: 'pages',     name: 'Pages',      sub: 'Standing site pages', count: 8,   accent: 'slate',  recent: 'About Us — edited 5d ago' },
  { key: 'photos',    name: 'Photos',     sub: 'Image gallery',       count: 184, accent: 'sage',   recent: 'Easter Service — Apr 5' },
  { key: 'files',     name: 'Files',      sub: 'Bulletins & slides',  count: 23,  accent: 'ink',    recent: 'Bulletin May 4 — uploaded Sat' },
  { key: 'sermons',   name: 'Sermons',    sub: 'Video archive',       count: 156, accent: 'terra',  recent: 'The Vineyard — May 4' },
];
const T = Object.fromEntries(TYPES.map(t => [t.key, t]));

// Abstract geometric marks — simple primitives, one per type.
function Mark({ kind, size = 40, stroke = 'currentColor', fill = 'none' }) {
  const s = size;
  const sw = Math.max(1.25, s / 22);
  if (kind === 'features') {
    // Frame (home slot)
    return (
      <svg width={s} height={s} viewBox="0 0 40 40" fill="none">
        <rect x="4" y="8" width="32" height="24" rx="1" stroke={stroke} strokeWidth={sw} />
        <path d="M4 26 L14 18 L22 24 L30 16 L36 22" stroke={stroke} strokeWidth={sw} strokeLinecap="round" strokeLinejoin="round" />
        <circle cx="28" cy="14" r="2" fill={stroke} />
      </svg>
    );
  }
  if (kind === 'blog') {
    return (
      <svg width={s} height={s} viewBox="0 0 40 40" fill="none">
        <path d="M8 10 H30" stroke={stroke} strokeWidth={sw} strokeLinecap="round" />
        <path d="M8 17 H32" stroke={stroke} strokeWidth={sw} strokeLinecap="round" />
        <path d="M8 24 H26" stroke={stroke} strokeWidth={sw} strokeLinecap="round" />
        <path d="M8 31 H20" stroke={stroke} strokeWidth={sw} strokeLinecap="round" />
      </svg>
    );
  }
  if (kind === 'events') {
    return (
      <svg width={s} height={s} viewBox="0 0 40 40" fill="none">
        <rect x="6" y="9" width="28" height="25" rx="1.5" stroke={stroke} strokeWidth={sw} />
        <path d="M6 16 H34" stroke={stroke} strokeWidth={sw} />
        <path d="M13 6 V12" stroke={stroke} strokeWidth={sw} strokeLinecap="round" />
        <path d="M27 6 V12" stroke={stroke} strokeWidth={sw} strokeLinecap="round" />
        <circle cx="20" cy="25" r="2.5" fill={stroke} />
      </svg>
    );
  }
  if (kind === 'pages') {
    return (
      <svg width={s} height={s} viewBox="0 0 40 40" fill="none">
        <rect x="10" y="6"  width="22" height="28" rx="1" stroke={stroke} strokeWidth={sw} />
        <rect x="6"  y="10" width="22" height="28" rx="1" stroke={stroke} strokeWidth={sw} fill={fill === 'none' ? 'transparent' : fill} />
      </svg>
    );
  }
  if (kind === 'files') {
    return (
      <svg width={s} height={s} viewBox="0 0 40 40" fill="none">
        <path d="M8 12 H18 L21 16 H32 V31 H8 Z" stroke={stroke} strokeWidth={sw} strokeLinejoin="round" />
        <path d="M14 22 H26" stroke={stroke} strokeWidth={sw} strokeLinecap="round" />
      </svg>
    );
  }
  if (kind === 'photos') {
    return (
      <svg width={s} height={s} viewBox="0 0 40 40" fill="none">
        <rect x="6" y="10" width="28" height="22" rx="1.5" stroke={stroke} strokeWidth={sw} />
        <path d="M6 26 L14 19 L20 24 L28 16 L34 21" stroke={stroke} strokeWidth={sw} strokeLinejoin="round" strokeLinecap="round" />
        <circle cx="14" cy="16" r="2" fill={stroke} />
      </svg>
    );
  }
  if (kind === 'sermons') {
    return (
      <svg width={s} height={s} viewBox="0 0 40 40" fill="none">
        <circle cx="20" cy="20" r="14" stroke={stroke} strokeWidth={sw} />
        <path d="M17 14 L27 20 L17 26 Z" fill={stroke} />
      </svg>
    );
  }
  return null;
}

const ACCENT = {
  terra: { ink: '#C24A1F', soft: '#F4D9CC', tint: '#FBE9DF' },
  sage:  { ink: '#5A7257', soft: '#DCE2D6', tint: '#EAEEE4' },
  gold:  { ink: '#A87A2A', soft: '#F1E2BD', tint: '#F8EED1' },
  slate: { ink: '#3D4A60', soft: '#D5DCE6', tint: '#E6EBF2' },
  ink:   { ink: '#1C1A17', soft: '#D8D2C5', tint: '#ECE7DA' },
};

// ============================================================
// Shared chrome
// ============================================================
function TopBar({ tone = 'cream' }) {
  const isCream = tone === 'cream';
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '20px 48px',
      borderBottom: `1px solid ${isCream ? '#E5DFD2' : 'rgba(255,255,255,0.12)'}`,
      background: isCream ? 'transparent' : '#1C1A17',
      color: isCream ? '#1C1A17' : '#F7F4EE',
    }}>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 12 }}>
        <span className="serif" style={{ fontSize: 20, fontWeight: 600, letterSpacing: '-0.01em' }}>Mt Zion</span>
        <span className="mono" style={{ fontSize: 11, letterSpacing: '0.16em', textTransform: 'uppercase', opacity: 0.55 }}>CMS</span>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 28, fontSize: 13 }}>
        <span style={{ opacity: 0.55 }}>Signed in as <span style={{ opacity: 0.9 }}>jim@mtzion.org</span></span>
        <a style={{ color: 'inherit', textDecoration: 'none', borderBottom: '1px solid currentColor', paddingBottom: 1 }}>View site →</a>
        <a style={{ color: 'inherit', textDecoration: 'none', opacity: 0.7 }}>Sign out</a>
      </div>
    </div>
  );
}

// ============================================================
// VARIANT A — BENTO (asymmetric, mixed sizes, two accent hits)
// ============================================================
function BentoVariant() {
  return (
    <div style={{ width: '100%', height: '100%', background: '#F7F4EE', display: 'flex', flexDirection: 'column' }}>
      <TopBar />
      <div style={{ padding: '32px 40px 0', display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
        <h1 className="serif" style={{ margin: 0, fontSize: 36, fontWeight: 500, letterSpacing: '-0.02em' }}>
          Mt Zion Dashboard
        </h1>
        <div className="mono" style={{ fontSize: 11, letterSpacing: '0.18em', textTransform: 'uppercase', color: '#8A8478' }}>
          Thursday · May 7
        </div>
      </div>

      {/* Main grid: 3 large squares + Events column */}
      <div style={{
        padding: '24px 40px 14px',
        display: 'grid',
        gridTemplateColumns: '1fr 1fr 1fr 0.66fr',
        gap: 14,
      }}>
        <BigSquare type={T.blog}     graphic="blog"     />
        <BigSquare type={T.pages}    graphic="pages"    />
        <BigSquare type={T.features} graphic="features" />
        <EventsTall type={T.events} />
      </div>

      {/* Secondary row: Photos + Sermons (Events is in the column above) */}
      <div style={{
        padding: '0 40px 14px',
        display: 'grid',
        gridTemplateColumns: '1.5fr 1.5fr 0.66fr',
        gap: 14,
      }}>
        <PhotosTile type={T.photos} />
        <CalendarTile />
        <div /> {/* keeps the Events column open */}
      </div>

      {/* Split upload bar — Files (left) | Sermon video (right) */}
      <UploadSplitBar />
    </div>
  );
}

// --- Big square tile (Blog / Pages / Features) ---
function BigSquare({ type, graphic }) {
  const a = ACCENT[type.accent];
  return (
    <div style={{
      aspectRatio: '1 / 1',
      borderRadius: 6,
      background: '#FBF9F4',
      border: '1px solid #E5DFD2',
      padding: 20,
      display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
      position: 'relative', overflow: 'hidden',
    }}>
      {/* Header label */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <span className="mono" style={{ fontSize: 10, letterSpacing: '0.18em', textTransform: 'uppercase', color: a.ink }}>
          {type.name}
        </span>
        <span className="mono" style={{ fontSize: 10, color: '#8A8478' }}>{type.count}</span>
      </div>

      {/* Graphic body */}
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '12px 0' }}>
        <SquareGraphic kind={graphic} accent={a} />
      </div>

      {/* Title + recent */}
      <div>
        <div className="serif" style={{ fontSize: 26, fontWeight: 500, letterSpacing: '-0.01em' }}>{type.name}</div>
        <div style={{ fontSize: 11.5, color: '#8A8478', marginTop: 2, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
          {type.recent}
        </div>
      </div>

      {/* Buttons */}
      <div style={{ display: 'flex', gap: 6, marginTop: 12 }}>
        <button style={{ ...btnSm, background: a.ink, flex: 1 }}>+ New</button>
        <button style={btnSmGhost}>All</button>
      </div>
    </div>
  );
}

function SquareGraphic({ kind, accent }) {
  if (kind === 'blog') {
    // Stylized blog post "page" with text lines
    return (
      <div style={{
        width: '78%', aspectRatio: '0.78', background: accent.tint,
        borderRadius: 3, padding: 14, display: 'flex', flexDirection: 'column', gap: 6,
        boxShadow: '4px 4px 0 ' + accent.soft,
      }}>
        <div className="serif" style={{ fontSize: 13, fontWeight: 600, color: accent.ink, lineHeight: 1.15, marginBottom: 4 }}>
          On stillness, and the<br/>shape of an evening.
        </div>
        <div style={{ height: 2, background: accent.ink, opacity: 0.35, borderRadius: 1, width: '85%' }} />
        <div style={{ height: 2, background: accent.ink, opacity: 0.35, borderRadius: 1, width: '95%' }} />
        <div style={{ height: 2, background: accent.ink, opacity: 0.35, borderRadius: 1, width: '70%' }} />
        <div style={{ height: 2, background: accent.ink, opacity: 0.2, borderRadius: 1, width: '90%', marginTop: 4 }} />
        <div style={{ height: 2, background: accent.ink, opacity: 0.2, borderRadius: 1, width: '80%' }} />
      </div>
    );
  }
  if (kind === 'pages') {
    // Stack of three page silhouettes, fanned
    return (
      <div style={{ position: 'relative', width: '70%', aspectRatio: '0.85' }}>
        {[0, 1, 2].map(i => (
          <div key={i} style={{
            position: 'absolute', inset: 0,
            background: i === 2 ? accent.tint : '#FBF9F4',
            border: `1px solid ${accent.soft}`,
            borderRadius: 3,
            transform: `translate(${i * 8}px, ${i * 8}px) rotate(${(i - 1) * -2}deg)`,
            padding: 12, display: 'flex', flexDirection: 'column', gap: 5,
          }}>
            {i === 2 && (
              <>
                <div style={{ height: 8, width: '60%', background: accent.ink, opacity: 0.7, borderRadius: 1 }} />
                <div style={{ height: 2, background: accent.ink, opacity: 0.25, borderRadius: 1, width: '90%', marginTop: 4 }} />
                <div style={{ height: 2, background: accent.ink, opacity: 0.25, borderRadius: 1, width: '80%' }} />
                <div style={{ height: 2, background: accent.ink, opacity: 0.25, borderRadius: 1, width: '85%' }} />
                <div style={{ height: 2, background: accent.ink, opacity: 0.25, borderRadius: 1, width: '70%' }} />
              </>
            )}
          </div>
        ))}
      </div>
    );
  }
  if (kind === 'features') {
    // A miniature home page slot preview — like a website hero
    return (
      <div style={{
        width: '85%', aspectRatio: '1.4', background: accent.tint,
        borderRadius: 3, padding: 10, display: 'flex', flexDirection: 'column', gap: 6,
        position: 'relative', overflow: 'hidden',
      }}>
        {/* fake browser bar */}
        <div style={{ display: 'flex', gap: 3, marginBottom: 4 }}>
          <div style={{ width: 5, height: 5, borderRadius: 5, background: accent.ink, opacity: 0.4 }} />
          <div style={{ width: 5, height: 5, borderRadius: 5, background: accent.ink, opacity: 0.4 }} />
          <div style={{ width: 5, height: 5, borderRadius: 5, background: accent.ink, opacity: 0.4 }} />
        </div>
        <div style={{ flex: 1, display: 'flex', gap: 4 }}>
          <div style={{ flex: 2, background: accent.ink, opacity: 0.85, borderRadius: 2, padding: 6, display: 'flex', flexDirection: 'column', justifyContent: 'flex-end' }}>
            <div className="serif" style={{ fontSize: 9, color: accent.tint, fontWeight: 600, lineHeight: 1.1 }}>Sunday<br/>Welcome</div>
          </div>
          <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 4 }}>
            <div style={{ flex: 1, background: '#FBF9F4', borderRadius: 2 }} />
            <div style={{ flex: 1, background: '#FBF9F4', borderRadius: 2 }} />
          </div>
        </div>
      </div>
    );
  }
  return null;
}

// --- Events tall (right column) ---
function EventsTall({ type }) {
  const a = ACCENT[type.accent];
  const events = [
    { d: '18', m: 'May', name: 'Spring Picnic' },
    { d: '21', m: 'May', name: 'Bible Study' },
    { d: '02', m: 'Jun', name: "Children's Choir" },
  ];
  return (
    <div style={{
      gridRow: 'span 1', borderRadius: 6,
      background: a.tint, padding: 20,
      display: 'flex', flexDirection: 'column',
      aspectRatio: '0.66 / 1',
    }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <span className="mono" style={{ fontSize: 10, letterSpacing: '0.18em', textTransform: 'uppercase', color: a.ink }}>Events</span>
        <span className="mono" style={{ fontSize: 10, color: a.ink, opacity: 0.7 }}>{type.count} upcoming</span>
      </div>
      <div className="serif" style={{ fontSize: 26, fontWeight: 500, letterSpacing: '-0.01em', marginTop: 14 }}>What's next</div>

      <div style={{ marginTop: 14, display: 'flex', flexDirection: 'column', gap: 10, flex: 1 }}>
        {events.map((e, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{
              width: 38, height: 38, borderRadius: 4,
              background: '#FBF9F4',
              display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
              border: `1px solid ${a.soft}`,
            }}>
              <div className="mono" style={{ fontSize: 8, letterSpacing: '0.1em', textTransform: 'uppercase', color: a.ink }}>{e.m}</div>
              <div className="serif" style={{ fontSize: 15, fontWeight: 600, lineHeight: 1, color: a.ink }}>{e.d}</div>
            </div>
            <div style={{ fontSize: 12.5, color: '#1C1A17' }}>{e.name}</div>
          </div>
        ))}
      </div>

      <div style={{ display: 'flex', gap: 6, marginTop: 14 }}>
        <button style={{ ...btnSm, background: a.ink, flex: 1 }}>+ New event</button>
        <button style={{ ...btnSmGhost, borderColor: a.ink, color: a.ink }}>All</button>
      </div>
    </div>
  );
}

// --- Photos tile (with mini gallery) ---
function PhotosTile({ type }) {
  const a = ACCENT[type.accent];
  const stripe = `repeating-linear-gradient(135deg, ${a.tint} 0 8px, ${a.soft} 8px 16px)`;
  return (
    <div style={{
      borderRadius: 6, background: '#FBF9F4', border: '1px solid #E5DFD2',
      padding: 20, display: 'flex', flexDirection: 'column',
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <span className="mono" style={{ fontSize: 10, letterSpacing: '0.18em', textTransform: 'uppercase', color: a.ink }}>Photos</span>
          <div className="serif" style={{ fontSize: 24, fontWeight: 500, letterSpacing: '-0.01em', marginTop: 6 }}>Gallery</div>
        </div>
        <div style={{ textAlign: 'right' }}>
          <div className="serif" style={{ fontSize: 26, fontWeight: 500, color: a.ink, lineHeight: 1 }}>{type.count}</div>
          <div className="mono" style={{ fontSize: 9, letterSpacing: '0.14em', textTransform: 'uppercase', color: '#8A8478', marginTop: 2 }}>images</div>
        </div>
      </div>

      <div style={{
        marginTop: 14, flex: 1,
        display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 6,
      }}>
        {[0, 1, 2, 3, 4, 5, 6, 7].map(i => (
          <div key={i} style={{
            aspectRatio: '1', borderRadius: 2,
            background: i % 3 === 0 ? a.ink : i % 3 === 1 ? a.tint : stripe,
            opacity: i % 3 === 0 ? 0.85 : 1,
          }} />
        ))}
      </div>

      <div style={{ marginTop: 14, fontSize: 11.5, color: '#8A8478' }}>{type.recent}</div>

      <div style={{ display: 'flex', gap: 6, marginTop: 12 }}>
        <button style={{ ...btnSm, background: a.ink, flex: 1 }}>+ Upload photos</button>
        <button style={btnSmGhost}>All</button>
      </div>
    </div>
  );
}

// --- Calendar tile (mini May 2026 calendar) ---
function CalendarTile() {
  const a = ACCENT.terra;
  // May 2026 starts on a Friday (May 1, 2026 = Friday).
  const monthName = 'May';
  const year = 2026;
  const firstDayOffset = 5; // Sun=0; May 1 is Fri = index 5
  const daysInMonth = 31;
  const today = 7;
  const eventDates = { 18: 'Spring Picnic', 21: 'Bible Study' };
  const cells = [];
  for (let i = 0; i < firstDayOffset; i++) cells.push(null);
  for (let d = 1; d <= daysInMonth; d++) cells.push(d);
  while (cells.length % 7 !== 0) cells.push(null);

  return (
    <div style={{
      borderRadius: 6, background: '#FBF9F4', border: '1px solid #E5DFD2',
      padding: 18, display: 'flex', flexDirection: 'column',
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <span className="mono" style={{ fontSize: 10, letterSpacing: '0.18em', textTransform: 'uppercase', color: a.ink }}>Calendar</span>
          <div className="serif" style={{ fontSize: 22, fontWeight: 500, letterSpacing: '-0.01em', marginTop: 4 }}>
            {monthName} <span style={{ color: '#8A8478' }}>{year}</span>
          </div>
        </div>
        <div style={{ display: 'flex', gap: 4 }}>
          <button style={chevBtn}>‹</button>
          <button style={chevBtn}>›</button>
        </div>
      </div>

      <div style={{
        marginTop: 14, display: 'grid', gridTemplateColumns: 'repeat(7, 1fr)', gap: 2,
        flex: 1,
      }}>
        {['S','M','T','W','T','F','S'].map((d, i) => (
          <div key={'h' + i} className="mono" style={{
            fontSize: 9, letterSpacing: '0.1em', textTransform: 'uppercase',
            color: '#8A8478', textAlign: 'center', padding: '4px 0',
          }}>{d}</div>
        ))}
        {cells.map((d, i) => {
          if (d === null) return <div key={'c' + i} />;
          const isToday = d === today;
          const hasEvent = eventDates[d];
          return (
            <div key={'c' + i} style={{
              aspectRatio: '1.1 / 1',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 12,
              fontVariantNumeric: 'tabular-nums',
              borderRadius: 3,
              background: isToday ? a.ink : 'transparent',
              color: isToday ? '#FBF9F4' : hasEvent ? a.ink : '#1C1A17',
              fontWeight: isToday || hasEvent ? 600 : 400,
              position: 'relative',
            }}>
              {d}
              {hasEvent && !isToday && (
                <div style={{
                  position: 'absolute', bottom: 3, left: '50%', transform: 'translateX(-50%)',
                  width: 4, height: 4, borderRadius: 4, background: a.ink,
                }} />
              )}
            </div>
          );
        })}
      </div>

      <div style={{
        marginTop: 10, paddingTop: 10, borderTop: '1px dashed #E5DFD2',
        fontSize: 11.5, color: '#4A463F', display: 'flex', justifyContent: 'space-between',
      }}>
        <span><span style={{ color: a.ink, fontWeight: 600 }}>2 events</span> in May</span>
        <a style={{ color: '#8A8478', textDecoration: 'none' }}>Open calendar →</a>
      </div>
    </div>
  );
}

const chevBtn = {
  width: 22, height: 22, borderRadius: 3, border: '1px solid #E5DFD2',
  background: '#FBF9F4', color: '#4A463F', cursor: 'pointer',
  fontSize: 13, lineHeight: 1, padding: 0, fontFamily: 'inherit',
};

// --- Split upload bar (Files left, Sermon video right) ---
function UploadSplitBar() {
  const fileTypes = [
    { label: 'Bulletin',     hint: 'PDF · weekly' },
    { label: 'Newsletter',   hint: 'PDF · monthly' },
    { label: 'Presentation', hint: 'PPT · slides' },
  ];
  return (
    <div style={{
      margin: '0 40px 28px',
      display: 'grid',
      gridTemplateColumns: '1fr 1fr',
      gap: 14,
    }}>
      {/* Left — Files */}
      <div style={{
        borderRadius: 6, padding: '18px 20px',
        background: '#FBF9F4', border: '1px solid #E5DFD2',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <Mark kind="files" size={20} stroke="#1C1A17" />
            <span className="mono" style={{ fontSize: 10, letterSpacing: '0.18em', textTransform: 'uppercase', color: '#1C1A17' }}>Files · upload</span>
          </div>
          <a style={{ fontSize: 11, color: '#8A8478', textDecoration: 'none' }}>23 in library →</a>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8 }}>
          {fileTypes.map(f => (
            <button key={f.label} style={uploadCard}>
              <div style={{
                width: 28, height: 36, border: '1.5px solid #1C1A17', borderRadius: 2,
                marginBottom: 8, position: 'relative',
              }}>
                <div style={{ position: 'absolute', top: 0, right: 0, width: 8, height: 8, background: '#FBF9F4', borderLeft: '1.5px solid #1C1A17', borderBottom: '1.5px solid #1C1A17' }} />
              </div>
              <div className="serif" style={{ fontSize: 14, fontWeight: 500, lineHeight: 1.1 }}>+ {f.label}</div>
              <div className="mono" style={{ fontSize: 9.5, color: '#8A8478', marginTop: 3, letterSpacing: '0.06em', textTransform: 'uppercase' }}>{f.hint}</div>
            </button>
          ))}
        </div>
      </div>

      {/* Right — Sermon video */}
      <div style={{
        borderRadius: 6, padding: '18px 20px',
        background: '#1C1A17', color: '#F7F4EE',
        position: 'relative', overflow: 'hidden',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
            <Mark kind="sermons" size={20} stroke="#F7F4EE" />
            <span className="mono" style={{ fontSize: 10, letterSpacing: '0.18em', textTransform: 'uppercase', opacity: 0.7 }}>Sermon · upload</span>
          </div>
          <a style={{ fontSize: 11, color: '#F7F4EE', opacity: 0.55, textDecoration: 'none' }}>156 archived →</a>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          {/* Drop area */}
          <div style={{
            flex: 1,
            border: '1.5px dashed rgba(247,244,238,0.35)',
            borderRadius: 4,
            padding: '18px 16px',
            display: 'flex', alignItems: 'center', gap: 14,
          }}>
            <div style={{
              width: 44, height: 44, borderRadius: 22,
              background: 'rgba(247,244,238,0.95)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
            }}>
              <svg width="14" height="14" viewBox="0 0 16 16"><path d="M5 3 L13 8 L5 13 Z" fill="#1C1A17" /></svg>
            </div>
            <div style={{ flex: 1 }}>
              <div className="serif" style={{ fontSize: 17, fontWeight: 500 }}>+ Upload sermon video</div>
              <div style={{ fontSize: 11.5, opacity: 0.6, marginTop: 2 }}>Drop a .mp4 here, or paste a YouTube/Vimeo link</div>
            </div>
          </div>
          <button style={{ ...btnSolid, background: '#F7F4EE', color: '#1C1A17', whiteSpace: 'nowrap' }}>Choose file</button>
        </div>
      </div>
    </div>
  );
}

const uploadCard = {
  background: '#F7F4EE', border: '1px solid #E5DFD2', borderRadius: 4,
  padding: '14px 12px', cursor: 'pointer',
  display: 'flex', flexDirection: 'column', alignItems: 'flex-start',
  textAlign: 'left', fontFamily: 'inherit',
};

// --- Sermons tile (dark, video-feel) ---
function SermonsTile({ type }) {
  return (
    <div style={{
      borderRadius: 6, background: '#1C1A17', color: '#F7F4EE',
      padding: 20, display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
      position: 'relative', overflow: 'hidden',
    }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <span className="mono" style={{ fontSize: 10, letterSpacing: '0.18em', textTransform: 'uppercase', opacity: 0.65 }}>Sermons</span>
        <span className="mono" style={{ fontSize: 10, opacity: 0.55 }}>{type.count} archived</span>
      </div>

      {/* Fake video preview */}
      <div style={{
        marginTop: 14, marginBottom: 14, flex: 1,
        background: 'linear-gradient(135deg, #2A2620 0%, #4A463F 100%)',
        borderRadius: 3, position: 'relative',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        minHeight: 110,
      }}>
        <div style={{
          width: 44, height: 44, borderRadius: 22,
          background: 'rgba(247,244,238,0.95)', display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <svg width="16" height="16" viewBox="0 0 16 16"><path d="M5 3 L13 8 L5 13 Z" fill="#1C1A17" /></svg>
        </div>
        <div style={{ position: 'absolute', bottom: 8, left: 10, right: 10, fontSize: 11, opacity: 0.85 }}>
          The Vineyard · 32 min
        </div>
      </div>

      <div>
        <div className="serif" style={{ fontSize: 22, fontWeight: 500, letterSpacing: '-0.01em' }}>Latest sermon</div>
        <div style={{ fontSize: 11.5, opacity: 0.6, marginTop: 4 }}>{type.recent}</div>
      </div>

      <div style={{ display: 'flex', gap: 6, marginTop: 12 }}>
        <button style={{ ...btnSm, background: '#F7F4EE', color: '#1C1A17', flex: 1 }}>+ New sermon</button>
        <button style={{ ...btnSmGhost, borderColor: 'rgba(247,244,238,0.3)', color: '#F7F4EE' }}>Browse</button>
      </div>
    </div>
  );
}

// Buttons
const btnSolid = {
  background: '#1C1A17', color: '#F7F4EE', border: 'none',
  padding: '10px 16px', borderRadius: 4, fontSize: 13, fontWeight: 500, cursor: 'pointer', fontFamily: 'inherit',
};
const btnGhost = {
  background: 'transparent', color: '#1C1A17', border: '1px solid #1C1A17',
  padding: '9px 16px', borderRadius: 4, fontSize: 13, fontWeight: 500, cursor: 'pointer', fontFamily: 'inherit',
};
const btnSm = {
  background: '#1C1A17', color: '#F7F4EE', border: 'none',
  padding: '7px 12px', borderRadius: 3, fontSize: 12, fontWeight: 500, cursor: 'pointer', fontFamily: 'inherit',
};
const btnSmGhost = {
  background: 'transparent', color: '#1C1A17', border: '1px solid #C9C2B2',
  padding: '6px 12px', borderRadius: 3, fontSize: 12, fontWeight: 500, cursor: 'pointer', fontFamily: 'inherit',
};

// ============================================================
// VARIANT B — EDITORIAL (numbered list, table-of-contents feel)
// ============================================================
function EditorialVariant() {
  return (
    <div style={{ width: '100%', height: '100%', background: '#FBF9F4', display: 'flex', flexDirection: 'column' }}>
      <TopBar />
      <div style={{ padding: '56px 96px 28px', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
        <div>
          <div className="mono" style={{ fontSize: 11, letterSpacing: '0.2em', textTransform: 'uppercase', color: '#8A8478', marginBottom: 18 }}>
            Contents · what you can publish
          </div>
          <h1 className="serif" style={{ margin: 0, fontSize: 72, fontWeight: 400, letterSpacing: '-0.025em', lineHeight: 0.95 }}>
            <em style={{ fontStyle: 'italic', color: '#C24A1F', fontWeight: 400 }}>Today</em>, the<br/>website is yours.
          </h1>
        </div>
        <div style={{ textAlign: 'right', fontSize: 13, color: '#4A463F', maxWidth: 240 }}>
          <div className="mono" style={{ fontSize: 11, letterSpacing: '0.16em', textTransform: 'uppercase', color: '#8A8478' }}>Last published</div>
          <div className="serif" style={{ fontSize: 22, fontWeight: 500, marginTop: 6, lineHeight: 1.2 }}>Bulletin, May 4</div>
          <div style={{ marginTop: 4, color: '#8A8478' }}>by Jim · 3 days ago</div>
        </div>
      </div>

      <div style={{ flex: 1, padding: '0 96px 48px' }}>
        {TYPES.map((t, i) => (
          <EditorialRow key={t.key} t={t} idx={i} />
        ))}
      </div>
    </div>
  );
}

function EditorialRow({ t, idx }) {
  const a = ACCENT[t.accent];
  return (
    <div style={{
      display: 'grid',
      gridTemplateColumns: '64px 1fr 1.2fr auto',
      alignItems: 'center',
      gap: 32,
      padding: '20px 0',
      borderTop: '1px solid #E5DFD2',
      borderBottom: idx === TYPES.length - 1 ? '1px solid #E5DFD2' : 'none',
    }}>
      <div className="serif" style={{ fontSize: 28, fontWeight: 400, color: '#8A8478', fontVariantNumeric: 'tabular-nums' }}>
        {String(idx + 1).padStart(2, '0')}
      </div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 18 }}>
        <div style={{
          width: 44, height: 44, borderRadius: 22,
          background: a.tint,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          flexShrink: 0,
        }}>
          <Mark kind={t.key} size={22} stroke={a.ink} />
        </div>
        <div>
          <div className="serif" style={{ fontSize: 28, fontWeight: 500, letterSpacing: '-0.01em', lineHeight: 1.1 }}>{t.name}</div>
          <div style={{ fontSize: 13, color: '#8A8478', marginTop: 2 }}>{t.sub}</div>
        </div>
      </div>
      <div style={{ fontSize: 13, color: '#4A463F' }}>
        <span className="mono" style={{ color: '#8A8478', marginRight: 10 }}>{String(t.count).padStart(3, ' ')}</span>
        {t.recent}
      </div>
      <div style={{ display: 'flex', gap: 10 }}>
        <button style={{ ...btnEditorial, color: a.ink, borderColor: a.ink }}>+ New</button>
        <button style={btnEditorialGhost}>All →</button>
      </div>
    </div>
  );
}

const btnEditorial = {
  background: 'transparent', border: '1px solid #1C1A17', color: '#1C1A17',
  padding: '9px 18px', borderRadius: 999, fontSize: 13, fontWeight: 500, cursor: 'pointer', fontFamily: 'inherit',
};
const btnEditorialGhost = {
  background: 'transparent', border: 'none', color: '#1C1A17',
  padding: '9px 8px', fontSize: 13, fontWeight: 500, cursor: 'pointer', fontFamily: 'inherit',
};

// ============================================================
// VARIANT C — COLOR-CODED CARDS (each type has a distinct hue)
// ============================================================
function ColorCardsVariant() {
  return (
    <div style={{ width: '100%', height: '100%', background: '#F7F4EE', display: 'flex', flexDirection: 'column' }}>
      <TopBar />
      <div style={{ padding: '40px 48px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end' }}>
        <div>
          <h1 className="serif" style={{ margin: 0, fontSize: 44, fontWeight: 500, letterSpacing: '-0.02em' }}>Publish something.</h1>
          <p style={{ margin: '8px 0 0', color: '#8A8478', fontSize: 14 }}>Six kinds of things live on the website.</p>
        </div>
        <div className="mono" style={{ fontSize: 11, letterSpacing: '0.16em', textTransform: 'uppercase', color: '#8A8478' }}>
          Thu · May 7 · 8:50 PM
        </div>
      </div>

      <div style={{
        flex: 1, padding: '12px 48px 48px',
        display: 'grid',
        gridTemplateColumns: 'repeat(3, 1fr)',
        gridTemplateRows: 'repeat(2, 1fr)',
        gap: 18,
      }}>
        {TYPES.map((t) => <ColorCard key={t.key} t={t} />)}
      </div>
    </div>
  );
}

function ColorCard({ t }) {
  const a = ACCENT[t.accent];
  return (
    <div style={{
      borderRadius: 8,
      padding: 26,
      background: '#FBF9F4',
      border: `1px solid ${a.soft}`,
      display: 'flex', flexDirection: 'column',
      position: 'relative', overflow: 'hidden',
    }}>
      {/* color stripe */}
      <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: 4, background: a.ink }} />

      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div style={{
          width: 52, height: 52, borderRadius: 8,
          background: a.tint, display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <Mark kind={t.key} size={28} stroke={a.ink} />
        </div>
        <div style={{ textAlign: 'right' }}>
          <div className="serif" style={{ fontSize: 36, fontWeight: 500, lineHeight: 1, color: a.ink, fontVariantNumeric: 'tabular-nums' }}>
            {t.count}
          </div>
          <div className="mono" style={{ fontSize: 10, letterSpacing: '0.14em', textTransform: 'uppercase', color: '#8A8478', marginTop: 2 }}>
            in library
          </div>
        </div>
      </div>

      <div style={{ marginTop: 22, flex: 1 }}>
        <div className="serif" style={{ fontSize: 26, fontWeight: 500, letterSpacing: '-0.01em' }}>{t.name}</div>
        <div style={{ fontSize: 13, color: '#8A8478', marginTop: 2 }}>{t.sub}</div>
        <div style={{
          marginTop: 16, paddingTop: 14, borderTop: '1px dashed #E5DFD2',
          fontSize: 12, color: '#4A463F',
        }}>
          <span className="mono" style={{ color: '#8A8478', marginRight: 8 }}>↳</span>
          {t.recent}
        </div>
      </div>

      <div style={{ display: 'flex', gap: 8, marginTop: 18 }}>
        <button style={{
          ...btnSolid, background: a.ink, color: '#fff', flex: 1,
        }}>+ New {t.name.toLowerCase().replace(/s$/, '')}</button>
        <button style={{ ...btnGhost, borderColor: a.soft, color: a.ink }}>All</button>
      </div>
    </div>
  );
}

// expose
Object.assign(window, {
  BentoVariant, EditorialVariant, ColorCardsVariant,
});
