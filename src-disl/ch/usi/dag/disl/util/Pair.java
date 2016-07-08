package ch.usi.dag.disl.util;

/**
 * Created by alexandernorth on 08/07/16.
 */
public final class Pair <F, S> {
        private final F first; //first member of pair
        private final S second; //second member of pair

        public Pair(F first, S second) {
            this.first = first;
            this.second = second;
        }

        public F getFirst() {
            return first;
        }

        public S getSecond() {
            return second;
        }
}
