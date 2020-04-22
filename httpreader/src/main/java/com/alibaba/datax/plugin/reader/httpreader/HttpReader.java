package com.alibaba.datax.plugin.reader.httpreader;

import com.alibaba.datax.common.element.*;
import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.spi.Reader;
import com.alibaba.datax.common.util.Configuration;
import com.jayway.jsonpath.JsonPath;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.UnsupportedCharsetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by yutel on 2020-4-8 .
 */
public class HttpReader extends Reader {
    public static class Job extends Reader.Job {
        private static final Logger LOG = LoggerFactory.getLogger(Job.class);
        private Configuration originConfig = null;
        private List<String> url = null;

        @Override
        public void init() {
            this.originConfig = this.getPluginJobConf();
            this.validateParameter();
        }

        private void validateParameter() {
            // Compatible with the old version, path is a string before
            String urlInString = this.originConfig.getNecessaryValue(Key.URL,
                    HttpReaderErrorCode.REQUIRED_VALUE);
            if (StringUtils.isBlank(urlInString)) {
                throw DataXException.asDataXException(
                        HttpReaderErrorCode.REQUIRED_VALUE,
                        "您需要指定待读取的源地址");
            }
            if (!urlInString.startsWith("[") && !urlInString.endsWith("]")) {
                url = new ArrayList<String>();
                url.add(urlInString);
            } else {
                url = this.originConfig.getList(Key.URL, String.class);
                if (null == url || url.size() == 0) {
                    throw DataXException.asDataXException(
                            HttpReaderErrorCode.REQUIRED_VALUE,
                            "您需要指定待读取的源地址");
                }
            }

            String encoding = this.originConfig.getString(Key.ENCODING, Constant.DEFAULT_ENCODING);
            if (StringUtils.isBlank(encoding)) {
                this.originConfig.set(Key.ENCODING, Constant.DEFAULT_ENCODING);
            } else {
                try {
                    encoding = encoding.trim();
                    this.originConfig.set(Key.ENCODING, encoding);
                    Charsets.toCharset(encoding);
                } catch (UnsupportedCharsetException uce) {
                    throw DataXException.asDataXException(
                            HttpReaderErrorCode.ILLEGAL_VALUE,
                            String.format("不支持您配置的编码格式 : [%s]", encoding), uce);
                } catch (Exception e) {
                    throw DataXException.asDataXException(
                            HttpReaderErrorCode.CONFIG_INVALID_EXCEPTION,
                            String.format("编码配置异常, 请联系我们: %s", e.getMessage()),
                            e);
                }
            }

            // column: 1. index type 2.value type 3.when type is Date, may have
            // format
            List<Configuration> columns = this.originConfig.getListConfiguration(Key.COLUMN);
            if (columns == null || columns.size() < 0) {
                throw DataXException.asDataXException(
                        HttpReaderErrorCode.REQUIRED_VALUE,
                        "您的columns数组不能为空");
            } else {
                for (Configuration eachColumnConf : columns) {
                    eachColumnConf.getNecessaryValue(Key.TYPE,
                            HttpReaderErrorCode.REQUIRED_VALUE);
                    eachColumnConf.getNecessaryValue(Key.NAME,
                            HttpReaderErrorCode.REQUIRED_VALUE);
                }
            }
        }

        @Override
        public void prepare() {
        }

        @Override
        public void post() {
        }

        @Override
        public void destroy() {
        }

        // warn: 如果源目录为空会报错，拖空目录意图=>空文件显示指定此意图
        @Override
        public List<Configuration> split(int adviceNumber) {
            LOG.debug("split() begin...");
            List<Configuration> readerSplitConfigs = new ArrayList<Configuration>();
            List<List<String>> splitedUrl = this.splitUrl(
                    this.url, adviceNumber);
            for (List<String> urls : splitedUrl) {
                Configuration splitedConfig = this.originConfig.clone();
                splitedConfig.set(Constant.URLS, urls);
                readerSplitConfigs.add(splitedConfig);
            }
            LOG.debug("split() ok and end...");
            return readerSplitConfigs;
        }

        private <T> List<List<T>> splitUrl(final List<T> sourceList,
                                           int adviceNumber) {
            List<List<T>> splitedList = new ArrayList<List<T>>();
            int averageLength = sourceList.size() / adviceNumber;
            averageLength = averageLength == 0 ? 1 : averageLength;

            for (int begin = 0, end = 0; begin < sourceList.size(); begin = end) {
                end = begin + averageLength;
                if (end > sourceList.size()) {
                    end = sourceList.size();
                }
                splitedList.add(sourceList.subList(begin, end));
            }
            return splitedList;
        }

    }

    public static class Task extends Reader.Task {
        private static Logger LOG = LoggerFactory.getLogger(Task.class);
        private PooledHttpClientAdaptor httpClientAdaptor = null;
        private Configuration readerSliceConfig;
        private List<String> urls;
        private List<Configuration> columns;
        private String dataPath;


        @Override
        public void init() {
            this.readerSliceConfig = this.getPluginJobConf();
            this.httpClientAdaptor = new PooledHttpClientAdaptor();
            this.dataPath = this.readerSliceConfig.getString(Key.DATA_PATH, Constant.DEFAULT_DATA_PATH);
            this.urls = this.readerSliceConfig.getList(Constant.URLS, String.class);
            this.columns = this.readerSliceConfig.getListConfiguration(Key.COLUMN);
        }

        @Override
        public void prepare() {

        }

        @Override
        public void post() {

        }

        @Override
        public void destroy() {

        }

        @Override
        public void startRead(RecordSender recordSender) {
            LOG.debug("start request urls...");
            for (String url : this.urls) {
                LOG.info(String.format("request url : [%s]", url));
                String jsonString = this.httpClientAdaptor.doGet(url);
                Object json = com.jayway.jsonpath.Configuration
                        .defaultConfiguration().jsonProvider().parse(jsonString);
                Object obj = JsonPath.read(json, dataPath);
                if (obj instanceof List) {
                    for (Object sub : (List) obj) {
                        column2Record(recordSender, sub);
                    }
                } else {
                    column2Record(recordSender, obj);
                }
            }
            LOG.debug("end request urls...");
        }

        private void column2Record(RecordSender recordSender, Object sub) {
            Record record = recordSender.createRecord();
            for (Configuration eachColumnConf : columns) {
                String columnType = eachColumnConf.getString(Key.TYPE);
                String columnName = eachColumnConf.getString(Key.NAME);
                if ("string".equalsIgnoreCase(columnType)) {
                    String val = JsonPath.read(sub, columnName);
                    record.addColumn(new StringColumn(val));
                } else if ("boolean".equalsIgnoreCase(columnType)) {
                    Boolean val = JsonPath.read(sub, columnName);
                    record.addColumn(new BoolColumn(val));
                } else if ("integer".equalsIgnoreCase(columnType)) {
                    Integer val = JsonPath.read(sub, columnName);
                    record.addColumn(new LongColumn(val));
                } else if ("long".equalsIgnoreCase(columnType)) {
                    Long val = JsonPath.read(sub, columnName);
                    record.addColumn(new LongColumn(val));
                } else if ("double".equalsIgnoreCase(columnType)) {
                    Double val = JsonPath.read(sub, columnName);
                    record.addColumn(new DoubleColumn(val));
                } else if ("date".equalsIgnoreCase(columnType)) {
                    String columnFormat = eachColumnConf.getString(Key.FORMAT);
                    if (StringUtils.isBlank(columnFormat)) {
                        Long val = JsonPath.read(sub, columnName);
                        record.addColumn(new DateColumn(val));
                    } else {
                        try {
                            String day = JsonPath.read(sub, columnName);
                            Date date = DateUtils.parseDate(day, columnFormat);
                            record.addColumn(new DateColumn(date));
                        } catch (ParseException e) {
                            LOG.error("格式：" + columnFormat, e);
                        }
                    }
                } else {
                    LOG.error(String.format("type:[%s] is not support", columnType));
                }
            }
            recordSender.sendToWriter(record);
        }

    }
}
