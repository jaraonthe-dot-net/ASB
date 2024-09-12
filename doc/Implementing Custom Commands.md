To [Index](index.md)

# Implementing Custom Commands
Custom commands are what makes up the user language. They represent the machine instruction set the user wishes to emulate.

A custom command is defined using the `.define` (or `.def`) directive:

```
.define <name> <parameters & command symbols> {
    ... // implementation
}
```

Where `<name>` is the command name, which is followed by a list of parameters and command symbols and finally the command implementation encased in `{ }`.

If the command name is prefixed with `&`, then what you are defining is a **function**. Functions have the same properties and behave the same way as commands, except that they cannot be used in the user program - i.e. they can only be invoked within implementations (of commands, functions, or [virtual register](Defining%20Registers.md#defining-virtual-registers) getters and setters).

A command's name can be followed by a list of parameters and command symbols. The order of these items is relevant.

## Parameters
Parameters are values that the command implementation can operate on. They behave like pointers, i.e. modifying a parameter's value modifies the value of the variable used as argument in the command invocation.

A parameter is defined by providing its type followed by the parameter name and its [length](ASB%20Language.md#length-definition). E.g.:

```
/register a ''32
/immediate imm ''10
/label myLabel
```

The following types exist:

- `/register`, `/reg`, `/variable`, or `/var`: Points to a machine register or a local variable used within a command or function implementation
- `/immediate` or `/imm`: An immediate, i.e. constant, literal numeric value given directly in the invocation.
- `/label`: a label that points to a specific location in the user language program. This is a constant literal name given directly in the invocation.
    - Note that this type does not have a length definition - its length is the same as the [program counter length](Defining%20System%20Properties.md#defining-the-program-counter).
    - A `/label` parameter behaves like a local variable and can be used like one (incl. [bitwise access](#bitwise-access), etc.)

Additionally, there is a string type that can only be used by built-in functions. All you need to know is that the argument for such a type is a string literal enclosed in quotation marks (`"`).

Note that the `/register` aka `/variable` type can take a [length range](ASB%20Language.md#length-definition), in which case that parameter accepts variables of different length. There are ways to handle such complexity, e.g.:

```
.define my_command /register a ''<=16 { // "a" accepts any variable up to length 16
    .variable length ''5
    &length length, a // Using &length
    // => local variable "length" states how long the "a" argument is...

    // ... and that length is used for local variable "local" ...
    .variable local ''length
    // ... which now has the same length as "a"
    // ...
}
```

### Parameter group
`/register` parameters can be defined with a group, by using the `.group` sub-directive in the parameter definition, i.e.:

```
/register <name> <length> .group <groupName>
```

A parameter can have **up to one** group. If a group has been assigned, then only [registers that belong to that group](Defining%20Registers.md#register-groups) can be used as argument for that parameter.

> [!NOTE]
> As [local variables](#local-variables) cannot have a group assigned, it is impossible to invoke a command using a parameter group with a local variable as argument.

Example:

```
.define cmd /register ''64 .group gg { ... }

.register gg1    ''64 .group gg
.register gg2    ''64 .group other_group .group gg
.register gg32   ''32 .group gg
.register not_gg ''64

cmd gg1    // Works
cmd gg2    // Works
cmd gg32   // Doesnt work - wrong length!
cmd not_gg // Doesn't work
```

## Command Symbols
Command symbols are used to make a command look more interesting. They comprise the following characters:

```
!$%&()*+,/<=>?@[]^`{|}~
```

A command name can not just be followed by the command parameters, but by these command symbols as well. They may be used in any way you wish (e.g. separating two parameters with a `,`).

When invoking a command all the parameters as well as the command symbols that the command has been defined with **must be present in the invocation**.

Example:

```
// Note the '==' command symbols
.define is /register a''>=1 == /register b''>=1 {
    &jumpif a != b, end
    &println "are same!" // print this if a and b's values are equal
  end:
}
is x0 == x1 // Correct invocation
is x0 x1    // Invalid invocation

// This is a different command (because the command symbol is different)
// Note the '>'
.define is /register a''>=1 > /register b''>1 {
    &jumpif a <= b, end
    &println "a is greater!" // print this if a's value is bigger than b's
  end:
}
is x0 > x1 // Correct
is x0 x1   // Now which command is that supposed to be?

```

Whitespace is ignored except where required (i.e. usually between two parameters/arguments that are not separated by a command symbol).

Note that in the command definition the following command symbols have to be escaped with the `\` character so they are not misunderstood by the compiler:

```
/{}
```

## Command identity
A command's identity is made up of its name, parameters (their types, lengths, groups), and command symbols; and the order in which parameters and command symbols are given. If any of these components differs, then the command has a different identity.

There may be multiple commands **with the same name**, but **no more than one command with the same identity**. ASB uses the command identity to determine which command to invoke.

Example:

```
// First variant
.define test /register r''16 { ... }

// Second variant
.define test /register r''8 { ... }

.register a16 ''16
.register a8  ''8

test a16 // invokes first variant
test a8  // invokes second variant
```

Note that, depending on which commands have been defined, there may be complex situations where resolving which command shall be invoked is a non-trivial matter. ASB employs sophisticated decision procedures (incl. tie-breaker rules) that should usually behave as desired, but nevertheless there can be constellations that cannot be resolved, which lead to a runtime error.

For completeness, here are the rules employed to resolve an invocation to a specific command:

```
Resolving invocation to invoked command procedure:
 - Command symbols must fit
 - Invocation arguments must fit command parameters:
   Here we have some ambiguity, so it is more complex. There may be
   more than one command that fits the arguments, in which case tie-
   breakers are applied to decide which one is picked.
   
 Arguments => PARAMETERS:
 - Immediate Argument => IMMEDIATE; if no command fits => LABEL
   (this ignores any tie-breaker order, the IMMEDIATE is always
   preferred if possible)
 - Register Argument (with bitwise access)        => REGISTER
 - Label Argument (which cannot be anything else) => LABEL
 - Name Argument                                  => REGISTER or LABEL
 - StringArgument                                 => STRING
 
 Parameter constraints that must be satisfied:
 - IMMEDIATE: immediate length <= parameter length
 - REGISTER:
   variable referenced in argument must exist; referenced variable
   must be numeric; if parameter has group, register used in argument
   must have same group; argument's length range must overlap with
   parameter's length range (consider dynamic length -
   Register Argument has a complex calculation for its length range
   that is used here)
 
 Tie-breakers are applied left to right to parameters; first parameter
 that is different between potential commands decides.
 The various tie-breakers (for each type applied in order as given):
 - Immediate Argument (IMMEDIATE):
   => select command with parameter with smaller length
      (Rationale: It's probably more efficient to use this command
      than one that can handle longer immediates)
 - Register Argument:
   => prefer group over non-group param;
   => if both have group: Prefer the one of which the group comes
      first in the register's group list;
   => as we have dynamic length we don't know which command to pick
      and we move on to the next argument (this may lead to an error
      being triggered)
 - Name Argument:
   => prefer REGISTER over LABEL;
   => if REGISTER: apply tie-breakers for Register Argument
```

> [!NOTE]
> As the command name is not the only thing taken into account when resolving which command to invoke, we end up with an overloading mechanism: The same command name may be used several times to do different things or to behave in (slightly) different ways.

---

## Implementation
A command or function implementation consists of a list of statements. Each statement is either a command or function invocation or a local variable definition. Statements are separated either by a Newline character or by `;`.

Within an implementation all commands as well as all functions (incl. [built-in functions](Built-in%20Functions.md)) can be invoked. An invocation starts with the invoked command's name followed by the invocation arguments and command symbols in the order defined by the invoked command or function.

> [!NOTE]
> The `&` prefix in a function's name must be included too in the invocation of that function.

Any register used must already exist before the implementation; but the implementation may invoke commands and functions that are defined later in the ASB program.

### Local variables
Local variables are scoped within the implementation that they are declared in. This makes them different from registers, which are globally scoped. If a command or function with a local variable invokes itself (recursion) then for each invocation there is an independent instance of the local variable only available within that instance of executed implementation.

To define a local variable us the `.variable` or `.var` directive followed by the variable name and [length definition](ASB%20Language.md#length-definition).

The length definition may use the `max` and `maxu` definition and may also use a register or variable instead of a fixed number; in this case that variable's value sets the local variable's length.

Example:

```
.define do_locals /register length ''14 {
    .variable kibs ''4    // 4 bits long
    &mov kibs, length'13:10 // accessing upper 4 bits of "length"
    &print "length is above "; &print kibs; &println "kiBs"

    .variable dynamic ''length    // Controlled by "length"
    // Filling "dynamic" with all 1s
    &mov dynamic, -1
}
```

Local variables, just like any other variable, must exist before it can be used. I.e. a local variable's definition must occur in the implementation code before any reference to this variable.

If a local variable has the same name as a (global) register, then that name refers to the local variable after its definition. Vice-versa, before the local variable's definition that name refers to the register.

A local variable is instantiated once the location of its definition is reached during execution of the implementation. When [jumping within the implementation](#flow-control), this may happen more than once, or never. The compiler does not take potential flow control into account; it simply assumes a local variable exists after its definition. If a jump leads to the definition being skipped you may run into a runtime error.

On the other side, reaching a local variable definition multiple times is not a problem; upon the first time the variable is instantiated and all further times are simply ignored.

### Bitwise access
Within implementations registers, parameters, or local variables can be accessed **bitwise**. That means that instead of reading or writing to the entire variable the operation at hand is done with only a portion of that variable.

To access a variable bitwise, the variable name is followed by `'` and then the number of the bit to access, or two numbers separated by `:` in order to access a range of bits (inclusive).

Bit position starts at `0` for least-significant bit; most significant bit is the highest number (`variable length - 1`). In bit ranges the order of given boundaries matters; bits are accessed in order from the first number to the second. Accessing a variable without bitwise access implicitly behaves like accessing bits in order from MSB to LSB.

Examples:

```
ra '0   // Access least-significant bit of "ra"
ra '7:0 // Access least-significant byte of "ra"
&mov ra'0:7, ra'7:0 // Flips bit order of "ra"'s least-significant byte

.variable tmp ''16
&mov tmp, tmp'0:15 // Flips bit order of entire "tmp"
```

Note that when bitwise-accessing a register with a **group**, that group is still visible despite the bitwise access. E.g.:

```
.define needs_group /register r0 ''64 .group main { ... }
.register m0 ''128 .group main

.define my_implementation {
    needs_group m0'100:37 // Works
    needs_group m0        // Doesn't work - length is wrong!
}
```

### Flow control
To jump around **within** an implementation, use the [`&jump` and `&jumpif` built-in functions](Built-in%20Functions.md#program-flow-within-implementation).

These functions execute jumps to a local label position. Labels are given as a label name followed by `:`.

Example:

```
// Within implementation:
&jump after:
// ... // skipped code
after:
// continue here...
```
