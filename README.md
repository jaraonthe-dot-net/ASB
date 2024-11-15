This is the Assembler Sandbox, or ASB for short.

ASB allows the user to experiment with machine instruction sets in a flexible manner. Machine commands can quickly be implemented and used within the same file. These custom machine commands make up the specific Assembler (or "user language") that the user experiments with. ASB may also be useful as a (limited) machine emulator.

For now, ASB comes with a basic [RISC-V implementation](asb/lib/risc-v/risc-v.asb), which you are free to modify and extend as you wish.

The ASB-specific source code comes in files with `.asb` extension.

See the [documentation](doc/index.md). Check out the [examples](asb/example/hello-world.asb).

Syntax highlighting configuration can be found in the [misc/](misc/README.txt) folder.

# Quick Start

Execute the ASB binary with one of the files in the `asb/example/` folder. E.g.:

```
asb asb/example/hello-world.asb

asb asb/example/basic.asb
```

You may want to use the cli flags `--trace`, `--statistics`, `--registers`, `--memory` (or any combination of them). Each of them provides additional information about the effects of the program run.

Then read into these example files to see what they are doing, and start experimenting.

# System Requirements

* Java 21 or higher

# Build from Source

Alternatively to using the provided release, you can build ASB from [source](https://github.com/jaraonthe-dot-net/ASB/) yourself. The codebase is completely written in Java, the `main()` method can be found in `net.jaraonthe.java.asb.ASB`.

(Alternatively, you can execute `build-release.sh` (on Linux) which will build the `.class` files and a `.jar` file in the `/build` folder.)

# About

Created by Jakob Rathbauer in Summer of 2024.

# License

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <[http://www.gnu.org/licenses/](http://www.gnu.org/licenses/)>.
