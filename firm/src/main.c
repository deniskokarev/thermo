#include <zephyr/kernel.h>
#include <zephyr/logging/log.h>

LOG_MODULE_REGISTER(main, LOG_LEVEL_INF);

#include "adc.h"
#include "thermo.h"
#include "bt.h"

void main(void) {
	int rc;
	if ((rc = adc_init())) {
		LOG_ERR("Cannot initialize ADC, error: %d", rc);
		return;
	}
	if (!thermo_init()) {
		LOG_ERR("Cannot initialize temperature sensor");
		return;
	}
	if (bt_start()) {
		LOG_ERR("Cannot start BT subsystem");
		return;
	}

	while (true) {
		static double val[THS_SZ];
		if (thermo_read(val)) {
			LOG_ERR("Failed to fetch sensor sample");
		} else {
			LOG_INF("T:   %.1fC", val[THS_TEMP]);
			LOG_INF("RH:  %.1f%%", val[THS_HUM]);
		}
		float bat;
		if (adc_sample(&bat, ACH_BAT_LVL)) {
			LOG_ERR("Failed to read battery voltage");
		} else {
			LOG_INF("Bat: %.2fV", bat);
		}
		k_sleep(K_MSEC(5000));
	}
}
