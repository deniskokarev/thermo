# SPDX-License-Identifier: Apache-2.0

cmake_minimum_required(VERSION 3.20.0)

set(BOARD_ROOT ${CMAKE_CURRENT_LIST_DIR})
set(BOARD nrf52dk_nrf52832)
set(SHIELD thermo)

find_package(Zephyr REQUIRED HINTS $ENV{ZEPHYR_BASE})
project(thermo)

target_sources(app PRIVATE src/main.c src/thermo.c)
