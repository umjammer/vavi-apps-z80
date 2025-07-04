[![Release](https://jitpack.io/v/umjammer/vavi-apps-z80.svg)](https://jitpack.io/#umjammer/vavi-apps-z80)
[![Java CI](https://github.com/umjammer/vavi-apps-z80/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-apps-z80/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-apps-z80/actions/workflows/codeql.yml/badge.svg)](https://github.com/umjammer/vavi-apps-z80/actions/workflows/codeql.yml)
![Java](https://img.shields.io/badge/Java-17-b07219)

# vavi-apps-z80

<img alt="zup logo" src="https://github.com/user-attachments/assets/9acfe8ba-d3b7-4434-9181-718bbed00711" width="160" />

### What is this?

vavi-apps-z80 is a Z80 processor simulator that can be used as the core component
for developing computer emulators (see for example [NestorMSX](https://bitbucket.org/konamiman/nestormsx)),
or to exercise pieces of Z80 code in custom test code. It is written in Java targetting the JVM version 17.

If you like vavi-apps-z80 you may want to take a look at [ZWatcher](https://github.com/Konamiman/ZWatcher) too.

vavi-apps-z80 is a fork of [Z80dotNet](https://github.com/Konamiman/Z80dotNet)

## Install

 * [maven](https://jitpack.io/#umjammer/vavi-apps-z80)

## Usage

### Hello, world!

```java
    var z80 = new Z80ProcessorImpl();
    z80.setAutoStopOnRetWithStackEmpty(true);

    var program = new byte[] {
      0x3E, 0x07,        // LD A,7
      (byte) 0xC6, 0x04, // ADD A,4
      0x3C,              // INC A
      (byte) 0xC9        // RET
    };
    z80.getMemory().setContents(0, program);

    z80.start(null);

    assert z80.getRegisters().getA() == 12;
    assert z80.getTStatesElapsedSinceStart() == 28;
```

### How to use

For your convenience, you can add Z80.NET to your
project [as a maven package](https://jitpack.io/#umjammer/vavi-apps-z80) if you want. In that case you may want to take
a look at the [release notes](docs/ReleaseNotes.txt).

1. Create an instance of [the Z80Processor class](src/main/java/konamiman/z80/Z80ProcessorImpl.cs).
2. Optionally, plug your own implementations of one or more of the [dependencies](docs/Dependencies.md).
3. [Configure your instance](docs/Configuration.md) as appropriate.
4. Optionally, register one or more [interrupt sources](docs/Interrupts.md), and capture the related events if you need to.
5. Optionally, capture [the memory access events](docs/MemoryAccessFlow.md)
   and/or [the instruction execution events](docs/InstructionExecutionFlow.md).
6. [Start the simulated processor execution](docs/HowExecutionWorks.md) by using one of the execution control methods.
7. Execution will stop (and the execution method invoked will then return) when one
   of [the execution stop conditions is met](docs/StopConditions.md). You can then
   check [the processor state](docs/State.md) and, if desired, resume execution.

Execution is completely synchronous: one single thread is used for everything, including firing events. As seen in the
Hello World example, you just invoke one of the starting methods and wait until it returns (there are means to force
this to happen, see [the execution stop conditions](docs/StopConditions.md)). If you want some kind of multithreading,
you'll have to implement it by yourself, I just tried to keep things simple. :-)

Interaction of the processor with the hosting code and the outside world (memory and ports) can be achieved by handling
the class events, by plugging custom implementations of the dependencies, or both at the same time. Interrupts can be
generated by using [interrupt sources](docs/Interrupts.md).

### Compatibility

vavi-apps-z80 implements all the documented Z80 behavior, plus all the undocumented instructions and flag effects as
per [The undocumented Z80 documented](http://www.myquest.nl/z80undocumented/) except for the following:

* The bit 3 and 5 flags are not modified by the BIT instruction
* The H, C and P/V flags are not modified by the INI, INIR, IND, INDR, OUTI, OTIR, OUTD and OTDR instructions

The processor class passes [the ZEXDOC test](https://github.com/KnightOS/z80e/blob/master/gpl/zexdoc.src) fully,
and [the ZEXALL test](https://github.com/KnightOS/z80e/blob/master/gpl/zexall.src) fully except for the BIT instruction.
You can try these tests yourself by running [the ZexallTest project](src/test/java/zexalltest/Program.java).

vavi-apps-z80 implements support for 16 bit port numbers, but it must be manually enabled.
See [the configuration documentation](docs/Configuration.md#the-extended-ports-space) for the details.

### Samples

 * [console](src/test/java/zexalltest/ConsoleTest.java)
 * [CP/M](src/test/java/zexalltest/CPMLoadTest.java)

## References

* https://github.com/jsanchezv/Z80Core
* https://github.com/codesqueak/Z80Processor

### Current Score (#0052fce)

 https://gist.github.com/umjammer/ea319aaa7b1ecf10a19b3ade2fd7187b

### Resources

The following resources have been used to develop this project:

* [Z80 official user manual](http://www.zilog.com/manage_directlink.php?filepath=docs/z80/um0080)
* [The undocumented Z80 documented](http://www.myquest.nl/z80undocumented/) by Sean Young.
* [Z80 instructions table](http://clrhome.org/table/) at [ClrHome.org](http://clrhome.org)
* [Z80 technical reference](http://www.worldofspectrum.org/faq/reference/z80reference.htm)
  at [WorldOfSpectrum.org](http://www.worldofspectrum.org)
* [Complete Z80 instruction set](http://www.ticalc.org/archives/files/fileinfo/195/19571.html)
  from [ticalc.org](http://www.ticalc.org). The instruction tables in the code are based on [the code](https://github.com/Konamiman/Z80dotNet/tree/master/Main/Instructions%20Execution/Core)
  automatically generated from a modified version of this file.

## TODO

 * too slow, [x100 slower](https://gist.github.com/umjammer/ea319aaa7b1ecf10a19b3ade2fd7187b) compare to [my z80](https://github.com/umjammer/vavi-apps-emu88/blob/master/src/main/java/vavi/apps/em88/Z80.java)
 * ⚠️ unit tests have [random fixture problems](https://github.com/umjammer/vavi-apps-z80/pull/9#issuecomment-2947711686), if it would be failed, rerun.
 * ~~git tree might be corrupted~~ fixed
 * ~~catch up with upstream update~~
 * remove dotnet4j dependency

---

<sub>image [z80](https://jp.pinterest.com/pin/400820435596646802/)</sub>