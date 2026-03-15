package com.lyao.searchEng.IR;
 public enum Zone {
        BODY(0.5f),
        AUTHOR (0.3f), 
        TITLE(0.2f);

        public final float weight;
        private Zone(float weight) {
            this.weight = weight;
         }
         public float getWeight() {
              return weight;
         }
     };