/* Worship section variations for Mt Zion UCC website */

const ART_W = 1440;
const ART_H = 760;

/* ---------- Shared bits ---------- */

const MtZionStyles = () => (
  <style>{`
    .mz-root {
      width: ${ART_W}px;
      height: ${ART_H}px;
      background: #f7f4ee;
      color: #1b1b18;
      font-family: 'Newsreader', 'Source Serif 4', Georgia, serif;
      position: relative;
      overflow: hidden;
      box-sizing: border-box;
    }
    .mz-root, .mz-root * { box-sizing: border-box; }
    .mz-mint-band { background: #e6efe6; }
    .mz-eyebrow {
      font-family: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, monospace;
      letter-spacing: 0.18em;
      text-transform: uppercase;
      font-size: 13px;
      color: #2f5a3f;
      font-weight: 500;
    }
    .mz-h2 {
      font-family: 'Newsreader', 'Source Serif 4', Georgia, serif;
      font-weight: 400;
      font-size: 56px;
      line-height: 1.05;
      letter-spacing: -0.01em;
      margin: 0;
      color: #1b1b18;
    }
    .mz-italic { font-style: italic; color: #2f5a3f; }
    .mz-rule {
      height: 1px;
      background: #1b1b18;
      opacity: 0.85;
      width: 100%;
    }
    .mz-time {
      font-family: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, monospace;
      letter-spacing: 0.14em;
      color: #2f5a3f;
      font-size: 14px;
      font-weight: 500;
    }
    .mz-service-title {
      font-family: 'Newsreader', 'Source Serif 4', Georgia, serif;
      font-weight: 400;
      font-size: 36px;
      line-height: 1.1;
      letter-spacing: -0.005em;
      color: #1b1b18;
      margin: 6px 0 10px;
    }
    .mz-body {
      font-family: 'Newsreader', 'Source Serif 4', Georgia, serif;
      font-size: 17px;
      line-height: 1.55;
      color: #4a4a44;
    }
    .mz-meta {
      font-family: 'Newsreader', 'Source Serif 4', Georgia, serif;
      font-size: 15px;
      color: #6a6a64;
    }
    .mz-link {
      font-family: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, monospace;
      letter-spacing: 0.14em;
      text-transform: uppercase;
      font-size: 12px;
      color: #1b1b18;
      text-decoration: none;
      border-bottom: 1px solid #1b1b18;
      padding-bottom: 4px;
      display: inline-flex;
      align-items: center;
      gap: 8px;
    }
    .mz-btn {
      display: inline-flex;
      align-items: center;
      gap: 10px;
      padding: 14px 22px;
      background: #2f5a3f;
      color: #f7f4ee;
      font-family: 'JetBrains Mono', ui-monospace, 'SF Mono', Menlo, monospace;
      letter-spacing: 0.14em;
      text-transform: uppercase;
      font-size: 12px;
      border: none;
      cursor: pointer;
    }
  `}</style>
);

const SectionHeader = ({ eyebrow = 'This Sunday', title, italic, ruleColor }) => (
  <div>
    <div className="mz-eyebrow">{eyebrow}</div>
    <h2 className="mz-h2" style={{ marginTop: 18 }}>
      {title}{italic && <> <span className="mz-italic">{italic}</span></>}
    </h2>
  </div>
);

/* ============================================================
   V1 — Refined Card  (closest to current site, single service)
   ============================================================ */
const V1RefinedCard = () => (
  <div className="mz-root">
    <MtZionStyles />
    {/* mint band fades out top */}
    <div style={{ position: 'absolute', inset: '0 0 auto 0', height: 90, background: '#e6efe6' }} />
    <div style={{ position: 'relative', padding: '78px 96px 0' }}>
      <SectionHeader title="Worship" italic="this Sunday" />
      <div style={{ height: 1, background: '#1b1b18', opacity: 0.85, marginTop: 36 }} />
    </div>

    <div style={{
      position: 'relative',
      display: 'grid',
      gridTemplateColumns: '1fr 1fr',
      gap: 56,
      padding: '48px 96px 0',
      alignItems: 'stretch',
    }}>
      <div style={{
        aspectRatio: '4 / 3',
        overflow: 'hidden',
        border: '1px solid rgba(27,27,24,0.12)',
      }}>
        <img src="assets/shepherd.jpg" alt="Stained glass — Good Shepherd"
          style={{ width: '100%', height: '100%', objectFit: 'cover', objectPosition: 'center 25%' }} />
      </div>

      <div style={{
        border: '1px solid rgba(27,27,24,0.12)',
        padding: '44px 40px',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'center',
        background: '#fbfaf6',
      }}>
        <div className="mz-time">10:30 AM &nbsp;·&nbsp; EVERY SUNDAY</div>
        <h3 className="mz-service-title" style={{ fontSize: 44, margin: '14px 0 18px' }}>
          Sunday Service
        </h3>
        <p className="mz-body" style={{ maxWidth: 460, margin: 0 }}>
          A traditional service of scripture, hymns, and reflection in the
          Sanctuary &mdash; with choir and organ. All are welcome at the Table,
          whatever your story, whatever your season.
        </p>
        <div className="mz-meta" style={{ marginTop: 22 }}>
          Sanctuary &nbsp;·&nbsp; ≈ 60 minutes &nbsp;·&nbsp; Nursery available
        </div>
        <div style={{ marginTop: 32, display: 'flex', gap: 28, alignItems: 'center' }}>
          <a className="mz-link" href="#">Plan a visit →</a>
          <a className="mz-link" href="#" style={{ borderColor: 'transparent', opacity: 0.65 }}>Watch online →</a>
        </div>
      </div>
    </div>
  </div>
);

/* ============================================================
   V2 — Gothic Arch  (image masked to the sanctuary window shape)
   ============================================================ */
const V2GothicArch = () => (
  <div className="mz-root">
    <MtZionStyles />
    <div style={{ position: 'absolute', inset: '0 0 auto 0', height: 90, background: '#e6efe6' }} />
    <div style={{ position: 'relative', padding: '78px 96px 0' }}>
      <SectionHeader title="Worship" italic="this Sunday" />
      <div style={{ height: 1, background: '#1b1b18', opacity: 0.85, marginTop: 36 }} />
    </div>

    <div style={{
      position: 'relative',
      display: 'grid',
      gridTemplateColumns: '0.85fr 1fr',
      gap: 80,
      padding: '40px 96px 0',
      alignItems: 'center',
    }}>
      {/* Arched window */}
      <div style={{ position: 'relative', height: 520, display: 'flex', justifyContent: 'center' }}>
        {/* Outer arch frame */}
        <div style={{
          position: 'relative',
          width: 360,
          height: 520,
          background: '#1b1b18',
          padding: 10,
          borderRadius: '180px 180px 6px 6px',
          boxShadow: '0 30px 60px -30px rgba(27,27,24,0.35)',
        }}>
          {/* Inner arch with image */}
          <div style={{
            width: '100%',
            height: '100%',
            borderRadius: '170px 170px 2px 2px',
            overflow: 'hidden',
            position: 'relative',
            background: '#000',
          }}>
            <img src="assets/shepherd.jpg" alt="Stained glass — Good Shepherd"
              style={{ width: '100%', height: '100%', objectFit: 'cover', objectPosition: 'center 30%' }} />
          </div>
          {/* Decorative mullion lines */}
          <div style={{
            position: 'absolute', left: '50%', top: 200, bottom: 20,
            width: 1, background: 'rgba(27,27,24,0.45)', transform: 'translateX(-0.5px)'
          }} />
        </div>
        {/* Plaque */}
        <div style={{
          position: 'absolute',
          bottom: -28,
          left: '50%',
          transform: 'translateX(-50%)',
          background: '#f7f4ee',
          padding: '8px 18px',
          fontFamily: "'JetBrains Mono', ui-monospace, monospace",
          fontSize: 10,
          letterSpacing: '0.2em',
          textTransform: 'uppercase',
          color: '#6a6a64',
          border: '1px solid rgba(27,27,24,0.18)',
        }}>
          The Good Shepherd · c. 1910
        </div>
      </div>

      {/* Right column copy */}
      <div style={{ paddingTop: 20 }}>
        <div className="mz-time" style={{ fontSize: 16 }}>10:30 AM &nbsp;·&nbsp; EVERY SUNDAY</div>
        <h3 className="mz-service-title" style={{ fontSize: 56, margin: '18px 0 22px', lineHeight: 1.05 }}>
          Gather in the<br />
          <span style={{ fontStyle: 'italic', color: '#2f5a3f' }}>Sanctuary.</span>
        </h3>
        <p className="mz-body" style={{ maxWidth: 480, margin: 0, fontSize: 18 }}>
          One service. Scripture, prayer, and song beneath the windows that
          have watched over this congregation since 1910 &mdash; with our
          choir and pipe organ.
        </p>
        <div className="mz-meta" style={{ marginTop: 22, fontSize: 14 }}>
          Sanctuary &nbsp;·&nbsp; ≈ 60 min &nbsp;·&nbsp; Nursery provided
        </div>
        <div style={{ marginTop: 36 }}>
          <button className="mz-btn">Plan a visit →</button>
        </div>
      </div>
    </div>
  </div>
);

/* ============================================================
   V3 — Cinematic Hero  (full-bleed image, text overlaid)
   ============================================================ */
const V3CinematicHero = () => (
  <div className="mz-root" style={{ background: '#10130f' }}>
    <MtZionStyles />
    {/* full-bleed image */}
    <div style={{ position: 'absolute', inset: 0 }}>
      <img src="assets/shepherd.jpg" alt="Stained glass — Good Shepherd"
        style={{ width: '100%', height: '100%', objectFit: 'cover', objectPosition: 'center 35%', filter: 'saturate(1.05)' }} />
      {/* darkening + tint */}
      <div style={{
        position: 'absolute', inset: 0,
        background: 'linear-gradient(90deg, rgba(16,19,15,0.85) 0%, rgba(16,19,15,0.55) 45%, rgba(16,19,15,0.15) 100%)'
      }} />
      <div style={{
        position: 'absolute', inset: 0,
        background: 'linear-gradient(180deg, rgba(16,19,15,0.45) 0%, rgba(16,19,15,0) 30%, rgba(16,19,15,0) 70%, rgba(16,19,15,0.45) 100%)'
      }} />
    </div>

    {/* content */}
    <div style={{
      position: 'relative',
      height: '100%',
      padding: '78px 96px',
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'space-between',
      color: '#f4efe3',
    }}>
      <div>
        <div className="mz-eyebrow" style={{ color: '#b9d3bd' }}>This Sunday</div>
        <h2 className="mz-h2" style={{ color: '#f4efe3', marginTop: 22, fontSize: 88, maxWidth: 820 }}>
          Worship at <span className="mz-italic" style={{ color: '#cfe2d6' }}>10:30.</span>
        </h2>
        <p style={{
          fontFamily: "'Newsreader', Georgia, serif",
          fontSize: 22,
          lineHeight: 1.45,
          maxWidth: 540,
          marginTop: 28,
          color: '#e7e1d2'
        }}>
          One service. Scripture, hymns, and reflection beneath windows that
          have lit this sanctuary for more than a century.
        </p>
      </div>

      <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', gap: 16, alignItems: 'center' }}>
          <button className="mz-btn" style={{ background: '#f4efe3', color: '#1b1b18' }}>Plan a visit →</button>
          <a className="mz-link" href="#" style={{ color: '#f4efe3', borderColor: 'rgba(244,239,227,0.55)' }}>Watch online →</a>
        </div>
        <div style={{
          fontFamily: "'JetBrains Mono', ui-monospace, monospace",
          fontSize: 12,
          letterSpacing: '0.2em',
          textTransform: 'uppercase',
          color: 'rgba(244,239,227,0.65)',
          textAlign: 'right',
          lineHeight: 1.7,
        }}>
          Sanctuary · 305 Main St<br />
          China Grove, NC
        </div>
      </div>
    </div>
  </div>
);

/* ============================================================
   V4 — Tinted / harmonized  (photo softened to match the palette)
   ============================================================ */
const V4Tinted = () => (
  <div className="mz-root">
    <MtZionStyles />
    <div style={{ position: 'absolute', inset: '0 0 auto 0', height: 90, background: '#e6efe6' }} />
    <div style={{ position: 'relative', padding: '78px 96px 0' }}>
      <SectionHeader title="Worship" italic="this Sunday" />
      <div style={{ height: 1, background: '#1b1b18', opacity: 0.85, marginTop: 36 }} />
    </div>

    <div style={{
      position: 'relative',
      display: 'grid',
      gridTemplateColumns: '1.1fr 1fr',
      gap: 64,
      padding: '48px 96px 0',
      alignItems: 'stretch',
    }}>
      {/* Image with tint overlay */}
      <div style={{ position: 'relative', height: 480, overflow: 'hidden' }}>
        <img src="assets/shepherd.jpg" alt="Stained glass — Good Shepherd"
          style={{
            width: '100%', height: '100%', objectFit: 'cover',
            objectPosition: 'center 28%',
            filter: 'saturate(0.85) contrast(0.96)',
          }} />
        {/* soft mint wash */}
        <div style={{
          position: 'absolute', inset: 0,
          background: 'linear-gradient(135deg, rgba(230,239,230,0.32) 0%, rgba(230,239,230,0.06) 50%, rgba(247,244,238,0.18) 100%)',
          mixBlendMode: 'screen',
        }} />
        {/* paper grain via subtle noise — use a translucent cream */}
        <div style={{
          position: 'absolute', inset: 0,
          background: 'rgba(247,244,238,0.08)',
        }} />
        {/* corner stamp */}
        <div style={{
          position: 'absolute', left: 20, bottom: 18,
          fontFamily: "'JetBrains Mono', ui-monospace, monospace",
          fontSize: 10, letterSpacing: '0.22em', textTransform: 'uppercase',
          color: 'rgba(247,244,238,0.92)',
          textShadow: '0 1px 2px rgba(0,0,0,0.4)',
        }}>
          From the Sanctuary
        </div>
      </div>

      {/* Side rail of details */}
      <div style={{ padding: '20px 0 0 0' }}>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 16 }}>
          <div style={{
            fontFamily: "'JetBrains Mono', ui-monospace, monospace",
            fontSize: 56, lineHeight: 1, letterSpacing: '-0.01em', color: '#2f5a3f', fontWeight: 500,
          }}>
            10:30
          </div>
          <div className="mz-time" style={{ fontSize: 13 }}>AM SUNDAYS</div>
        </div>

        <h3 className="mz-service-title" style={{ fontSize: 44, margin: '24px 0 14px' }}>
          Sunday Service
        </h3>
        <p className="mz-body" style={{ maxWidth: 460, margin: 0 }}>
          Scripture, hymns, and a quiet hour together &mdash; under the
          stained-glass shepherd that has watched over this congregation
          for more than a hundred years.
        </p>

        <div style={{ marginTop: 36, borderTop: '1px solid rgba(27,27,24,0.15)' }}>
          <Row label="Place" value="The Sanctuary" />
          <Row label="Music" value="Choir & pipe organ" />
          <Row label="Children" value="Nursery available" />
        </div>

        <div style={{ marginTop: 28 }}>
          <a className="mz-link" href="#">Plan a visit →</a>
        </div>
      </div>
    </div>
  </div>
);

const Row = ({ label, value }) => (
  <div style={{
    display: 'grid', gridTemplateColumns: '140px 1fr',
    padding: '14px 0', borderBottom: '1px solid rgba(27,27,24,0.15)',
    alignItems: 'baseline',
  }}>
    <div className="mz-eyebrow" style={{ fontSize: 11 }}>{label}</div>
    <div className="mz-body" style={{ fontSize: 16 }}>{value}</div>
  </div>
);

/* ============================================================
   V5 — Editorial detail  (tight crop + scripture quote)
   ============================================================ */
const V5Editorial = () => (
  <div className="mz-root">
    <MtZionStyles />
    <div style={{ position: 'absolute', inset: '0 0 auto 0', height: 90, background: '#e6efe6' }} />
    <div style={{ position: 'relative', padding: '78px 96px 0' }}>
      <SectionHeader title="Worship" italic="this Sunday" />
      <div style={{ height: 1, background: '#1b1b18', opacity: 0.85, marginTop: 36 }} />
    </div>

    <div style={{
      position: 'relative',
      display: 'grid',
      gridTemplateColumns: '0.9fr 1.1fr',
      gap: 0,
      padding: '52px 0 0 96px',
      alignItems: 'stretch',
      height: 540,
    }}>
      {/* Cropped detail */}
      <div style={{ position: 'relative', overflow: 'hidden', marginRight: 56 }}>
        <img src="assets/shepherd.jpg" alt=""
          style={{
            width: '100%', height: '100%', objectFit: 'cover',
            objectPosition: '50% 12%', transform: 'scale(1.4)',
            transformOrigin: '50% 30%',
          }} />
      </div>

      {/* Right text block extends to edge */}
      <div style={{
        position: 'relative',
        padding: '36px 96px 0 0',
        display: 'flex', flexDirection: 'column',
      }}>
        <div className="mz-time">10:30 AM &nbsp;·&nbsp; EVERY SUNDAY</div>

        <div style={{
          fontFamily: "'Newsreader', Georgia, serif",
          fontStyle: 'italic',
          fontSize: 30,
          lineHeight: 1.32,
          color: '#1b1b18',
          margin: '26px 0 14px',
          maxWidth: 520,
        }}>
          &ldquo;I am the good shepherd. The good shepherd lays down his life for the sheep.&rdquo;
        </div>
        <div className="mz-meta" style={{ letterSpacing: '0.08em', fontSize: 13, textTransform: 'uppercase', color: '#2f5a3f' }}>
          — John 10 : 11
        </div>

        <div style={{
          height: 1, background: 'rgba(27,27,24,0.2)',
          margin: '36px 0 24px', width: 80,
        }} />

        <p className="mz-body" style={{ maxWidth: 460, margin: 0 }}>
          Join us each Sunday at 10:30 for a traditional service of scripture,
          hymns, and reflection in the Sanctuary &mdash; with choir and pipe organ.
          Whoever you are, wherever you are on life&rsquo;s journey, you are welcome here.
        </p>

        <div style={{ marginTop: 'auto', paddingBottom: 36, display: 'flex', gap: 28, alignItems: 'center' }}>
          <button className="mz-btn">Plan a visit →</button>
          <a className="mz-link" href="#" style={{ borderColor: 'transparent', opacity: 0.65 }}>This week&rsquo;s bulletin →</a>
        </div>
      </div>
    </div>
  </div>
);

/* ============================================================
   Assemble
   ============================================================ */
function App() {
  return (
    <DesignCanvas title="Worship Section — Mt Zion UCC" subtitle="Stained-glass integrations · 10:30 service">
      <DCSection id="variations" title="Worship Section">
        <DCArtboard id="v1" label="V1 · Refined card (closest to current)" width={ART_W} height={ART_H}>
          <V1RefinedCard />
        </DCArtboard>
        <DCArtboard id="v2" label="V2 · Gothic arch frame" width={ART_W} height={ART_H}>
          <V2GothicArch />
        </DCArtboard>
        <DCArtboard id="v3" label="V3 · Cinematic full-bleed hero" width={ART_W} height={ART_H}>
          <V3CinematicHero />
        </DCArtboard>
        <DCArtboard id="v4" label="V4 · Tinted / harmonized" width={ART_W} height={ART_H}>
          <V4Tinted />
        </DCArtboard>
        <DCArtboard id="v5" label="V5 · Editorial detail + scripture" width={ART_W} height={ART_H}>
          <V5Editorial />
        </DCArtboard>
      </DCSection>
    </DesignCanvas>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App />);
