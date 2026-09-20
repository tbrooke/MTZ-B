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

// Image field — see con/image-field.
//
// The hidden input is the only thing the form submits; everything else here
// is the picture of it. Setting the value also fires an `input` event on the
// input so the autosave above notices — a programmatic .value= does not.

(function () {
  'use strict';

  function csrf() {
    var el = document.querySelector('input[name="__anti-forgery-token"]');
    return el ? el.value : '';
  }

  function setImage(field, id, url) {
    var input   = field.querySelector('input[type="hidden"]');
    var preview = field.querySelector('.con-imgpick-preview');
    var clear   = field.querySelector('[data-pick="clear"]');
    input.value = id || '';
    preview.innerHTML = id
      ? '<img src="' + url + '" alt="">'
      : '<span class="con-imgpick-empty">No image</span>';
    if (clear) clear.hidden = !id;
    input.dispatchEvent(new Event('input', { bubbles: true }));
  }

  var dialog = null;
  var target = null; // the field the open dialog is choosing for

  function openChooser(field) {
    target = field;
    if (!dialog) {
      dialog = document.createElement('dialog');
      dialog.id = 'con-imgpick';
      dialog.className = 'con-imgpick-modal';
      document.body.appendChild(dialog);
      dialog.addEventListener('click', function (e) {
        var tile = e.target.closest('[data-pick-id]');
        if (tile) {
          setImage(target, tile.dataset.pickId, tile.dataset.pickUrl);
          dialog.close();
          return;
        }
        if (e.target.closest('[data-pick="close"]') || e.target === dialog) dialog.close();
      });
    }
    dialog.innerHTML = '<div class="con-imgpick-dialog"><p class="con-imgpick-none">Loading…</p></div>';
    dialog.showModal();
    fetch('/console/media/pick', { credentials: 'same-origin' })
      .then(function (r) { return r.text(); })
      .then(function (html) {
        dialog.innerHTML = html;
        if (window.htmx) window.htmx.process(dialog);
        var q = dialog.querySelector('input[name="q"]');
        if (q) q.focus();
      })
      .catch(function () {
        dialog.innerHTML = '<div class="con-imgpick-dialog"><p class="con-imgpick-none">Could not load Media.</p>' +
          '<button type="button" class="con-btn con-btn--quiet" data-pick="close">Close</button></div>';
      });
  }

  function uploadInto(field) {
    var picker = document.createElement('input');
    picker.type = 'file';
    picker.accept = 'image/*';
    picker.addEventListener('change', function () {
      var file = picker.files[0];
      if (!file) return;
      var preview = field.querySelector('.con-imgpick-preview');
      preview.innerHTML = '<span class="con-imgpick-empty">Uploading…</span>';
      var fd = new FormData();
      fd.append('file', file);
      fd.append('__anti-forgery-token', csrf());
      fetch('/console/media/pick/upload', { method: 'POST', body: fd, credentials: 'same-origin' })
        .then(function (r) {
          if (r.status === 413) throw new Error('too large for the server (' + Math.round(file.size / 1024 / 1024 * 10) / 10 + ' MB)');
          return r.json().then(function (data) {
            if (!r.ok || !data.id) throw new Error(data.error || ('HTTP ' + r.status));
            return data;
          });
        })
        .then(function (data) { setImage(field, data.id, data.url); })
        .catch(function (e) {
          preview.innerHTML = '<span class="con-imgpick-empty is-error">Upload failed: ' + e.message + '</span>';
        });
    });
    picker.click();
  }

  document.addEventListener('click', function (e) {
    var btn = e.target.closest('[data-image-field] [data-pick]');
    if (!btn) return;
    var field = btn.closest('[data-image-field]');
    var what  = btn.dataset.pick;
    if (what === 'choose') openChooser(field);
    else if (what === 'upload') uploadInto(field);
    else if (what === 'clear') setImage(field, null, null);
  });
})();
