#pragma once

#include "ex.hpp"

namespace dkv::thermo {

struct BtError : public Error {
	using Error::Error;
};

/**
 * start BT subsystem
 * @throws BtError on err
 */
void bt_start();

} // namespace dkv::thermo
