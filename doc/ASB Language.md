The ASB Language is where custom commands and system properties are defined and then used as desired.

- **Meta language** refers to constructs that are already provided by ASB, which are required to define and implement the user language.
- The **user language** is the effective language that the user creates by utilizing ASB. This comprises **custom commands** and system properties.
- **(Custom) commands** are the main part of the user language, which correspond to the assembler instruction set of an architecture that the user wishes to emulate or design.
- **Functions** are similar to custom commands, but they can only be used in specific places. **Built-in functions** come as part of the ASB meta language, which are used to implement custom commands.
- **User (language) program** is the effective program encoded using the user language. It comprises custom commands.
- **ASB program** is everything written in the ASB language by the user. It comprises the user program as well as the definition of the user language.

Both meta and user language are mixed within ASB language source code files. Basically, once a custom command is defined, it can be used.

# Basic Format
An ASB program consists of one or more source files, which have the file ending `.asb`. The expected encoding is 8-bit **ASCII**. The behavior of codepoints outside of ASCII (e.g. ISO 8859, UTF-8) is undefined.
## Whitespace
Whitespace is meaningful in the following ways:
- Statements end at the end of a line. A new line is marked by the Line Feed or Newline character (Codepoint `0xA`).
- The Carriage Return (Codepoint `0xD`) and the Vertial Tab (Codepoint `0xB`) characters are always ignored.
- Any continuous amount of Space (Codepoint `0x20`) and Horizontal Tab (Codepoint `0x9`) characters are interpreted as one separator between two atoms. Such a separator is optional in all cases where the atom separation can be determined by different means (e.g. a `%` atom followed by a name atom).
> [!NOTE]
> It follows that any indentation (be it with Spaces or Tabs) has no meaning.

## Numbers
Numbers, both in the meta language as well as the user language, can be given in decimal, hexadecimal, octal, or binary format. The decimal format also supports negative numbers.

In any of the formats additional `_` characters may be used to visually subdivide the number further. It has no effect on the value of the given number. This character cannot be used at the beginning of the number, though.

- **Decimal** format: Any of the decimal digits, but the first digit must not be `0`. May be prefixed with `-`, which makes it a negative number in Two's complement encoding.
  Regex: `-?[1-9][_0-9]*`
  > [!NOTE]
  > Some places do not allow a negative number to be used, e.g. when giving a length or bit position.
  
- **Hexadecimal** format: Any of the decimal digits and `a`- `f` (also uppercase); prefixed by `0x`.
  Regex: `0x[_0-9A-Fa-f]+`
- **Octal** format: Any of the decimal digits up to `7`, prefixed by `0`.
  Regex: `0[_0-7]+`
- **Binary** format: The binary digits `0` and `1`, prefixed by `0b`.
  Regex: `0b[_01]+`
# Meta language syntax
- All meta language operations are done via **ASB directives** which start with a `.` followed by a keyword.
- Some directives have **sub-directives**, which also start with a keyword prefixed with a `.`. These sub-directives can either be given on the same line as the directive they belong to, or the directive is extended to multiple lines by encasing all its sub-directives in in one pair of curly braces (`{}`).
- Some directives or sub-directives contain an **implementation body** which is also encased in curly braces and can span multiple lines as well.
- **Functions** are prefixed with a `&`. They can only be called inside implementation bodies.
> [!NOTE]
> ASB comes with pre-defined, built-in functions, but the user can also add their own (the same restriction regarding invocation applies).

- Parameter types are prefixed with a `/`. Register and variable lengths are given in amount of bits, prefixed with `''`. Individual bits of registers or variables are accessed by giving a single bits number or a range of bits where two bit numbers are separated by `:`, in both cases prefixed with a single `'`.
> [!NOTE]
> It is not possible to access individual bits within the user language in this way - if such a mechanism is desired, it must be implemented with custom commands.

- Line comments start with `//` (they end **right before** the next New Line).
- Multi-line comments start with `/*` and end with `*/`. If the start and end markers are not on the same line the comment is treated like a single New Line (so that different statements are separated as intended).

# Types
Command and function parameters have a type. They are prefixed with a `/`.
The following types exist:
- `/register` or `/reg`: Points to a machine register
- `/variable` or `/var`: Points to a local variable used within a commands or functions implementation
- `/immediate` or `/imm`: An immediate, i.e. constant, numeric value given directly in the invocation.
- `/label` a label that points to a specific location in the user language program. This is a constant name given directly in the invocation.

Technically, `/register` and `/variable` types are treated the same; they can be used interchangeably.

Every type except `/label` is followed by a length definition, which is `''` followed by a number; this defines how many bits this parameter's source must have.

> [!TODO]
> - Describe max-length (`''<`). Is there also a min-length?
> - Describe register groups here?

# Defining system properties
The architecture emulated by ASB comprises the following:
- One simple CPU with a single thread of execution, no pipelining, no reordering (i.e. user language commands are executed individually, one after the other, in the order given).
- Data memory of arbitrary (but defined) size.
- Individually defined registers, each with individually defined bit length.
- A Program counter pointing to the current command in the user program.

## Defining memory
Memory size must be defined if any command is used that accesses the data memory. Otherwise this definition is optional.

Memory size is defined by giving the length of memory addresses, and the size of each memory word, each in bits. A memory address then points to a single memory word. The full size of addressable and usable memory is thus $2^{addressBits} * wordSize$ bits.
> [!NOTE]
> Your machine must be able to handle the amount of memory required for the user program that you run. ASB does not blindly allocate the full memory size defined here though, only chunks of it as required. So, you can define the memory as big as you like, as long as the user program doesn't use the entire address space.

Use the `.memory` directive with the two sub-directives `.word` and `.address`:
```
.memory .word ''<wordSize> .address ''<addressLength>
```

Instead of `.address` the alternative keyword `.addr` can be used. The sub-directives can be given in either order. Additionally, the entire directive can be given on multiple lines like so:
```
.memory {
    .word ''<wordSize>
    .address ''<addressLength>
}
```

Replace `<wordSize>` with the amount of bits in each memory word, and `<addressLength>` with the amount of bits in a memory address.

Memory size may be defined more than once; the latter definition effectively overriding earlier definitions. However, **after the first command that accesses data memory** is executed, memory MUST NOT be reconfigured any more; otherwise an error occurs.

Also, if a command is executed that accesses data memory, but memory size has not been configured by this point, an error occurs as well.

## Defining registers
>[!TODO]

## Defining the program counter
The program counter points to the next to execute, or currently executed, command in the user program.

It starts at `0`, which points to the first command. Every executed command is worth exactly one step of the program counter, i.e. the program counter is incremented by `1` after every executed command. Jump functions modify the program counter accordingly.

The bit length of the program counter may be configured, but that is optional. If not defined, the program counter size is set to `64` bit.

To define program counter size use the `.program_counter` or the alternative `.pc` directive:
```
.program_counter ''<length>
```
Replace `<length>` with the amount of bits in the program counter.

You do not need to specify the program counter size if no command is reading or setting its value directly. Especially if all jumping occurs via the use of labels, nothing further needs to be done.
>[!TODO]
>Note that implementing some sort of `RET` command to return to the calling location at the end of a function call does require accessing the program counter value.

The size of the program counter restricts the size of the user program; a program with more commands (in its source code) than the (unsigned) value of the program counter can enumerate will lead to an error.

Program counter size may be defined more than once; the latter definition effectively overriding earlier definitions. However, **after the first user program command** is executed the program counter size MUST NOT be reconfigured any more; otherwise an error occurs.

# Defining custom commands
>[!TODO]

## Built-in functions
>[!TODO]
>List and explain.
>- getpc aka get_program_counter
>- setpc aka set_program_counter
>- I have a list somewhere

## Bitwise access
> [!TODO]

# User language syntax
>[!TODO]

# Loading other files
> [!TODO]

