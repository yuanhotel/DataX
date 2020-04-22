# DataX HttpReader 说明


------------

## 1 快速介绍

HttpReader提供了读取本地文件系统数据存储的能力。在底层实现上，HttpReader获取本地文件数据，并转换为DataX传输协议传递给Writer。

**本地文件内容存放的是一张逻辑意义上的二维表，例如CSV格式的文本信息。**


## 2 功能与限制

HttpReader实现了从网络上读取数据并转为DataX协议的功能，对于DataX而言，HttpReader实现了返回json格式的响应消息。目前HttpReader支持功能如下：

1. 支持且仅支持读取json的格式的作息。

2. 支持类jsonPath对数据进行抽取。

3. 支持多种类型数据读取(使用String表示)，支持列常量

4. 多个Url可以支持并发读取。

我们暂时不能做到：

1. 单个url支持多线程并发读取。


## 3 功能说明


### 3.1 配置样例

```json
{
    "setting": {},
    "job": {
        "setting": {
            "speed": {
                "channel": 2
            }
        },
        "content": [
            {
                "reader": {
                    "name": "httpreader",
                    "parameter": {
                        "url": ["https://c.m.163.com/ug/api/wuhan/app/data/list-total"],
                        "urlMethod" :"get"
                        "urlHeader" :{"a":"a1"}
                        "urlParam" :{"a":"a1","b":2}
                        "encoding": "UTF-8",
                        "dataPath": "$.data.chinaDayList",
                        "column": [
                            {
                                "name": "date",
                                "type": "string"
                            },
                            {
                                "name": "total.confirm",
                                "type": "long"
                            },
                            {
                                "name": "total.suspect",
                                "type": "long"
                            },
                            {
                                "name": "total.heal",
                                "type": "long"
                            },
                            {
                                "name": "total.dead",
                                "type": "long"
                            },
                            {
                                "name": "total.severe",
                                "type": "long"
                            }
                        ]
                    }
                },
                "writer": {
                    "name": "txtfilewriter",
                    "parameter": {
                        "path": "/home/haiwei.luo/case00/result",
                        "fileName": "luohw",
                        "writeMode": "truncate",
                        "format": "yyyy-MM-dd"
                    }
                }
            }
        ]
    }
}
```

### 3.2 参数说明

* **url**

	* 描述：远程的路径信息，注意这里可以支持填写多个路径。 <br />

		当指定单个远程路径，HttpReader暂时只能使用单线程进行数据抽取。

		当指定多个远程路径，HttpReader支持使用多线程进行数据抽取。线程并发数通过通道数指定。

		**特别需要注意的是，DataX会将一个作业下同步的所有url视作同一种数据格式。用户必须自己保证所有的url能够适配同一套schema信息。并且提供给DataX权限可读。**

	* 必选：是 <br />

	* 默认值：无 <br />
* **urlMethon**

	* 描述：访问地址的方法。 <br />

		访问地址时用get还是post。

	* 必选：否 <br />

	* 默认值：get <br />

* **urlHeader**

	* 描述：远程地址的头信息。 <br />

	* 必选：否 <br />

	* 默认值：无 <br />

* **urlParam**

	* 描述：远程地址的参数信息。 <br />

		**特别需要注意的是，参数为键值对形式。**

	* 必选：否 <br />

	* 默认值：无 <br />

* **column**

	* 描述：读取字段列表，type指定源数据的类型，name指定当前列的值的属性名称。 <br />

		用户必须指定Column字段信息，配置如下：

		```json
		{
           "type": "integer",
           "name": "total"    //根据date的名称读取json的值
        },
        {
           "type": "string",
           "name": "user.sex"  //从json先读取user,再从user中读取值
        },
        {
           "type": "string",
           "value": "alibaba"  //从HttpReader内部生成alibaba的字符串字段作为当前字段
        },
        {
           "type": "date",
           "value": "2020-04-22",
           "format": "yyyy-MM-dd" //以yyyy-MM-dd格式化字符串字段作为时间值
        }
		```


	* 必选：是 <br />

	* 默认值：无 <br />

* **dataPath**

	* 描述：读取的返回json的信息的数据部分的路径 <br />

	* 必选：是 <br />

	* 默认值：$.data <br />

* **encoding**

	* 描述：解析返回信息的编码配置。<br />

 	* 必选：否 <br />

 	* 默认值：UTF-8 <br />

### 3.3 类型转换

本地文件本身不提供数据类型，该类型是DataX HttpReader定义：

| DataX 内部类型| json 数据类型    |
| -------- | -----  |
|
| Integer  |Integer |
| Long     |Long |
| Float    |Float|
| Double   |Double|
| String   |String|
| Boolean  |Boolean |
| Date     |Date |

其中：

* 本地文件 Long是指本地文件文本中使用整形的字符串表示形式，例如"19901219"。
* 本地文件 Double是指本地文件文本中使用Double的字符串表示形式，例如"3.1415"。
* 本地文件 Boolean是指本地文件文本中使用Boolean的字符串表示形式，例如"true"、"false"。不区分大小写。
* 本地文件 Date是指本地文件文本中使用Date的字符串表示形式，例如"2014-12-31"，Date可以指定format格式。


## 4 性能报告

略

## 5 约束限制

略

## 6 FAQ

略


