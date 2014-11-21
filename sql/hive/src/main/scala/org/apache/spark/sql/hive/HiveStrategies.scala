/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.hive

import org.apache.hadoop.conf.Configuration
import org.apache.hadoop.mapreduce.Job
import org.apache.hadoop.hive.ql.parse.ASTNode

import org.apache.spark.annotation.Experimental
import org.apache.spark.Logging
import org.apache.spark.sql.catalyst.analysis.UnresolvedAttribute
import org.apache.spark.sql.catalyst.expressions._
import org.apache.spark.sql.catalyst.expressions.codegen.GeneratePredicate
import org.apache.spark.sql.catalyst.planning._
import org.apache.spark.sql.catalyst.plans._
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.catalyst.types.StringType
import org.apache.spark.sql.execution.{DescribeCommand, OutputFaker, SparkPlan}
import org.apache.spark.sql.hive
import org.apache.spark.sql.hive.execution._
import org.apache.spark.sql.hive.orc._
import org.apache.spark.sql.parquet.ParquetRelation
import org.apache.spark.sql.{SQLContext, SchemaRDD, Strategy}

import scala.collection.JavaConversions._

private[hive] trait HiveStrategies {
  // Possibly being too clever with types here... or not clever enough.
  self: SQLContext#SparkPlanner =>

  val hiveContext: HiveContext

  /**
   * :: Experimental ::
   * Finds table scans that would use the Hive SerDe and replaces them with our own native parquet
   * table scan operator.
   *
   * TODO: Much of this logic is duplicated in HiveTableScan.  Ideally we would do some refactoring
   * but since this is after the code freeze for 1.1 all logic is here to minimize disruption.
   *
   * Other issues:
   *  - Much of this logic assumes case insensitive resolution.
   */
  @Experimental
  object ParquetConversion extends Strategy {
    implicit class LogicalPlanHacks(s: SchemaRDD) {
      def lowerCase =
        new SchemaRDD(s.sqlContext, s.logicalPlan)

      def addPartitioningAttributes(attrs: Seq[Attribute]) = {
        // Don't add the partitioning key if its already present in the data.
        if (attrs.map(_.name).toSet.subsetOf(s.logicalPlan.output.map(_.name).toSet)) {
          s
        } else {
          new SchemaRDD(
            s.sqlContext,
            s.logicalPlan transform {
              case p: ParquetRelation => p.copy(partitioningAttributes = attrs)
            })
        }
      }
    }

    implicit class PhysicalPlanHacks(originalPlan: SparkPlan) {
      def fakeOutput(newOutput: Seq[Attribute]) =
        OutputFaker(
          originalPlan.output.map(a =>
            newOutput.find(a.name.toLowerCase == _.name.toLowerCase)
              .getOrElse(
                sys.error(s"Can't find attribute $a to fake in set ${newOutput.mkString(",")}"))),
          originalPlan)
    }

    def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
      case PhysicalOperation(projectList, predicates, relation: MetastoreRelation)
          if relation.tableDesc.getSerdeClassName.contains("Parquet") &&
             hiveContext.convertMetastoreParquet =>

        // Filter out all predicates that only deal with partition keys
        val partitionsKeys = AttributeSet(relation.partitionKeys)
        val (pruningPredicates, otherPredicates) = predicates.partition {
          _.references.subsetOf(partitionsKeys)
        }

        // We are going to throw the predicates and projection back at the whole optimization
        // sequence so lets unresolve all the attributes, allowing them to be rebound to the
        // matching parquet attributes.
        val unresolvedOtherPredicates = otherPredicates.map(_ transform {
          case a: AttributeReference => UnresolvedAttribute(a.name)
        }).reduceOption(And).getOrElse(Literal(true))

        val unresolvedProjection = projectList.map(_ transform {
          case a: AttributeReference => UnresolvedAttribute(a.name)
        })

        if (relation.hiveQlTable.isPartitioned) {
          val rawPredicate = pruningPredicates.reduceOption(And).getOrElse(Literal(true))
          // Translate the predicate so that it automatically casts the input values to the correct
          // data types during evaluation
          val castedPredicate = rawPredicate transform {
            case a: AttributeReference =>
              val idx = relation.partitionKeys.indexWhere(a.exprId == _.exprId)
              val key = relation.partitionKeys(idx)
              Cast(BoundReference(idx, StringType, nullable = true), key.dataType)
          }

          val inputData = new GenericMutableRow(relation.partitionKeys.size)
          val pruningCondition =
            if(codegenEnabled) {
              GeneratePredicate(castedPredicate)
            } else {
              InterpretedPredicate(castedPredicate)
            }

          val partitions = relation.hiveQlPartitions.filter { part =>
            val partitionValues = part.getValues
            var i = 0
            while (i < partitionValues.size()) {
              inputData(i) = partitionValues(i)
              i += 1
            }
            pruningCondition(inputData)
          }

          hiveContext
            .parquetFile(partitions.map(_.getLocation).mkString(","))
            .addPartitioningAttributes(relation.partitionKeys)
            .lowerCase
            .where(unresolvedOtherPredicates)
            .select(unresolvedProjection:_*)
            .queryExecution
            .executedPlan
            .fakeOutput(projectList.map(_.toAttribute)):: Nil
        } else {
          hiveContext
            .parquetFile(relation.hiveQlTable.getDataLocation.toString)
            .lowerCase
            .where(unresolvedOtherPredicates)
            .select(unresolvedProjection:_*)
            .queryExecution
            .executedPlan
            .fakeOutput(projectList.map(_.toAttribute)) :: Nil
        }
      case _ => Nil
    }
  }

  object Scripts extends Strategy {
    def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
      case logical.ScriptTransformation(input, script, output, child) =>
        ScriptTransformation(input, script, output, planLater(child))(hiveContext) :: Nil
      case _ => Nil
    }
  }

  object DataSinks extends Strategy {
    def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
      case logical.InsertIntoTable(table: MetastoreRelation, partition, child, overwrite) =>
        execution.InsertIntoHiveTable(
          table, partition, planLater(child), overwrite)(hiveContext) :: Nil
      case hive.InsertIntoHiveTable(table: MetastoreRelation, partition, child, overwrite) =>
        execution.InsertIntoHiveTable(
          table, partition, planLater(child), overwrite)(hiveContext) :: Nil
      case logical.CreateTableAsSelect(
             Some(database), tableName, child, allowExisting, Some(extra: ASTNode)) =>
        CreateTableAsSelect(
          database,
          tableName,
          child,
          allowExisting,
          extra) :: Nil
      case _ => Nil
    }
  }

  /**
   * Retrieves data using a HiveTableScan.  Partition pruning predicates are also detected and
   * applied.
   */
  object HiveTableScans extends Strategy {
    def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
      case PhysicalOperation(projectList, predicates, relation: MetastoreRelation) =>
        // Filter out all predicates that only deal with partition keys, these are given to the
        // hive table scan operator to be used for partition pruning.
        val partitionKeyIds = AttributeSet(relation.partitionKeys)
        val (pruningPredicates, otherPredicates) = predicates.partition {
          _.references.subsetOf(partitionKeyIds)
        }

        pruneFilterProject(
          projectList,
          otherPredicates,
          identity[Seq[Expression]],
          HiveTableScan(_, relation, pruningPredicates.reduceLeftOption(And))(hiveContext)) :: Nil
      case _ =>
        Nil
    }
  }

  case class HiveCommandStrategy(context: HiveContext) extends Strategy {
    def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
      case logical.NativeCommand(sql) => NativeCommand(sql, plan.output)(context) :: Nil

      case hive.DropTable(tableName, ifExists) => execution.DropTable(tableName, ifExists) :: Nil

      case hive.AddJar(path) => execution.AddJar(path) :: Nil

      case hive.AddFile(path) => execution.AddFile(path) :: Nil

      case hive.AnalyzeTable(tableName) => execution.AnalyzeTable(tableName) :: Nil

      case describe: logical.DescribeCommand =>
        val resolvedTable = context.executePlan(describe.table).analyzed
        resolvedTable match {
          case t: MetastoreRelation =>
            Seq(DescribeHiveTableCommand(t, describe.output, describe.isExtended)(context))
          case o: LogicalPlan =>
            Seq(DescribeCommand(planLater(o), describe.output)(context))
        }

      case _ => Nil
    }
  }

  object OrcOperations extends Strategy with Logging{
    def apply(plan: LogicalPlan): Seq[SparkPlan] = plan match {
      case WriteToOrcFile(path, child) =>
        val relation =
          OrcRelation.create(path, child, sparkContext.hadoopConfiguration, sqlContext)
        // Note: overwrite=false following parquet convention, although we don't have
        // this limitation
        InsertIntoOrcTable(relation, planLater(child), overwrite = false) :: Nil
      case logical.InsertIntoTable(table: OrcRelation, partition, child, overwrite) =>
        InsertIntoOrcTable(table, planLater(child), overwrite) :: Nil
      case PhysicalOperation(projectList, filters, relation: OrcRelation) =>
        val prunePushedDownFilters = {
          OrcRelation.jobConf =  sparkContext.hadoopConfiguration
          if (ORC_FILTER_PUSHDOWN_ENABLED) {
            val job = new Job(OrcRelation.jobConf)
            val conf: Configuration = job.getConfiguration
            logInfo("Orc push down filter enabled:" + filters)
            // We allow partial pushdown on the top level

            (filters: Seq[Expression]) => {
              val recordFilter = OrcFilters.createFilter(filters)
              if (recordFilter.isDefined) {

                logInfo("Parsed filters:" + recordFilter)
                /**
                 * To test it, we can set follows so that the reader w
                 * ill not read whole file if small
                 * sparkContext.hadoopConfiguration.setInt(
                 * "mapreduce.input.fileinputformat.split.maxsize", 50)
                 */
                conf.set(SARG_PUSHDOWN, toKryo(recordFilter.get))
                conf.setBoolean("hive.optimize.index.filter", true)
                conf.setInt("mapreduce.input.fileinputformat.split.maxsize", 50)
                OrcRelation.jobConf = conf
              }
              // no matter whether it is filtered or not in orc,
              // we need to do more fine grained filter
              // in the upper layer, return all of them
              filters
            }
          } else {
            identity[Seq[Expression]] _
          }
        }
        pruneFilterProject(
          projectList,
          filters,
          prunePushedDownFilters,
          OrcTableScan(_, relation, None)) :: Nil
      case x => Nil
    }
  }
}
