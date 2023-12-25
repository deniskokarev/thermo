//
// Created by Denis Kokarev on 12/25/23.
//

#ifndef THERMO_THERMO_H
#define THERMO_THERMO_H

enum {
	THS_TEMP,
	THS_HUM,
	THS_SZ,
};

/**
 * init temperature sensor
 * @return true on success
 */
bool thermo_init();

/**
 * read temperature/humidity sample
 * @param sample - pointer to S_SZ double elements
 * @return 0 on success
 */
int thermo_read(double sample[THS_SZ]);

#endif //THERMO_THERMO_H
