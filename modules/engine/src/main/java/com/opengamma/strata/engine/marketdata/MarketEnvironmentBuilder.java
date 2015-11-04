/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata;

import static java.util.stream.Collectors.toMap;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.opengamma.strata.basics.market.MarketDataId;
import com.opengamma.strata.basics.market.ObservableId;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.Messages;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.engine.marketdata.scenario.MarketDataBox;

/**
 * A mutable builder for building up {@link MarketEnvironment} instances.
 */
public final class MarketEnvironmentBuilder {

  /** The valuation date associated with the market data. */
  private MarketDataBox<LocalDate> valuationDate = MarketDataBox.empty();

  /** The number of scenarios for which this builder contains market data. */
  private Integer scenarioCount;

  /** The single value market data items, keyed by ID. */
  private final Map<MarketDataId<?>, MarketDataBox<?>> values = new HashMap<>();

  /** Time series of observable market data values, keyed by ID. */
  private final Map<ObservableId, LocalDateDoubleTimeSeries> timeSeries = new HashMap<>();

  /**
   * Creates an empty builder.
   */
  MarketEnvironmentBuilder() {
  }

  /**
   * Creates a builder pre-populated with data.
   *
   * @param valuationDate  the valuation date associated with the market data
   * @param scenarioCount  the number of scenarios for which this builder contains market data
   * @param values  the single value market data items, keyed by ID
   * @param timeSeries  time series of observable market data values, keyed by ID
   */
  MarketEnvironmentBuilder(
      MarketDataBox<LocalDate> valuationDate,
      int scenarioCount,
      Map<? extends MarketDataId<?>, MarketDataBox<?>> values,
      Map<? extends ObservableId, LocalDateDoubleTimeSeries> timeSeries) {

    this.valuationDate = ArgChecker.notNull(valuationDate, "valuationDate");
    this.scenarioCount = scenarioCount;
    this.values.putAll(values);
    this.timeSeries.putAll(timeSeries);
  }

  /**
   * Adds a single item of market data, replacing any existing value with the same ID.
   *
   * @param id  the ID of the market data
   * @param value  the market data value
   * @param <T>  the type of the market data value
   * @return this builder
   */
  public <T> MarketEnvironmentBuilder addValue(MarketDataId<T> id, T value) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(value, "value");
    values.put(id, MarketDataBox.ofSingleValue(value));
    return this;
  }

  /**
   * Adds multiple items of market data, replacing any existing values with the same IDs.
   *
   * @param values  the items of market data, keyed by ID
   * @return this builder
   */
  public MarketEnvironmentBuilder addValues(Map<? extends MarketDataId<?>, ?> values) {
    ArgChecker.notNull(values, "values");
    Map<? extends MarketDataId<?>, MarketDataBox<Object>> boxedValues = values.entrySet().stream()
        .map(MarketEnvironmentBuilder::checkTypes)
        .collect(toMap(e -> e.getKey(), e -> MarketDataBox.ofSingleValue(e.getValue())));
    this.values.putAll(boxedValues);
    return this;
  }

  /**
   * Adds multiple items of market data, replacing any existing values with the same IDs.
   *
   * @param values  the items of market data, keyed by ID
   * @return this builder
   */
  public MarketEnvironmentBuilder addBoxedValues(Map<? extends MarketDataId<?>, ? extends MarketDataBox<?>> values) {
    ArgChecker.notNull(values, "values");
    values.entrySet().forEach(e -> {
      checkType(e.getKey(), e.getValue());
      updateScenarioCount(e.getValue());
    });
    this.values.putAll(values);
    return this;
  }

  /**
   * Adds a time series of observable market data values, replacing any existing time series with the same ID.
   *
   * @param id  the ID of the values
   * @param timeSeries  a time series of observable market data values
   * @return this builder
   */
  public MarketEnvironmentBuilder addTimeSeries(ObservableId id, LocalDateDoubleTimeSeries timeSeries) {
    ArgChecker.notNull(id, "id");
    ArgChecker.notNull(timeSeries, "timeSeries");
    this.timeSeries.put(id, timeSeries);
    return this;
  }

  /**
   * Adds multiple time series of observable market data, replacing any existing time series with the same IDs.
   *
   * @param series  the time series of market data, keyed by ID
   * @return this builder
   */
  public MarketEnvironmentBuilder addTimeSeries(Map<? extends ObservableId, LocalDateDoubleTimeSeries> series) {
    ArgChecker.notNull(series, "series");
    timeSeries.putAll(series);
    return this;
  }

  /**
   * Sets the valuation date associated with the market data, replacing the existing valuation date.
   *
   * @param valuationDate  the valuation date associated with the market data
   * @return this builder
   */
  public MarketEnvironmentBuilder valuationDate(LocalDate valuationDate) {
    ArgChecker.notNull(valuationDate, "valuationDate");
    this.valuationDate = MarketDataBox.ofSingleValue(valuationDate);
    return this;
  }

  /**
   * Sets the valuation date associated with the market data, replacing the existing valuation date.
   *
   * @param valuationDate  the valuation date associated with the market data
   * @return this builder
   */
  public MarketEnvironmentBuilder valuationDate(MarketDataBox<LocalDate> valuationDate) {
    ArgChecker.notNull(valuationDate, "valuationDate");

    if (valuationDate.getScenarioCount() == 0) {
      throw new IllegalArgumentException("Valuation date must not be empty");
    }
    updateScenarioCount(valuationDate);
    this.valuationDate = valuationDate;
    return this;
  }

  /**
   * Builds a set of market data from the data in this builder.
   * <p>
   * It is possible to continue to add more data to a builder after calling {@code build()}. Any
   * {@code BaseMarketData} instances built previously will be unaffected.
   *
   * @return a set of market data from the data in this builder
   */
  public MarketEnvironment build() {
    // If scenarioCount is null it means all market data boxes have single values.
    if (scenarioCount == null) {
      scenarioCount = 1;
    }
    return new MarketEnvironment(valuationDate, scenarioCount, values, timeSeries);
  }

  private static Map.Entry<? extends MarketDataId<?>, ?> checkTypes(Map.Entry<? extends MarketDataId<?>, ?> entry) {
    if (!entry.getKey().getMarketDataType().isInstance(entry.getValue())) {
      throw new IllegalArgumentException(
          Messages.format(
              "Market data value {} does not match the type of the key {}",
              entry.getValue(),
              entry.getKey()));
    }
    return entry;
  }

  private static void checkType(MarketDataId<?> id, MarketDataBox<?> box) {
    if (!id.getMarketDataType().isAssignableFrom(box.getMarketDataType())) {
      throw new IllegalArgumentException(
          Messages.format(
              "Market data type {} of value {} is not compatible with the market data type of the ID {}",
              box.getMarketDataType().getName(),
              box,
              id.getMarketDataType().getName()));
    }
  }

  private void updateScenarioCount(MarketDataBox<?> box) {
    // If the box has a single value then it can be used with any number of scenarios - the same value is used
    // for all scenarios.
    if (box.isSingleValue()) {
      return;
    }
    int scenarioCount = box.getScenarioCount();

    if (this.scenarioCount == null || this.scenarioCount == 1) {
      this.scenarioCount = scenarioCount;
      return;
    }
    if (scenarioCount != this.scenarioCount) {
      throw new IllegalArgumentException(
          Messages.format(
              "Cannot add value {} with {} scenarios to an environment with {} scenarios",
              box,
              scenarioCount,
              this.scenarioCount));
    }
  }
}
