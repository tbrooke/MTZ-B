(ns com.mtzion.tasks
  "Project-specific CLI tasks.

  com.biffweb.cljrun merges several task maps, so these sit alongside Biff's own
  (dev, css, deploy, test…) and are invoked the same way: `clj -M:run <task>`.
  Help text is taken from each task fn's own docstring.")

(def tasks
  {"import"      'com.mtzion.content.ingest/import-task
   "content-doc" 'com.mtzion.content.doc/write-doc})
