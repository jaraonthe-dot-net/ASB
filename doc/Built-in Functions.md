To [Index](index.md)

# Built-in Functions
Built-in Functions are commands that are shipped with ASB. They are used to implement your custom commands.

Functions can only be used within command implementations, with the exception of [special functions](#special-functions) `&assert` and `&print*`, which you can use anywhere.

---

## Moving Data
### `&mov`
Moves data between registers, memory cells, and from immediates.

**Registers**:

```
&mov dstRegister, imm
&mov dstRegister, srcRegister
```

Where `srcRegister` and `dstRegister` are registers or local variables, and `imm` is an immediate.

`&mov` reads a value from the second parameter and writes it to the first. If two registers are used, they must have the same length. If the second parameter is an immediate, that immediate must fit into the length of the first parameter.

**Memory**:

```
&mov @dstAddress, imm
&mov @dstAddress, @srcAddress
&mov @dstAddress, srcRegister
&mov dstRegister, @srcAddress
```

Where `srcAddress`, `dstAddress`, `srcRegister`, and `dstRegister` are registers or local variables, and `imm` is an immediate.

`@` marks a memory address, i.e. a value read from the register or local variable given after it is used as a memory address. The length of these variables must be the same as the configured memory address length.

`&mov` reads a value from the second parameter (or the memory location its value points to) and writes it to the first (or the memory location its value points to). The lengths of `srcRegister` and `dstRegister` must be the same as the configured memory word length, and `imm` must fit into the memory word length.

### `&movif`

```
&movif a == b, dst, src
&movif a >  b, dst, src
&movif a >= b, dst, src
&movif a <  b, dst, src
&movif a <= b, dst, src
&movif a != b, dst, src
```

Where `a` and `b` are registers or local variables, and up to one can be an immediate.

Where `dst` and `src` are parameters as accepted by [`&mov`](#mov), i.e. they can be `@address` (`address` being a register or local variable of which the value is used as a memory address) or a register or local variable. `src` may additionally be an immediate.

`&movif` compares `a` and `b` according to the given operator and if the comparison is true carries out a `&mov` from `src` to `dst`.

Length requirements of `&mov` apply to `src` and `dst`.

### `&sign_extend`

```
&sign_extend dstRegister, srcRegister
```

Where `srcRegister` and `dstRegister` are registers or local variables. The length of `srcRegister` must not be greater than the length of `dstRegister`.

`&sign_extend` reads the value from `srcRegister`, sign-extends it to fit the length of `dstRegister` and writes it to the latter.

In sign-extending, a value of a shorter length is extended to a longer length by filling the extraneous high bits either with 0s or 1s. If the MSB of the original value is 0 then 0 is taken to fill, if it is 1 then 1 is taken to fill. This means that negative numbers stored in two's-complement remain negative in the longer length.

### `&zero_extend`

```
&zero_extend dstRegister, srcRegister
```

Where `srcRegister` and `dstRegister` are registers or local variables. The length of `srcRegister` must not be greater than the length of `dstRegister`.

`&zero_extend` reads the value from `srcRegister`, extends it with 0s to fit the length of `dstRegister` and writes it to the latter.

### `&length`

```
&length dstRegister, srcRegister
```

Where `srcRegister` and `dstRegister` are registers or local variables.

`&length` writes the bit length of `srcRegister` to `dstRegister`. `dstRegister` must be big enough to fit that value.

This function can be useful when using dynamic length, e.g. to initialize a local variable with the same length as a parameter:

```
.define dynamic_operation /register reg ''<=16 {
    .variable l ''6
    &length l, reg

    // tmp has the same length as reg
    .variable tmp ''l

    &add l, l, l
    // tmp2 has double the length of reg
    .variable tmp2 ''l

    // ...
}
.register a8  ''8
.register b16 ''16

// command can be called with registers of different bit length
dynamic_operation a8
dynamic_operation b16
```

### `&normalize`

```
&normalize variable
```

Where `variable` is a register or local variable. It makes most sense to be an `/immediate` parameter, though.

`&normalize` ensures the given parameter contains a value that is not negative. If operating on a parameter which has a negative immediate as value, that value is transformed into a two's-complement encoding.

Immediates may be given as negative numbers, in which case they are treated as negative numbers (and stored as such in the `/immediate` parameter), as opposed to any other numeric variable where ASB does not assume anything about their binary encoding.

This is done because the length of an immediate is not explicitly given - it is possible to implement very powerful commands that support parameters with very big length (up to 8192 bits). Immediately encoding a negative immediate in two's-complement could lead to unwieldy long values.

Note that `&mov`ing an `/immediate` parameter's value to any other register, local variable, or parameter that is not an `/immediate` type will automatically transform a negative value to a two's-complement-encoded number with the length of the destination variable.

Further note that a negative immediate may be passed along to a `/register` parameter when using an `/immediate` parameter in an invocation:

```
.define firstCommand /immediate imm''8 {
	secondCommand imm
}
.define secondCommand /register reg''8 {
	// reg is still -5
	&normalize reg
	// now reg is 0xFB (251)
}
firstCommand -5

```

An example where `&normalize` is required:

```
.define test /immediate len ''maxu500 {
    &normalize len
    &assert len <= 500, "unsupported length"
    
    .variable test ''len
    // ...
}
```

The `len` parameter is used to configure the length of a local variable. `&assert` is able to handle negative immediates as you would expect (i.e. as two's-complement) and thus allow e.g. `-50` (aka `462` in two's complement (9 bits length)) only for the local variable initialisation to fail because lengths must not be negative. `&normalize` solves this conundrum.

---

## Arithmetic Operations
### `&add`

```
&add dstRegister, src1Register, src2Imm
&add dstRegister, src1Register, src2Register
```

Where `src1Register`, `src2Register`, and `dstRegister` are registers or local variables, and `src2Imm` is an immediate.

`&add` adds the two source operands and writes the result to `dstRegister`. Any overflow or carry-out is ignored.

All parameters must have the same length. If the third parameter is an immediate, that immediate must fit into the length of the other parameters.

### `&addc`

Similar to [`&add`](#add), but provides the carry-out bit.

```
&addc dstRegister, src1Register, src2Imm
&addc dstRegister, src1Register, src2Register
```

Where `src1Register`, `src2Register`, and `dstRegister` are registers or local variables, and `src2Imm` is an immediate.

`&addc` adds the two source operands and writes the result to `dstRegister`, which is one bit longer than the sources; its MSB receives the carry-out bit of the add operation.

The source parameters must have the same length, and the destination parameter must be one bit longer. If the third parameter is an immediate, that immediate must fit into the length of the second parameter.

### `&sub`

```
&sub dstRegister, src1Register, src2Imm
&sub dstRegister, src1Register, src2Register
&sub dstRegister, src1Imm,      src2Register
```

Where `src1Register`, `src2Register`, and `dstRegister` are registers or local variables, and `src1Imm` and `src2Imm` are immediates.

`&sub` subtracts the last operand from the second operand and writes the result to `dstRegister`. Any overflow or carry-out is ignored.

All parameters must have the same length. If one of the source parameters is an immediate, that immediate must fit into the length of the other parameters.

### `&subc`

```
&subc dstRegister, src1Register, src2Imm
&subc dstRegister, src1Register, src2Register
&subc dstRegister, src1Imm,      src2Register
```

Where `src1Register`, `src2Register`, and `dstRegister` are registers or local variables, and `src1Imm` and `src2Imm` are immediates.

`&sub` subtracts the last operand from the second operand and writes the result to `dstRegister`, which is one bit longer than the sources; its MSB receives the carry-out bit of the subtract operation.

The source parameters must have the same length, and the destination parameter must be one bit longer. If one of the source parameters is an immediate, that immediate must fit into the length of the other source parameter.

### `&mul`
```
&mul dstRegister, src1Register, src2Imm
&mul dstRegister, src1Register, src2Register
```

Where `src1Register`, `src2Register`, and `dstRegister` are registers or local variables, and `src2Imm` is an immediate.

`&mul` multiplies the two source operands and writes the result to `dstRegister`, which has double the length of the sources; thus no information is lost.

The source parameters must have the same length, and the destination parameter must have double the length of the sources. If the third parameter is an immediate, that immediate must fit into the length of the second parameter.

Note that `&mul` assumes that the source values are positive integers. If you want to multiply values that may be negative, you need to sign-extend them, like so (see [Wikipedia](https://en.wikipedia.org/wiki/Two%27s_complement#Multiplication) for the technical background):

```
// Multiplication supporting positive and negative 32-bit numbers
.define mul /register rd''64, /register rs1''32, /register rs2''32 {
    // Sign-extend to double length before multiplying
    .variable ext1''64; &sign_extend ext1, rs1
    .variable ext2''64; &sign_extend ext2, rs2

    // Result still needs to be double the input
    .variable result''128
    &mul result, rs1, rs2
    // Using only the lower half of the result
    &mov rd, result'63:0
}
```

### `&div`

```
&div dstRegister, src1Register, src2Imm
&div dstRegister, src1Register, src2Register
&div dstRegister, src1Imm,      src2Register
```

Where `src1Register`, `src2Register`, and `dstRegister` are registers or local variables, and `src1Imm` and `src2Imm` are immediates.

`&div` divides the second operand by the third operand and writes the result to `dstRegister`, rounding towards zero.

All parameters must have the same length. If one of the source parameters is an immediate, that immediate must fit into the length of the other parameters.

Note that `&div` assumes that the source values are positive integers. If you want to divide values that may be negative, you need to work around that. The following example works with absolute values, effectively rounding towards zero from both positive and negative values (which is common behavior):

```
.define &abs /register dst''32, /register src''32 {
    &mov dst, src
    &jumpif dst'31 == 0, end
    &twos_complement dst
  end:
}
.define &twos_complement /register reg''32 {
    &not reg, reg
    &add reg, reg, 1
}

// Divide
.define div /register rd''32, /register rs1''32, /register rs2''32 {
    .variable result_is_negative''1
    &movif rs1'31 != rs2'31, result_is_negative, 1

    .variable abs1''32; &abs abs1, rs1
    .variable abs2''32; &abs abs2, rs2

    // Divide absolute values
    &div rd, abs1, abs2
    &jumpif result_is_negative == 0, end
    &twos_complement rd
  end:
}
```

### `&rem`

```
&src dstRegister, src1Register, src2Imm
&src dstRegister, src1Register, src2Register
&src dstRegister, src1Imm,      src2Register
```

Where `src1Register`, `src2Register`, and `dstRegister` are registers or local variables, and `src1Imm` and `src2Imm` are immediates.

`&rem` divides the second operand by the third operand and writes the **remainder** to `dstRegister`.

All parameters must have the same length. If one of the source parameters is an immediate, that immediate must fit into the length of the other parameters.

Note that (in accordance with [`&div`](#div)) `&rem` assumes that the source values are positive integers. If you want to work with values that may be negative, you need to work around that. The following example works with absolute values, making the remainder negative if the dividend is negative; this goes hand-in-hand with the `&div` example above (which rounds towards zero):

```
.define &abs /register dst''32, /register src''32 {
    &mov dst, src
    &jumpif dst'31 == 0, end
    &twos_complement dst
  end:
}
.define &twos_complement /register reg''32 {
    &not reg, reg
    &add reg, reg, 1
}

// Remainder
.define rem /register rd''32, /register rs1''32, /register rs2''32 {
    .variable abs1''32; &abs abs1, rs1
    .variable abs2''32; &abs abs2, rs2

    // Work with absolute values
    &rem rd, abs1, abs2

    // remainder is negative if rs1 is negative
    &jumpif rs1'31 == 0, end
    &twos_complement rd
  end:
}
```

---

## Logical Operations
### `&and`

```
&and dstRegister, src1Register, src2Imm
&and dstRegister, src1Register, src2Register
```

Where `src1Register`, `src2Register`, and `dstRegister` are registers or local variables, and `src2Imm` is an immediate.

`&and` performs a bitwise AND operation on the two source operands and writes the result to `dstRegister`.

All parameters must have the same length. If the third parameter is an immediate, that immediate must fit into the length of the other parameters.

### `&or`

```
&or dstRegister, src1Register, src2Imm
&or dstRegister, src1Register, src2Register
```

Where `src1Register`, `src2Register`, and `dstRegister` are registers or local variables, and `src2Imm` is an immediate.

`&or` performs a bitwise OR operation on the two source operands and writes the result to `dstRegister`.

All parameters must have the same length. If the third parameter is an immediate, that immediate must fit into the length of the other parameters.

### `&xor`

```
&xor dstRegister, src1Register, src2Imm
&xor dstRegister, src1Register, src2Register
```

Where `src1Register`, `src2Register`, and `dstRegister` are registers or local variables, and `src2Imm` is an immediate.

`&xor` performs a bitwise XOR operation on the two source operands and writes the result to `dstRegister`.

All parameters must have the same length. If the third parameter is an immediate, that immediate must fit into the length of the other parameters.

### `&not`

```
&not dstRegister, srcImm
&not dstRegister, srcRegister
```

Where `srcRegister` and `dstRegister` are registers or local variables, and `srcImm` is an immediate.

`&not` performs a bitwise NOT operation on the source operand and writes the result to `dstRegister`.

All parameters must have the same length. If the last parameter is an immediate, that immediate must fit into the length of the first parameter.

---

## Manipulating the Program Counter
The program counter tracks which instruction in the user program is currently being executed. With the functions below the program counter can be manipulated and thus jumps be implemented.

### `&get_program_counter` (aka `&get_pc`)

```
&get_program_counter dstRegister
&get_pc              dstRegister    // Alias
```

Where `dstRegister` is a register or local variable.

`&get_program_counter` writes the current value of the program counter to `dstRegister`. The latter's length must be the same as the configured program counter length (see [`&get_program_counter_length`](#get_program_counter_length-aka-get_pc_length)).

Note that the program counter counts every instruction (in the user program) as an increment of 1. A label's value is calculated in the same way.

### `&set_program_counter` (aka `&set_pc`)

```
&set_program_counter srcRegister
&set_pc              srcRegister    // Alias
```

Where `srcRegister` is a register or local variable.

`&src_program_counter` overwrites the program counter with the value from `srcRegister`. The latter's length must be the same as the configured program counter length (see [`&get_program_counter_length`](#get_program_counter_length-aka-get_pc_length)).

This is the way in which to implement jumps. A `/label` parameter's value can be directly used with this function (but not a label name, though):

```
.define jump_to /label label {
    &set_program_counter label
}
jump_to end
// this is skipped
end:
```

As a `/label` parameter behaves the same way as a local variable, it is also possible to modify its value before overwriting the program counter with it (or storing it in a register, e.g. to implement a function call).

If the program counter is set to a value that doesn't point to an instruction (because it is bigger than the user program is long) execution halts.

Note that the program counter counts every instruction (in the user program) as an increment of 1. A label's value is calculated in the same way.

---

## Program Flow within Implementation
While controlling the program flow of the user program is done via [manipulating the program counter](#manipulating-the-program-counter), in order to jump around within a custom command's implementation, the following functions can be used.

Note that these can NOT be used to jump within the user program.

### `&jump`

```
&jump label
```

Where `label` is a local label name.

`&jump` executes a jump to the given `label`, so that the instruction following the label definition is executed next.

### `&jumpif`

```
&jumpif a == b, label
&jumpif a >  b, label
&jumpif a >= b, label
&jumpif a <  b, label
&jumpif a <= b, label
&jumpif a != b, label
```

Where `a` and `b` are registers or local variables, and up to one can be an immediate.

Where `label` is a local label name.

`&jumpif` compares `a` and `b` according to the given operator and if the comparison is true executes a jump to the given `label`, so that the instruction following the label definition is executed next. Otherwise the program flow continues normally.

---

## System Info
### `&get_memory_word_length`

```
&get_memory_word_length dstRegister
```

Where `dstRegister` is a register or local variable.

`&get_memory_word_length` writes the configured memory word length to `dstRegister`. `dstRegister` must be big enough to fit that value.

The memory word length defines how many bits every single memory cell has.

### `&get_memory_address_length` (aka `&get_memory_addr_length`)

```
&get_memory_address_length dstRegister
&get_memory_addr_length    dstRegister    // Alias
```

Where `dstRegister` is a register or local variable.

`&get_memory_address_length` writes the configured memory address length to `dstRegister`. `dstRegister` must be big enough to fit that value.

The memory address length defines how many bits are used to address memory, and thus how many memory cells there are.

### `&get_program_counter_length` (aka `&get_pc_length`)

```
&get_program_counter_length dstRegister
&get_pc_length              dstRegister    // Alias
```

Where `dstRegister` is a register or local variable.

`&get_program_counter` writes the configured length of the program counter to `dstRegister`. `dstRegister` must be big enough to fit that value.

---

## Special Functions
These are the only built-in functions that can be invoked in the user language as well.

### `&print`, `&println`

```
&print register
&print immediate
&print @addressReg
&print @addressImm
&print string

&println register
&println immediate
&println @addressReg
&println @addressImm
&println string

&println
```

Where `register` and `addressReg` are registers or local variables, `immediate` and `addressImm` are immediates, and `string` is a string literal (enclosed in quotation marks `"`).

`&print` prints the given parameter's value. In case of parameter's preceded with `@`, the values are used as memory addresses, and the value of the addressed memory cell is printed. Numeric values are printed in decimal format.

`&println` behaves the same, but additionally prints a Newline character after the output. `&println` can also be used without a parameter, in which case only a Newline character is printed.

Note that immediates may be given as a negative number, in which case they will also be printed as such. The same applies to parameter's which have an immediate as value (which may have been passed along via several invocations). See [`&normalize`](#normalize) for more information about this phenomenon.

### `&print_*`, `&println_*`

```
&print_s register       // signed
&print_s @addressReg
&print_s @addressImm

&print_x register       // hex
&print_x @addressReg
&print_x @addressImm

&print_o register       // octal
&print_o @addressReg
&print_o @addressImm

&print_b register       // binary
&print_b @addressReg
&print_b @addressImm

&println_s register
&println_s @addressReg
&println_s @addressImm

&println_x register
&println_x @addressReg
&println_x @addressImm

&println_o register
&println_o @addressReg
&println_o @addressImm

&println_b register
&println_b @addressReg
&println_b @addressImm
```

Where `register` and `addressReg` are registers and `addressImm` is an immediate.

These functions print the given numeric value in a specific format. In case of parameter's preceded with `@`, the values are used as memory addresses, and the value of the addressed memory cell is printed.

- `*_s` interpretes the given value as two's-complement number, printing it as a decimal number with prepended minus sign if it is negative.
- `*_x` prints the given value as a hexadecimal number, prepended with "0x".
- `*_o` prints the given value as an octal number, prepended with "0".
- `*_b` prints the given value as a binary number, prepended with "0b".

`&println_*` additionally prints a Newline character after the output.

### `&assert`

```
&assert a == b
&assert a >  b
&assert a >= b
&assert a <  b
&assert a <= b
&assert a != b

&assert a == b, message
&assert a >  b, message
&assert a >= b, message
&assert a <  b, message
&assert a <= b, message
&assert a != b, message
```

Where `a` and `b` are registers or local variables, and up to one can be an immediate; and `message` is a string literal (enclosed in quotation marks `"`).

`&assert` compares `a` and `b` according to the given operator and if the comparison is true the program continues normally; but if the comparison fails the program halts with an error.

Optionally, a custom error `message` can be given which will be displayed in case the comparison fails.
