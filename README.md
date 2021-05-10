由于某些场景,查询时需要特定指定主节点读取数据,配置选项时是从节点读取数据的(readPreference=secondary),综合考虑,改造mongo客户端.


改造并不难,改造了两个类MongoIterable和MongoCollection:  
1.MongoIterable改造了find方法,新增了两个方法 default MongoCursor<TResult> iterator(ReadPreference readPreference)和default TResult first(ReadPreference readPreference)  
2.MongoCollection改造了count方法.
