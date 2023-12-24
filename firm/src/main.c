/*
 * Copyright (c) 2012-2014 Wind River Systems, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include <zephyr/kernel.h>
#include <zephyr/device.h>
#include <zephyr/logging/log.h>

LOG_MODULE_REGISTER(thermo, LOG_LEVEL_INF);

#include <zephyr/drivers/sensor.h>

enum {
	S_TEMP = 0,
	S_HUM = 1,
};

static const enum sensor_channel s_vals[] = {
		[S_TEMP] = SENSOR_CHAN_AMBIENT_TEMP,
		[S_HUM] = SENSOR_CHAN_HUMIDITY,
};

#define ZU DT_PATH(zephyr_user)

#if (DT_NODE_HAS_PROP(ZU, thermo))
#define THERMO_DEV_NODE DT_PHANDLE(ZU, thermo)
#else
#error "Unsupported board, no zephyr,user thermo alias"
#endif

void main(void) {
	LOG_INF("Hello World! %s\n", CONFIG_BOARD);
	const struct device *const thermo_dev = DEVICE_DT_GET(THERMO_DEV_NODE);

	if (!device_is_ready(thermo_dev)) {
		LOG_ERR("Device \"%s\" not ready", thermo_dev->name);
		return;
	}
	LOG_INF("Found sensor \"%s\"", thermo_dev->name);

	while (true) {
		int rc = sensor_sample_fetch(thermo_dev);
		if (rc) {
			LOG_ERR("Failed to fetch sensor sample (%d)", rc);
		}
		struct sensor_value val[ARRAY_SIZE(s_vals)];
		for (int sv = 0; sv < ARRAY_SIZE(s_vals); sv++) {
			rc = sensor_channel_get(thermo_dev, s_vals[sv], &val[sv]);
			if (rc) {
				LOG_ERR("Failed to get data (%d) data for sensor, err = %d", s_vals[sv], rc);
				continue;
			}
		}
		LOG_INF("T:%.1fC", sensor_value_to_double(&val[S_TEMP]));
		LOG_INF("RH:%.1f%%", sensor_value_to_double(&val[S_HUM]));
		k_sleep(K_MSEC(5000));
	}
}
