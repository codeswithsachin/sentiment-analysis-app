# Presentation notes: Sentiment Analysis Web Application

## 1. What it does (20 seconds)
It scores any text as Positive, Neutral or Negative, using two different methods you can switch between,
and shows everything on a live dashboard. Subject: Social Media Analytics.

## 2. The two engines (1 minute)
**Lexicon (rule based):** every word has a hand-assigned score, e.g. "great" = +3.1, "terrible" = -2.1.
Rules adjust it: "not good" flips the sign, "very good" is stronger, CAPS and "!" add emphasis, and
text after "but" counts more. Scores are summed and normalised to -1..+1. Same idea as VADER.

**Naive Bayes (machine learning):** trained on labelled examples (text + positive/neutral/negative).
It learns, for every word, how often it appears in each class, then for new text multiplies those
probabilities together (with Laplace smoothing) and picks the most likely class. Negation is handled by
tagging the 3 words after "not"/"never" as "not_word", so "not good" is a different feature from "good".

## 3. Live demo script (3 minutes)
1. **Analyze tab:** click the Positive / Negative / Mixed sample buttons, point at the highlighted words.
2. Switch the **Engine** dropdown to Naive Bayes and analyze the same text — compare the results.
3. **Live feed:** click "Auto" and let a few posts stream in.
4. **Batch tab:** analyze the 8 sample lines, then upload a small CSV.
5. **Dashboard:** show the trend chart and top positive/negative words (built from what was just analyzed).
6. **Lexicon tab:** add a slang word, e.g. `lit` = 2.5, then go back and analyze "this party is lit" to show
   the new word working immediately.
7. **Model Lab:** show the current dataset, then click "Run evaluation" and read out the accuracy and
   confusion matrix for both engines.

## 4. Design (30 seconds)
Plain Java, no frameworks: `com.sun.net.httpserver.HttpServer` for the API, hand-written JSON/CSV parsers,
and `<canvas>` for the charts. `AnalysisService` holds all logic; `Server`/`ApiHandlers` only translate
HTTP to calls on it, so the same service could be reused with Spring Boot later without changing the logic.

## 5. Viva questions and answers
**Q: What is VADER and how is your lexicon method similar?** VADER (Valence Aware Dictionary and sEntiment
Reasoner) is a lexicon and rule-based sentiment tool tuned for social media text. Ours follows the same idea:
word scores, negation, booster words, CAPS and punctuation, normalised the same way.

**Q: How does Naive Bayes work here, in one sentence?** It estimates P(class) and P(word | class) from
training data, then for new text picks the class that maximises P(class) x product of P(word | class),
using logs to avoid tiny numbers.

**Q: Why Laplace (add-one) smoothing?** So a word never seen in a class doesn't force its probability to zero,
which would make the whole product zero regardless of other words.

**Q: How do you handle negation in Naive Bayes?** Words right after a negation word are prefixed "not_", so
"not_good" is learned as a separate feature from "good", instead of being ignored.

**Q: How do you measure how good the model is?** Precision, recall and F1 per class, overall accuracy, and a
confusion matrix, computed on a held-out test split that was never used for training.

**Q: What is the confusion matrix telling us?** Rows are the true label, columns are the predicted label; the
diagonal is correct predictions, everything off the diagonal is a mistake, and which cell is largest shows
which two classes get confused most.

**Q: What are the limits of each method?** Lexicon: no learning, only knows words you gave it, cannot
understand sarcasm. Naive Bayes: needs labelled data, assumes words are independent given the class (not
true in real language, but works well in practice), and can be biased by an unbalanced training set.

**Q: How would you improve it further?** More and more varied labelled data, n-grams (word pairs) instead
of single words, or a modern embedding-based model (e.g. a transformer) instead of Naive Bayes.
