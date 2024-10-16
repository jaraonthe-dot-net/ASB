Here you find syntax highlighting definitions for .asb files.

- Windows: Notepad++ (https://notepad-plus-plus.org/downloads/).
  - In Notepad++, go to Language > User Defined Language
    > Define your language... > Import... the given .xml file

- Linux: Gnome gedit (and possibly others): provided here is a .lang file for
  GtkSourceView
  (https://gnome.pages.gitlab.gnome.org/gtksourceview/gtksourceview5/index.html),
  which is used by a variety of editors including gedit.
  - Sadly, installing the file is not so straight-forward. Depending on which
    version of GtKSourceView is used, the language definition files are kept in
    a location like /usr/share/gtksourceview-*/language-specs/.
  - With root access you can copy the given .lang file here, and if everything
    goes right gedit should automatically support .asb files once it has been
    restarted.
  - If it doesn't work, try one of the other possible directories, and check
    that chmod is set correctly (i.e. the same as other .lang files).
  - Alternatively, to only install the language for your own user, put the .lang
    file into ~/.local/share/gtksourceview-*/language-specs/ (figure out out
    what to put for * with the above steps).
