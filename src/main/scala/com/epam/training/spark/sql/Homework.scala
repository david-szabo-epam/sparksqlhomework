package com.epam.training.spark.sql

import org.apache.spark.sql.{Column, DataFrame, Row}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkConf, SparkContext}

object Homework {
  val DELIMITER = ";"
  val RAW_BUDAPEST_DATA = "data/budapest_daily_1901-2010.csv"
  val OUTPUT_DUR = "output"

  def main(args: Array[String]): Unit = {
    val sparkConf: SparkConf = new SparkConf()
      .setAppName("EPAM BigData training Spark SQL homework")
      .setIfMissing("spark.master", "local[2]")
      .setIfMissing("spark.sql.shuffle.partitions", "10")
    val sc = new SparkContext(sparkConf)
    val sqlContext = new HiveContext(sc)

    processData(sqlContext)

    sc.stop()

  }

  def processData(sqlContext: HiveContext): Unit = {

    /**
      * Task 1
      * Read csv data with DataSource API from provided file
      * Hint: schema is in the Constants object
      */
    val climateDataFrame: DataFrame = readCsvData(sqlContext, Homework.RAW_BUDAPEST_DATA)

    /**
      * Task 2
      * Find errors or missing values in the data
      * Hint: try to use udf for the null check
      */
    val errors: Array[Row] = findErrors(climateDataFrame)
    println(errors)

    /**
      * Task 3
      * List average temperature for a given day in every year
      */
    val averageTemeperatureDataFrame: DataFrame = averageTemperature(climateDataFrame, 1, 2)

    /**
      * Task 4
      * Predict temperature based on mean temperature for every year including 1 day before and after
      * For the given month 1 and day 2 (2nd January) include days 1st January and 3rd January in the calculation
      * Hint: if the dataframe contains a single row with a single double value you can get the double like this "df.first().getDouble(0)"
      */
    val predictedTemperature: Double = predictTemperature(climateDataFrame, 1, 2)
    println(s"Predicted temperature: $predictedTemperature")

  }

  def readCsvData(sqlContext: HiveContext, rawDataPath: String): DataFrame = {
    sqlContext.read
      .option("header", true)
      .option("delimiter", ";")
      .schema(Constants.CLIMATE_TYPE)
      .csv(rawDataPath)

  }

  def findErrors(climateDataFrame: DataFrame): Array[Row] = {
    def countNulls(column: Column) = {
      count(lit(1)) - count(column)
    }

    climateDataFrame
      .select(
        countNulls(col("observation_date")),
        countNulls(col("mean_temperature")),
        countNulls(col("max_temperature")),
        countNulls(col("min_temperature")),
        countNulls(col("precipitation_mm")),
        countNulls(col("precipitation_type")),
        countNulls(col("sunshine_hours"))
      ).collect()
  }

  def averageTemperature(climateDataFrame: DataFrame, monthNumber: Int, dayOfMonth: Int): DataFrame = {
    climateDataFrame
      .filter(onDay(col("observation_date"), monthNumber, dayOfMonth))
      .select(col("mean_temperature"))
  }

  def predictTemperature(climateDataFrame: DataFrame, monthNumber: Int, dayOfMonth: Int): Double = {
    climateDataFrame
      .filter(onDay(col("observation_date"), monthNumber, dayOfMonth)
        .or(onDay(date_add(col("observation_date"), 1), monthNumber, dayOfMonth))
        .or(onDay(date_sub(col("observation_date"), 1), monthNumber, dayOfMonth)))
      .select(sum(col("mean_temperature")) / count(col("mean_temperature"))).first().getDouble(0)
  }

  private def onDay(column: Column, monthNumber: Int, dayOfMonth: Int) = {
    (month(column) === monthNumber).and(dayofmonth(column) === dayOfMonth)
  }


}


