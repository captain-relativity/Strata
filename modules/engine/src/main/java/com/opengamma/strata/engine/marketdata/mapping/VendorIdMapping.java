/**
 * Copyright (C) 2015 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.strata.engine.marketdata.mapping;

import com.opengamma.strata.marketdata.id.MarketDataVendor;
import com.opengamma.strata.marketdata.id.ObservableId;

/**
 * Provides mappings from {@link ObservableId} instances requested by calculations to ID instances that
 * are suitable for querying a market data vendor to get the market data.
 * <p>
 * The {@code StandardId} and the {@link MarketDataVendor} in an {@code ObservableId} are not necessarily related.
 * Therefore it is necessary to get a standard ID that is suitable for the vendor before requesting
 * the market data.
 */
public interface VendorIdMapping {

  /**
   * Returns a mapping that always returns the ID that is passed in.
   *
   * @return a mapping that always returns the ID that is passed in
   */
  public static VendorIdMapping identity() {
    return id -> id;
  }

  /**
   * Returns an observable ID that can be used for looking up the market data in a market data provider.
   *
   * @param id  an observable ID containing a standard ID and a market data vendor
   * @return an observable ID that can be used for looking up the market data in a market data provider
   */
  public abstract ObservableId idForVendor(ObservableId id);
}
