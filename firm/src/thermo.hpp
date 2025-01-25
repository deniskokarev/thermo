#pragma once

#include <array>
#include "ex.hpp"

namespace dkv::thermo {

class ThermoError : public FormattedError<> {
public:
	template<typename... Args>
	ThermoError(int code, const char *fmt, Args &&... args) :
			FormattedError(code, fmt, std::forward<Args>(args)...) {
	}
};

enum {
	THS_TEMP,
	THS_HUM,
	THS_SZ,
};

using ThermoSample = std::array<double, THS_SZ>;

/**
 * init temperature sensor
 * @throws ThermoError on error
 */
void thermo_init();

/**
 * read temperature/humidity sample
 * @returns sample vector of temperature and humidity
 * @throws ThermoError on error
 */
const ThermoSample thermo_read();

} // namespace dkv::thermo
