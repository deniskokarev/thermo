#pragma once

#include <utility>
#include "ex.hpp"

namespace dkv::thermo {

enum {
	ACH_BAT_LVL,
	ACH_SZ,
};

class AdcError : public FormattedError<> {
public:
	template<typename... Args>
	AdcError(int code, const char *fmt, Args &&... args) :
			FormattedError(code, fmt, std::forward<Args>(args)...) {
	}
};

/**
 * Initialize ADC sensor
 * @throws AdcError on error
 */
void adc_init();

/**
 * read adc value from the given channel
 * @param chan ACH_ channel number
 * @return voltage value on the selected channel
 * @throws AdcError in case of a problem
 */
float adc_sample(int chan);

} // namespace dkv::thermo
