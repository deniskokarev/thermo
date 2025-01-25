# BLE Thermometer

Zephyr 3.7-compatible

TODO:

- Battery level percentage
- Settings to memorize BT addr

The sensor device serving over GATT:

- Temperature
- Humidity
- Battery level
- Device Info: name, manufacturer, serial, etc

The debug console via Segger RTT interface is disabled by default for power-saving.
To enable set CONFIG_SHELL=y in prj.conf

# Zephyr

The firmware is Zephyr 3.7-compatible

The project doesn't include Zephyr distribution. You should be installing it yourself as per
[Getting Started Guide](https://docs.zephyrproject.org/latest/develop/getting_started/index.html)
It is recommended to use default location and python `venv` method.

Install ARM toll-chain as well.
Recommending [Zephyr SDK](https://docs.zephyrproject.org/latest/develop/toolchains/zephyr_sdk.html).
*(You need only `arm` target to compile this project)*

# Build

If Zephyr was installed at the default location the `zephyr.env` will include it correctly.

Setup environment

```
source ./zephyr.env
alias b="cmake --build build -- -j16"
alias f="cmake --build build --target flash"
```

Rebuild using `b`, flash using `f`

Likewise `west build` and `west flash` will work as well

if needed to remove protection from nRF chip

```
nrfjprog --recover
nrfjprog --eraseall
```

build and flash

```
cmake -B build .
```

or

```
west build
```

flash

```
west flash
```

# Run

### On Prod board

connect via nRF Connect and explore provided GATT characteristics

for debug open /Applications/SEGGER/JLink/JLinkRTTViewer.app

or open an RTT logger (make sure the CONFIG_LOG=y in prj.conf):

```
/Applications/SEGGER/JLink/JLinkRTTLogger -Device NRF52832_XXAA -If SWD -Speed 4000 -RTTChannel 0 /tmp/nrf.log

tail -f /tmp/nrf.log
```

### On Dev Board

Modify prj.conf and select the dev BOARD and SHIELD. Remove `build` dir and re-make the firmware.
Flash using `f` and connect to the device with `picocom`

```
picocom -b 115200 /dev/cu.usbmodem0010503670791
```

exit with ^A^X

# C++ Exceptions

It seems that C++ exceptions just work out of the box. The flash and ram overhead is very minor:
before
```
           FLASH:      138760 B       512 KB     26.47%
             RAM:       37648 B        64 KB     57.45%
```
after:
```
           FLASH:      151360 B       512 KB     28.87%
             RAM:       39088 B        64 KB     59.64%
```

Unhandled exceptions lead to `abort()`, like so:

```
...
abort()
[00:00:00.277,099] <err> os: r0/a1:  0x00000004  r1/a2:  0x00000000  r2/a3:  0x0000000c
[00:00:00.277,130] <err> os: r3/a4:  0x00000004 r12/ip:  0x20009fe8 r14/lr:  0x0000b821
[00:00:00.277,130] <err> os:  xpsr:  0x210b0000
[00:00:00.277,130] <err> os: Faulting instruction address (r15/pc): 0x0000b830
[00:00:00.277,160] <err> os: >>> ZEPHYR FATAL ERROR 4: Kernel panic on CPU 0
[00:00:00.277,221] <err> os: Current thread: 0x200033a0 (main)
[00:00:01.727,081] <err> os: Halting system
```

alas, addr2line doesn't show the stack trace

```
% $addr2line -e build/zephyr/zephyr.elf 0x0000b830 0x0000b821
/Users/dkv/zephyrproject/zephyr/lib/libc/common/source/stdlib/abort.c:14
/Users/dkv/zephyrproject/modules/hal/cmsis/CMSIS/Core/Include/cmsis_gcc.h:1315
```
