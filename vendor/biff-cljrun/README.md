# cljrun

A tool&mdash;nay, a convention&mdash;for curating and running Clojure CLI tasks.

I made cljrun because I wanted to provide a bundle of default CLI tasks for
[Biff](https://biffweb.com) projects without having to copy a bunch of boilerplate into new
projects. Both the task implementations (functions) and task "bundles" (maps, see below) should be
defined in library code. As a "task bundle" maintainer, this allows me to both update task
implementations and add new tasks without requiring users to do anything besides bumping a version.

I also wanted a solution that:

- works without anything installed other than `clj`.
- is useful for curating tasks even if those tasks weren't written with cljrun in mind.

I currently only use cljrun in Biff projects, so the bundle of tasks there is largely Biff-specific.
However I also like the idea of providing a more "vanilla" bundle of default tasks that could be
useful more broadly in non-Biff projects. (e.g. there could be tasks for creating new projects,
running tests, updating deps, building jars, publishing to clojars...). Maybe I'll do that before
publicly announcing this tool.

## Demo

This repo defines a couple example tasks in `src/dev/`:

```bash
$ clj -M:run -h
Available commands:

  a - task-a/task-a
  b - task-b/task-b

$ clj -M:run a --message hello
Task A args: ("--message" "hello")
```

## Usage

The main idea is that tasks are defined as maps like so:

```clojure
(ns com.example.tasks)

(def tasks
  {"my-task" 'com.example/my-task
   "nrepl" 'nrepl.cmdline/-main})
```

To ensure reasonable start-up time, this namespace holding the tasks map shouldn't require anything.
Individual tasks are required only when they're ran.

Then you add an alias to `deps.edn` that calls the cljrun task runner, passing in the `tasks` map
above:

```clojure
;; :run is used as the alias by convention
:aliases {:run {:extra-deps {io.github.biffweb/cljrun {:git/tag "v1.0" :git/sha "f10b128"}
                             ;; If your tasks are defined in a library, add it here:
                             com.example/tasks {:mvn/version "1.0"}}
                ;; If your tasks are defined in the current project, make sure they're on the
                ;; classpath:
                :extra-paths ["src/dev"]
                ;; Pass the tasks map symbol(s) in here:
                :main-opts ["-m" "com.biffweb.cljrun" "com.example.tasks/tasks" "--"]}}
```

Then you can do `clj -M:run my-task` to run the task, or `clj -M:run -h` to see the available tasks.
For extra ergonomics you can put `alias cljrun='clj -M:run'` in your `.bashrc`.

I generally define the `:run` alias in my project `deps.edn` files, but you could also stick it in
`~/.clojure/deps.edn`.

### Defining your own tasks

Continuing the example above, if you want to define (or override) some additional project-specific
tasks, you can reference them in `src/dev/tasks/tasks.clj`:

```clojure
(ns tasks)

(def tasks
  {"another-task" 'tasks.another-task/task})
```

And then pass `tasks/tasks` to cljrun in `deps.edn`:

```clojure
;; Everything before the "--" will be resolved and merged together:
:main-opts ["-m" "com.biffweb.cljrun" "com.example.tasks/tasks" "tasks/tasks" "--"]
```

### Writing tasks that call other tasks

cljrun provides a `run-task` function for calling other tasks:

```clojure
(ns com.example.my-task
  (:require [com.biffweb.cljrun :refer [run-task]]))

(defn my-task [& args]
  ;; Calls the "another-task" task from the task map that was passed to cljrun:
  (run-task "another-task" "--foo" "bar"))
```

This will ensure that if the user has defined a custom `"another-task"` task, you'll call it. If you
don't care about that, you can instead call the task function directly:

```clojure
(ns com.example.my-task
  (:require [com.example.another-task :refer [another-task]]))

(defn my-task [& args]
  (another-task "--foo" "bar"))
```

### Argument parsing

cljrun doesn't do any argument-parsing; all arguments are passed as strings and you can parse them
using whatever methods you like.
