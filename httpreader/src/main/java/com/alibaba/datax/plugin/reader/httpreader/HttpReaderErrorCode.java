package com.alibaba.datax.plugin.reader.httpreader;

import com.alibaba.datax.common.spi.ErrorCode;


public enum HttpReaderErrorCode implements ErrorCode {
	REQUIRED_VALUE("HttpReader-00", "您缺失了必须填写的参数值."),
	ILLEGAL_VALUE("HttpReader-01", "您填写的参数值不合法."),
	MIXED_NAME_VALUE("HttpReader-02", "您的列信息配置同时包含了name,value."),
	NO_NAME_VALUE("HttpReader-03","您明确的配置列信息,但未填写相应的name,value."),
	URI_NOT_FOUND("HttpReader-04", "您配置的网络路径找不到."),
	WITH_CHARSET_ERROR("HttpReader-05", "您配置的文件编码和实际文件编码不符合."),
	READ_FILE_IO_ERROR("HttpReader-06", "您配置的文件在读取时出现IO异常."),
	SECURITY_NOT_ENOUGH("HttpReader-07", "您缺少权限执行相应的文件操作."),
	CONFIG_INVALID_EXCEPTION("HttpReader-08", "您的参数配置错误."),
	RUNTIME_EXCEPTION("HttpReader-10", "出现运行时异常, 请联系我们");

	private final String code;
	private final String description;

	private HttpReaderErrorCode(String code, String description) {
		this.code = code;
		this.description = description;
	}

	@Override
	public String getCode() {
		return this.code;
	}

	@Override
	public String getDescription() {
		return this.description;
	}

	@Override
	public String toString() {
		return String.format("Code:[%s], Description:[%s].", this.code,
				this.description);
	}
}
