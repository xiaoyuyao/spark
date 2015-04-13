#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

############ RDD Actions and Transformations ############

#' @rdname aggregateRDD
#' @seealso reduce
#' @export
setGeneric("aggregateRDD", function(x, zeroValue, seqOp, combOp) { standardGeneric("aggregateRDD") })

#' @rdname cache-methods
#' @export
setGeneric("cache", function(x) { standardGeneric("cache") })

#' @rdname coalesce
#' @seealso repartition
#' @export
setGeneric("coalesce", function(x, numPartitions, ...) { standardGeneric("coalesce") })

#' @rdname checkpoint-methods
#' @export
setGeneric("checkpoint", function(x) { standardGeneric("checkpoint") })

#' @rdname collect-methods
#' @export
setGeneric("collect", function(x, ...) { standardGeneric("collect") })

#' @rdname collect-methods
#' @export
setGeneric("collectAsMap", function(x) { standardGeneric("collectAsMap") })

#' @rdname collect-methods
#' @export
setGeneric("collectPartition",
           function(x, partitionId) {
             standardGeneric("collectPartition")
           })

#' @rdname count
#' @export
setGeneric("count", function(x) { standardGeneric("count") })

#' @rdname countByValue
#' @export
setGeneric("countByValue", function(x) { standardGeneric("countByValue") })

#' @rdname distinct
#' @export
setGeneric("distinct", function(x, numPartitions = 1L) { standardGeneric("distinct") })

#' @rdname filterRDD
#' @export
setGeneric("filterRDD", function(x, f) { standardGeneric("filterRDD") })

#' @rdname first
#' @export
setGeneric("first", function(x) { standardGeneric("first") })

#' @rdname flatMap
#' @export
setGeneric("flatMap", function(X, FUN) { standardGeneric("flatMap") })

#' @rdname fold
#' @seealso reduce
#' @export
setGeneric("fold", function(x, zeroValue, op) { standardGeneric("fold") })

#' @rdname foreach
#' @export
setGeneric("foreach", function(x, func) { standardGeneric("foreach") })

#' @rdname foreach
#' @export
setGeneric("foreachPartition", function(x, func) { standardGeneric("foreachPartition") })

# The jrdd accessor function.
setGeneric("getJRDD", function(rdd, ...) { standardGeneric("getJRDD") })

#' @rdname glom
#' @export
setGeneric("glom", function(x) { standardGeneric("glom") })

#' @rdname keyBy
#' @export
setGeneric("keyBy", function(x, func) { standardGeneric("keyBy") })

#' @rdname lapplyPartition
#' @export
setGeneric("lapplyPartition", function(X, FUN) { standardGeneric("lapplyPartition") })

#' @rdname lapplyPartitionsWithIndex
#' @export
setGeneric("lapplyPartitionsWithIndex",
           function(X, FUN) {
             standardGeneric("lapplyPartitionsWithIndex")
           })

#' @rdname lapply
#' @export
setGeneric("map", function(X, FUN) { standardGeneric("map") })

#' @rdname lapplyPartition
#' @export
setGeneric("mapPartitions", function(X, FUN) { standardGeneric("mapPartitions") })

#' @rdname lapplyPartitionsWithIndex
#' @export
setGeneric("mapPartitionsWithIndex",
           function(X, FUN) { standardGeneric("mapPartitionsWithIndex") })

#' @rdname maximum
#' @export
setGeneric("maximum", function(x) { standardGeneric("maximum") })

#' @rdname minimum
#' @export
setGeneric("minimum", function(x) { standardGeneric("minimum") })

#' @rdname sumRDD 
#' @export
setGeneric("sumRDD", function(x) { standardGeneric("sumRDD") })

#' @rdname name
#' @export
setGeneric("name", function(x) { standardGeneric("name") })

#' @rdname numPartitions
#' @export
setGeneric("numPartitions", function(x) { standardGeneric("numPartitions") })

#' @rdname persist
#' @export
setGeneric("persist", function(x, newLevel) { standardGeneric("persist") })

#' @rdname pipeRDD
#' @export
setGeneric("pipeRDD", function(x, command, env = list()) { standardGeneric("pipeRDD")})

#' @rdname reduce
#' @export
setGeneric("reduce", function(x, func) { standardGeneric("reduce") })

#' @rdname repartition
#' @seealso coalesce
#' @export
setGeneric("repartition", function(x, numPartitions) { standardGeneric("repartition") })

#' @rdname sampleRDD
#' @export
setGeneric("sampleRDD",
           function(x, withReplacement, fraction, seed) {
             standardGeneric("sampleRDD")
           })

#' @rdname saveAsObjectFile
#' @seealso objectFile
#' @export
setGeneric("saveAsObjectFile", function(x, path) { standardGeneric("saveAsObjectFile") })

#' @rdname saveAsTextFile
#' @export
setGeneric("saveAsTextFile", function(x, path) { standardGeneric("saveAsTextFile") })

#' @rdname setName
#' @export
setGeneric("setName", function(x, name) { standardGeneric("setName") })

#' @rdname sortBy
#' @export
setGeneric("sortBy",
           function(x, func, ascending = TRUE, numPartitions = 1L) {
             standardGeneric("sortBy")
           })

#' @rdname take
#' @export
setGeneric("take", function(x, num) { standardGeneric("take") })

#' @rdname takeOrdered
#' @export
setGeneric("takeOrdered", function(x, num) { standardGeneric("takeOrdered") })

#' @rdname takeSample
#' @export
setGeneric("takeSample",
           function(x, withReplacement, num, seed) {
             standardGeneric("takeSample")
           })

#' @rdname top
#' @export
setGeneric("top", function(x, num) { standardGeneric("top") })

#' @rdname unionRDD
#' @export
setGeneric("unionRDD", function(x, y) { standardGeneric("unionRDD") })

#' @rdname unpersist-methods
#' @export
setGeneric("unpersist", function(x, ...) { standardGeneric("unpersist") })

#' @rdname zipRDD
#' @export
setGeneric("zipRDD", function(x, other) { standardGeneric("zipRDD") })

#' @rdname zipWithIndex
#' @seealso zipWithUniqueId
#' @export
setGeneric("zipWithIndex", function(x) { standardGeneric("zipWithIndex") })

#' @rdname zipWithUniqueId
#' @seealso zipWithIndex
#' @export
setGeneric("zipWithUniqueId", function(x) { standardGeneric("zipWithUniqueId") })


############ Binary Functions #############

#' @rdname countByKey
#' @export
setGeneric("countByKey", function(x) { standardGeneric("countByKey") })

#' @rdname flatMapValues
#' @export
setGeneric("flatMapValues", function(X, FUN) { standardGeneric("flatMapValues") })

#' @rdname keys
#' @export
setGeneric("keys", function(x) { standardGeneric("keys") })

#' @rdname lookup
#' @export
setGeneric("lookup", function(x, key) { standardGeneric("lookup") })

#' @rdname mapValues
#' @export
setGeneric("mapValues", function(X, FUN) { standardGeneric("mapValues") })

#' @rdname values
#' @export
setGeneric("values", function(x) { standardGeneric("values") })



############ Shuffle Functions ############

#' @rdname aggregateByKey
#' @seealso foldByKey, combineByKey
#' @export
setGeneric("aggregateByKey",
           function(x, zeroValue, seqOp, combOp, numPartitions) {
             standardGeneric("aggregateByKey")
           })

#' @rdname cogroup
#' @export
setGeneric("cogroup",
           function(..., numPartitions) {
             standardGeneric("cogroup")
           },
           signature = "...")

#' @rdname combineByKey
#' @seealso groupByKey, reduceByKey
#' @export
setGeneric("combineByKey",
           function(x, createCombiner, mergeValue, mergeCombiners, numPartitions) {
             standardGeneric("combineByKey")
           })

#' @rdname foldByKey
#' @seealso aggregateByKey, combineByKey
#' @export
setGeneric("foldByKey",
           function(x, zeroValue, func, numPartitions) {
             standardGeneric("foldByKey")
           })

#' @rdname join-methods
#' @export
setGeneric("fullOuterJoin", function(x, y, numPartitions) { standardGeneric("fullOuterJoin") })

#' @rdname groupByKey
#' @seealso reduceByKey
#' @export
setGeneric("groupByKey", function(x, numPartitions) { standardGeneric("groupByKey") })

#' @rdname join-methods
#' @export
setGeneric("join", function(x, y, ...) { standardGeneric("join") })

#' @rdname join-methods
#' @export
setGeneric("leftOuterJoin", function(x, y, numPartitions) { standardGeneric("leftOuterJoin") })

#' @rdname partitionBy
#' @export
setGeneric("partitionBy", function(x, numPartitions, ...) { standardGeneric("partitionBy") })

#' @rdname reduceByKey
#' @seealso groupByKey
#' @export
setGeneric("reduceByKey", function(x, combineFunc, numPartitions) { standardGeneric("reduceByKey")})

#' @rdname reduceByKeyLocally
#' @seealso reduceByKey
#' @export
setGeneric("reduceByKeyLocally",
           function(x, combineFunc) {
             standardGeneric("reduceByKeyLocally")
           })

#' @rdname join-methods
#' @export
setGeneric("rightOuterJoin", function(x, y, numPartitions) { standardGeneric("rightOuterJoin") })

#' @rdname sortByKey
#' @export
setGeneric("sortByKey", function(x, ascending = TRUE, numPartitions = 1L) {
  standardGeneric("sortByKey")
})


################### Broadcast Variable Methods #################

#' @rdname broadcast
#' @export
setGeneric("value", function(bcast) { standardGeneric("value") })



####################  DataFrame Methods ########################

#' @rdname schema
#' @export
setGeneric("columns", function(x) {standardGeneric("columns") })

#' @rdname schema
#' @export
setGeneric("dtypes", function(x) { standardGeneric("dtypes") })

#' @rdname explain
#' @export
setGeneric("explain", function(x, ...) { standardGeneric("explain") })

#' @rdname filter
#' @export
setGeneric("filter", function(x, condition) { standardGeneric("filter") })

#' @rdname DataFrame
#' @export
setGeneric("groupBy", function(x, ...) { standardGeneric("groupBy") })

#' @rdname insertInto
#' @export
setGeneric("insertInto", function(x, tableName, ...) { standardGeneric("insertInto") })

#' @rdname intersect
#' @export
setGeneric("intersect", function(x, y) { standardGeneric("intersect") })

#' @rdname isLocal
#' @export
setGeneric("isLocal", function(x) { standardGeneric("isLocal") })

#' @rdname limit
#' @export
setGeneric("limit", function(x, num) {standardGeneric("limit") })

#' @rdname sortDF
#' @export
setGeneric("orderBy", function(x, col) { standardGeneric("orderBy") })

#' @rdname schema
#' @export
setGeneric("printSchema", function(x) { standardGeneric("printSchema") })

#' @rdname registerTempTable
#' @export
setGeneric("registerTempTable", function(x, tableName) { standardGeneric("registerTempTable") })

#' @rdname sampleDF
#' @export
setGeneric("sampleDF",
           function(x, withReplacement, fraction, seed) {
             standardGeneric("sampleDF")
          })

#' @rdname saveAsParquetFile
#' @export
setGeneric("saveAsParquetFile", function(x, path) { standardGeneric("saveAsParquetFile") })

#' @rdname saveAsTable
#' @export
setGeneric("saveAsTable", function(df, tableName, source, mode, ...) {
  standardGeneric("saveAsTable")
})

#' @rdname saveAsTable
#' @export
setGeneric("saveDF", function(df, path, source, mode, ...) { standardGeneric("saveDF") })

#' @rdname schema
#' @export
setGeneric("schema", function(x) { standardGeneric("schema") })

#' @rdname select
#' @export
setGeneric("select", function(x, col, ...) { standardGeneric("select") } )

#' @rdname select
#' @export
setGeneric("selectExpr", function(x, expr, ...) { standardGeneric("selectExpr") })

#' @rdname showDF
#' @export
setGeneric("showDF", function(x,...) { standardGeneric("showDF") })

#' @rdname sortDF
#' @export
setGeneric("sortDF", function(x, col, ...) { standardGeneric("sortDF") })

#' @rdname subtract
#' @export
setGeneric("subtract", function(x, y) { standardGeneric("subtract") })

#' @rdname tojson
#' @export
setGeneric("toJSON", function(x) { standardGeneric("toJSON") })

#' @rdname DataFrame
#' @export
setGeneric("toRDD", function(x) { standardGeneric("toRDD") })

#' @rdname unionAll
#' @export
setGeneric("unionAll", function(x, y) { standardGeneric("unionAll") })

#' @rdname filter
#' @export
setGeneric("where", function(x, condition) { standardGeneric("where") })

#' @rdname withColumn
#' @export
setGeneric("withColumn", function(x, colName, col) { standardGeneric("withColumn") })

#' @rdname withColumnRenamed
#' @export
setGeneric("withColumnRenamed", function(x, existingCol, newCol) {
  standardGeneric("withColumnRenamed") })


###################### Column Methods ##########################

#' @rdname column
#' @export
setGeneric("approxCountDistinct", function(x, ...) { standardGeneric("approxCountDistinct") })

#' @rdname column
#' @export
setGeneric("asc", function(x) { standardGeneric("asc") })

#' @rdname column
#' @export
setGeneric("avg", function(x, ...) { standardGeneric("avg") })

#' @rdname column
#' @export
setGeneric("cast", function(x, dataType) { standardGeneric("cast") })

#' @rdname column
#' @export
setGeneric("contains", function(x, ...) { standardGeneric("contains") })
#' @rdname column
#' @export
setGeneric("countDistinct", function(x, ...) { standardGeneric("countDistinct") })

#' @rdname column
#' @export
setGeneric("desc", function(x) { standardGeneric("desc") })

#' @rdname column
#' @export
setGeneric("endsWith", function(x, ...) { standardGeneric("endsWith") })

#' @rdname column
#' @export
setGeneric("getField", function(x, ...) { standardGeneric("getField") })

#' @rdname column
#' @export
setGeneric("getItem", function(x, ...) { standardGeneric("getItem") })

#' @rdname column
#' @export
setGeneric("isNull", function(x) { standardGeneric("isNull") })

#' @rdname column
#' @export
setGeneric("isNotNull", function(x) { standardGeneric("isNotNull") })

#' @rdname column
#' @export
setGeneric("last", function(x) { standardGeneric("last") })

#' @rdname column
#' @export
setGeneric("like", function(x, ...) { standardGeneric("like") })

#' @rdname column
#' @export
setGeneric("lower", function(x) { standardGeneric("lower") })

#' @rdname column
#' @export
setGeneric("rlike", function(x, ...) { standardGeneric("rlike") })

#' @rdname column
#' @export
setGeneric("startsWith", function(x, ...) { standardGeneric("startsWith") })

#' @rdname column
#' @export
setGeneric("sumDistinct", function(x) { standardGeneric("sumDistinct") })

#' @rdname column
#' @export
setGeneric("upper", function(x) { standardGeneric("upper") })
