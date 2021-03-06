/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.gengoai.apollo.ml.data.sampling;

import com.gengoai.apollo.ml.DataSet;
import com.gengoai.apollo.ml.Datum;
import com.gengoai.apollo.ml.InMemoryDataSet;
import com.gengoai.apollo.ml.observation.Variable;
import com.gengoai.collection.counter.Counter;
import com.gengoai.stream.MStream;
import lombok.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class OverSampling extends BaseObservationDataSetSampler implements Serializable {
   private static final long serialVersionUID = 1L;

   public OverSampling(@NonNull String observationName) {
      super(observationName);
   }

   @Override
   public DataSet sample(@NonNull DataSet dataSet) {
      Counter<String> fCount = calculateClassDistribution(dataSet);
      int targetCount = (int) fCount.minimumCount();
      List<Datum> outputData = new ArrayList<>();
      for(Object label : fCount.items()) {
         MStream<Datum> fStream = dataSet.stream()
                                         .filter(e -> e.get(getObservationName())
                                                       .getVariableSpace()
                                                       .map(Variable::getName)
                                                       .anyMatch(label::equals))
                                         .map(Datum::copy)
                                         .cache();

         int count = (int) fStream.count();
         int curCount = 0;

         while(curCount + count < targetCount) {
            fStream.forEach(outputData::add);
            curCount += count;
         }

         if(curCount < targetCount) {
            fStream.sample(false, targetCount - curCount)
                   .forEach(outputData::add);
         } else if(count == targetCount) {
            fStream.forEach(outputData::add);
         }
      }
      return new InMemoryDataSet(outputData, dataSet.getMetadata(), dataSet.getNDArrayFactory());
   }
}//END OF OverSampling
