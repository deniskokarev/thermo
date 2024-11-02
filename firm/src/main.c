#include <zephyr/kernel.h>
#include <zephyr/logging/log.h>

LOG_MODULE_REGISTER(main, LOG_LEVEL_INF);

#include "adc.h"
#include "thermo.h"
#include "bt.h"

int main(void) {
	int rc;
	if ((rc = adc_init())) {
		LOG_ERR("Cannot initialize ADC, error: %d", rc);
		return -1;
	}
	if (!thermo_init()) {
		LOG_ERR("Cannot initialize temperature sensor");
		return -2;
	}
	if (bt_start()) {
		LOG_ERR("Cannot start BT subsystem");
		return -3;
	}

	while (true) {
		float bat;
		if (adc_sample(&bat, ACH_BAT_LVL)) {
			LOG_ERR("Failed to read battery voltage");
		} else {
			LOG_INF("Bat: %.2fV", (double)bat);
		}
		k_sleep(K_MSEC(5000));
	}
	return 0;
}
