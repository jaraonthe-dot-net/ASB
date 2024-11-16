To [Index](index.md) - [Up](Defining%20System%20Properties.md)

# Defining Registers
A register is a single value that is available globally in our virtual system. They emulate hardware registers that most architectures have. A register can be read from and written to at any location in the ASB program.

A register is defined by using the `.register` (or `.reg`) directive like so:

```
.register <name> <length>
```

Where `<name>` is the name of the register, and `<length>` is a [length definition](ASB%20Language.md#length-definition) which sets how many bits the register has (in its simplest form `''` followed by a number). This may be followed by `.group` sub-directives (see [below](#register-groups)).

E.g.:

```
.register x0 ''42
```

This defines register `x0` which has a length of 42 bits.

> [!NOTE]
> For length, the [`max` and `maxu` notation](ASB%20Language.md#length-definition) can also be used (same applies below).

A register retains the value written to it until it is modified again. Every register is initialized with the value `0`.

## Defining Register Aliases
A register alias assigns an alternative name to a register, using the `.register_alias` (or `.reg_alias`) directive:

```
.register_alias <name>, <aliasedName>
```

Where `<name>` is the name of the alias, and `<aliasedName>` is the name of the register to alias to. This may be followed by `.group` sub-directives (see [below](#register-groups)).

E.g.:

```
.register_alias ab, x0
```

This assigns the name `ab` to the existing register `x0`.

The register being aliased must already exist when using this directive.

After this definition, the original register can be accessed with both names. It makes no difference which name is used, the effect is the same (except for [groups](#register-groups)).

> [!NOTE]
> Registers can have more than one alias, so effectively there is no limit of the amount of names a register can have.

## Defining Virtual Registers
A virtual register looks like a regular register to the outside, but what happens when reading from or writing to such a register is controlled by a getter and a setter function implemented by the user.

Use the `.virtual_register` directive (or one of its alternatives) with the sub-directives `.get`, `.set`, and optionally `.store` (plus`.group` sub-directives, see [below](#register-groups)):

```
.virtual_register <name> <length> .get {...} .set {...}
```

Where `<name>` is the name of the virtual register, and `<length>` is a [length definition](ASB%20Language.md#length-definition) which sets how many bits the register has (in its simplest form `''` followed by a number).

All sub-directives can be given in any order. Additionally, the entire directive can be given on multiple lines like so:

```
.virtual register <name> <length> {
    .get {
        ...
    }
    .set {
        ...
    }
}
```

Alternatives for `.virtual_register` are: `.virtual_reg`, `.virt_register`, `.virt_reg`, `.register_virtual`, `.register_virt`, `.reg_virtual`, or `.register_virtual`.

The `.get` and `.set` are used to provide a getter and setter implementation, respectively. Both sub-directives are followed by an implementation block akin to a [command implementation](Implementing%20Custom%20Commands.md).

When reading from or writing to a virtual register, its respective getter or setter implementation are invoked. The following parameters are implicitly available in these implementations:

- `out` is available in the getter; it shall contain the value read from the register. This variable is initialized to `0` (i.e. if it is not modified in the getter, then the result of reading from the virtual register is `0`).
- `in` is available in the setter; this is the value that shall be written to the virtual register (which of course you are not required to do).

Both `out` and `in` have the length the virtual register is defined with.

Additionally, the optional `.store` sub-directive configures internal storage for this virtual register:

```
.store <length>
```

Where `<length>` is a [length definition](ASB%20Language.md#length-definition) that defines the amount of bits stored herein.

If internal storage is defined, then the `store` variable is available within the getter and setter implementations, and can be used as you wish.

> [!NOTE]
> The virtual register length and storage length may be different.

Note that, in order to use internal storage within a getter or setter, the `.store` sub-directive MUST be used BEFORE the respective `.get` or `.set` sub-directive (otherwise the `store` is not visible in that implementation).

Example:

```
.virtual_register reverse ''16 {
    .store ''16
    .get {
        &mov out, store'0:15
    }
    .set {
        &mov store, in
    }
}
```

Which effectively provides the stored value in reverse order (of bits) (using [bitwise access](Implementing%20Custom%20Commands.md#bitwise-access)). Visibly, this is a register called `reverse` with a length of 16 bits.

## Register Groups
One or more groups can be assigned to any register described above, and to [local variables](Implementing%20Custom%20Commands.md#local-variables) too (below, "register" refers to local variables as well).

A group is identified by its name - every register that has the same group name assigned is considered to be part of the same group; a register can be part of several groups.

To assign a group to a register, use the `.group` sub-directive as part of the register definition:

```
.group <groupName>
```

Where `<groupName>` is the name of the group. This (optional) sub-directive can be used several times within a register definition, effectively making that register part of several groups.

Groups may be used in [custom commands](Implementing%20Custom%20Commands.md#parameter-group) to control which registers are allowed as arguments.

> [!NOTE]
> This is not the only mechanism to control which registers can be used with a particular command; the register length must fit as well.  
> Thus, in many situations it is not necessary to use groups at all.
