cmake_minimum_required(VERSION 3.20.0)

set(BOARD qemu_x86)

find_package(Zephyr REQUIRED HINTS $ENV{ZEPHYR_BASE})

project(thermo_test)

zephyr_include_directories(../src)

target_sources(app PRIVATE src/test.cpp ../src/db.cpp)
