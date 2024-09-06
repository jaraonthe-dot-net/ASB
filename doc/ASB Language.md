# ASB Language
The ASB Language is where custom commands and system properties are defined and then used as desired.

- **Meta language** refers to constructs that are already provided by ASB, which are required to define and implement the user language.
- The **user language** is the effective language that the user creates by utilizing ASB. This comprises **custom commands** and system properties.
- **(Custom) commands** are the main part of the user language, which correspond to the assembler instruction set of an architecture that the user wishes to emulate or design.
- **Functions** are similar to custom commands, but they can only be used in specific places. **Built-in functions** come as part of the ASB meta language, which are used to implement custom commands.
- **User (language) program** is the effective program encoded using the user language. It comprises custom commands.
- **ASB program** is everything written in the ASB language by the user. It comprises the user program as well as the definition of the user language.

Both meta and user language are mixed within ASB language source code files. Basically, once a custom command is defined, it can be used.

## Basic Format
An ASB program consists of one or more source files, which have the file ending `.asb`. The expected encoding is 8-bit **ASCII**. The behavior of codepoints outside of ASCII (e.g. ISO 8859, UTF-8) is undefined.
### Whitespace
Whitespace is meaningful in the following ways:
- Statements end at the end of a line. A new line is marked by the Line Feed or Newline character (Codepoint `0x0A`).
    - Additionally, the `;` character has the same effect as a Newline.
- The behavior for Carriage Return (Codepoint `0x0D`) and Vertical Tab (Codepoint `0x0B`) characters is undefined.
- Any continuous amount of Space (Codepoint `0x20`) and Horizontal Tab (Codepoint `0x09`) characters are interpreted as one separator between two atoms. Such a separator is optional in all cases where the atom separation can be determined by different means (e.g. a `%` atom followed by a name atom).
> [!NOTE]
> It follows that any indentation (be it with Spaces or Tabs) has no meaning.

### Numbers
Numbers, both in the meta language as well as the user language, can be given in decimal, hexadecimal, octal, or binary format. The decimal format also supports negative numbers.

In any of the formats additional `_` characters may be used to visually subdivide the number further. It has no effect on the value of the given number. This character cannot be used at the beginning of the number, though.

- **Decimal** format: Any of the decimal digits, but the first digit must not be `0`. May be prefixed with `-`, which makes it a negative number in Two's complement encoding.
  Regex: `-?[1-9][_0-9]*`
> [!NOTE]
> Some places do not allow a negative number to be used, e.g. when giving a length or bit position.
  
- **Hexadecimal** format: Any of the decimal digits and `a`- `f` (also uppercase); prefixed by `0x` (or `0X`).
  Regex: `0(x|X)_?[0-9A-Fa-f][_0-9A-Fa-f]*`
- **Octal** format: Any of the decimal digits up to `7`, prefixed by `0`.
  Regex: `0[_0-7]*`
- **Binary** format: The binary digits `0` and `1`, prefixed by `0b` (or `0B`).
  Regex: `0(b|B)_?[01][_01]*`

### Common syntax elements
>[!TODO]
>Note things that apply to both meta and user language. E.g.: `\` escape char, freely available symbols, etc.

## Meta language syntax
- All meta language operations are done via **ASB directives** which start with a `.` followed by a keyword.
- Some directives have **sub-directives**, which also start with a keyword prefixed with a `.`. These sub-directives can either be given on the same line as the directive they belong to, or the directive is extended to multiple lines by encasing all its sub-directives in in one pair of curly braces (`{}`).
    - The `}` closing brace must be followed by a Newline or `;` character (to properly separate it from the next item).
- Some directives or sub-directives contain an **implementation body** which is also encased in curly braces and can span multiple lines as well.
- **Functions** are prefixed with a `&`. They can only be called inside implementation bodies.
> [!NOTE]
> ASB comes with pre-defined, built-in functions, but the user can also add their own (the same restriction regarding invocation applies).

- Parameter types are prefixed with a `/`. Register and variable lengths are given in amount of bits, prefixed with `''`. Individual bits of registers or variables are accessed by giving a single bits number or a range of bits where two bit numbers are separated by `:`, in both cases prefixed with a single `'`.
> [!NOTE]
> It is not possible to access individual bits within the user language in this way - if such a mechanism is desired, it must be implemented with custom commands.

- Line comments start with `#` or `//` (they end **right before** the next New Line).
- Multi-line comments start with `/*` and end with `*/`. If the start and end markers are not on the same line the comment is treated like a single New Line (so that different statements are separated as intended).

## Types
Command and function parameters have a type. They are prefixed with a `/` (in the command or function definition).
The following types exist:
- `/register` or `/reg`: Points to a machine register
- `/variable` or `/var`: Points to a local variable used within a commands or functions implementation
- `/immediate` or `/imm`: An immediate, i.e. constant, numeric value given directly in the invocation.
- `/label`: a label that points to a specific location in the user language program. This is a constant name given directly in the invocation.

Technically, `/register` and `/variable` types are treated the same; they can be used interchangeably.

Every type except `/label` is followed by a length definition, which is `''` followed by a number; this defines how many bits this parameter's source must have.

> [!TODO]
> - Describe max-length (`''<`). Is there also a min-length?
> - Describe register groups here?
> - Should /string be a type? Or is this a special parameter type only usable with specific built-in functions, i.e. in userland you cannot use this type?

## Defining custom commands
>[!TODO]

### Built-in functions
>[!TODO]
>List and explain.
>- getpc aka get_program_counter
>- setpc aka set_program_counter
>- I have a list somewhere

### Bitwise access
> [!TODO]

## User language syntax
>[!TODO]

## Loading other files
> [!TODO]

