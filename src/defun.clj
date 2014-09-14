(ns defun
  (:use [clojure.core.match :only (match)]
        [clojure.walk :only [postwalk]]))

(defmacro defun
  [name & fdecl]
  (let [m (if (string? (first fdecl))
            {:doc (first fdecl)}
            {})
        fdecl (if (string? (first fdecl))
                (next fdecl)
                fdecl)
        m (if (map? (first fdecl))
            (conj m (first fdecl))
            m)
        fdecl (if (map? (first fdecl))
                (next fdecl)
                fdecl)
        fdecl (if (vector? (first fdecl))
                (list fdecl)
                fdecl)
        m (if (map? (last fdecl))
            (conj m (last fdecl))
            m)
        fdecl (if (map? (last fdecl))
                (butlast fdecl)
                fdecl)
        m (conj {:arglists (list 'quote '([ & args]))} m)
        m (let [inline (:inline m)
                ifn (first inline)
                iname (second inline)]
            ;; same as: (if (and (= 'fn ifn) (not (symbol? iname))) ...)
            (if (if (clojure.lang.Util/equiv 'fn ifn)
                  (if (instance? clojure.lang.Symbol iname) false true))
              ;; inserts the same fn name to the inline fn if it does not have one
              (assoc m :inline (cons ifn (cons (clojure.lang.Symbol/intern (.concat (.getName ^clojure.lang.Symbol name) "__inliner"))
                                               (next inline))))
              m))
        m (conj (if (meta name) (meta name) {}) m)
        fdecl (postwalk
               (fn [form]
                 (if (and (list? form) (= 'recur (first form)))
                   (list 'recur (cons 'vector (next form)))
                   form)) fdecl)]
    `(defn ~name ~m [ & ~'arguments*]
       (match [(vec ~'arguments*)]
              ~@(mapcat
                 (fn [form]
                   [[(first form)] (cons 'do (next form))])
                 fdecl)))))
