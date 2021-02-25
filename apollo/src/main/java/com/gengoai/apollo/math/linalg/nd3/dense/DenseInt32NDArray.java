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

package com.gengoai.apollo.math.linalg.nd3.dense;

import com.gengoai.apollo.math.linalg.nd3.NDArray;
import com.gengoai.apollo.math.linalg.nd3.Shape;
import com.gengoai.conversion.Cast;
import lombok.NonNull;
import org.tensorflow.DataType;
import org.tensorflow.Tensor;

public class DenseInt32NDArray extends NDArray<Integer> {
   private static final long serialVersionUID = 1L;
   private int[][] data;

   public DenseInt32NDArray(@NonNull Shape shape) {
      super(shape);
      this.data = new int[shape.sliceLength()][shape.matrixLength()];
   }

   public DenseInt32NDArray(@NonNull int[] v) {
      super(Shape.shape(v.length));
      this.data = new int[1][v.length];
      System.arraycopy(v, 0, this.data[0], 0, v.length);
   }

   public DenseInt32NDArray(@NonNull Shape shape, @NonNull int[] v) {
      super(shape);
      this.data = new int[1][v.length];
      System.arraycopy(v, 0, this.data[0], 0, v.length);
   }

   public DenseInt32NDArray(@NonNull int[][] v) {
      super(Shape.shape(v.length, v[0].length));
      this.data = new int[1][shape().matrixLength()];
      for (int row = 0; row < v.length; row++) {
         for (int col = 0; col < v[row].length; col++) {
            set(row,col, v[row][col]);
         }
      }
   }

   public DenseInt32NDArray(@NonNull int[][][] v) {
      super(Shape.shape(v.length, v[0].length, v[0][0].length));
      this.data = new int[shape().sliceLength()][shape().matrixLength()];
      for (int channel = 0; channel < v.length; channel++) {
         for (int row = 0; row < v[channel].length; row++) {
            for (int col = 0; col < v[channel][row].length; col++) {
               set(channel,row,col, v[channel][row][col]);
            }
         }
      }
   }

   public DenseInt32NDArray(@NonNull int[][][][] v) {
      super(Shape.shape(v.length, v[0].length, v[0][0].length, v[0][0][0].length));
      this.data = new int[shape().sliceLength()][shape().matrixLength()];
      for (int kernel = 0; kernel < v.length; kernel++) {
         for (int channel = 0; channel < v[kernel].length; channel++) {
            for (int row = 0; row < v[kernel][channel].length; row++) {
               for (int col = 0; col < v[kernel][channel][row].length; col++) {
                  set(kernel,channel,row,col, v[kernel][channel][row][col]);
               }
            }
         }
      }
   }

   public static NDArray<Integer> fromTensor(@NonNull Tensor<?> tensor) {
      if (tensor.dataType() == DataType.INT32) {
         Shape s = Shape.shape(tensor.shape());
         switch (s.rank()) {
            case 1:
               return new DenseInt32NDArray(tensor.copyTo(new int[s.columns()]));
            case 2:
               return new DenseInt32NDArray(tensor.copyTo(new int[s.rows()][s.columns()]));
            case 3:
               return new DenseInt32NDArray(tensor.copyTo(new int[s.channels()][s.rows()][s.columns()]));
            default:
               return new DenseInt32NDArray(tensor.copyTo(new int[s.kernels()][s.channels()][s.rows()][s
                     .columns()]));
         }
      }
      throw new IllegalArgumentException("Unsupported type '" + tensor.dataType().name() + "'");
   }

   @Override
   public Integer get(int kernel, int channel, int row, int col) {
      return data[shape().calculateSliceIndex(kernel, channel)][shape().calculateMatrixIndex(row, col)];
   }


   @Override
   public double getDouble(int kernel, int channel, int row, int col) {
      return data[shape().calculateSliceIndex(kernel, channel)][shape().calculateMatrixIndex(row, col)];
   }

   @Override
   public Class<?> getType() {
      return Integer.class;
   }

   @Override
   public boolean isDense() {
      return true;
   }

   @Override
   public boolean isNumeric() {
      return true;
   }

   @Override
   public NDArray<Integer> reshape(@NonNull Shape newShape) {
      if (shape().length() != newShape.length()) {
         throw new IllegalArgumentException("Cannot change the total number of elements from " +
                                                  shape().length() +
                                                  " to " +
                                                  newShape.length() +
                                                  " on reshape");
      }
      int[][] temp = new int[newShape.sliceLength()][newShape.matrixLength()];
      for (int i = 0; i < length(); i++) {
         double v = getDouble(i);
         int sliceIndex = newShape.toSliceIndex(i);
         int matrixIndex = newShape.toMatrixIndex(i);
         temp[sliceIndex][matrixIndex] = (int) v;
      }
      this.data = temp;
      shape().reshape(newShape);
      return this;
   }


   @Override
   public NDArray<Integer> set(int kernel, int channel, int row, int col, @NonNull Integer value) {
      data[shape().calculateSliceIndex(kernel, channel)][shape().calculateMatrixIndex(row, col)] = value;
      return this;
   }

   @Override
   public NDArray<Integer> set(int kernel, int channel, int row, int col, double value) {
      data[shape().calculateSliceIndex(kernel, channel)][shape().calculateMatrixIndex(row, col)] = (int) value;
      return this;
   }

   @Override
   public NDArray<Integer> setSlice(int index, @NonNull NDArray<Integer> slice) {
      if (!slice.shape().equals(shape().matrixShape())) {
         throw new IllegalArgumentException("Unable to set slice of different shape");
      }
      if (slice instanceof DenseInt32NDArray) {
         DenseInt32NDArray m = Cast.as(slice);
         System.arraycopy(m.data[0], 0, data[index], 0, (int)slice.length());
         return this;
      }
      return super.setSlice(index, slice);
   }

   @Override
   public NDArray<Integer> slice(int index) {
      DenseInt32NDArray v = new DenseInt32NDArray(Shape.shape(shape().rows(),
                                                              shape().columns()));
      v.data[0] = data[index];
      return v;
   }

   @Override
   public NDArray<Integer> slice(int startKernel, int startChannel, int endKernel, int endChannel) {
      Shape os = toSliceShape(startKernel, startChannel, endKernel, endChannel);
      DenseInt32NDArray v = new DenseInt32NDArray(os);
      for (int kernel = startKernel; kernel < endKernel; kernel++) {
         for (int channel = startChannel; channel < endChannel; channel++) {
            int ti = shape().calculateSliceIndex(kernel, channel);
            int oi = os.calculateSliceIndex(kernel - startKernel, channel - startChannel);
            v.data[oi] = data[ti];
         }
      }
      return v;
   }

}
