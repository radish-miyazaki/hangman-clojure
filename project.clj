(defproject hangman "0.1.0-SNAPSHOT"
  :description "Hagman game in Clojure"
  :url "https://github.com/radish-miyazaki/hangman-clojure"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/test.check "1.1.1"]]
  :main hangman.core
  :repl-options {:init-ns hangman.core})

