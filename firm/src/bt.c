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


static const struct bt_data ad[] = {
		BT_DATA_BYTES(BT_DATA_FLAGS, (BT_LE_AD_GENERAL | BT_LE_AD_NO_BREDR)),
		BT_DATA_BYTES(BT_DATA_UUID16_ALL, BT_UUID_16_ENCODE(BT_UUID_DIS_VAL)),
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

/**
 * start BT subsystem
 * @return 0 on success, otherwise err code
 */
int bt_start() {
	int err = bt_enable(NULL);
	if (err) {
		LOG_ERR("Bluetooth init failed (err %d)", err);
		return err;
	}

	if (IS_ENABLED(CONFIG_BT_SETTINGS)) {
		settings_load();
	}

	err = settings_subsys_init();
	if (err) {
		LOG_ERR("settings subsys initialization: fail (err %d)", err);
	} else {
		LOG_INF("settings subsys initialization: OK");
	}

	settings_load();
	settings_runtime_load();

	LOG_INF("Bluetooth initialized");

	err = bt_le_adv_start(BT_LE_ADV_CONN_NAME, ad, ARRAY_SIZE(ad), NULL, 0);
	if (err) {
		LOG_ERR("Advertising failed to start (err %d)", err);
		return err;
	}

	LOG_INF("Advertising successfully started");
	return 0;
}
