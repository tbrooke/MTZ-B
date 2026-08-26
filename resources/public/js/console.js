// Console autosave.
//
// Plain JS on purpose: the Tiptap bundle (admin.js) is built by esbuild, and
// nothing here needs bundling, so keeping autosave out of it means editing this
// file does not require `npm run build`.
//
// The editor's HTML lives in a hidden input that Tiptap's onUpdate keeps
// current. ProseMirror is contenteditable, so its own `input` events bubble to
// the form — one listener on the form covers the title, the details fields and
// the body alike.

(function () {
  'use strict';

  var DEBOUNCE_MS = 1500;

  function ready(fn) {
    if (document.readyState !== 'loading') fn();
    else document.addEventListener('DOMContentLoaded', fn);
  }

  ready(function () {
    var form = document.getElementById('con-post-form');
    if (!form) return;

    var url = form.dataset.autosave;
    if (!url) return; // a post that has never been saved has nowhere to save to

    var indicator = document.getElementById('con-saved');
    var timer = null;
    var dirty = false;
    var inFlight = false;

    function show(text, cls) {
      indicator = document.getElementById('con-saved');
      if (!indicator) return;
      indicator.textContent = text;
      indicator.className = 'con-saved' + (cls ? ' ' + cls : '');
    }

    function save() {
      if (inFlight) { schedule(); return; }
      inFlight = true;
      show('Saving…');

      fetch(url, { method: 'POST', body: new FormData(form) })
        .then(function (r) {
          if (!r.ok) throw new Error('HTTP ' + r.status);
          return r.text();
        })
        .then(function (html) {
          var el = document.getElementById('con-saved');
          if (el) el.outerHTML = html;
          dirty = false;
        })
        .catch(function () {
          // Say what went wrong and that the work is still here — the one thing
          // the writer needs to know is whether it is safe to keep typing.
          show('Not saved — check your connection. Your text is still on screen.', 'is-error');
          dirty = true;
        })
        .then(function () { inFlight = false; });
    }

    function schedule() {
      dirty = true;
      show('Unsaved changes');
      clearTimeout(timer);
      timer = setTimeout(save, DEBOUNCE_MS);
    }

    form.addEventListener('input', schedule);
    form.addEventListener('change', schedule);

    // A normal Save submit is about to write the same thing; don't race it.
    form.addEventListener('submit', function () {
      clearTimeout(timer);
      dirty = false;
    });

    window.addEventListener('beforeunload', function (e) {
      if (!dirty) return;
      e.preventDefault();
      e.returnValue = '';
    });
  });
})();
