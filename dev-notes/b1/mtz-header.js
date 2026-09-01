<script>
(function () {
  if (window.__mtzHeaderScroll) return;
  window.__mtzHeaderScroll = true;
  var s = document.documentElement.style;

  var on = null;
  function shrink() {
    var next = window.scrollY > 12;
    if (next === on) return;
    on = next;
    if (next) {
      s.setProperty("--mtz-bar-shrink", "0.79");
      s.setProperty("--mtz-logo-shrink", "0.75");
      s.setProperty("--mtz-nav-shrink", "0.93");
      s.setProperty("--mtz-bar-bg", "rgba(255,255,255,0.92)");
      s.setProperty("--mtz-bar-shadow", "0 1px 0 rgba(0,0,0,0.04), 0 8px 24px -16px rgba(0,0,0,0.18)");
    } else {
      s.removeProperty("--mtz-bar-shrink");
      s.removeProperty("--mtz-logo-shrink");
      s.removeProperty("--mtz-nav-shrink");
      s.removeProperty("--mtz-bar-bg");
      s.removeProperty("--mtz-bar-shadow");
    }
  }

  // The right nav group carries the Preschool chip and longer words, so it is
  // wider than the left. Equal 1fr spacers centre the wordmark BETWEEN the
  // groups, which is not the centre of the bar. Leading the row by the
  // difference makes the two groups balance, so the wordmark lands dead centre.
  // Measured rather than hard-coded so renaming a menu item cannot skew it.
  function lead() {
    var nav = document.querySelector("#navbar nav");
    if (!nav || nav.children.length < 8) return;
    if (window.innerWidth < 900) { s.removeProperty("--mtz-nav-lead"); return; }
    var k = nav.children;
    var box = function (el) { return el.getBoundingClientRect(); };
    var L = box(k[3]).right - box(k[0]).left;
    var R = box(k[7]).right - box(k[4]).left;
    var d = Math.round(R - L);
    s.setProperty("--mtz-nav-lead", (d > 0 ? d : 0) + "px");
  }

  window.addEventListener("scroll", shrink, { passive: true });
  window.addEventListener("resize", lead);
  if (document.fonts && document.fonts.ready) document.fonts.ready.then(lead);
  shrink();
  requestAnimationFrame(function () { lead(); requestAnimationFrame(lead); });
})();
</script>
