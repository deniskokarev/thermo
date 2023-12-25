//
// Created by Denis Kokarev on 12/25/23.
//
#include <zephyr/device.h>
#include <zephyr/logging/log.h>

LOG_MODULE_REGISTER(thermo, LOG_LEVEL_INF);

#include <zephyr/drivers/sensor.h>
#include "thermo.h"

static const enum sensor_channel s_vals[THS_SZ] = {
		[THS_TEMP] = SENSOR_CHAN_AMBIENT_TEMP,
		[THS_HUM] = SENSOR_CHAN_HUMIDITY,
};

#define ZU DT_PATH(zephyr_user)

#if (DT_NODE_HAS_PROP(ZU, thermo))
#define THERMO_DEV_NODE DT_PHANDLE(ZU, thermo)
#else
#error "Unsupported board, no 'zephyr,user' thermo alias"
#endif

static const struct device *const thermo_dev = DEVICE_DT_GET(THERMO_DEV_NODE);

/**
 * init temperature sensor
 * @return true on success
 */
bool thermo_init() {
	int rc = device_is_ready(thermo_dev);
	if (!rc) {
		LOG_ERR("Device \"%s\" not ready", thermo_dev->name);
	} else {
		LOG_INF("Initialized sensor \"%s\"", thermo_dev->name);
	}
	return rc;
}

/**
 * read temperature/humidity sample
 * @param sample - pointer to S_SZ double elements
 * @return 0 on success
 */
int thermo_read(double sample[THS_SZ]) {
	int rc = sensor_sample_fetch(thermo_dev);
	if (rc) {
		LOG_ERR("Failed to fetch sensor sample (%d)", rc);
	}
	for (int sv = 0; sv < THS_SZ; sv++) {
		struct sensor_value val;
		rc = sensor_channel_get(thermo_dev, s_vals[sv], &val);
		if (rc) {
			LOG_ERR("Failed to get data (%d) data for sensor, err = %d", s_vals[sv], rc);
			return rc;
		}
		sample[sv] = sensor_value_to_double(&val);
	}
	return 0;
}
