#include <cmath>
#include <zephyr/kernel.h>
#include <zephyr/logging/log.h>

LOG_MODULE_REGISTER(main, LOG_LEVEL_INF);

#include "adc.hpp"
#include "thermo.hpp"
#include "bt.hpp"

using namespace dkv::thermo;

int main(void) {
	try {
		adc_init();
		thermo_init();
		bt_start();
	} catch (const AdcError &ex) {
		LOG_ERR("ADC sensor init error: %s, code: %d", ex.msg, ex.code);
		return -1;
	} catch (const ThermoError &ex) {
		LOG_ERR("Temp sensor init error: %s, code: %d", ex.msg, ex.code);
		return -2;
	} catch (const BtError &ex) {
		LOG_ERR("Bluetooth init error: %s, code: %d", ex.msg, ex.code);
		return -3;
	}
	try {
		float prev_bat = 0;
		while (true) {
			float bat = adc_sample(ACH_BAT_LVL);
			if (std::abs(bat - prev_bat) > 0.005f) {
				LOG_INF("Batt Voltage changed: %.2fV", (double) bat);
				prev_bat = bat;
			}
			k_sleep(K_MSEC(5000));
		}
	} catch (const Error &ex) {
		LOG_ERR("Finishing main loop due to an error: %s, code: %d", ex.msg, ex.code);
		return -4;
	}
}
