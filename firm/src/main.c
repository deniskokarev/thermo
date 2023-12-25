#include <zephyr/kernel.h>
#include <zephyr/logging/log.h>

LOG_MODULE_REGISTER(main, LOG_LEVEL_INF);

#include "thermo.h"

void main(void) {
	if (!thermo_init()) {
		LOG_ERR("Cannot initialize temperature sensor");
		return;
	}

	while (true) {
		static double val[THS_SZ];
		if (thermo_read(val)) {
			LOG_ERR("Failed to fetch sensor sample");
		} else {
			LOG_INF("T:%.1fC", val[THS_TEMP]);
			LOG_INF("RH:%.1f%%", val[THS_HUM]);
		}
		k_sleep(K_MSEC(5000));
	}
}
