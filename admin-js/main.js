import { Editor } from '@tiptap/core'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import Table from '@tiptap/extension-table'
import TableRow from '@tiptap/extension-table-row'
import TableCell from '@tiptap/extension-table-cell'
import TableHeader from '@tiptap/extension-table-header'
import Typography from '@tiptap/extension-typography'

function csrfToken() {
  const el = document.querySelector('input[name="__anti-forgery-token"]')
  return el ? el.value : ''
}

function uploadImage(editor) {
  const picker = document.createElement('input')
  picker.type = 'file'
  picker.accept = 'image/*'
  picker.addEventListener('change', async () => {
    const file = picker.files[0]
    if (!file) return
    const fd = new FormData()
    fd.append('file', file)
    fd.append('__anti-forgery-token', csrfToken())
    try {
      const resp = await fetch('/admin/upload', { method: 'POST', body: fd })
      const data = await resp.json()
      if (data.url) {
        editor.chain().focus().setImage({ src: data.url }).run()
      } else {
        alert('Upload failed: ' + (data.error || 'unknown error'))
      }
    } catch (e) {
      alert('Image upload failed')
    }
  })
  picker.click()
}

let imgBrowserEditor = null

function openImageBrowser(editor) {
  imgBrowserEditor = editor
  let dialog = document.getElementById('mtz-img-browser')
  if (!dialog) {
    dialog = document.createElement('dialog')
    dialog.id = 'mtz-img-browser'
    dialog.innerHTML = '<div class="img-browser-wrap"><p style="padding:24px;color:var(--mtz-ink-mute)">Loading…</p></div>'
    document.body.appendChild(dialog)
    dialog.addEventListener('click', e => {
      const btn = e.target.closest('[data-img-url]')
      if (btn) {
        imgBrowserEditor.chain().focus().setImage({ src: btn.dataset.imgUrl }).run()
        dialog.close()
      }
    })
  }
  dialog.showModal()
  fetch('/admin/images/browse?insert=1')
    .then(r => r.text())
    .then(html => {
      dialog.querySelector('.img-browser-wrap').outerHTML = html
      if (window.htmx) htmx.process(dialog)
    })
    .catch(() => { dialog.querySelector('.img-browser-wrap').innerHTML = '<p style="padding:24px">Failed to load images.</p>' })
}

function createToolbar(editor) {
  const bar = document.createElement('div')
  bar.className = 'tiptap-toolbar'
  const btns = [
    ['B',       'Bold',          () => editor.chain().focus().toggleBold().run(),             () => editor.isActive('bold')],
    ['I',       'Italic',        () => editor.chain().focus().toggleItalic().run(),           () => editor.isActive('italic')],
    ['H2',      'Heading 2',     () => editor.chain().focus().toggleHeading({level:2}).run(), () => editor.isActive('heading',{level:2})],
    ['H3',      'Heading 3',     () => editor.chain().focus().toggleHeading({level:3}).run(), () => editor.isActive('heading',{level:3})],
    ['• List',  'Bullet list',   () => editor.chain().focus().toggleBulletList().run(),       () => editor.isActive('bulletList')],
    ['1. List', 'Ordered list',  () => editor.chain().focus().toggleOrderedList().run(),      () => editor.isActive('orderedList')],
    ['❝',      'Blockquote',    () => editor.chain().focus().toggleBlockquote().run(),       () => editor.isActive('blockquote')],
    ['—',       'Divider',       () => editor.chain().focus().setHorizontalRule().run(),      () => false],
    ['Link',    'Add link',      () => { const u=prompt('URL'); if(u) editor.chain().focus().setLink({href:u}).run() }, () => editor.isActive('link')],
    ['Unlink',  'Remove link',   () => editor.chain().focus().unsetLink().run(),              () => false],
    ['↑ Image', 'Upload image',  () => uploadImage(editor),                                  () => false],
    ['⊞ Browse','Browse library',() => openImageBrowser(editor),                             () => false],
  ]
  btns.forEach(([label, title, action, isActive]) => {
    const btn = document.createElement('button')
    btn.type = 'button'
    btn.title = title
    btn.textContent = label
    btn.className = 'tiptap-btn' + (isActive() ? ' is-active' : '')
    btn.addEventListener('mousedown', e => e.preventDefault())
    btn.addEventListener('click', action)
    bar.appendChild(btn)
  })
  editor.on('selectionUpdate', () => {
    bar.querySelectorAll('button').forEach((btn, i) => {
      btn.className = 'tiptap-btn' + (btns[i][3]() ? ' is-active' : '')
    })
  })
  return bar
}

function initVideoUploads() {
  const widget     = document.getElementById('sermon-video-widget')
  if (!widget) return

  const idInput    = document.getElementById('sermon-video-id')
  const fileInput  = document.getElementById('sermon-video-input')
  const pickBtn    = document.getElementById('sermon-video-btn')
  const fileLabel  = document.getElementById('sermon-video-filename')
  const progressEl = document.getElementById('sermon-video-progress')
  const bar        = document.getElementById('sermon-video-bar')
  const pct        = document.getElementById('sermon-video-pct')
  const status     = document.getElementById('sermon-video-status')

  if (!fileInput || !idInput || !pickBtn) return

  const form        = widget.closest('form')
  const submitBtns  = form ? Array.from(form.querySelectorAll('[type="submit"]')) : []
  let uploadInProgress = false

  const lockForm   = () => {
    uploadInProgress = true
    submitBtns.forEach(b => { b.disabled = true; b.title = 'Wait for video upload to finish' })
  }
  const unlockForm = () => {
    uploadInProgress = false
    submitBtns.forEach(b => { b.disabled = false; b.title = '' })
  }

  if (form) {
    form.addEventListener('submit', e => {
      if (uploadInProgress) {
        e.preventDefault()
        if (status) { status.textContent = '⚠ Upload still in progress — please wait'; status.style.color = '#c0392b' }
      }
    })
  }

  pickBtn.addEventListener('click', () => fileInput.click())

  fileInput.addEventListener('change', async () => {
    const file = fileInput.files[0]
    if (!file) return

    lockForm()
    if (fileLabel) fileLabel.textContent     = file.name
    if (status)    status.textContent        = 'Getting upload slot…'
    if (status)    status.style.color        = 'var(--mtz-ink-soft)'
    if (progressEl) progressEl.style.display = 'block'
    if (bar)       bar.style.width           = '0'
    if (pct)       pct.textContent           = '0%'

    const title = document.querySelector('input[name="title"]')?.value || 'Sermon'
    const fd = new FormData()
    fd.append('__anti-forgery-token', csrfToken())
    fd.append('title', title)

    let slot
    try {
      const r = await fetch('/admin/sermons/upload-slot', { method: 'POST', body: fd })
      slot = await r.json()
    } catch (e) {
      if (status) { status.textContent = '✗ Could not get upload slot'; status.style.color = '#c0392b' }
      unlockForm()
      return
    }

    if (!slot.uploadUrl) {
      if (status) { status.textContent = '✗ ' + (slot.error || 'No upload URL returned'); status.style.color = '#c0392b' }
      unlockForm()
      return
    }

    if (status) status.textContent = 'Uploading…'

    const xhr = new XMLHttpRequest()
    xhr.open('POST', slot.uploadUrl)

    xhr.upload.addEventListener('progress', e => {
      if (e.lengthComputable) {
        const p = Math.round(e.loaded / e.total * 100)
        if (bar) bar.style.width = p + '%'
        if (pct) pct.textContent = p + '%'
      }
    })

    xhr.addEventListener('load', () => {
      if (xhr.status >= 200 && xhr.status < 300) {
        idInput.value = slot.uid
        if (bar)    bar.style.width    = '100%'
        if (pct)    pct.textContent   = '100%'
        if (status) { status.textContent = '✓ Uploaded — save the form to attach this video'; status.style.color = '#5A7257' }
        if (pickBtn) pickBtn.textContent = 'Replace Video'
      } else {
        if (status) { status.textContent = '✗ Upload failed (HTTP ' + xhr.status + ') — video ID: ' + slot.uid; status.style.color = '#c0392b' }
        idInput.value = slot.uid
      }
      unlockForm()
    })

    xhr.addEventListener('error', () => {
      if (status) { status.textContent = '✗ Upload failed (network error) — video ID: ' + slot.uid; status.style.color = '#c0392b' }
      unlockForm()
    })

    xhr.setRequestHeader('Content-Type', file.type || 'video/mp4')
    xhr.send(file)
  })
}

document.addEventListener('DOMContentLoaded', () => {
  initVideoUploads()

  document.querySelectorAll('[data-tiptap]').forEach(wrapper => {
    const name = wrapper.dataset.tiptap
    const input = document.querySelector(`input[name="${name}"]`)
    const editor = new Editor({
      element: wrapper,
      extensions: [
        StarterKit,
        Typography,
        Image,
        Link.configure({ openOnClick: false }),
        Table.configure({ resizable: true }),
        TableRow,
        TableCell,
        TableHeader,
      ],
      content: input ? input.value : '',
      onUpdate: ({ editor }) => {
        if (input) input.value = editor.getHTML()
      },
    })
    wrapper.insertBefore(createToolbar(editor), wrapper.firstChild)
  })
})
