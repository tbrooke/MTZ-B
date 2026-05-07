import { Editor } from '@tiptap/core'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Link from '@tiptap/extension-link'
import Table from '@tiptap/extension-table'
import TableRow from '@tiptap/extension-table-row'
import TableCell from '@tiptap/extension-table-cell'
import TableHeader from '@tiptap/extension-table-header'
import Typography from '@tiptap/extension-typography'

function createToolbar(editor) {
  const bar = document.createElement('div')
  bar.className = 'tiptap-toolbar'
  const btns = [
    ['B',      'Bold',           () => editor.chain().focus().toggleBold().run(),                     () => editor.isActive('bold')],
    ['I',      'Italic',         () => editor.chain().focus().toggleItalic().run(),                   () => editor.isActive('italic')],
    ['H2',     'Heading 2',      () => editor.chain().focus().toggleHeading({level:2}).run(),         () => editor.isActive('heading',{level:2})],
    ['H3',     'Heading 3',      () => editor.chain().focus().toggleHeading({level:3}).run(),         () => editor.isActive('heading',{level:3})],
    ['• List', 'Bullet list',    () => editor.chain().focus().toggleBulletList().run(),               () => editor.isActive('bulletList')],
    ['1. List','Ordered list',   () => editor.chain().focus().toggleOrderedList().run(),              () => editor.isActive('orderedList')],
    ['❝',     'Blockquote',     () => editor.chain().focus().toggleBlockquote().run(),               () => editor.isActive('blockquote')],
    ['—',      'Divider',        () => editor.chain().focus().setHorizontalRule().run(),              () => false],
    ['Link',   'Add link',       () => { const u=prompt('URL'); if(u) editor.chain().focus().setLink({href:u}).run() }, () => editor.isActive('link')],
    ['Unlink', 'Remove link',    () => editor.chain().focus().unsetLink().run(),                      () => false],
    ['Image',  'Add image (URL)',() => { const u=prompt('Image URL'); if(u) editor.chain().focus().setImage({src:u}).run() }, () => false],
  ]
  btns.forEach(([label, title, action, isActive]) => {
    const btn = document.createElement('button')
    btn.type = 'button'
    btn.title = title
    btn.textContent = label
    btn.className = 'tiptap-btn' + (isActive() ? ' is-active' : '')
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

document.addEventListener('DOMContentLoaded', () => {
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
