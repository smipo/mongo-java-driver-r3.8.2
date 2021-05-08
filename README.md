生产环境使用mongo副本集时,遇到了一个问题,mongo配置了?readPreference=secondary(表示读操作只在从节点),
在操作一个Document的增删改时,需要读取数据,填充到另一个Document的数据,
因为配置了secondary,主从数据同步未完成,导致读数据未读到,而填充了错误的数据。
又因为只是在某些查询数据时需要从主节点读取,若将配置改成?readPreference=primary(表示读操作只在主节点),那么副本集的优势不存在了,
综合考虑,所以需要将mongo客户端代码改造,当需要读取数据时可以通过方法从主节点或者从节点读取,以上是背景.

改造并不难,改造了两个类MongoIterable和MongoCollection:
1.MongoIterable改造了find方法,新增了两个方法 default MongoCursor<TResult> iterator(ReadPreference readPreference)和default TResult first(ReadPreference readPreference)
2.MongoCollection改造了count方法.
