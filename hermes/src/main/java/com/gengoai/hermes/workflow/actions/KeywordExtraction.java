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

package com.gengoai.hermes.workflow.actions;

import com.gengoai.collection.counter.Counter;
import com.gengoai.conversion.Cast;
import com.gengoai.hermes.Types;
import com.gengoai.hermes.corpus.DocumentCollection;
import com.gengoai.hermes.extraction.keyword.*;
import com.gengoai.hermes.workflow.Action;
import com.gengoai.hermes.workflow.ActionDescription;
import com.gengoai.hermes.workflow.Context;
import com.gengoai.stream.MCounterAccumulator;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.kohsuke.MetaInfServices;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class KeywordExtraction implements Action {
   private static final long serialVersionUID = 1L;
   private int N = Integer.MAX_VALUE;
   private KeywordExtractor extractor = new TermKeywordExtractor();
   private boolean keepGlobalCounts = false;

   public static Counter<String> getKeywords(@NonNull Context context) {
      return Cast.as(context.get(Types.KEYWORDS.name()));
   }

   @Override
   public DocumentCollection process(DocumentCollection corpus, Context context) throws Exception {
      extractor.fit(corpus);
      final MCounterAccumulator<String> globalKeywordCounts = keepGlobalCounts
            ? corpus.getStreamingContext().counterAccumulator()
            : null;
      corpus.update("KeywordExtraction", doc -> {
         List<String> keywords = new ArrayList<>(extractor.extract(doc).count().topN(N).items());
         doc.put(Types.KEYWORDS, keywords);
         if (keepGlobalCounts) {
            keywords.forEach(k -> globalKeywordCounts.increment(k, 1));
         }
      });
      if (keepGlobalCounts) {
         context.property(Types.KEYWORDS.name(), globalKeywordCounts.value());
      }
      return corpus;
   }

   public void setAlgorithm(@NonNull String name) {
      switch (name.toLowerCase()) {
         case "tfidf":
            this.extractor = new TFIDFKeywordExtractor();
            break;
         case "tf":
            this.extractor = new TermKeywordExtractor();
            break;
         case "rake":
            this.extractor = new RakeKeywordExtractor();
            break;
         case "np":
            this.extractor = new NPClusteringKeywordExtractor();
            break;
         case "text-rank":
            this.extractor = new TextRank();
            break;
         default:
            throw new IllegalArgumentException(String.format("Unknown keyword extraction algorithm: '%s'", name));
      }
   }

   @MetaInfServices
   public static class KeywordExtractionDescription implements ActionDescription {
      @Override
      public String description() {
         return "Extracts keywords from documents storing them both on the document using the 'KEYWORD' attribute and on the context using 'KEYWORDS'. " +
               "Note that keywords are only kept on the context if the property 'keepGlobalCounts' is true (it is 'false' by default). " +
               "Additionally, you can specify the maximum number of keywords per document by setting the parameter 'n'. " +
               "The keyword extraction algorithm can be set via JSON using either 'algorithm' or 'extractor' as follows:" +
               "\n\nVia Workflow Json:\n" +
               "--------------------------------------\n" +
               "{\n" +
               "   \"@type\"=\"" + KeywordExtraction.class.getName() + "\",\n" +
               "   \"algorithm\"=\"tfidf\"|\"tf\"|\"rake\"|\"np\"|\"text-rank\"\n," +
               "   \"extactor\"={EXTRACTOR DEFINITION}\n," +
               "   \"n\"=NUMBER\n," +
               "   \"keepGlobalCounts\"=true|false\n" +
               "}";
      }

      @Override
      public String name() {
         return KeywordExtraction.class.getName();
      }
   }

}//END OF KeywordExtraction
