(defproject skradanka "0.1.0-SNAPSHOT"
  :repositories [["clojars" {:url "https://clojars.org/repo"
                             :sign-releases false}]]
  :clean-targets ^{:protect false} ["target"]
  :main skradanka.start
  :aot [skradanka.start])
