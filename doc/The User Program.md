To [Index](index.md)

# The User Program
After having [defined registers](Defining%20Registers.md), [implemented custom commands](Implementing%20Custom%20Commands.md), and [set system properties](Defining%20System%20Properties.md) it is time to implement the actual user program.

The user program consists of command invocations. Functions (incl. [built-in functions](Built-in%20Functions.md)) can not be invoked here (except [`&print*` and `&assert`](Built-in%20Functions.md#special-functions)).

All commands and registers can only be used after they have been defined.

Additionally, **labels** can be used to point to locations within the program. Labels are given as a label name followed by `:`. (See [`&set_program_counter`](Built-in%20Functions.md#set_program_counter-aka-set_pc) on how to manipulate the program counter in combination with custom commands in order to implement jumping capabilities).
