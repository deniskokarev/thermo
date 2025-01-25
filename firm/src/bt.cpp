//
// Created by Denis Kokarev on 12/25/23.
//
#include <zephyr/kernel.h>
#include <zephyr/logging/log.h>

LOG_MODULE_REGISTER(bt, LOG_LEVEL_INF);

#include <zephyr/bluetooth/bluetooth.h>
#include <zephyr/bluetooth/conn.h>
#include <zephyr/bluetooth/uuid.h>
#include <zephyr/settings/settings.h>

#include <zephyr/bluetooth/gatt.h>
#include <zephyr/sys/byteorder.h>

#include "thermo.hpp"
#include "bt.hpp"

namespace dkv::thermo {

static const struct bt_data ad[] = {
		BT_DATA_BYTES(BT_DATA_FLAGS, (BT_LE_AD_GENERAL | BT_LE_AD_NO_BREDR)),
		BT_DATA_BYTES(BT_DATA_UUID16_ALL, BT_UUID_16_ENCODE(BT_UUID_DIS_VAL),
		              BT_UUID_16_ENCODE(BT_UUID_ESS_VAL)),
};

static void connected(struct bt_conn *conn, uint8_t err) {
	if (err) {
		LOG_ERR("Connection failed (err 0x%02x)", err);
	} else {
		LOG_INF("Connected");
	}
}

static void disconnected(struct bt_conn *conn, uint8_t reason) {
	LOG_INF("Disconnected (reason 0x%02x)", reason);
}

BT_CONN_CB_DEFINE(conn_callbacks) = {
		.connected = connected,
		.disconnected = disconnected,
};

static int settings_runtime_load(void) {
	settings_runtime_set("bt/dis/model",
	                     CONFIG_BT_DIS_MODEL,
	                     sizeof(CONFIG_BT_DIS_MODEL));
	settings_runtime_set("bt/dis/manuf",
	                     CONFIG_BT_DIS_MANUF,
	                     sizeof(CONFIG_BT_DIS_MANUF));
	settings_runtime_set("bt/dis/serial",
	                     CONFIG_BT_DIS_SERIAL_NUMBER_STR,
	                     sizeof(CONFIG_BT_DIS_SERIAL_NUMBER_STR));
	settings_runtime_set("bt/dis/fw",
	                     CONFIG_BT_DIS_FW_REV_STR,
	                     sizeof(CONFIG_BT_DIS_FW_REV_STR));
	settings_runtime_set("bt/dis/hw",
	                     CONFIG_BT_DIS_HW_REV_STR,
	                     sizeof(CONFIG_BT_DIS_HW_REV_STR));
	return 0;
}


void bt_start() {
	int err = bt_enable(NULL);
	if (err) {
		throw BtError{err, "Bluetooth init failed"};
	}

	if (IS_ENABLED(CONFIG_BT_SETTINGS)) {
		settings_load();
	}

	err = settings_subsys_init();
	if (err) {
		throw BtError{err, "Settings subsys initialization failed"};
	} else {
		LOG_INF("Settings subsys initialization: OK");
	}

	settings_load();
	settings_runtime_load();

	LOG_INF("Bluetooth initialized");

	err = bt_le_adv_start(BT_LE_ADV_PARAM(BT_LE_ADV_OPT_CONNECTABLE |
	                                      BT_LE_ADV_OPT_USE_NAME,
	                                      BT_GAP_ADV_SLOW_INT_MIN,
	                                      BT_GAP_ADV_SLOW_INT_MAX, NULL), ad, ARRAY_SIZE(ad), NULL, 0);
	if (err) {
		throw BtError{err, "Advertising failed to start"};
	}

	LOG_INF("Advertising successfully started");
}

static struct esp_ctx_s {
	struct k_work_delayable update_work;
	uint16_t temp_val;
	uint16_t hum_val;
	int notify_cnt;
} esp_ctx = {.temp_val = 20 * 100, .hum_val = 50 * 100, .notify_cnt = 0};

static void update_temp() {
	try {
		auto val = thermo_read();
		LOG_INF("T:   %.1fC", val[THS_TEMP]);
		LOG_INF("RH:  %.1f%%", val[THS_HUM]);
		esp_ctx.temp_val = val[THS_TEMP] * 100;
		esp_ctx.hum_val = val[THS_HUM] * 100;
	} catch (const ThermoError &ex) {
		LOG_ERR("Failed to fetch sensor sample: %s, err: %d", ex.msg, ex.code);
	}
}

static ssize_t read_temp(struct bt_conn *conn, const struct bt_gatt_attr *attr,
                         void *buf, uint16_t len, uint16_t offset) {
	update_temp();

	const uint16_t *u16 = static_cast<uint16_t *>(attr->user_data);
	uint16_t value = sys_cpu_to_le16(*u16);
	return bt_gatt_attr_read(conn, attr, buf, len, offset, &value,
	                         sizeof(value));
}

#define UPDATE_T (K_MSEC(5 * 1000))

static void update_handler(struct k_work *work);

static void reschedule(int upd) {
	if (!esp_ctx.notify_cnt) {
		k_work_init_delayable(&esp_ctx.update_work, update_handler);
		k_work_schedule(&esp_ctx.update_work, UPDATE_T);
	}
	esp_ctx.notify_cnt += upd;
	if (!esp_ctx.notify_cnt) {
		k_work_cancel_delayable(&esp_ctx.update_work);
	}
}

static void temp_ccc_cfg(const struct bt_gatt_attr *attr,
                         uint16_t value) {
	int upd = (value == BT_GATT_CCC_NOTIFY) ? +1 : -1;
	reschedule(upd);
}

BT_GATT_SERVICE_DEFINE(ess_svc,
                       BT_GATT_PRIMARY_SERVICE(BT_UUID_ESS),

                       BT_GATT_CHARACTERISTIC(BT_UUID_TEMPERATURE,
                                              BT_GATT_CHRC_READ | BT_GATT_CHRC_NOTIFY,
                                              BT_GATT_PERM_READ,
                                              read_temp, NULL, &esp_ctx.temp_val),
                       BT_GATT_CUD("Temp", BT_GATT_PERM_READ),
                       BT_GATT_CCC(temp_ccc_cfg,
                                   BT_GATT_PERM_READ | BT_GATT_PERM_WRITE),

                       BT_GATT_CHARACTERISTIC(BT_UUID_HUMIDITY,
                                              BT_GATT_CHRC_READ | BT_GATT_CHRC_NOTIFY,
                                              BT_GATT_PERM_READ,
                                              read_temp, NULL, &esp_ctx.hum_val),
                       BT_GATT_CUD("Humidity", BT_GATT_PERM_READ),
                       BT_GATT_CCC(temp_ccc_cfg,
                                   BT_GATT_PERM_READ | BT_GATT_PERM_WRITE),
);

static void update_handler(struct k_work *work) {
	update_temp();
	struct k_work_delayable *dwork = k_work_delayable_from_work(work);
	uint16_t v = sys_cpu_to_le16(esp_ctx.temp_val);
	bt_gatt_notify(NULL, &ess_svc.attrs[2], &v, sizeof(v));
	v = sys_cpu_to_le16(esp_ctx.hum_val);
	bt_gatt_notify(NULL, &ess_svc.attrs[6], &v, sizeof(v));
	k_work_reschedule(dwork, UPDATE_T); // re-trigger next invocation
}

} // namespace dkv::thermo
