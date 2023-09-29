(ns hangman.core
  (:require
   [clojure.java.io :as jio]
   [clojure.spec.alpha :as s]
   [clojure.string :as str]
   [clojure.test.check.generators :as gen]))

;; --------------------
;; protocols & records
;; --------------------
(defprotocol Player
  (next-guess [player progress]))

(defrecord ChoicesPlayer [choices]
  Player
  (next-guess [_ _]
    (let [guess (first @choices)]
      (swap! choices rest)
      guess)))

;; --------------------
;; specs
;; --------------------
(defonce letters (mapv char (range (int \a) (inc (int \z)))))

(defn- random-letter []
  (rand-nth letters))

(def random-player
  (reify Player
    (next-guess [_ _] (random-letter))))

(defn- choices-player [choices]
  (->ChoicesPlayer (atom choices)))

(defn- alpha-player []
  (choices-player letters))

(defn- freq-player []
  (choices-player (seq "etooinshrdlcumwfgypbvkjxqz")))

(defn- shuffled-player []
  (choices-player (shuffle letters)))

(defn- valid-letter? [c]
  (<= (int \a) (int c) (int \z)))

(defn- player? [p]
  (satisfies? Player p))

(defn- letters-left
  [progress]
  (->> progress (keep #{\_}) count))

(s/def ::player
  (s/with-gen player?
    #(s/gen #{random-player
              shuffled-player
              alpha-player
              freq-player})))
(s/def ::letter (set letters))
(s/def ::word
  (s/with-gen
    (s/and string?
           #(pos? (count %))
           #(every? valid-letter? (seq %)))
    #(gen/fmap
      (fn [letters] (apply str letters))
      (s/gen (s/coll-of ::letter :min-count 1)))))
(s/def ::progress-letter
  (conj (set letters) \_))
(s/def ::progress
  (s/coll-of ::progress-letter :min-count 1))
(s/def ::verbose (s/with-gen boolean? #(s/gen false?)))
(s/def ::score pos-int?)

(s/fdef hangman.core/new-progress
  :args (s/cat :word ::word)
  :ret ::progress
  :fn (fn [{:keys [args ret]}]
        (= (count (:word args)) (count ret) (letters-left ret))))
(s/fdef hangman.core/update-progress
  :args (s/cat :progress ::progress
               :word ::word
               :guess ::letter)
  :ret ::progress
  :fn (fn [{:keys [args ret]}]
        (>= (-> args :progress letters-left)
            (-> ret letters-left))))
(s/fdef hangman.core/complete?
  :args (s/cat :progress ::progress :word ::word)
  :ret boolean?)
(s/fdef hangman.core/game
  :args (s/cat :word ::word :player ::player :opts (s/keys* :opt-un [::verbose]))
  :ret ::score)

;; --------------------
;; core
;; --------------------
(defn new-progress [word]
  (repeat (count word) \_))

(defn update-progress [progress word guess]
  (map #(if (= %1 guess) guess %2) word progress))

(defn complete? [progress word]
  (= progress (seq word)))

(defn report [begin-progress guess end-progress]
  (println)
  (println "You guessed: " guess)
  (if (= begin-progress end-progress)
    (if (some #{guess} end-progress)
      (println "Sorry, you already guessed: " guess)
      (println "Sorry, the word does not contain: " guess))
    (println "The letter " guess " is in the word!"))
  (println "Progress so far: " (apply str end-progress)))

(defn game
  [word player & {:keys [verbose] :or {verbose false}}]
  (when verbose
    (println "You are guessing a word with" (count word) "letters."))
  (loop [progress (new-progress word) guesses 1]
    (let [guess (next-guess player progress)
          progress' (update-progress progress word guess)]
     (when verbose (report progress guess progress'))
     (if (complete? progress' word)
       guesses
       (recur progress' (inc guesses))))))

;; --------------------
;; main
;; --------------------
(defonce available-words
  (with-open [r (jio/reader "words.txt")]
    (->> (line-seq r)
         (filter #(every? valid-letter? %))
         vec)))

(defn random-word []
  (rand-nth available-words))

(defn take-guess []
  (println)
  (println "Enter a letter: ")
  (flush)
  (let [input (.readLine *in*)
        line (str/trim input)]
    (cond
      (str/blank? line) (recur)
      (valid-letter? (first line)) (first line)
      :else (do
              (println "That is not a valid letter!")
              (recur)))))

(def interactive-player
  (reify Player
    (next-guess [_ _] (take-guess))))

(defn -main []
  (game (random-word) interactive-player :verbose true))

