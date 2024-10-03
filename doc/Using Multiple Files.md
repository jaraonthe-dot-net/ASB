To [Index](index.md)

# Using Multiple Files
Your [register definitions](Defining%20Registers.md), [command implementations](Implementing%20Custom%20Commands.md), [system properties](Defining%20System%20Properties.md), and even [user program](The%20User%20Program.md) can be split up across several files as you wish.

You have the following options:

- The ASB command accepts more than one file, which it will be executed in order.
- Additionally, you can use the `--include` cli flag to specify extra files which will be parsed first. See `asb --help`.
- Use the `.include` directive within your ASB program to include another file.
    - This will execute the file as if it contents where present instead of the `.include` directive.
    - `.include` is followed by a string (enclosed in `"`) which is a file reference either absolute or relative to the current file.
    - You may use `/` as directory separator on both Windows and Linux.
    - Example:

```
// From ../asb/example/virtual-register.asb:
.include "../lib/risc-v/risc-v.asb" // Loads the RISC-V implementation
```

- Alternatively you can use `.include_once` which behaves the same as `.include` except that a file that has already been parsed will not be included again. This usually makes sense when loading a file that contains instruction set definitions (which cannot be executed more than once).
