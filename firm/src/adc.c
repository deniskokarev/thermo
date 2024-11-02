//
// Created by Denis Kokarev on 12/25/23.
//
#include <zephyr/drivers/adc.h>
#include <zephyr/logging/log.h>

LOG_MODULE_REGISTER(adc, LOG_LEVEL_INF);

#include "adc.h"

#if !DT_NODE_EXISTS(DT_PATH(zephyr_user)) || \
    !DT_NODE_HAS_PROP(DT_PATH(zephyr_user), io_channels)
#error "Unsupported board, no 'zephyr,user' io-channels alias"
#endif

#define ADC_NCHAN   DT_PROP_LEN(DT_PATH(zephyr_user), io_channels)

#if ACH_SZ != DC_NCHAN
#error "ACH enums should match the ADC channels size"
#endif

#define DT_SPEC_AND_COMMA(node_id, prop, idx) \
    ADC_DT_SPEC_GET_BY_IDX(node_id, idx),

/* Data of ADC io-channels specified in devicetree. */
static const struct adc_dt_spec adc_channels[] = {
		DT_FOREACH_PROP_ELEM(DT_PATH(zephyr_user), io_channels,
		                     DT_SPEC_AND_COMMA)
};

#define DT_BUF_DEF(node_id, prop, I) { .buffer = &buf[I], .buffer_size = sizeof(buf[I]), },

static int16_t buf[ADC_NCHAN];

static struct adc_sequence sequence[ADC_NCHAN] = {
		DT_FOREACH_PROP_ELEM(DT_PATH(zephyr_user), io_channels, DT_BUF_DEF)
};

/**
 * @return 0 on success, otherwise error code
 */
int adc_init() {
	/* Configure channels individually prior to sampling. */
	for (size_t i = 0U; i < ARRAY_SIZE(adc_channels); i++) {
		if (!device_is_ready(adc_channels[i].dev)) {
			LOG_ERR("ADC controller device not ready");
			return -EINVAL;
		}

		int rc = adc_channel_setup_dt(&adc_channels[i]);
		if (rc < 0) {
			LOG_ERR("Could not setup channel #%d (%d)", i, rc);
			return -EINVAL;
		}
		rc = adc_sequence_init_dt(&adc_channels[i], &sequence[i]);
		if (rc < 0) {
			LOG_ERR("Could not setup channel #%d sequence (%d)", i, rc);
			return -EINVAL;
		}
	}
	return 0;
}

// nordic iref = 0.6v, and with x6 scale factor 4095 == 3.6v
static float adc2v(float adc) {
	return adc * 3.6f / 4096;
}

/**
 * read adc value from the given channel
 * @param sample the resulting value in volts
 * @param chan ACH_ channel number
 * @return 0 on success, otherwise err code
 */
int adc_sample(float *sample, int chan) {
	int err = adc_read(adc_channels[chan].dev, &sequence[chan]);
	if (err < 0) {
		LOG_ERR("- %s, channel %d: error: %d ",
		        adc_channels[chan].dev->name,
		        adc_channels[chan].channel_id,
		        err);
		return err;
	} else {
		*sample = adc2v(buf[chan]);
		return 0;
	}
}
