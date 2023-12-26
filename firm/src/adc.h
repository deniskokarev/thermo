//
// Created by Denis Kokarev on 12/25/23.
//

#ifndef THERMO_ADC_H
#define THERMO_ADC_H

enum {
	ACH_BAT_LVL,
	ACH_SZ,
};

/**
 * @return 0 on success, otherwise error code
 */
int adc_init();

/**
 * read adc value from the given channel
 * @param sample the resulting value in volts
 * @param chan ACH_ channel number
 * @return 0 on success, otherwise err code
 */
int adc_sample(float *sample, int chan);

#endif //THERMO_ADC_H
