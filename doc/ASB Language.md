To [Index](index.md)

# ASB Language
The ASB Language is wherein custom commands and system properties are defined and then used as desired.

- **Meta language** refers to constructs that are already provided by ASB, which are required to define and implement the user language.
- The **user language** is the effective language that the user creates by utilizing ASB. This comprises **custom commands** and system properties.
- **(Custom) commands** are the main part of the user language, which correspond to the assembler instruction set of an architecture that the user wishes to emulate or design.
- **Functions** are similar to custom commands, but they can only be used in specific places. **Built-in functions** come as part of the ASB meta language, which are used to implement custom commands.
- **User (language) program** is the effective program encoded using the user language. It comprises invocations of custom commands.
- **ASB program** is everything written in the ASB language by the user. It comprises the user program as well as the definition of the user language.

Both meta and user language are mixed within ASB language source code files. Basically, once a custom command is defined, it can be used immediately.

---

## Basic format
An ASB program consists of one or more source files, which have the file ending `.asb`. The expected encoding is 8-bit **ASCII**. The behavior of codepoints outside of ASCII (e.g. ISO 8859, UTF-8) is undefined.

ASB is generally case-insensitive (except for strings).

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

In any of the formats, additional `_` characters may be used to visually subdivide the number further. It has no effect on the value of the given number. This character cannot be used at the beginning of the number, though.

- **Decimal** format: Any of the decimal digits, but the first digit must not be `0` (unless the entire number is zero). May be prefixed with `-`, which makes it a negative number in Two's complement encoding.  
  Regex: `-?[1-9][_0-9]*`
  
> [!NOTE]
> Some places do not allow a negative number to be used, e.g. when giving a length or bit position.
  
- **Hexadecimal** format: Any of the decimal digits and `a`- `f` (also uppercase); prefixed by `0x` (or `0X`).  
  Regex: `0(x|X)_?[0-9A-Fa-f][_0-9A-Fa-f]*`
- **Octal** format: Any of the decimal digits up to `7`, prefixed by `0`.  
  Regex: `0[_0-7]*`
- **Binary** format: The binary digits `0` and `1`, prefixed by `0b` (or `0B`).  
  Regex: `0(b|B)_?[01][_01]*`

### Names
Command, Function, and Register names contain letters, digits, the `_` and the `.` character, but the first character must not be a `.` nor a digit.  
Regex: `[A-Za-z_][A-Za-z0-9_.]*`

> [!NOTE]
> ASB is case-insensitive, and this applies to names too. I.e. `abc` and `ABC` are the same name.

### Label Names
Label names are more permissive than [names](#names) of other entities. They also contain letters, digits, the `_` and the `.` character, but don't have extra rules for the first character.  
Regex: `[A-Za-z0-9_.]+`

> [!NOTE]
> Label names are ambiguous, as they may not be distinguishable from a register name or immediate number (in a command invocation). ASB determines from context what is meant. There can be edge-cases (i.e. when overloading a command with different parameter types) where a register or immediate may be preferred over a label name.  
> You are safe if your label names do not look like numbers and are not the same as register names.

> [!NOTE]
> ASB is case-insensitive, and this applies to label names too. I.e. `xyz` and `XyZ` refer to the same label.

---

## Common syntax

These are syntactical elements that can appear both in the meta and the user language.

- Line comments start with `#` or `//` (they end **right before** the next New Line).
- Multi-line comments start with `/*` and end with `*/`. If the start and end markers are not on the same line the comment is treated like a single New Line (so that different statements are separated as intended).
- [Label names](#label-names) directly followed (without whitespace) by `:` are a label marking this location with this name.
- Command Symbols can be used to make a command look more interesting. They are part of the command's identity. They comprise the following characters: ``!$%&()*+,/<=>?@[]^`{|}~``
	- When [defining a command](Implementing%20Custom%20Commands.md), some of these symbols have a different meaning. To use them as a command symbol here, you must escape them by prefixing them with a `\` character.

---

## Meta language syntax
- All meta language operations are done via **ASB directives** which start with a `.` followed by a keyword.
- Some directives have **sub-directives**, which also start with a keyword prefixed with a `.`. These sub-directives can either be given on the same line as the directive they belong to, or the directive is extended to multiple lines by encasing all its sub-directives in one pair of curly braces (`{}`).
    - The `}` closing brace must be followed by a Newline or `;` character (to properly separate it from the next item).
- Some directives or sub-directives contain an **implementation body** which is also encased in curly braces and can span multiple lines as well.
- **Functions** are prefixed with an `&`. They can only be called inside implementation bodies (except where noted otherwise).

> [!NOTE]
> ASB comes with pre-defined, [built-in functions](Built-in%20Functions.md), but the user can also add their own (the same restriction regarding invocation applies).

- Command and function parameters have a type. They are prefixed with a `/` (in the command or function definition).
- **Bitwise Access**: Individual bits of registers or variables are accessed by giving a single bits number or a range of bits where two bit numbers are separated by `:`, in both cases prefixed with a single `'`.
    - Instead of a number literal a variable name can be given, in which case the variable's current value is used to determine the bit position accessed.
    - Bit positions start at `0` for the least-significant bit.

> [!NOTE]
> It is not possible to access individual bits within the user language in this way - if such a mechanism is desired, it must be implemented with custom commands.

### Length definition

Register, local variable, and parameter definitions (except `/label`) are followed by a length definition, which defines how big the variable is.

A length is given in amount of bits, prefixed with `''`. The amount of bits can either be given as a number or in the `max <number>` or `maxu <number>` notation:

- `max <number>`: Length is calculated to fit the given positive number and the negative of that number as well (i.e. supporting two's-complement).
- `maxu <number>`: Length is calculated to fit the given positive number (i.e. unsigned).

Example:

```
.define my_command /register first ''max 13, /register second ''maxu 13 {
    // first  has 5 bits to fit 13 and -13
    // second has 4 bits to fit 13 but not -13
}
```

The effective length is calculated at compile time.

Additionally, for a local variable another variable can be used as the length value; in this case the local variable's length is determined at runtime.

The length of any entity must be at least `1` and at most `8192` bits (aka 1kiB).

Additionally, the amount of bits for `/register` or `/variable` parameters can be given as a range of supported lengths by using the `<= <amount>`, `>= <amount>`, or `<amount>..<amount>` notation:

- `<= <amount>`: The length of the argument must not exceed the given amount.
- `>= <amount>`: The length of the argument must be at least the given amount.
- `<amount>..<amount>`: The length of the argument must be within the range given here (inclusive).

Amount can be given as a number or in the `max <number>` or `maxu <number>` notation.
