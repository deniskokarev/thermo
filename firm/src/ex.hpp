#pragma once

namespace dkv::thermo {

struct Error {
	int code;
	const char *msg;

	Error(int code, const char *msg) : code{code}, msg{msg} {
	}
};

template<size_t MSG_SZ = 64>
class FormattedError : public Error {
	char msg_buf[MSG_SZ];
public:
	FormattedError(int code, const char *fmt, ...) : Error(code, msg_buf) {
		va_list ap;
		va_start(ap, fmt);
		vsnprintk(msg_buf, MSG_SZ, fmt, ap);
		msg_buf[MSG_SZ - 1] = 0;
		va_end(ap);
	}
};

} // namespace dkv::thermo
