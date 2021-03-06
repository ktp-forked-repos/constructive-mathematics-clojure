(ns ConstructiveMathematics.integers
  (:refer-clojure :exclude [compare max min zero? range]) ; suppress the shadowing warning
  (:require [clojure.core :as core]) ; allow to still reach clojure.core/compare through core/compare
)

(require '[ConstructiveMathematics.natural-numbers :as natural-numbers])

(def zero {:sign :zero})
(defn positive [n] {:sign :positive, :n n})
(defn negative [n] {:sign :negative, :n n})

(def sign :sign)
(def natural-part :n)
(defn sign-n [i] [(sign i) (natural-part i)])

(defn zero? [i] (= :zero (sign i)))
(defn positive? [i] (= :positive (sign i)))
(defn negative? [i] (= :negative (sign i)))

(defn make-integer [plus minus]
  (let [compare (natural-numbers/compare plus minus)]
    (case compare
      :equal zero
      :greater-than (positive (natural-numbers/subtract plus minus))
      :less-than (negative (natural-numbers/subtract minus plus)))))

(defn successor-of [i]
  (case (sign i)
      :positive (-> i natural-part natural-numbers/successor-of positive)
      :zero (positive natural-numbers/one)
      :negative (let [n (natural-part i)]
                  (if (natural-numbers/one? n)
                    zero
                    (negative (natural-numbers/predecessor-of n)))))) 

(def one (successor-of zero))
(def two (successor-of one))
(def three (successor-of two))
(def four (successor-of three))

(defn negate [i] =
  (let [[sign n] (sign-n i)]
    (case sign
      :positive (negative n)
      :zero zero
      :negative (positive n))))

(defn predecessor-of [i] (negate (successor-of (negate i))))

(def minus-one (predecessor-of zero))
(def minus-two (predecessor-of minus-one))

(defn add [i1 i2]
  (let [[sign n] (sign-n i2)]
    (case sign
      :zero i1
      :positive (add (successor-of i1) (predecessor-of i2))
      :negative (add (predecessor-of i1) (successor-of i2)))))

(defn equal-to [i1 i2]
  (let [[sign1 n1] (sign-n i1)
        [sign2 n2] (sign-n i2)]
    (cond
     (= :positive sign1 sign2) (natural-numbers/equal-to n1 n2)
     (= :negative sign1 sign2) (natural-numbers/equal-to n1 n2)
     (= :zero sign1 sign2) true
     :else false)))

(defn less-than [i1 i2]
  (let [[sign1 n1] (sign-n i1)
        [sign2 n2] (sign-n i2)]
    (case [sign1 sign2]
      [:negative :positive] true
      [:negative :zero] true
      [:zero :positive] true
      [:positive :positive] (natural-numbers/less-than n1 n2)
      [:negative :negative] (natural-numbers/less-than n2 n1)
      false)))

(defn compare [i1 i2]
  (cond
   (equal-to i1 i2) :equal
   (less-than i1 i2) :less-than
   :else :greater-than))

(defn max [i1 i2] (if (less-than i1 i2) i2 i1))
(defn min [i1 i2] (if (less-than i1 i2) i1 i2))

(defn less-than-or-equal-to [i1 i2]
  (not= :greater-than (compare i1 i2)))

(defn subtract [i1 i2]
  (case (sign i2)
      :zero i1
      :positive (subtract (predecessor-of i1) (predecessor-of i2))
      :negative (subtract (successor-of i1) (successor-of i2))))

(defn multiply [i1 i2]
    (case (sign i2)
      :zero zero
      :negative (multiply (negate i1) (negate i2))
      :positive (add i1 (multiply i1 (predecessor-of i2)))))

(defn square [i]
  (multiply i i))

(defn absolute-value [i]
  (if (less-than i zero) (negate i) i))

(defn try-divide [i1 i2] =
  (cond
     (zero? i2) (throw (Exception. "division by zero is not allowed"))
     (negative? i2) (try-divide (negate i1) (negate i2))
     (zero? i1) zero
     (negative? i1) (let [td (try-divide (negate i1) i2)]
                           (if td (negate td)))
     :else ; both positive
       (if (less-than i1 i2)
         nil
         (let [td (try-divide (subtract i1 i2) i2)]
           (if td (successor-of td))))))

(defn divide [i1 i2]
  (let [td (try-divide i1 i2)]
    (or td (throw (Exception. "cannot divide a smaller integer by a larger one")))))

(defn gcd [i1 i2]
    (cond
     (negative? i1) (gcd (negate i1) i2)
     (negative? i2) (gcd i1 (negate i2))
     (zero? i1) i2
     (zero? i2) i1
     ; both are positive
     (natural-numbers/less-than-or-equal-to (natural-part i1) (natural-part i2)) (gcd i1 (subtract i2 i1))
     :else (gcd (subtract i1 i2) i2)))

(defn to-counting [i]
  (if (positive? i)
    (natural-part i)
    (throw (Exception. "Only positive integer can become counting number"))))

(defn modulo [i b]
  (and
    (positive? b)
    (cond
      (zero? i) zero
      (negative? i) (modulo (add i b) b)
      (positive? i) (if (less-than i b) i (modulo (subtract i b) b)))))

(defn is-divisible-by [i1 i2] (zero? (modulo i1 (absolute-value i2))))

(defn is-even [i]
    (case (sign i)
     :zero true
     :negative (is-even (negate i))
     :positive (not (is-even (predecessor-of i)))))

(defn is-odd [i] (not (is-even i)))

(defn range [lo hi]
  (if (less-than hi lo) nil
    (cons lo (lazy-seq (range (successor-of lo) hi)))))

(defn almost-square-root [i]
  (cond
    (negative? i) (throw (Exception. "cannot take square root of a negative number"))
    (zero? i) zero
    :else (let [loop-f (fn loop-f [i']
                         (let [next-i' (successor-of i')]
                           (if (less-than i (square next-i'))
                             i'
                             (loop-f next-i'))))]
            (loop-f one))))

(defn prime? [i]
  (cond
    (zero? i) false
    (negative? i) (prime? (negate i))
    (equal-to i one) false
    :else (not-any? #(is-divisible-by i %) (range two (almost-square-root i)))))

(defn factorial [i]
  (cond 
    (negative? i) (throw (Exception. "cannot factorial a negative number"))
    (zero? i) one
    :else (reduce multiply (range one i))))


(defn all-integers-from [i]
  (lazy-cat [i (negate i)] (all-integers-from (successor-of i))))

(def all-integers 
  (cons zero (all-integers-from one)))

(def all-primes
  (->> natural-numbers/all-naturals
    (map positive)
    (filter prime?)))
