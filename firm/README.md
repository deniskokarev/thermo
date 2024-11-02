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

Install ARM toll-chain as well. Recommending [Zephyr SDK](https://docs.zephyrproject.org/latest/develop/toolchains/zephyr_sdk.html).
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

connect via nRF Connect and explore provided GATT characteristics

for debug open /Applications/SEGGER/JLink/JLinkRTTViewer.app

or open an RTT logger (make sure the CONFIG_LOG=y in prj.conf):

```
/Applications/SEGGER/JLink/JLinkRTTLogger -Device NRF52832_XXAA -If SWD -Speed 4000 -RTTChannel 0 /tmp/nrf.log

tail -f /tmp/nrf.log
```