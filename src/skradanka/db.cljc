(ns skradanka.db
  (:require [datascript.core :as d]
            [clojure.string :as str]))

;;; DB setup

(def schema
  {:trope  {:db/cardinality :db.cardinality/many}
   :trait  {:db/cardinality :db.cardinality/many}
   :values {:db/cardinality :db.cardinality/many}
                                        ; represents a relationship between two characters
   :ship   {:db/valueType :db.type/ref
            :db/cardinality :db.cardinality/many}
   :src    {:db/valueType :db.type/ref}
   :dst    {:db/valueType :db.type/ref}
                                        ; action stuff
   :perp   {:db/valueType :db.type/ref} ; whodunnit?
   :harms  {:db/valueType :db.type/ref
            :db/cardinality :db.cardinality/many}
   :helps  {:db/valueType :db.type/ref
            :db/cardinality :db.cardinality/many}
   :harms-value {:db/cardinality :db.cardinality/many}
   :helps-value {:db/cardinality :db.cardinality/many}

   :position-x {}
   :position-y {}})

(def rules
  '[[(likes ?c1 ?c2)
     [?liking :charge :like] [?liking :src ?c1] [?liking :dst ?c2]]

    [(dislikes ?c1 ?c2)
     [?disliking :charge :dislike] [?disliking :src ?c1] [?disliking :dst ?c2]]

    [(neutral ?c1 ?c2)
     [?neutrality :charge :neutral] [?neutrality :src ?c1] [?neutrality :dst ?c2]]

    [(crush ?c1 ?c2)
     [?crush :secret-crush? true] [?crush :src ?c1] [?crush :dst ?c2]]

    [(jealous-of ?envier ?envied ?crushee)
     [?crush :secret-crush? true] [?crush :src ?envier] [?crush :dst ?crushee]
     [?liking :charge :like] [?liking :src ?crushee] [?liking :dst ?envied]
     [(not= ?envied ?envier)] ; you probably shouldn't be jealous of yourself
     ]

    [(harmed-by ?c1 ?c2)
     [?harm :harms ?c1] [?harm :perp ?c2]]

    [(helped-by ?c1 ?c2)
     [?help :helps ?c1] [?help :perp ?c2]]

    [(holds-value-harmed-by ?c1 ?c2)
     [?c1 :values ?v] [?harm :harms-value ?v] [?harm :perp ?c2]]

    [(holds-value-helped-by ?c1 ?c2)
     [?c1 :values ?v] [?help :helps-value ?v] [?help :perp ?c2]]

    [(motive-to-harm ?c1 ?c2)
     (or (dislikes ?c1 ?c2)
         (jealous-of ?c1 ?c2 _)
         (harmed-by ?c1 ?c2)
         (holds-value-harmed-by ?c1 ?c2))]

    [(motive-to-help ?c1 ?c2)
     (or (likes ?c1 ?c2)
         (helped-by ?c1 ?c2)
         (holds-value-helped-by ?c1 ?c2)
         (and [?c1 :values :communalism]
              (neutral ?c1 ?c2)))]

    [(scientist ?c)
     (or [?c :role :professor]
         [?c :role :student]
         [?c :role :astronomer]
         [?c :role :archivist])]

    ;; TODO not sure if this explicit approach is really needed,
    ;; but (not (scientist ?c)) was complaining about unbound vars
    [(nonscientist ?c)
     (or [?c :role :observatory-caretaker]
         [?c :role :groundskeeper]
         [?c :role :psychic]
         [?c :role :reporter]
         [?c :role :skier]
         [?c :role :volunteer]
         [?c :role :high-schooler]
         [?c :role :giftshopkeep]
         [?c :role :security]
         [?c :role :tourist]
         [?c :role :tour-guide])]

    [(directly-involves ?action ?char)
     (or [?action :perp ?char]
         [?action :harms ?char]
         [?action :helps ?char])

     [(directly-involves-both ?action ?c1 ?c2)
      (directly-involves ?action ?c1)
      (directly-involves ?action ?c2)]]
    ])

(defonce conn
  (d/create-conn schema))

;;; character generation

(def char-names [
                 ;; "Ada" "Adrian" "Alexis" "Alice" "Amber" "Andy" "Audrey"
                 "Aly" "Amy" "Aaron" "Altair" "Amanda" "Amber"
                 "Becca" "Bob" "Brian"
                 ;; "Catherine" "Chris" "Cindy" "Colin"
                 "Cale" "Carl" "Cindy"
                 ;; "Dan" "Dave" "Dorothy"
                 "Dylan" "Donna"
                 "Edgar" "Eliza" "Emily" "Esteban" "Evan" "Evie"
                 "Emily"
                 "Fred"
                 "Georgia" "Grace"
                 "Heather" "Henry"
                 "Isaac" "Isabel" "Ivan"
                 "Jason" "Jess" "Jocelyn" "John" "Josie" "Julie" "Jesus"
                 "Jim"
                 "Karin" "Kate" "Kevin"
                 "Laura" "Lea" "Lex" "Louis"
                 "Leah"
                 "Mark" "Mary" "Maureen"
                 "Megan"
                 "Nathan" "Nick"
                 "Neil Breen"
                 "Omar"
                 "Peter"
                 "Quinn"
                 "Rachel" "Ross"
                 "Robert"
                 "Sarah"
                 ;; "Tracy"
                 "Thgil"
                 "Vi"
                 "Will"
                 "Zed"
                 ])

(def all-values
  ;; from game design doc
  [:science :survival :communalism :funding :comfort :order :faith :progress
   :technoutopianism :theocracy])

(def primary-tropes
  ["Absent-Minded Professor" "Big Fun" "Boss" "Bad-to-the-bone"
   "Parent Figure" "Innocent" "Clown" "Nerd" "Tortured Artist"
   "Gentle Giant" "Scruffy and Gruff" "Regal Presence" "Seductive"
   "Dumb Muscle" "Elderly Master" "Know-it-all" "Strict/By-the-books"
   "Rugged" "Lone Wolf" "Athlete" "Eccentric"])

(def secondary-tropes
  ["Peacemaker" "Pessimist" "Optimist" "Introvert" "Extrovert" "Jerk"
   "Resigned" "Loyalist" "Friend Next Door" "Child" "Outspoken"
   "Mousey/Shy" "Conscience" "Imposter/Pretender" "Side Kick"
   "Astrology Buff" "Socially Awkward/Misses Cues"])

(def all-traits
  ;; from https://github.com/ExpressiveIntelligence/CozyMysteryCo/blob/master/asp/characterGen/identities.lp
  ;; note some duplicates to make them able to show up more than once in a single cast
  ["rich" "in debt" "dying" "important online" "celebrity" "outsider"
   "secret expert" "in hiding" "has funding" "has theory" "has theory"
   "secretly right" "protective of observatory" "protective of observatory"
   "hates this place"])

(def neil-breen-bingo
  ["Someone Drunk",
   "Pool Party",
   "Driving in the Desert",
   ;; "Greenscreen",
   "Magic Neil Breen",
   ;; "Flubbing their line",
   "Skull or Skeleton",
   "Shirtless Neil Breen",
   ;; "Fade effect",
   "Neil Breen Talks to Himself",
   "Terrible People",
   "Hacking",
   "Someone Disapeers",
   "Woman with no Bra",
   ;; "Porno Quality Acting",
   "Ghosts",
   "Ripped Clothes",
   "Someone Turns young",
   "Topless Woman lying face down",
   ;; "Acting Achieves opposite effect",
   "Dead Wife",
   "Clothes on the Ground",
   ;; "Bad Gun Effects",
   "Violence against Laptops",
   "Throwing Stuff",
   "Magic Rock",
   ;; "Shot of Someones feet",
   ;; "Fake Sound Effects",
   "Nonspecific companies",
   ;; "Bad Lip Syncing",
   "Corporate businessmen",
   ;; "Stock Footage",
   ;; "Sound does not Sync",
   "Blood on Neil Breen's Face",
   "Characters Forget something happened",
   ;; "Repurposed Prop",
   ;; "Stock Music",
   ;; "Scene that is leftover footage",
   ;; "Dream Sequences",
   ;; "Actors Forget their lines",
   ;; "Repourposed location",
   "Old Technology",
   ;; "People clearly not in the same shot",
   "Neil Breen is Better than You",
   "Creepy Smile",
   "Lazer Pointer",
   ;; "Neil Breen Credits Himself",
   "Terrible Death Scene"])


(def didaskalia
  [
   "Greenscreen",
   "Flubbing their line",
   "Fade effect",
   "Porno Quality Acting",
   "Acting Achieves opposite effect",
   "Bad Gun Effects",
   "Shot of Someones feet",
   "Fake Sound Effects",
   "Bad Lip Syncing",
   "Stock Footage",
   "Sound does not Sync",
   "Repurposed Prop",
   "Stock Music",
   "Scene that is leftover footage",
   "Dream Sequences",
   "Actors Forget their lines",
   "Repourposed location",
   "People clearly not in the same shot",
   "Neil Breen Credits Himself",
   ])

(defn describe-didaskalia [didaskalium]
  #_(get {"Greenscreen"
        "Flubbing their line"
        ;; "Fade effect"
        "Porno Quality Acting"
        "Acting Achieves opposite effect",
        "Bad Gun Effects",
        "Shot of Someones feet",
        "Fake Sound Effects",
        "Bad Lip Syncing",
        "Stock Footage",
        "Sound does not Sync",
        "Repurposed Prop",
        "Stock Music",
        "Scene that is leftover footage",
        "Dream Sequences",
        "Actors Forget their lines",
        "Repourposed location",
        "People clearly not in the same shot",
        "Neil Breen Credits Himself",}
         didaskalium didaskalium)
  didaskalium)

(defn describe-trait [trait]
  (get {"Shirtless Neil Breen"                 "is shirtless"
        "Lazer Pointer"                        "has a laser pointer in hand"
        "Ghosts"                               "is a ghost"
        "Woman with no Bra"                    "totally has no bra"
        "Throwing Stuff"                       "is throwing stuff"
        "Clothes on the Ground"                "has thrown their clothes on the ground"
        "Neil Breen Talks to Himself"          "is talking to themself"
        "Neil Breen Credits Himself"           "credits themself"
        "Someone Drunk"                        "is drunk"
        "Characters Forget something happened" "forgot what they were doing"
        "Corporate businessmen"                "is a corporate businessperson"
        "Someone Turns young"                  "suddenly seems much younger"
        "Nonspecific companies"                "is from one of those damned evil companies"
        "Terrible Death Scene"                 "is dying horribly"
        "Magic Rock"                           "is cherishing a magical rock"
        "Topless Woman lying face down"        "is topless lying on the ground face down"
        "Blood on Neil Breen's Face"           "has blood on their face"
        "Dead Wife"                            "has just lost a spouse"
        "Creepy Smile"                         "has a creepy smile"
        "Driving in the Desert"                "is driving through the desert"
        "Violence against Laptops"             "is smashing a laptop around"
        "Neil Breen is Better than You"        "thinks they're better than everyone"
        "Skull or Skeleton"                    "is carrying a skull"
        "Magic Neil Breen"                     "is a magical being"
        "Ripped Clothes"                       "has ripped their clothes"
        "Pool Party"                           "is throwing a pool party"
        "Hacking"                              "is hacking into a computer system"
        "Someone Disapeers"                    "has recently disappeared"
        "Terrible People"                      "is a terrible person"
        "Old Technology"                       "uses a lot of retro technology"

        "important online"          "is important online"
        "secretly right"            "is secretly right"
        "secret expert"             "is pretending to be a newbie"
        "dying"                     "is dying"
        "rich"                      "is very rich"
        "protective of observatory" "is afraid of losing their status"
        "has funding"               "wants to invest a lot of money"
        "in debt"                   "is heavily in debt"
        "outsider"                  "is an outsider"
        "celebrity"                 "is a celebrity"
        "has theory"                "has a unifying theory of the world"
        "in hiding"                 "is in hiding"}
       trait trait))



(def all-roles
  ;; from https://github.com/ExpressiveIntelligence/CozyMysteryCo/blob/master/asp/characterGen/identities.lp
  ;; note some duplicates to make them able to show up more than once in a single cast
  [:professor :professor
   :student :student :student :student
   :observatory-caretaker :groundskeeper :psychic :reporter :skier
   :astronomer :volunteer :high-schooler :giftshopkeep :security
   :tourist :tour-guide :archivist])

(def neil-breen-roles
  [:neil-breen-character
   :neil-breen-character
   :neil-breen-character
   :neil-breen-character
   :neil-breen-character
   :neil-breen-character
   :neil-breen-character
   :neil-breen-character
   :neil-breen-character
   :neil-breen-character
   :neil-breen-character
   :neil-breen-character])

(defn gen-ship [id1 id2]
  (let [charge (rand-nth [:like :dislike :neutral])]
    {:type :ship
     :charge charge
     :secret-crush? (and (= charge :like) (< (rand) 0.2))
     :src id1
     :dst id2}))

;; TODO also generate professional & romantic relationships
(defn gen-cast [{:keys [:size]
                 :or   {size 2}}]
  (let [ids      (take size (map (comp - inc) (range)))
        names    (take size (shuffle char-names))
        tropes-a (take size (shuffle primary-tropes))
        tropes-b (take size (shuffle secondary-tropes))
        traits   (take size (shuffle all-traits))
        roles    (take size (shuffle all-roles))
        chars    (mapv #(-> {:type   :char
                             :db/id  %1
                             :name   %2
                             :role   %3
                             :trope  [%4 %5]
                             :trait  [%6]
                             :values (vec (take 2 (shuffle all-values)))})
                       ids names roles tropes-a tropes-b traits)
        pairs    (for [id1 ids id2 ids :when (not= id1 id2)] [id1 id2])
        charges  (map (fn [[id1 id2]] (gen-ship id1 id2)) pairs)]
    (into chars charges)))

(defn gen-cast-neil-breen [{:keys [:size]
                            :or   {size 2}}]
  (let [ids      (take size (map (comp - inc) (range)))
        names    (take size (shuffle char-names))
        tropes-a (take size (shuffle primary-tropes))
        tropes-b (take size (shuffle secondary-tropes))
        traits   (take size (shuffle (into neil-breen-bingo
                                           all-traits)))
        roles    (take size (shuffle (into neil-breen-roles
                                           all-roles)))
        chars    (mapv #(-> {:type   :char
                             :db/id  %1
                             :name   %2
                             :role   %3
                             :trope  [%4 %5]
                             :trait  [%6]
                             :values (vec (take 2 (shuffle all-values)))})
                       ids names roles tropes-a tropes-b traits)
        pairs    (for [id1 ids id2 ids :when (not= id1 id2)] [id1 id2])
        charges  (map (fn [[id1 id2]] (gen-ship id1 id2)) pairs)]
    (into chars charges)))

                                        ;(defn find-professors [db]
                                        ;  (d/q '[:find :))

                                        ;(defn gen-romantic-ship [db]
                                        ;  ())

;;; DB helpers

(defn ->id [db thing]
  (cond
    (integer? thing)
    thing
    (string? thing)
    (d/q '[:find ?c . :in $ ?name :where [?c :name ?name]] db thing)
    (map? thing)
    (:db/id thing)
    :else
    (throw (str "can't convert to id: " (pr-str thing)))))

(defn id->name [db id]
  (:name (d/pull db [:name] id)))

(defn name->id [db name]
  (d/q '[:find ?c . :in $ ?name :where [?c :name ?name]] db name))

(defn entity [db id]
  ;; TODO this doesn't actually work. i cry
  (into {:db/id id} (d/touch (d/entity db id))))

(defn describe-char [db id]
  (let [id       (->id db id)
        ent      (d/pull db [:db/id :name :role :trope :trait :values] id)
        ship-ids (map first (d/q '[:find ?s :in $ ?c :where [?s :src ?c]] db id))
        ships    (map #(d/pull db [:charge :dst] %) ship-ids)
        ships    (group-by :charge ships)]
    (assoc ent
           :likes    (mapv #(id->name db (:db/id (:dst %))) (:like ships))
           :dislikes (mapv #(id->name db (:db/id (:dst %))) (:dislike ships))
           :neutral  (mapv #(id->name db (:db/id (:dst %))) (:neutral ships)))))

(defn all-char-ids [db]
  (map first (d/q '[:find ?c :where [?c :type :char]] db)))

(defn describe-all-chars [db]
  (into [] (for [id (all-char-ids db)]
             (describe-char db id))))

;;; query functions – use these to mine for narratively interesting situations

(defn find-mismatches [db]
  (d/q '[:find ?n1 ?n2 :in $ %
         :where (likes ?c1 ?c2) (dislikes ?c2 ?c1)
         [?c1 :name ?n1] [?c2 :name ?n2]]
       db rules))

(defn find-mutual-likes [db]
  (d/q '[:find ?n1 ?n2 :in $ %
         :where (likes ?c1 ?c2) (likes ?c2 ?c1)
         [?c1 :name ?n1] [?c2 :name ?n2]]
       db rules))

(defn find-mutual-dislikes [db]
  (d/q '[:find ?n1 ?n2 :in $ %
         :where (dislikes ?c1 ?c2) (dislikes ?c2 ?c1)
         [?c1 :name ?n1] [?c2 :name ?n2]]
       db rules))

(defn find-secret-crushes [db]
  (d/q '[:find ?n1 ?n2
         :where [?crush :secret-crush? true] [?crush :src ?c1] [?crush :dst ?c2]
         [?c1 :name ?n1] [?c2 :name ?n2]]
       db))

(defn find-jealousies [db]
  "1 is jealous of 2, because 1 has a crush on 3, and 3 likes 2"
  (d/q '[:find ?n1 ?n2 ?n3 :in $ %
         :where (jealous-of ?c1 ?c2 ?c3)
         [?c1 :name ?n1] [?c2 :name ?n2] [?c3 :name ?n3]]
       db rules))

(defn find-chars-by-value [db value]
  (d/q '[:find ?n :in $ ?v :where [?c :values ?v] [?c :name ?n]] db value))

(defn find-common-values [db]
  (d/q '[:find ?n1 ?n2 ?v
         :where [?c1 :values ?v] [?c2 :values ?v]
         [(not= ?c1 ?c2)]
         [?c1 :name ?n1] [?c2 :name ?n2]]
       db))

;; TODO doesn't work
(defn find-history [db id]
  (d/q '[:find ?a :in $ % ?c
         :where (directly-involves ?a ?c)]
       db rules (->id db id)))

;; TODO doesn't work
(defn find-common-history [db id1 id2]
  (d/q '[:find ?a :in $ % ?c1 ?c2
         :where (directly-involves-both ?a ?c1 ?c2)]
       db rules (->id db id1) (->id db id2)))

;;; action generation

(def all-actions
  [;; generic social moves

   {:type :action
    :name :insult
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where (motive-to-harm ?c1 ?c2)
             [?c1 :name ?n1] [?c2 :name ?n2]]
    :harms [2]
    :harms-value [:communalism]
    :significance :lowest}

   {:type :action
    :name :accuse-of-violating-value
    :query '[:find ?c1 ?n1 ?c2 ?n2 ?v :in $ %
             :where (motive-to-harm ?c1 ?c2)
             [?harm :harms-value ?v] [?harm :perp ?c2]
             [?c1 :name ?n1] [?c2 :name ?n2]]
    :harms [2]
    :harms-value [:communalism]
    :helps-value [4] ; helps the value being "defended"?
    :significance :lowest}

   {:type :action
    :name :accuse-of-hypocrisy
    :query '[:find ?c1 ?n1 ?c2 ?n2 ?v :in $ %
             :where (motive-to-harm ?c1 ?c2)
             [?harm :harms-value ?v] [?harm :perp ?c2] [?c2 :values ?v]
             [?c1 :name ?n1] [?c2 :name ?n2]]
    :harms [2]
    :harms-value [:communalism]
    :helps-value [4] ; helps the value being "defended"?
    :significance :low}

   {:type :action
    :name :ignore
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where (motive-to-harm ?c1 ?c2)
             [?c1 :name ?n1] [?c2 :name ?n2]]
    :harms [2]
    :harms-value [:communalism]
    :significance :lowest}

   {:type :action
    :name :compliment
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where (motive-to-help ?c1 ?c2)
             [?c1 :name ?n1] [?c2 :name ?n2]]
    :helps [2]
    :helps-value [:communalism]
    :significance :lowest}

   {:type :action
    :name :discuss-shared-value
    :query '[:find ?c1 ?n1 ?c2 ?n2 ?v :in $ %
             :where [?c1 :values ?v] [?c2 :values ?v]
             [(not= ?c1 ?c2)]
             [?c1 :name ?n1] [?c2 :name ?n2]]
    :helps [0 2]
    :helps-value [:communalism 4] ; helps the shared value in addition to communalism
    :significance :low}

   {:type :action
    :name :praise-own-value
    :query '[:find ?c1 ?n1 ?c2 ?n2 ?v :in $ %
             :where [?c1 :values ?v]
             [?c2 :type :char] ; need this so we don't sometimes get past actions as ?c2
             [(not= ?c1 ?c2)] ; TODO this isn't actually working
             [?c1 :name ?n1] [?c2 :name ?n2]]
    :helps-value [4]
    :significance :lowest}

   {:type :action
    :name :praise-others-value
    :query '[:find ?c1 ?n1 ?c2 ?n2 ?v :in $ %
             :where [?c2 :values ?v]
             [?c1 :type :char] ; need this so we don't sometimes get past actions as ?c1
             [(not= ?c1 ?c2)] ; TODO this isn't actually working
             [?c1 :name ?n1] [?c2 :name ?n2]]
    :helps-value [:communalism 4]
    :significance :lowest}

   {:type :action
    :name :disparage-others-value
    :query '[:find ?c1 ?n1 ?c2 ?n2 ?v :in $ %
             :where [?c2 :values ?v]
             [?c1 :type :char] ; need this so we don't sometimes get past actions as ?c1
             [(not= ?c1 ?c2)] ; TODO this isn't actually working
             [?c1 :name ?n1] [?c2 :name ?n2]]
    :harms [2]
    :harms-value [:communalism 4]
    :significance :lowest}

   {:type :action
    :name :make-request
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where [?c1 :type :char] [?c2 :type :char]
             [(not= ?c1 ?c2)]
             [?c1 :name ?n1] [?c2 :name ?n2]]
    :significance :low}

   {:type :action
    :name :confide-in
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where [?liking :charge :like] [?liking :src ?c1] [?liking :dst ?c2]
             [?c1 :name ?n1] [?c2 :name ?n2]]
    :helps-value [:communalism]
    :significance :low}

   {:type :action
    :name :notice-presence
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where [?c1 :type :char] [?c2 :type :char]
             [(not= ?c1 ?c2)]
             [?c1 :name ?n1] [?c2 :name ?n2]]
    :significance :lowest}

   {:type :action
    :name :assert-authority
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where [?c1 :role :professor] [?c2 :type :char]
             [(not= ?c1 ?c2)]
             [?c1 :name ?n1] [?c2 :name ?n2]]
    :helps-value [:order]
    :significance :lowest}

   {:type :action
    :name :undermine-authority
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where [?c1 :type :char] [?c2 :role :professor]
             [(not= ?c1 ?c2)]
             [?c1 :name ?n1] [?c2 :name ?n2]]
    :harms [2]
    :harms-value [:order]
    :significance :low}

   ;; movement?

   {:type :action
    :name :flee
    :query '[:find ?c1 ?n1 :in $ %
             :where [?c1 :name ?n1]]
    :significance :lowest}

   ;; actions that require special traits

   {:type :action
    :name :use-secret-expertise
    :query '[:find ?c1 ?n1 :in $ %
             :where [?c1 :trait "secret expert"] [?c1 :name ?n1]]
    :significance :high}

   {:type :action
    :name :confess-to
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where (crush ?c1 ?c2) [?c1 :name ?n1] [?c2 :name ?n2]]
    :significance :high}

   {:type :action
    :name :tweet-about-this
    :query '[:find ?c ?n :in $ %
             :where (or [?c :role :journalist]
                        [?c :trait "celebrity"]
                        [?c :trait "important online"])
             [?c :name ?n]]
    :significance :lowest}

   {:type :action
    :name :do-science
    :query '[:find ?c ?n :in $ %
             :where (scientist ?c) [?c :name ?n]]
    :helps-value [:progress :science]
    :significance :lowest}

   {:type :action
    :name :assist-with-research
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where (scientist ?c1) (scientist ?c2)
             [?c1 :name ?n1] [?c2 :name ?n2]]
    :helps [2]
    :helps-value [:communalism :progress :science]
    :significance :lowest}

   {:type :action
    :name :interfere-with-research
    :query '[:find ?c1 ?n1 ?c2 ?n2 :in $ %
             :where (nonscientist ?c1) (scientist ?c2)
             [?c1 :name ?n1] [?c2 :name ?n2]]
    :harms [2]
    :harms-value [:communalism :progress :science]
    :significance :low}


   ])

(defn action-name->str [action-name]
  (get {:disparage-others-value    "disparages value held by"
        :discuss-shared-value      "discusses a shared value with"
        :praise-others-value       "praises value held by"
        :make-request              "makes a request of"
        :praise-own-value          "praises their own value to"
        :notice-presence           "notices the presence of"
        :ignore                    "is ignoring"
        :insult                    "insults"
        :accuse-of-violating-value "thinks that a value has been violated by"
        :compliment                "compliments"
        :confide-in                "confides in"
        :confess-to                "confesses to"
        :accuse-of-hypocrisy       "accuses the hypocritical"
        :assert-authority          "asserts their authority before"
        :undermine-authority       "undermines the authority of"
        :assist-with-research      "assists with research done by"
        :interfere-with-research   "interferes with research done by"}
       action-name (str action-name)))


(defn describe-trope [trope]
  #_(get {"Absent-Minded Professor"
        "has disheveled hair and non-matching socks"
        ;; "Big Fun"
        ;; "Boss"
        ;; "Bad-to-the-bone"
        ;; "Parent Figure" "Innocent" "Clown" "Nerd" "Tortured Artist"
        ;; "Gentle Giant" "Scruffy and Gruff" "Regal Presence" "Seductive"
        ;; "Dumb Muscle" "Elderly Master" "Know-it-all" "Strict/By-the-books"
        ;; "Rugged" "Lone Wolf" "Athlete" "Eccentric"
        }
         trope trope)

  (str "AKA " "\"" trope "\""))


(defn action->str
  ([action]
   (let [[c1-id c1-name c2-id c2-name & action-vals] (:vars action)]
     (str c1-name " " (action-name->str (:name action)) " " c2-name
          (when (seq action-vals) (str ", the value of " (str/join " " (map name action-vals)))))))
  ([action db]
   (let [[c1-id c1-name c2-id c2-name & action-vals] (:vars action)
         c1                                          (describe-char db c1-id)
         c2                                          (describe-char db c2-id)]
     (str c1-name
          " who " (describe-trait (first (:trait c1)))
          " " (action-name->str (:name action)) " " c2-name
          (when (seq action-vals) (str ", that of " (str/join " " (map name action-vals))))
          (when-let [harms (:harms action)]
            (let [harms-value (:harms-value action)]
              (str ". This move harms " (case harms
                                          [2]   c2-name
                                          [0]   c1-name
                                          [0 2] "them both"
                                          "noone")
                   " and is an abomination to those who value "
                   (str/join " and " (into #{}
                                           (map (fn [v] (cond (keyword? v) (name v)
                                                              (integer? v) (name (first action-vals))))
                                                harms-value))))))
          (when-let [helps (:helps action)]
            (let [helps-value (:helps-value action)]
              (str ". This move helps " (case helps
                                          [2]   c2-name
                                          [0]   c1-name
                                          [0 2] "them both"
                                          "noone")
                   " and is celebrated by those who value "
                   (str/join " and " (into #{}
                                           (map (fn [v] (cond (keyword? v) (name v)
                                                              (integer? v) (name (first action-vals))))
                                                helps-value))))))
          ". " c2-name " " (describe-trait (first (:trait c2))) ". "
          (when-let [trope (:trope c2)]
            (str c2-name
                 " " (describe-trope (first trope))
                 " " (describe-trope (second trope))
                 ". "))
          "\n"

          "[" (describe-didaskalia (rand-nth didaskalia)) "]"
          "\n"
          ))))

(defn print-actions! [actions]
  (println "==== ACTIONS: ====")
  (doseq [action actions]
    (println (action->str action)))
  (println "=================="))

(defn available-actions [db]
  (mapcat (fn [action]
            (let [varsets (d/q (:query action) db rules)]
              (map #(assoc (dissoc action :query) :vars %) varsets)))
          all-actions))

(defn draw-actions
  ([db] (draw-actions db 5))
  ([db limit]
   (let [actions (vec (take limit (shuffle (available-actions db))))]
     (print-actions! actions)
     actions)))

(defn draw-actions-for-char
  ([db char] (draw-actions-for-char db char 5))
  ([db char limit]
   (let [char (->id db char)
         actions
         (->> (available-actions db)
              (filter #(= (first (:vars %)) char))
              (shuffle)
              (take limit)
              (vec))]
     (print-actions! actions)
     actions)))

(defn perform-action! [conn action]
  (let [perp-id       (first (:vars action))
        harmed-ids    (map #(nth (:vars action) %) (:harms action))
        helped-ids    (map #(nth (:vars action) %) (:helps action))
        harmed-values (map #(if (integer? %) (nth (:vars action) %) %) (:harms-value action))
        helped-values (map #(if (integer? %) (nth (:vars action) %) %) (:helps-value action))
        action        (assoc action
                             :perp perp-id
                             :harms harmed-ids
                             :helps helped-ids
                             :harms-value harmed-values
                             :helps-value helped-values)]
    (d/transact! conn [action])))

(defn gen-world! [{:keys [conn]}]
  (let [cast (gen-cast {:size 2})]
    (d/reset-conn! conn (d/empty-db schema))
    (d/transact! conn cast)
    (doseq [char cast :when (map? char)]
      (prn char))))

(defn gen-world-neil-breen! [{:keys [conn]}]
  (let [cast (gen-cast-neil-breen {:size 5})]
    (d/reset-conn! conn (d/empty-db schema))
    (d/transact! conn cast)
    (doseq [char cast :when (map? char)]
      (prn char))))


(defn perform-random-action! [conn]
  (let [action (first (draw-actions @conn 1))]
    (perform-action! conn action)
    (println action)
    action))



(defn -main []
  (gen-world! conn))

(comment

  (gen-world! conn)

  @conn

  (let [action (first (draw-actions @conn 1))]
    (perform-action! conn action)
    action)

  


  )
