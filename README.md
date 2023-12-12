# translator.json
Parses and translates json to clojure data structures

## Usage

Add to deps.edn:

```clojure
{org.fversnel/translator.json {:git/url "https://github.com/fversnel/translator.json"
                               :sha "..."}}
```

```clojure
(require '[clojure.java.io]) ;; optional
(require :reload '[org.fversnel.translator.json :as json])
```

Given the following json string:

```clojure
(def json-string
  "{\"string\": \"value\",
    \"number\": 42,
    \"list\": [42, 43],
    \"map\": {\"inner-string\": \"inner-value\",
              \"inner-list\": [56, 67]}}")

(def json-value
  (json/parse 
    {::json/keywordize-keys? true}
    json-string))
```

translator.json uses a protocol to extend types that can be parsed
as json. 

```clojure
(json/parse json-string)
(json/parse (.getBytes json-string))
(json/parse (clojure.java.io/input-stream (.getBytes json-string)))
(json/parse (java.io.StringReader. json-string))
```

Additionally you can pass a translation directive to the parser
that will help translate your json to the desired clojure data structure:

```clojure
(json/parse
  {::json/translation 
    {"string" ::string
    ;; Just as keys can be translated, values can be as well
    ;; In this case we take the value and increment it
    "number" [::number inc]
    "list" [::list inc]
    ;; to translate the 'outer' key you can optionally specify
    ;; the new name of the key in the map describing the translation
    "map" {::json/translated-key ::map
           "inner-string" [::inner-string (partial str "prefix-")]
           "inner-list" [::inner-list dec]}}}
  json-string)
```

A merge directive can be given to merge an inner map into the outer map:

```clojure
(json/parse
  {::json/translation
    {"string" ::string
    ;; Just as keys can be translated, values can be as well
    ;; In this case we take the value and increment it
    "number" [::number inc]
    "list" [::list inc]
    ;; Sometimes json maps are uselessly nested
    ;; With clojure's namespaced keys we can easily merge maps without conflicts
    ;; With the merge-with-parent? directive we will automatically merge the
    ;; inner map into the outer map
    "map" {::json/merge-with-parent? true
            "inner-string" [::inner-string (partial str "prefix-")]
            "inner-list" [::inner-list dec]}}}
  json-string)
```