// Home page — quiet, type-driven, mirrors live site structure but expanded.
// Two hero variants (photo / type) share a common BelowHero body.

function HomeChurch({ heroStyle = "photo" }) {
  return (
    <>
      <section id="home">
        {heroStyle === "photo" ? <PhotoHero /> : <TypeHero />}
      </section>
      <HomeChurchBody />
    </>
  );
}

function PhotoHero() {
  return (
    <>
      <section style={{ position: "relative", maxWidth: "none", padding: 0 }}>
        <div style={{
          height: 620,
          borderBottom: "1px solid var(--mtz-ink)",
          backgroundImage: "url('images/church-exterior.jpg')",
          backgroundSize: "cover",
          backgroundPosition: "center 60%",
        }} />
        <div style={{
          position: "absolute", inset: 0,
          background: "linear-gradient(180deg, rgba(0,0,0,0.10) 0%, rgba(0,0,0,0.10) 35%, rgba(0,0,0,0.65) 100%)",
          pointerEvents: "none",
        }} />
        <div style={{
          position: "absolute", left: 0, right: 0, bottom: 0,
          padding: "0 32px 56px", maxWidth: "var(--mtz-page-w)", margin: "0 auto",
        }}>
          <div style={{ maxWidth: 760, color: "#fff" }}>
            <p className="mtz-kicker" style={{ color: "rgba(255,255,255,0.85)", marginBottom: 18 }}>
              Welcome to Mt Zion UCC · est. 1858
            </p>
            <h1 className="mtz-h1" style={{ color: "#fff", margin: 0, fontSize: 72, lineHeight: 1.02 }}>
              A family-oriented church in China Grove —
              <em style={{ fontStyle: "italic", color: "var(--mtz-mint-accent)" }}> come as you are.</em>
            </h1>
            <p style={{
              margin: "24px 0 0",
              fontFamily: "var(--mtz-serif-body)",
              fontSize: 20,
              color: "rgba(255,255,255,0.92)",
              maxWidth: 620,
              lineHeight: 1.45,
            }}>
              Sunday Service at 10:30
            </p>
            <div className="mtz-row" style={{ gap: 12, marginTop: 28 }}>
              <a className="mtz-btn mtz-btn--primary" href="/contact">Get in Touch</a>
              <a className="mtz-btn" href="/community" style={{ background: "transparent", color: "#fff", border: "1px solid #fff" }}>Meet Our Community</a>
            </div>
          </div>
        </div>
      </section>
      <section className="mtz-section" style={{ paddingTop: 56, paddingBottom: 48 }}>
        <p className="mtz-lede" style={{ maxWidth: 760, fontSize: 26 }}>
          If you're looking for a place to call home, join us one Sunday morning or
          at one of our community events. Maybe you'll find that Mount Zion is the
          family you've been looking for.
        </p>
      </section>
      <hr className="mtz-rule" />
    </>
  );
}

function TypeHero() {
  return (
    <>
      <section className="mtz-section" style={{ paddingTop: 80, paddingBottom: 64 }}>
        <p className="mtz-kicker">Welcome to Mt Zion UCC · est. 1858</p>
        <h1 className="mtz-h1" style={{ maxWidth: 900 }}>
          A family-oriented church in&nbsp;China&nbsp;Grove —
          <em style={{ fontStyle: "italic", color: "var(--mtz-mint-dark)" }}> come as you are.</em>
        </h1>
        <p className="mtz-lede">
          If you're looking for a place to call home, join us one Sunday morning or
          at one of our community events. Maybe you'll find that Mount Zion is the
          family you've been looking for.
        </p>
        <p className="mtz-mute" style={{ fontFamily: "var(--mtz-serif-body)", fontSize: 19, marginTop: 8, marginBottom: 0, maxWidth: 620 }}>
          Sunday Service at 10:30
        </p>
        <div className="mtz-row" style={{ gap: 12, marginTop: 28 }}>
          <a className="mtz-btn mtz-btn--primary" href="/contact">Get in Touch</a>
          <a className="mtz-btn mtz-btn--ghost" href="/community">Meet Our Community</a>
        </div>
      </section>
      <hr className="mtz-rule" />
    </>
  );
}

function HomeChurchBody() {
  return (
    <>
      {/* FEATURED EVENTS — prominent flyers from the church secretary */}
      <section id="events" className="mtz-section--tint">
        <div className="mtz-section-inner">
          <div className="mtz-row" style={{ justifyContent: "space-between", alignItems: "baseline", marginBottom: 28 }}>
            <div>
              <p className="mtz-kicker" style={{ margin: 0 }}>What's Coming Up</p>
              <h2 className="mtz-h2" style={{ margin: "4px 0 0" }}>Mark your calendar.</h2>
            </div>
            <a className="mtz-arrow-link" href="/events">All events →</a>
          </div>

          <div className="mtz-grid mtz-grid--2" style={{ gap: 36, alignItems: "stretch" }}>
            {[
              {
                src: "images/friends-family-sunday.jpg",
                alt: "Friends and Family Sunday — May 4 at 10:30 AM",
                kicker: "This Sunday · May 4 · 10:30 AM",
                title: "Friends and Family Sunday",
                blurb: "Worship in our SonCourt, a mini-concert from our preschoolers, and a hot-dog dinner after. Come home.",
                cta: "Plan to be there",
              },
              {
                src: "images/vbs-rome-2026.jpg",
                alt: "Vacation Bible School: Paul and the Underground Church — June 14–16",
                kicker: "Save the Date · June 14–16",
                title: "Vacation Bible School",
                blurb: "Paul and the Underground Church. Dinner 5:30, program 6:00–8:00 — for kids of all ages.",
                cta: "Register your child",
              },
            ].map((e) => (
              <article key={e.title} className="mtz-card" style={{ background: "var(--mtz-bg)", overflow: "hidden" }}>
                <a href="/events" style={{ display: "block" }}>
                  <img
                    src={e.src}
                    alt={e.alt}
                    style={{
                      display: "block",
                      width: "100%",
                      aspectRatio: "1230 / 780",
                      objectFit: "cover",
                      borderBottom: "1px solid var(--mtz-rule)",
                    }}
                  />
                </a>
                <div className="mtz-card-body" style={{ padding: 24 }}>
                  <p className="mtz-card-meta" style={{ marginBottom: 8 }}>{e.kicker}</p>
                  <h3 className="mtz-h3" style={{ fontSize: 22, marginBottom: 8 }}>{e.title}</h3>
                  <p style={{ color: "var(--mtz-ink-soft)", margin: "0 0 16px", fontSize: 15.5 }}>{e.blurb}</p>
                  <a className="mtz-arrow-link" href="/events">{e.cta} →</a>
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>

      {/* THIS SUNDAY — compact strip below the flyers */}
      <section id="worship" className="mtz-section" style={{ paddingTop: 56, paddingBottom: 56 }}>
        <div className="mtz-row" style={{
          justifyContent: "space-between",
          alignItems: "baseline",
          paddingBottom: 16,
          borderBottom: "1px solid var(--mtz-ink)",
          marginBottom: 0,
          flexWrap: "wrap",
          gap: 16,
        }}>
          <div>
            <p className="mtz-kicker" style={{ margin: 0 }}>This Sunday</p>
            <h3 className="mtz-h3" style={{ margin: "4px 0 0", fontSize: 24 }}>Worship times</h3>
          </div>
          <div className="mtz-row" style={{ gap: 24, flexWrap: "wrap" }}>
            <a className="mtz-arrow-link" href="#">This week's bulletin →</a>
            <a className="mtz-arrow-link" href="#">Watch on Facebook →</a>
          </div>
        </div>
        <div className="mtz-grid" style={{
          gridTemplateColumns: "repeat(4, 1fr)",
          gap: 0,
        }}>
          {[
            ["8:30 AM",  "Early Worship",       "Contemplative"],
            ["9:30 AM",  "Sunday School",       "All ages"],
            ["10:30 AM", "Traditional Worship", "Choir & organ"],
            ["6:00 PM",  "Youth Group",         "Fellowship Hall"],
          ].map(([time, title, sub], i) => (
            <div key={title} style={{
              padding: "20px 16px",
              borderRight: i < 3 ? "1px solid var(--mtz-rule)" : "none",
              borderBottom: "1px solid var(--mtz-rule)",
            }}>
              <div className="mtz-mono" style={{ fontSize: 12.5, letterSpacing: "0.12em", color: "var(--mtz-mint-dark)", fontWeight: 600, marginBottom: 6 }}>{time}</div>
              <div style={{ fontFamily: "var(--mtz-serif-display)", fontSize: 19, fontWeight: 500, marginBottom: 2 }}>{title}</div>
              <div className="mtz-mute" style={{ fontSize: 13 }}>{sub}</div>
            </div>
          ))}
        </div>
      </section>

      {/* ALWAYS AT MT ZION — regular weekly activities */}
      <section id="always" className="mtz-section" style={{ paddingTop: 72, paddingBottom: 72 }}>
        <div style={{ marginBottom: 40 }}>
          <h2 className="mtz-h2" style={{ marginBottom: 8 }}>Always at Mt.&nbsp;Zion</h2>
          <p className="mtz-mute" style={{ fontSize: 18, margin: 0, fontFamily: "var(--mtz-serif-body)" }}>
            Regular activities throughout the week.
          </p>
        </div>
        <div className="mtz-grid" style={{
          gridTemplateColumns: "repeat(3, 1fr)",
          gap: 40,
        }}>
          {[
            { name: "Pickleball",            when: "Wed · 6:30 PM",      desc: "Open play in Fellowship Hall. Paddles provided; all skill levels welcome." },
            { name: "Tai Chi",               when: "Tue · 9:00 AM",      desc: "Gentle movement and breath, led by Linda Sloop in the Education Wing." },
            { name: "Youth Group",           when: "Sun · 6:00 PM",      desc: "Middle and high school students gather for games, dinner, and discussion." },
            { name: "Choir Rehearsal",       when: "Wed · 7:00 PM",      desc: "Sanctuary choir prepares for Sunday worship. New voices always welcome." },
            { name: "Bible Study",           when: "Thu · 10:00 AM",     desc: "Pastor Jim leads a small-group study in the church library. Coffee provided." },
            { name: "Community Food Drive",  when: "First Sat · 9–11 AM",desc: "Sort and distribute donations for Rowan Helping Ministries." },
          ].map((a) => (
            <article key={a.name} className="mtz-card" style={{ border: 0, background: "transparent" }}>
              <div className="mtz-img" style={{
                aspectRatio: "5/4",
                borderRadius: 6,
                marginBottom: 18,
              }}>
                <span className="mtz-img-label">{a.name.toLowerCase()} · photo</span>
              </div>
              <h3 className="mtz-h3" style={{ fontSize: 24, marginBottom: 6 }}>{a.name}</h3>
              <p className="mtz-mono" style={{
                fontSize: 12.5,
                letterSpacing: "0.12em",
                color: "var(--mtz-mint-dark)",
                margin: "0 0 10px",
                fontWeight: 600,
                textTransform: "uppercase",
              }}>{a.when}</p>
              <p style={{ color: "var(--mtz-ink-soft)", margin: 0, fontSize: 15.5, lineHeight: 1.55 }}>
                {a.desc}
              </p>
            </article>
          ))}
        </div>
      </section>

      {/* NEWS & ANNOUNCEMENTS */}
      <section id="news" className="mtz-section--cream">
        <div className="mtz-section-inner">
          <div className="mtz-row" style={{ justifyContent: "space-between", alignItems: "baseline", marginBottom: 28 }}>
            <h2 className="mtz-h2" style={{ margin: 0 }}>News &amp; Announcements</h2>
            <a className="mtz-arrow-link" href="#">All news →</a>
          </div>
          <div className="mtz-grid mtz-grid--3">
            {[
              { tag: "Pastor's Note", title: "On grace, in plain language",          date: "May 1, 2026",  excerpt: "A reflection on what it means to receive — and offer — grace in a noisy season." },
              { tag: "Outreach",      title: "Spring food drive: 1,240 lbs collected", date: "Apr 28, 2026", excerpt: "Thank you to everyone who contributed. Rowan Helping Ministries received the delivery on Monday." },
              { tag: "Music",         title: "Choir welcomes four new voices",        date: "Apr 22, 2026", excerpt: "Rehearsals continue Wednesdays at 7 PM. New members always welcome." },
            ].map((n) => (
              <article key={n.title} className="mtz-card">
                <div className="mtz-img" style={{ aspectRatio: "16/10", borderRadius: 0, borderLeft: 0, borderRight: 0, borderTop: 0 }}>
                  <span className="mtz-img-label">image · 800×500</span>
                </div>
                <div className="mtz-card-body">
                  <p className="mtz-card-meta" style={{ marginBottom: 8 }}>{n.tag} · {n.date}</p>
                  <h3 className="mtz-h3" style={{ fontSize: 22, marginBottom: 10 }}>{n.title}</h3>
                  <p style={{ color: "var(--mtz-ink-soft)", fontSize: 15, margin: 0 }}>{n.excerpt}</p>
                </div>
              </article>
            ))}
          </div>
        </div>
      </section>

      {/* ABOUT / HISTORY teaser */}
      <section id="about" className="mtz-section">
        <div className="mtz-grid mtz-grid--2" style={{ gap: 64, alignItems: "center" }}>
          <div className="mtz-img" style={{ aspectRatio: "4/5", minHeight: 0 }}>
            <span className="mtz-img-label">archival photo · ca. 1910</span>
          </div>
          <div>
            <p className="mtz-kicker">Our Story · 168 Years</p>
            <h2 className="mtz-h2">A congregation that has gathered on this hill since 1858.</h2>
            <p className="mtz-prose" style={{ color: "var(--mtz-ink-soft)" }}>
              From a one-room log meetinghouse to the sanctuary that stands today,
              Mount Zion's story is told in the people who have shown up — week after
              week, generation after generation. Our growing digital archive opens
              that story to anyone who'd like to look.
            </p>
            <div className="mtz-row" style={{ gap: 12, marginTop: 24 }}>
              <a className="mtz-btn mtz-btn--ghost" href="/about">Read our history</a>
              <a className="mtz-arrow-link" href="/about#archive">Search the archive →</a>
            </div>
          </div>
        </div>
      </section>

      {/* OUTREACH preview */}
      <section id="outreach" className="mtz-section--tint">
        <div className="mtz-section-inner">
          <div className="mtz-row" style={{ justifyContent: "space-between", alignItems: "baseline", marginBottom: 28 }}>
            <div>
              <p className="mtz-kicker">Outreach</p>
              <h2 className="mtz-h2" style={{ margin: 0 }}>Showing up, beyond Sunday.</h2>
            </div>
            <a className="mtz-arrow-link" href="/outreach">All ministries →</a>
          </div>
          <div className="mtz-grid mtz-grid--3">
            {[
              { name: "Rowan Helping Ministries", note: "Monthly food sort · first Saturday", body: "Sorting and distributing donations for our neighbors in need across Rowan County." },
              { name: "Habitat for Humanity",     note: "Spring & fall builds",               body: "Mt Zion volunteers join two builds each year, with lunch provided on site." },
              { name: "China Grove Backpack",    note: "Weekly · school year",                  body: "Packing weekend meals for elementary students who need food at home." },
            ].map((o) => (
              <article key={o.name} className="mtz-card mtz-card-body" style={{ padding: 28 }}>
                <p className="mtz-card-meta" style={{ marginBottom: 8 }}>{o.note}</p>
                <h3 className="mtz-h3" style={{ fontSize: 22 }}>{o.name}</h3>
                <p style={{ color: "var(--mtz-ink-soft)", margin: 0, fontSize: 15 }}>{o.body}</p>
              </article>
            ))}
          </div>
        </div>
      </section>
    </>
  );
}

function HomePreschool() {
  return (
    <>
      <section style={{ position: "relative", maxWidth: "none", padding: 0 }}>
        <div className="mtz-img" style={{
          height: 560, borderRadius: 0, border: 0,
          borderBottom: "1px solid var(--mtz-ink)", minHeight: 0,
        }}>
          <span className="mtz-img-label" style={{ position: "absolute", top: 16, right: 16 }}>
            hero photo · classroom or playground
          </span>
        </div>
        <div style={{
          position: "absolute", inset: 0,
          background: "linear-gradient(180deg, rgba(255,255,255,0) 30%, rgba(0,0,0,0.35) 100%)",
          pointerEvents: "none",
        }} />
        <div style={{
          position: "absolute", left: 0, right: 0, bottom: 0,
          padding: "0 32px 56px", maxWidth: "var(--mtz-page-w)", margin: "0 auto",
        }}>
          <div style={{ maxWidth: 760, color: "#fff" }}>
            <p className="mtz-kicker" style={{ color: "rgba(255,255,255,0.85)" }}>Mt Zion Preschool</p>
            <h1 className="mtz-h1" style={{ color: "#fff", margin: 0, fontSize: 64 }}>
              Where curiosity, kindness, and faith grow together.
            </h1>
            <div className="mtz-row" style={{ gap: 12, marginTop: 28 }}>
              <a className="mtz-btn mtz-btn--primary" href="#">Schedule a Tour</a>
              <a className="mtz-btn" href="#" style={{ background: "transparent", color: "#fff", border: "1px solid #fff" }}>2026–27 Admissions</a>
            </div>
          </div>
        </div>
      </section>
      <section className="mtz-section" style={{ paddingTop: 56, paddingBottom: 32 }}>
        <p className="mtz-lede">
          A nurturing Christian preschool program in China Grove,
          providing quality early-childhood education for ages 2–5.
        </p>
      </section>
      <hr className="mtz-rule" />
      <section className="mtz-section">
        <div className="mtz-grid mtz-grid--3">
          {[
            { t: "Two-Year-Olds", d: "T/Th mornings", h: "Sensory play, story circle, music." },
            { t: "3K", d: "M/W/F mornings",  h: "Pre-literacy, fine motor, friendships." },
            { t: "4K", d: "M–F mornings",   h: "Kindergarten readiness, faith stories." },
          ].map((p) => (
            <div key={p.t} className="mtz-card">
              <div className="mtz-img" style={{ aspectRatio: "4/3", borderRadius: 0, borderLeft: 0, borderRight: 0, borderTop: 0 }}>
                <span className="mtz-img-label">classroom photo</span>
              </div>
              <div className="mtz-card-body">
                <p className="mtz-card-meta">{p.d}</p>
                <h3 className="mtz-h3">{p.t}</h3>
                <p style={{ color: "var(--mtz-ink-soft)", margin: 0 }}>{p.h}</p>
              </div>
            </div>
          ))}
        </div>
      </section>
    </>
  );
}

window.HomeChurch = HomeChurch;
window.HomePreschool = HomePreschool;
