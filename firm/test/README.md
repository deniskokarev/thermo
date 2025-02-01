# thermo unit tests

It requires `brew install qemu` on Mac.

Run using twister from parent dir
```
$ZEPHYR_BASE/scripts/twister -O /tmp/zt --platform qemu_x86 -T test
...
INFO    - Total complete:    1/   1  100%  skipped:    0, failed:    0, error:    0
```

Or Build/Run independently

Build:
```
west build
...
```
Run:
```
west build -t run
```
Debug:
```
west build -t debugserver_qemu
```
attach using gdb on port :1234

```
% x86_64-zephyr-elf-gdb ./build/zephyr/zephyr.elf

(gdb) target remote localhost:1234
(gdb) b test.cpp:30
Breakpoint 1 at 0x103e8f: file /Users/dkv/work/arm/nordic/thermo/firm/test/src/test.cpp, line 39.
(gdb) c
Breakpoint 1, thermo_suite_test_01_list_alloc () at /Users/dkv/work/arm/nordic/thermo/firm/test/src/test.cpp:39
39				for (int i=0; i<N; i++) {
```

## TODO config CLion with qemu gdb
