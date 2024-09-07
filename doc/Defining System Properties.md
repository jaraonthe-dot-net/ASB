To [Index](index.md)

# Defining System Properties
The architecture emulated by ASB comprises the following:

- One simple CPU with a single thread of execution, no pipelining, no reordering (i.e. user language commands are executed individually, one after the other, in the order given).
- Data memory of arbitrary (but defined) size.
- Individually defined registers, each with individually defined bit length.
- A Program counter pointing to the current command in the user program.
- Program size is limited: Amount of commands cannot be higher than the maximum unsigned value the program counter length supports or $2^{31} - 1$, whichever is smaller.

---

## Defining memory
Memory size must be defined if any command is used that accesses the data memory. Otherwise this definition is optional.

Memory size is defined by giving the length of memory addresses, and the length of memory words, both in bits. A memory address then points to a single memory word. The full size of addressable and usable memory is thus $2^{addressBits} * wordLength$ bits.

> [!NOTE]
> Your machine must be able to handle the amount of memory required for the user program that you run. ASB does not blindly allocate the full memory size defined here though, only chunks of it as required. So, you can define the memory as big as you like, as long as the user program doesn't use the entire address space.

Use the `.memory` directive with the two sub-directives `.word` and `.address`:

```
.memory .word ''<wordLength> .address ''<addressLength>
```

Instead of `.address` the alternative keyword `.addr` can be used. The sub-directives can be given in either order. Additionally, the entire directive can be given on multiple lines like so:

```
.memory {
    .word ''<wordLength>
    .address ''<addressLength>
}
```

Replace `<wordLength>` with the amount of bits in each memory word, and `<addressLength>` with the amount of bits in a memory address.

Memory size may be defined more than once; the latter definition effectively overriding earlier definitions. However, **after the first user program command** is executed, memory MUST NOT be reconfigured any more; otherwise an error occurs.

Also, if a command is executed that accesses data memory, but memory size has not been configured by this point, an error occurs as well.

---

## Defining the program counter
The program counter points to the next to execute, or currently executed, command in the user program.

It starts at `0`, which points to the first command. Every executed command is worth exactly one step of the program counter, i.e. the program counter is incremented by `1` after every executed command. Jump functions modify the program counter accordingly.

The bit length of the program counter may be configured, but that is optional. If not defined, the program counter length is set to `64` bit.

To define program counter size use the `.program_counter` or the alternative `.pc` directive:

```
.program_counter ''<length>
```

Replace `<length>` with the amount of bits in the program counter.

The length of the program counter restricts the size of the user program; a program with more commands (in its source code) than the (unsigned) value of the program counter can enumerate will lead to an error. Furthermore, for technical reasons, even if the program counter would allow more, the amount of commands cannot exceed $2^{31} - 1$.

Program counter size may be defined more than once; the latter definition effectively overriding earlier definitions. However, **after the first user program command** is executed the program counter size MUST NOT be reconfigured any more; otherwise an error occurs.

---

## Defining registers
>[!TODO]