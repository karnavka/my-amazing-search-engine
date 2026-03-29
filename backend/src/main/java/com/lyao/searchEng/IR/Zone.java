package com.lyao.searchEng.IR;
 public enum Zone {
        BODY(0.3f),
        AUTHOR (0.2f), 
        TITLE(0.5f);

        public final float weight;
        private Zone(float weight) {
            this.weight = weight;
         }
         public float getWeight() {
              return weight;
         }
     };